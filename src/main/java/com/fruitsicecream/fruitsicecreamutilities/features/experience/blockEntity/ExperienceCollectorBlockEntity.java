package com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity;

import com.fruitsicecream.fruitsicecreamutilities.core.config.ModConfig;
import com.fruitsicecream.fruitsicecreamutilities.core.init.ModBlockEntities;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.ExperienceCollectorBlock;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.menu.ExperienceCollectorMenu;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.network.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * NUEVA VERSIÓN: El Collector ahora es el maestro de su red.
 *
 * Responsabilidades:
 * - Mantener cache de su red de tuberías
 * - Escanear periódicamente la red
 * - Transferir XP desde cores conectados
 * - Actualizar UI con estadísticas
 */
public class ExperienceCollectorBlockEntity extends BlockEntity implements MenuProvider {
    private int storedExperience = 0;
    private int tier;

    // Cache de la red
    private NetworkManager.XPNetwork cachedNetwork = null;
    private int scanCooldown = 0;
    private static final int SCAN_INTERVAL = 100; // Escanear cada 5 segundos

    // Estadísticas para UI (se calculan del network)
    private final Map<Integer, Integer> connectedCoresByTier = new HashMap<>();
    private int totalConnectedCores = 0;
    private int totalProductionRate = 0;

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> ExperienceCollectorBlockEntity.this.storedExperience;
                case 1 -> ExperienceCollectorBlockEntity.this.tier;
                case 2 -> connectedCoresByTier.getOrDefault(1, 0);
                case 3 -> connectedCoresByTier.getOrDefault(2, 0);
                case 4 -> connectedCoresByTier.getOrDefault(3, 0);
                case 5 -> connectedCoresByTier.getOrDefault(4, 0);
                case 6 -> connectedCoresByTier.getOrDefault(5, 0);
                case 7 -> totalConnectedCores;
                case 8 -> totalProductionRate;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> ExperienceCollectorBlockEntity.this.storedExperience = value;
                case 1 -> ExperienceCollectorBlockEntity.this.tier = value;
                case 2 -> connectedCoresByTier.put(1, value);
                case 3 -> connectedCoresByTier.put(2, value);
                case 4 -> connectedCoresByTier.put(3, value);
                case 5 -> connectedCoresByTier.put(4, value);
                case 6 -> connectedCoresByTier.put(5, value);
                case 7 -> totalConnectedCores = value;
                case 8 -> totalProductionRate = value;
            }
        }

        @Override
        public int getCount() {
            return 9;
        }
    };

    public ExperienceCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXPERIENCE_COLLECTOR_BE.get(), pos, state);
    }

    public void setTier(int tier) {
        this.tier = tier;
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ExperienceCollectorBlockEntity blockEntity) {
        if (level.isClientSide) return;

        // Escaneo periódico de la red
        blockEntity.scanCooldown--;
        if (blockEntity.scanCooldown <= 0) {
            blockEntity.scanNetwork();
            blockEntity.scanCooldown = SCAN_INTERVAL;
        }

        // Transferir XP desde los cores (cada tick)
        if (blockEntity.cachedNetwork != null && blockEntity.cachedNetwork.valid) {
            blockEntity.transferXPFromCores();
        }

        // Actualizar nivel de luz
        if (ModConfig.GENERAL.enableLightEmission.get()) {
            blockEntity.updateLightLevel();
        }
    }

    /**
     * Escanea la red desde este Collector.
     * Solo se hace periódicamente (cada SCAN_INTERVAL ticks).
     */
    private void scanNetwork() {
        if (level == null) return;

        try {
            cachedNetwork = NetworkManager.scanFromCollector(level, worldPosition, tier);

            // Actualizar estadísticas para UI
            updateNetworkStats();
            setChanged();

        } catch (Exception e) {
            // Si falla el escaneo, invalidar cache y reintentar después
            cachedNetwork = null;
            scanCooldown = SCAN_INTERVAL * 2; // Esperar el doble
        }
    }

    /**
     * Actualiza las estadísticas para la UI basándose en la red actual.
     */
    private void updateNetworkStats() {
        if (cachedNetwork == null || !cachedNetwork.valid) {
            connectedCoresByTier.clear();
            totalConnectedCores = 0;
            totalProductionRate = 0;
            return;
        }

        connectedCoresByTier.clear();
        totalConnectedCores = cachedNetwork.cores.size();
        totalProductionRate = cachedNetwork.getTotalProductionRate(level);

        // Contar cores por tier
        for (NetworkManager.CoreConnection core : cachedNetwork.cores) {
            connectedCoresByTier.put(core.tier,
                    connectedCoresByTier.getOrDefault(core.tier, 0) + 1);
        }
    }

    /**
     * Transfiere XP disponible desde los cores conectados.
     * Esta lógica se ejecuta CADA TICK.
     */
    private void transferXPFromCores() {
        if (level == null || cachedNetwork == null || !cachedNetwork.valid) return;
        if (isFull()) return; // No transferir si estamos llenos

        // Calcular cuánto XP podemos aceptar
        int spaceAvailable = getMaxCapacity() - storedExperience;
        if (spaceAvailable <= 0) return;

        // Obtener tasa de flujo según config
        int flowRate = getFlowRatePerTick();
        int xpToTransfer = Math.min(spaceAvailable, flowRate);

        if (xpToTransfer <= 0) return;

        // Extraer XP de los cores (priorizando por tier)
        int transferred = extractXPFromCores(xpToTransfer);

        if (transferred > 0) {
            storedExperience += transferred;
            setChanged();

            if (ModConfig.GENERAL.enableLightEmission.get()) {
                updateLightLevel();
            }
        }
    }

    /**
     * Extrae XP de los cores conectados, priorizando por tier (mayor primero).
     *
     * @param maxAmount Cantidad máxima a extraer
     * @return Cantidad realmente extraída
     */
    private int extractXPFromCores(int maxAmount) {
        int remaining = maxAmount;

        // Ordenar cores por tier descendente (ya vienen ordenados del NetworkManager)
        for (NetworkManager.CoreConnection coreConnection : cachedNetwork.cores) {
            if (remaining <= 0) break;

            BlockEntity be = level.getBlockEntity(coreConnection.pos);
            if (!(be instanceof ExperienceCoreBlockEntity core)) continue;

            int coreXP = core.getStoredExperience();
            if (coreXP <= 0) continue;

            int toExtract = Math.min(remaining, coreXP);
            core.setStoredExperience(coreXP - toExtract);
            remaining -= toExtract;
        }

        return maxAmount - remaining;
    }

    /**
     * Obtiene la tasa de flujo por tick según config.
     */
    private int getFlowRatePerTick() {
        if (!ModConfig.PIPES.limitedFlowrate.get()) {
            // Flujo ilimitado - transferir todo lo disponible
            return Integer.MAX_VALUE;
        }

        // Flujo limitado - usar la config
        return ModConfig.PIPES.getFlowRate(tier);
    }

    /**
     * Fuerza un re-escaneo inmediato de la red.
     * Llamado cuando cambia la topología (pipes rotas/colocadas).
     */
    public void invalidateNetwork() {
        cachedNetwork = null;
        scanCooldown = 0; // Escanear en el próximo tick
    }

    // ========================================
    // MÉTODOS DE ALMACENAMIENTO
    // ========================================

    private void updateLightLevel() {
        if (level == null || level.isClientSide) return;

        BlockState currentState = getBlockState();
        int currentLightInState = currentState.getValue(ExperienceCollectorBlock.LIGHT_LEVEL);
        int newLight = getLightLevel();

        if (currentLightInState != newLight) {
            BlockState newState = currentState.setValue(ExperienceCollectorBlock.LIGHT_LEVEL, newLight);
            level.setBlock(worldPosition, newState, 3);
        }
    }

    public int getLightLevel() {
        int maxCap = getMaxCapacity();
        if (maxCap == 0) return 0;
        float fillPercentage = (float) storedExperience / maxCap;
        return (int) (fillPercentage * 15);
    }

    public int getMaxCapacity() {
        return ModConfig.COLLECTORS.getMaxCapacity(tier);
    }

    public int getStoredExperience() {
        return storedExperience;
    }

    public void setStoredExperience(int amount) {
        this.storedExperience = Math.min(amount, getMaxCapacity());
        setChanged();

        if (ModConfig.GENERAL.enableLightEmission.get()) {
            updateLightLevel();
        }
    }

    public float getFillPercentage() {
        int maxCap = getMaxCapacity();
        if (maxCap == 0) return 0;
        return (float) storedExperience / maxCap * 100;
    }

    public boolean isFull() {
        return storedExperience >= getMaxCapacity();
    }

    public boolean canAcceptXP(int amount) {
        return storedExperience + amount <= getMaxCapacity();
    }

    public int addExperience(int amount) {
        int spaceAvailable = getMaxCapacity() - storedExperience;
        int amountToAdd = Math.min(amount, spaceAvailable);

        if (amountToAdd > 0) {
            storedExperience += amountToAdd;
            setChanged();

            if (ModConfig.GENERAL.enableLightEmission.get()) {
                updateLightLevel();
            }
        }

        return amountToAdd;
    }

    public int getTier() {
        return tier;
    }

    // ========================================
    // MÉTODOS DE ESTADÍSTICAS (Para UI)
    // ========================================

    public Map<Integer, Integer> getConnectedCoresByTier() {
        return new HashMap<>(connectedCoresByTier);
    }

    public int getTotalConnectedCores() {
        return totalConnectedCores;
    }

    public int getTotalProductionRate() {
        return totalProductionRate;
    }

    public int getAverageProductionPerCore() {
        if (totalConnectedCores == 0) return 0;
        return totalProductionRate / totalConnectedCores;
    }

    // ========================================
    // COLECCIÓN DE XP
    // ========================================

    public void collectExperience(Player player) {
        if (storedExperience <= 0 || level == null || level.isClientSide) return;

        Vec3 spawnPos = new Vec3(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 1.0,
                worldPosition.getZ() + 0.5
        );

        spawnExperienceOrbs(level, spawnPos, storedExperience);
        storedExperience = 0;
        setChanged();

        if (ModConfig.GENERAL.enableLightEmission.get()) {
            updateLightLevel();
        }
    }

    private void spawnExperienceOrbs(Level level, Vec3 pos, int totalXP) {
        while (totalXP > 0) {
            int orbValue = getExperienceOrbValue(totalXP);
            totalXP -= orbValue;

            ExperienceOrb orb = new ExperienceOrb(level, pos.x, pos.y, pos.z, orbValue);
            level.addFreshEntity(orb);
        }
    }

    private int getExperienceOrbValue(int remaining) {
        if (remaining >= 2477) return 2477;
        if (remaining >= 1237) return 1237;
        if (remaining >= 617) return 617;
        if (remaining >= 307) return 307;
        if (remaining >= 149) return 149;
        if (remaining >= 73) return 73;
        if (remaining >= 37) return 37;
        if (remaining >= 17) return 17;
        if (remaining >= 7) return 7;
        if (remaining >= 3) return 3;
        return 1;
    }

    // ========================================
    // MENU PROVIDER
    // ========================================

    @Override
    public Component getDisplayName() {
        String tierName = tier == 1 ? "basic" : "advanced";
        return Component.translatable("container.fruitsicecreamutilities." + tierName + "_exp_collector");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ExperienceCollectorMenu(containerId, playerInventory, this, this.dataAccess);
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    // ========================================
    // NBT PERSISTENCE
    // ========================================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("StoredExperience", storedExperience);
        tag.putInt("Tier", tier);
        tag.putInt("ScanCooldown", scanCooldown);

        // No guardamos la red completa - se recalcula al cargar
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storedExperience = tag.getInt("StoredExperience");
        tier = tag.getInt("Tier");
        scanCooldown = tag.getInt("ScanCooldown");

        // Forzar re-escaneo al cargar
        cachedNetwork = null;
    }
}