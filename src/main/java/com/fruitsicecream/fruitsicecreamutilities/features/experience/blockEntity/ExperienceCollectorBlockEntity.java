package com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity;

import com.fruitsicecream.fruitsicecreamutilities.core.init.ModBlockEntities;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.ExperienceCollectorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ExperienceCollectorBlockEntity extends BlockEntity {
    private int storedExperience = 0;
    private int tier;
    private int maxCapacity;

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