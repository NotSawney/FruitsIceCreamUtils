package com.fruitsicecream.fruitsicecreamutilities.features.debug.items;

import com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.ExperienceCoreBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class DebugStickItem extends Item {

    public DebugStickItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (player == null) {
            return InteractionResult.PASS;
        }

        Block block = level.getBlockState(context.getClickedPos()).getBlock();

        // Si clickeamos un Experience Core, mostrar info
        if (block instanceof ExperienceCoreBlock core) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.literal("=== Experience Core Info ===")
                        .withStyle(ChatFormatting.GOLD));
                player.sendSystemMessage(Component.literal("Tier: MK-" + toRoman(core.getTier()))
                        .withStyle(ChatFormatting.YELLOW));
                player.sendSystemMessage(Component.literal("XP/Hour: " + core.getXpPerHour())
                        .withStyle(ChatFormatting.GREEN));
                player.sendSystemMessage(Component.literal("XP/Tick: " +
                                String.format("%.4f", core.getXpPerHour() / 72000.0))
                        .withStyle(ChatFormatting.AQUA));
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private String toRoman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(number);
        };
    }
}