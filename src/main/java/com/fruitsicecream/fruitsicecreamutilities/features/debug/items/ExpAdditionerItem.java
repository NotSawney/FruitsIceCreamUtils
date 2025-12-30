package com.fruitsicecream.fruitsicecreamutilities.features.debug.items;

import com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity.ExperienceCoreBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ExpAdditionerItem extends Item {
    private static final int[] XP_VALUES = {10, 100, 1000};
    private static final String NBT_XP_MODE = "XpMode";

    public ExpAdditionerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            // Shift + Right Click en el aire = cambiar modo
            if (!level.isClientSide) {
                int currentMode = getCurrentMode(stack);
                int nextMode = (currentMode + 1) % XP_VALUES.length;
                setMode(stack, nextMode);

                player.sendSystemMessage(Component.literal("XP Addition Mode: ")
                        .withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(XP_VALUES[nextMode] + " XP")
                                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)));
            }
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) {
            return InteractionResult.PASS;
        }

        BlockEntity be = level.getBlockEntity(context.getClickedPos());

        if (be instanceof ExperienceCoreBlockEntity coreEntity) {
            if (!level.isClientSide && player.isShiftKeyDown()) {
                int currentMode = getCurrentMode(stack);
                int xpToAdd = XP_VALUES[currentMode];

                int beforeXP = coreEntity.getStoredExperience();
                int maxCap = coreEntity.getMaxCapacity();

                // Añadir XP (respetando el límite máximo)
                int newXP = Math.min(beforeXP + xpToAdd, maxCap);
                int actualAdded = newXP - beforeXP;

                coreEntity.setStoredExperience(newXP);

                // Mensaje de feedback
                player.sendSystemMessage(Component.literal("Added ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.literal(actualAdded + " XP")
                                .withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(" to Experience Core")
                                .withStyle(ChatFormatting.GREEN)));

                if (actualAdded < xpToAdd) {
                    player.sendSystemMessage(Component.literal("Core reached maximum capacity!")
                            .withStyle(ChatFormatting.GOLD));
                }

                // Info del estado actual
                float fillPercent = coreEntity.getFillPercentage();
                ChatFormatting color = fillPercent >= 100.0f ? ChatFormatting.RED :
                        fillPercent >= 75.0f ? ChatFormatting.GOLD : ChatFormatting.GREEN;

                player.sendSystemMessage(Component.literal(String.format("Current: %d / %d XP (%.1f%%)",
                                newXP, maxCap, fillPercent))
                        .withStyle(color));

                player.sendSystemMessage(Component.literal("Light Level: " + coreEntity.getLightLevel() + " / 15")
                        .withStyle(ChatFormatting.YELLOW));
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private int getCurrentMode(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.getInt(NBT_XP_MODE);
    }

    private void setMode(ItemStack stack, int mode) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(NBT_XP_MODE, mode);
    }

    public static int getXpValue(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        int mode = tag.getInt(NBT_XP_MODE);
        return XP_VALUES[mode];
    }
}