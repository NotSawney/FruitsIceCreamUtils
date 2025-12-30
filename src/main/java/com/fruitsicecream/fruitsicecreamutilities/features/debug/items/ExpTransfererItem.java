package com.fruitsicecream.fruitsicecreamutilities.features.debug.items;

import com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity.ExperienceCoreBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ExpTransfererItem extends Item {

    public ExpTransfererItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (player == null) {
            return InteractionResult.PASS;
        }

        BlockEntity be = level.getBlockEntity(context.getClickedPos());

        if (be instanceof ExperienceCoreBlockEntity coreEntity) {
            if (!level.isClientSide) {
                // Por ahora solo mostramos info de debug sobre el push system
                player.sendSystemMessage(Component.literal("=== Transfer Debug Info ===")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

                player.sendSystemMessage(Component.literal(""));

                player.sendSystemMessage(Component.literal("Push System Status:")
                        .withStyle(ChatFormatting.AQUA));

                player.sendSystemMessage(Component.literal("  Push Interval: " +
                                coreEntity.getPushInterval() + " ticks")
                        .withStyle(ChatFormatting.WHITE));

                player.sendSystemMessage(Component.literal("  XP per Push: " +
                                coreEntity.getXpPerPush() + " XP")
                        .withStyle(ChatFormatting.WHITE));

                player.sendSystemMessage(Component.literal("  Can Push: " +
                                (coreEntity.getStoredExperience() >= coreEntity.getXpPerPush() ? "YES" : "NO"))
                        .withStyle(coreEntity.getStoredExperience() >= coreEntity.getXpPerPush() ?
                                ChatFormatting.GREEN : ChatFormatting.RED));

                player.sendSystemMessage(Component.literal(""));

                // WIP notice
                player.sendSystemMessage(Component.literal("Transfer System: WIP")
                        .withStyle(ChatFormatting.GOLD));
                player.sendSystemMessage(Component.literal("Waiting for Collector implementation...")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));

                player.sendSystemMessage(Component.literal(""));
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}