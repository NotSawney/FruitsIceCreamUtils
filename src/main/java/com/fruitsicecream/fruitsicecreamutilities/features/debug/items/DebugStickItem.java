package com.fruitsicecream.fruitsicecreamutilities.features.debug.items;

import com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.ExperienceCoreBlock;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity.ExperienceCoreBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

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
        BlockEntity be = level.getBlockEntity(context.getClickedPos());

        if (block instanceof ExperienceCoreBlock core && be instanceof ExperienceCoreBlockEntity coreEntity) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.literal("=== Experience Core Debug Info ===")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

                player.sendSystemMessage(Component.literal(""));

                // Info básica
                player.sendSystemMessage(Component.literal("Tier: MK-" + toRoman(core.getTier()))
                        .withStyle(ChatFormatting.YELLOW));

                // Capacidad y llenado
                int stored = coreEntity.getStoredExperience();
                int max = coreEntity.getMaxCapacity();
                float percent = coreEntity.getFillPercentage();
                ChatFormatting capacityColor = percent >= 100.0f ? ChatFormatting.RED :
                        percent >= 75.0f ? ChatFormatting.GOLD : ChatFormatting.GREEN;

                player.sendSystemMessage(Component.literal(String.format("Capacity: %d / %d XP (%.1f%%)",
                                stored, max, percent))
                        .withStyle(capacityColor));

                if (coreEntity.isFull()) {
                    player.sendSystemMessage(Component.literal("⚠ FULL - Not generating!")
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                }

                player.sendSystemMessage(Component.literal(""));

                // Rates de producción
                player.sendSystemMessage(Component.literal("Production:").withStyle(ChatFormatting.AQUA));
                player.sendSystemMessage(Component.literal("  XP/Hour: " + core.getXpPerHour())
                        .withStyle(ChatFormatting.WHITE));
                player.sendSystemMessage(Component.literal("  XP/Tick: " +
                                String.format("%.4f", core.getXpPerHour() / 72000.0))
                        .withStyle(ChatFormatting.GRAY));

                player.sendSystemMessage(Component.literal(""));

                // Push mechanics
                player.sendSystemMessage(Component.literal("Push Mechanics:").withStyle(ChatFormatting.LIGHT_PURPLE));
                player.sendSystemMessage(Component.literal("  Push Interval: " +
                                coreEntity.getPushInterval() + " ticks (" +
                                String.format("%.2f", coreEntity.getPushInterval() / 20.0) + "s)")
                        .withStyle(ChatFormatting.WHITE));
                player.sendSystemMessage(Component.literal("  XP per Push: " +
                                coreEntity.getXpPerPush() + " XP")
                        .withStyle(ChatFormatting.WHITE));
                player.sendSystemMessage(Component.literal("  Pushes/Hour: " +
                                (72000 / coreEntity.getPushInterval()))
                        .withStyle(ChatFormatting.GRAY));

                player.sendSystemMessage(Component.literal(""));

                // Nivel de luz actual
                int lightLevel = coreEntity.getLightLevel();
                player.sendSystemMessage(Component.literal("Light Level: " + lightLevel + " / 15")
                        .withStyle(lightLevel > 0 ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY));

                player.sendSystemMessage(Component.literal(""));
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