package com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity;

import com.fruitsicecream.fruitsicecreamutilities.core.config.ModConfig;
import com.fruitsicecream.fruitsicecreamutilities.core.init.ModBlockEntities;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.ExperienceCollectorBlock;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.menu.ExperienceCollectorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public class ExperienceCollectorBlockEntity extends BlockEntity implements MenuProvider {
    private int storedExperience = 0;
    private int tier;

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

    // DEPRECATED - Mantener para compatibilidad
    @Deprecated
    public void setTierAndCapacity(int tier, int maxCapacity) {
        this.tier = tier;
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ExperienceCollectorBlockEntity blockEntity) {
        if (level.isClientSide) return;

        // Actualizar nivel de luz si está habilitado
        if (ModConfig.GENERAL.enableLightEmission.get()) {
            blockEntity.updateLightLevel();
        }

        // Escanear cores conectados cada 100 ticks (5 segundos)
        if (level.getGameTime() % 100 == 0) {
            blockEntity.scanConnectedCores();
        }
    }

    private void scanConnectedCores() {
        if (level == null) return;

        connectedCoresByTier.clear();
        totalConnectedCores = 0;
        totalProductionRate = 0;

        scanDirection(Direction.UP);
        scanDirection(Direction.DOWN);

        setChanged();
    }

    private void scanDirection(Direction direction) {
        if (level == null) return;

        BlockPos checkPos = worldPosition.relative(direction);
        BlockEntity be = level.getBlockEntity(checkPos);

        if (be instanceof ExperienceCoreBlockEntity coreEntity) {
            int coreTier = coreEntity.getTier();

            if (isCompatibleCore(coreTier)) {
                connectedCoresByTier.put(coreTier,
                        connectedCoresByTier.getOrDefault(coreTier, 0) + 1);

                totalConnectedCores++;
                totalProductionRate += coreEntity.getXpPerHour();
            }
        }
    }

    private boolean isCompatibleCore(int coreTier) {
        return ModConfig.COLLECTORS.isCoreTierCompatible(tier, coreTier);
    }

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

    // Getter que lee de la config
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

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("StoredExperience", storedExperience);
        tag.putInt("Tier", tier);

        CompoundTag coresTag = new CompoundTag();
        for (Map.Entry<Integer, Integer> entry : connectedCoresByTier.entrySet()) {
            coresTag.putInt("tier_" + entry.getKey(), entry.getValue());
        }
        tag.put("ConnectedCores", coresTag);
        tag.putInt("TotalCores", totalConnectedCores);
        tag.putInt("TotalProduction", totalProductionRate);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storedExperience = tag.getInt("StoredExperience");
        tier = tag.getInt("Tier");

        if (tag.contains("ConnectedCores")) {
            CompoundTag coresTag = tag.getCompound("ConnectedCores");
            connectedCoresByTier.clear();
            for (int i = 1; i <= 5; i++) {
                String key = "tier_" + i;
                if (coresTag.contains(key)) {
                    connectedCoresByTier.put(i, coresTag.getInt(key));
                }
            }
        }
        totalConnectedCores = tag.getInt("TotalCores");
        totalProductionRate = tag.getInt("TotalProduction");
    }
}