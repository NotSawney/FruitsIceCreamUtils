package com.fruitsicecream.fruitsicecreamutilities.features.experience.menu;

import com.fruitsicecream.fruitsicecreamutilities.core.init.ModMenuTypes;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity.ExperienceCoreBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ExperienceCoreMenu extends AbstractContainerMenu {
    private final ExperienceCoreBlockEntity blockEntity;
    private final ContainerData data;

    public ExperienceCoreMenu(int containerId, Inventory playerInventory,
                              ExperienceCoreBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.EXPERIENCE_CORE_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        addDataSlots(this.data);
    }

    public ExperienceCoreMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                getBlockEntity(playerInventory, extraData),
                new SimpleContainerData(4)); // Ahora son 4 valores
    }

    private static ExperienceCoreBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(extraData.readBlockPos());
        if (be instanceof ExperienceCoreBlockEntity) {
            return (ExperienceCoreBlockEntity) be;
        }
        throw new IllegalStateException("Block entity is not an ExperienceCoreBlockEntity!");
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

    public ExperienceCoreBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getStoredExperience() {
        return data.get(0);
    }

    public int getTier() {
        return data.get(1);
    }

    public int getXpPerHour() {
        return data.get(2);
    }

    public int getMaxCapacity() {
        return data.get(3);
    }

    public float getFillPercentage() {
        int max = getMaxCapacity();
        if (max == 0) return 0;
        return (float) getStoredExperience() / max * 100;
    }

    public void collectExperience(Player player) {
        if (!player.level().isClientSide) {
            blockEntity.collectExperience(player);
        }
    }
}