package com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity;

import com.fruitsicecream.fruitsicecreamutilities.core.init.ModBlockEntities;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.ExperienceCollectorBlock;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.menu.ExperienceCollectorMenu;
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

public class ExperienceCollectorBlockEntity extends BlockEntity implements MenuProvider {
    private int storedExperience = 0;
    private int tier;
    private int maxCapacity;

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> ExperienceCollectorBlockEntity.this.storedExperience;
                case 1 -> ExperienceCollectorBlockEntity.this.tier;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> ExperienceCollectorBlockEntity.this.storedExperience = value;
                case 1 -> ExperienceCollectorBlockEntity.this.tier = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public ExperienceCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXPERIENCE_COLLECTOR_BE.get(), pos, state);
    }

    public void setTierAndCapacity(int tier, int maxCapacity) {
        this.tier = tier;
        this.maxCapacity = maxCapacity;
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ExperienceCollectorBlockEntity blockEntity) {
        if (level.isClientSide) return;

        // Actualizar nivel de luz cada tick
        blockEntity.updateLightLevel();

        // TODO: Network discovery cada 100 ticks (5 segundos)
        // Buscar cores conectados directamente o via pipelines
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
        if (maxCapacity == 0) return 0;

        float fillPercentage = (float) storedExperience / maxCapacity;
        return (int) (fillPercentage * 15);
    }

    public int getStoredExperience() {
        return storedExperience;
    }

    public void setStoredExperience(int amount) {
        this.storedExperience = Math.min(amount, maxCapacity);
        setChanged();
        updateLightLevel();
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public float getFillPercentage() {
        if (maxCapacity == 0) return 0;
        return (float) storedExperience / maxCapacity * 100;
    }

    public boolean isFull() {
        return storedExperience >= maxCapacity;
    }

    public boolean canAcceptXP(int amount) {
        return storedExperience + amount <= maxCapacity;
    }

    public int addExperience(int amount) {
        int spaceAvailable = maxCapacity - storedExperience;
        int amountToAdd = Math.min(amount, spaceAvailable);

        if (amountToAdd > 0) {
            storedExperience += amountToAdd;
            setChanged();
            updateLightLevel();
        }

        return amountToAdd;
    }

    public int getTier() {
        return tier;
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
        updateLightLevel();
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
        tag.putInt("MaxCapacity", maxCapacity);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storedExperience = tag.getInt("StoredExperience");
        tier = tag.getInt("Tier");
        maxCapacity = tag.getInt("MaxCapacity");
    }
}