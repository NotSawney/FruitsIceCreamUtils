package com.fruitsicecream.fruitsicecreamutilities.features.debug.items;

import com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.ExperienceCollectorBlock;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.ExperienceCoreBlock;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity.ExperienceCollectorBlockEntity;
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

import java.util.Map;

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

        // Experience Core
        if (block instanceof ExperienceCoreBlock core && be instanceof ExperienceCoreBlockEntity coreEntity) {
            if (!level.isClientSide) {
                showCoreDebugInfo(player, core, coreEntity);
            }
            return InteractionResult.SUCCESS;
        }
        // Experience Collector
        else if (block instanceof ExperienceCollectorBlock collector && be instanceof ExperienceCollectorBlockEntity collectorEntity) {
            if (!level.isClientSide) {
                showCollectorDebugInfo(player, collector, collectorEntity);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private void showCoreDebugInfo(Player player, ExperienceCoreBlock core, ExperienceCoreBlockEntity coreEntity) {
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

    private void showCollectorDebugInfo(Player player, ExperienceCollectorBlock collector, ExperienceCollectorBlockEntity collectorEntity) {
        player.sendSystemMessage(Component.literal("=== Experience Collector Debug Info ===")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));

        player.sendSystemMessage(Component.literal(""));

        // Info básica
        String tierName = collector.getTier() == 1 ? "Basic" : "Advanced";
        player.sendSystemMessage(Component.literal("Type: " + tierName)
                .withStyle(ChatFormatting.YELLOW));

        // Capacidad y llenado
        int stored = collectorEntity.getStoredExperience();
        int max = collectorEntity.getMaxCapacity();
        float percent = collectorEntity.getFillPercentage();
        ChatFormatting capacityColor = percent >= 100.0f ? ChatFormatting.RED :
                percent >= 75.0f ? ChatFormatting.GOLD : ChatFormatting.GREEN;

        player.sendSystemMessage(Component.literal(String.format("Capacity: %d / %d XP (%.1f%%)",
                        stored, max, percent))
                .withStyle(capacityColor));

        if (collectorEntity.isFull()) {
            player.sendSystemMessage(Component.literal("⚠ FULL - Cannot accept more XP!")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }

        player.sendSystemMessage(Component.literal(""));

        // Cores conectados
        player.sendSystemMessage(Component.literal("Connected Cores:").withStyle(ChatFormatting.LIGHT_PURPLE));

        Map<Integer, Integer> coresByTier = collectorEntity.getConnectedCoresByTier();
        if (coresByTier.isEmpty()) {
            player.sendSystemMessage(Component.literal("  No cores connected")
                    .withStyle(ChatFormatting.RED));
        } else {
            for (int tier = 1; tier <= 5; tier++) {
                int count = coresByTier.getOrDefault(tier, 0);
                if (count > 0) {
                    player.sendSystemMessage(Component.literal("  MK-" + toRoman(tier) + ": " + count + " core(s)")
                            .withStyle(getTierChatColor(tier)));
                }
            }
        }

        player.sendSystemMessage(Component.literal("  Total: " + collectorEntity.getTotalConnectedCores() + " cores")
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));

        player.sendSystemMessage(Component.literal(""));

        // Estadísticas de producción
        player.sendSystemMessage(Component.literal("Production Statistics:").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal("  Total Rate: " +
                        collectorEntity.getTotalProductionRate() + " XP/h")
                .withStyle(ChatFormatting.GREEN));

        if (collectorEntity.getTotalConnectedCores() > 0) {
            player.sendSystemMessage(Component.literal("  Avg per Core: " +
                            collectorEntity.getAverageProductionPerCore() + " XP/h")
                    .withStyle(ChatFormatting.YELLOW));
        }

        player.sendSystemMessage(Component.literal(""));

        // Nivel de luz actual
        int lightLevel = collectorEntity.getLightLevel();
        player.sendSystemMessage(Component.literal("Light Level: " + lightLevel + " / 15")
                .withStyle(lightLevel > 0 ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY));

        player.sendSystemMessage(Component.literal(""));
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

    private ChatFormatting getTierChatColor(int tier) {
        return switch (tier) {
            case 1 -> ChatFormatting.GRAY;
            case 2 -> ChatFormatting.WHITE;
            case 3 -> ChatFormatting.GOLD;
            case 4 -> ChatFormatting.AQUA;
            case 5 -> ChatFormatting.LIGHT_PURPLE;
            default -> ChatFormatting.WHITE;
        };
    }
}