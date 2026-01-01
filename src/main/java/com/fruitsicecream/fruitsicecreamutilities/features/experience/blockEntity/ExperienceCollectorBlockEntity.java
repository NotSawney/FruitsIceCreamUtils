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
 * VERSIÓN OPTIMIZADA del Collector.
 * Maneja redes grandes con intervalos de escaneo adaptativos.
 */
public class ExperienceCollectorBlockEntity extends BlockEntity implements MenuProvider {
    private int storedExperience = 0;
    private int tier;

    // Cache de la red
    private NetworkManager.XPNetwork cachedNetwork = null;
    private int scanCooldown = 0;

    // Intervalos de escaneo adaptativos
    private static final int SCAN_INTERVAL_SMALL = 100;  // 5 segundos - redes pequeñas
    private static final int SCAN_INTERVAL_MEDIUM = 200; // 10 segundos - redes medianas
    private static final int SCAN_INTERVAL_LARGE = 400;  // 20 segundos - redes grandes

    // Estadísticas para UI
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
        // Si el BE ya está marcado para removerse, no se hace nada
        if (level.isClientSide || blockEntity.isRemoved()) return;

        blockEntity.scanCooldown--;
        if (blockEntity.scanCooldown <= 0) {
            // SEGURIDAD: Solo escanear si el chunk central está validado
            if (level.hasChunkAt(pos)) {
                blockEntity.scanNetwork();
                blockEntity.scanCooldown = blockEntity.getScanInterval();
            }
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
     * Determina el intervalo de escaneo según el tamaño de la red.
     */
    private int getScanInterval() {
        if (cachedNetwork == null) {
            return SCAN_INTERVAL_SMALL;
        }

        int networkSize = cachedNetwork.networkSize;

        // Si la red alcanzó el límite de escaneo, usar intervalo más largo
        if (cachedNetwork.hitScanLimit) {
            if (ModConfig.GENERAL.enableDebugLogging.get()) {
                System.out.println("[Collector] Large network detected at " + worldPosition +
                        ", using extended scan interval");
            }
            return SCAN_INTERVAL_LARGE;
        }

        // Intervalos adaptativos según tamaño
        if (networkSize > 300) {
            return SCAN_INTERVAL_LARGE;
        } else if (networkSize > 100) {
            return SCAN_INTERVAL_MEDIUM;
        } else {
            return SCAN_INTERVAL_SMALL;
        }
    }

    /**
     * Escanea la red desde este Collector.
     */
    private void scanNetwork() {
        if (level == null) return;

        try {
            long startTime = System.nanoTime();

            cachedNetwork = NetworkManager.scanFromCollector(level, worldPosition, tier);

            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;

            // Log si el escaneo tardó mucho
            if (durationMs > 50 && ModConfig.GENERAL.enableDebugLogging.get()) {
                System.out.println("[Collector] Network scan at " + worldPosition +
                        " took " + durationMs + "ms (size: " + cachedNetwork.networkSize +
                        ", cores: " + cachedNetwork.cores.size() + ")");
            }

            // Advertencia si alcanzó el límite
            if (cachedNetwork.hitScanLimit && ModConfig.GENERAL.enableDebugLogging.get()) {
                System.out.println("[Collector] WARNING: Network at " + worldPosition +
                        " hit scan limit! Consider reducing network size.");
            }

            updateNetworkStats();
            setChanged();

        } catch (Exception e) {
            // Si falla, invalidar y esperar más tiempo
            if (ModConfig.GENERAL.enableDebugLogging.get()) {
                System.err.println("[Collector] Network scan failed at " + worldPosition + ": " + e.getMessage());
                e.printStackTrace();
            }
            cachedNetwork = null;
            scanCooldown = SCAN_INTERVAL_LARGE;
        }
    }

    /**
     * Actualiza las estadísticas para la UI.
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
     * Transfiere XP desde los cores conectados.
     */
    private void transferXPFromCores() {
        if (level == null || cachedNetwork == null || !cachedNetwork.valid) return;
        if (isFull()) return;

        int spaceAvailable = getMaxCapacity() - storedExperience;
        if (spaceAvailable <= 0) return;

        int flowRate = getFlowRatePerTick();
        int xpToTransfer = Math.min(spaceAvailable, flowRate);

        if (xpToTransfer <= 0) return;

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
     * Extrae XP de los cores conectados.
     */
    private int extractXPFromCores(int maxAmount) {
        int remaining = maxAmount;

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
            return Integer.MAX_VALUE;
        }
        return ModConfig.PIPES.getFlowRate(tier);
    }

    /**
     * Invalida la red y fuerza re-escaneo inmediato.
     */
    public void invalidateNetwork() {
        cachedNetwork = null;
        scanCooldown = 0;
    }

    // ========================================
    // ALMACENAMIENTO
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
    // ESTADÍSTICAS (Para UI)
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
    // NBT
    // ========================================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("StoredExperience", storedExperience);
        tag.putInt("Tier", tier);
        tag.putInt("ScanCooldown", scanCooldown);
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