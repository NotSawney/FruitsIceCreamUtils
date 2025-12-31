package com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity;

import com.fruitsicecream.fruitsicecreamutilities.core.config.ModConfig;
import com.fruitsicecream.fruitsicecreamutilities.core.init.ModBlockEntities;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.ExperienceCoreBlock;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.menu.ExperienceCoreMenu;
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

public class ExperienceCoreBlockEntity extends BlockEntity implements MenuProvider {
    private int storedExperience = 0;
    private int tier;
    private int tickCounter = 0;
    private int pushCounter = 0;

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> ExperienceCoreBlockEntity.this.storedExperience;
                case 1 -> ExperienceCoreBlockEntity.this.tier;
                case 2 -> getXpPerHour(); // Obtiene de config
                case 3 -> getMaxCapacity(); // Obtiene de config
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> ExperienceCoreBlockEntity.this.storedExperience = value;
                case 1 -> ExperienceCoreBlockEntity.this.tier = value;
                // Los valores 2 y 3 son read-only (config)
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public ExperienceCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXPERIENCE_CORE_BE.get(), pos, state);
    }

    public void setTier(int tier) {
        this.tier = tier;
        setChanged();
    }

    // DEPRECATED - Mantener para compatibilidad pero ya no usa estos valores
    @Deprecated
    public void setTierAndRate(int tier, int xpPerHour, int maxCapacity) {
        this.tier = tier;
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ExperienceCoreBlockEntity blockEntity) {
        if (level.isClientSide) return;

        // Generación de XP
        blockEntity.tickCounter++;
        int ticksPerGen = ModConfig.GENERAL.ticksPerGeneration.get();

        if (blockEntity.tickCounter >= ticksPerGen) {
            blockEntity.tickCounter = 0;
            blockEntity.generateExperience();
        }

        // Push logic (si hay XP almacenada)
        if (blockEntity.storedExperience > 0) {
            blockEntity.pushCounter++;
            int pushInterval = blockEntity.getPushInterval();

            if (blockEntity.pushCounter >= pushInterval) {
                blockEntity.pushCounter = 0;
                blockEntity.tryPushToCollector();
            }
        }

        // Actualizar luz si está habilitado
        if (ModConfig.GENERAL.enableLightEmission.get()) {
            blockEntity.updateLightLevel();
        }
    }

    private void generateExperience() {
        int maxCap = getMaxCapacity();

        // Solo generar si no estamos a capacidad máxima
        if (storedExperience >= maxCap) {
            return;
        }

        int xpPerHour = getXpPerHour();
        int xpToAdd = Math.max(1, xpPerHour / 360);
        storedExperience = Math.min(storedExperience + xpToAdd, maxCap);
        setChanged();
    }

    private void tryPushToCollector() {
        if (level == null) return;

        ExperienceCollectorBlockEntity collector = findCollectorAboveOrBelow();

        if (collector != null && isCompatibleWithCollector(collector)) {
            int xpToPush = Math.min(storedExperience, getXpPerPush());

            if (xpToPush > 0 && collector.canAcceptXP(xpToPush)) {
                int actuallyAdded = collector.addExperience(xpToPush);
                storedExperience -= actuallyAdded;
                setChanged();
            }
        }
    }

    @Nullable
    private ExperienceCollectorBlockEntity findCollectorAboveOrBelow() {
        if (level == null) return null;

        BlockEntity beAbove = level.getBlockEntity(worldPosition.above());
        if (beAbove instanceof ExperienceCollectorBlockEntity collector) {
            return collector;
        }

        BlockEntity beBelow = level.getBlockEntity(worldPosition.below());
        if (beBelow instanceof ExperienceCollectorBlockEntity collector) {
            return collector;
        }

        return null;
    }

    private boolean isCompatibleWithCollector(ExperienceCollectorBlockEntity collector) {
        return ModConfig.COLLECTORS.isCoreTierCompatible(collector.getTier(), this.tier);
    }

    private void updateLightLevel() {
        if (level == null || level.isClientSide) return;

        BlockState currentState = getBlockState();
        int currentLightInState = currentState.getValue(ExperienceCoreBlock.LIGHT_LEVEL);
        int newLight = getLightLevel();

        if (currentLightInState != newLight) {
            BlockState newState = currentState.setValue(ExperienceCoreBlock.LIGHT_LEVEL, newLight);
            level.setBlock(worldPosition, newState, 3);
        }
    }

    public int getLightLevel() {
        int maxCap = getMaxCapacity();
        if (maxCap == 0) return 0;

        float fillPercentage = (float) storedExperience / maxCap;
        return (int) (fillPercentage * 15);
    }

    // Getters que leen de la config
    public int getXpPerHour() {
        return ModConfig.CORES.getXpPerHour(tier);
    }

    public int getMaxCapacity() {
        return ModConfig.CORES.getMaxCapacity(tier);
    }

    public int getPushInterval() {
        return ModConfig.CORES.getPushInterval(tier);
    }

    public int getXpPerPush() {
        return ModConfig.CORES.getXpPerPush(tier);
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

    public int getTier() {
        return tier;
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("StoredExperience", storedExperience);
        tag.putInt("Tier", tier);
        tag.putInt("TickCounter", tickCounter);
        tag.putInt("PushCounter", pushCounter);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storedExperience = tag.getInt("StoredExperience");
        tier = tag.getInt("Tier");
        tickCounter = tag.getInt("TickCounter");
        pushCounter = tag.getInt("PushCounter");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.fruitsicecreamutilities.exp_core_mk" + tier);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ExperienceCoreMenu(containerId, playerInventory, this, this.dataAccess);
    }
}