package com.fruitsicecream.fruitsicecreamutilities.features.experience.menu;

import com.fruitsicecream.fruitsicecreamutilities.core.init.ModMenuTypes;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity.ExperienceCollectorBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ExperienceCollectorMenu extends AbstractContainerMenu {
    private final ExperienceCollectorBlockEntity blockEntity;
    private final ContainerData data;

    public ExperienceCollectorMenu(int containerId, Inventory playerInventory,
                                   ExperienceCollectorBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.EXPERIENCE_COLLECTOR_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        addDataSlots(this.data);
    }

    public ExperienceCollectorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                getBlockEntity(playerInventory, extraData),
                new SimpleContainerData(9));
    }

    private static ExperienceCollectorBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(extraData.readBlockPos());
        if (be instanceof ExperienceCollectorBlockEntity) {
            return (ExperienceCollectorBlockEntity) be;
        }
        throw new IllegalStateException("Block entity is not an ExperienceCollectorBlockEntity!");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null &&
                blockEntity.getLevel() != null &&
                blockEntity.getLevel().getBlockEntity(blockEntity.getBlockPos()) == blockEntity &&
                player.distanceToSqr(blockEntity.getBlockPos().getX() + 0.5,
                        blockEntity.getBlockPos().getY() + 0.5,
                        blockEntity.getBlockPos().getZ() + 0.5) <= 64;
    }

    public ExperienceCollectorBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getStoredExperience() {
        return data.get(0);
    }

    public int getTier() {
        return data.get(1);
    }

    // Cores conectados por tier
    public int getCoresMKI() {
        return data.get(2);
    }

    public int getCoresMKII() {
        return data.get(3);
    }

    public int getCoresMKIII() {
        return data.get(4);
    }

    public int getCoresMKIV() {
        return data.get(5);
    }

    public int getCoresMKV() {
        return data.get(6);
    }

    public int getTotalConnectedCores() {
        return data.get(7);
    }

    public int getTotalProductionRate() {
        return data.get(8);
    }

    public int getMaxCapacity() {
        return blockEntity.getMaxCapacity();
    }

    public float getFillPercentage() {
        int max = getMaxCapacity();
        if (max == 0) return 0;
        return (float) getStoredExperience() / max * 100;
    }

    public int getAverageProductionPerCore() {
        int total = getTotalConnectedCores();
        if (total == 0) return 0;
        return getTotalProductionRate() / total;
    }
}