package com.fruitsicecream.fruitsicecreamutilities.features.debug.items;

import com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity.ExperienceCollectorBlockEntity;
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

import java.util.Map;

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

        // Verificar si es un Core o un Collector
        if (be instanceof ExperienceCoreBlockEntity coreEntity) {
            if (!level.isClientSide && player.isShiftKeyDown()) {
                addXpToBlock(player, stack, coreEntity.getStoredExperience(),
                        coreEntity.getMaxCapacity(), () -> {
                            int currentMode = getCurrentMode(stack);
                            int xpToAdd = XP_VALUES[currentMode];
                            int beforeXP = coreEntity.getStoredExperience();
                            int newXP = Math.min(beforeXP + xpToAdd, coreEntity.getMaxCapacity());
                            coreEntity.setStoredExperience(newXP);
                            return newXP - beforeXP;
                        }, coreEntity::getFillPercentage, coreEntity::getLightLevel);
            }
            return InteractionResult.SUCCESS;
        }
        else if (be instanceof ExperienceCollectorBlockEntity collectorEntity) {
            if (!level.isClientSide) {
                if (player.isShiftKeyDown()) {
                    // Shift + Click = Añadir XP
                    addXpToCollector(player, stack, collectorEntity);
                } else {
                    // Click normal = Mostrar info de cores conectados
                    showCollectorInfo(player, collectorEntity);
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private void addXpToCollector(Player player, ItemStack stack, ExperienceCollectorBlockEntity collectorEntity) {
        int currentMode = getCurrentMode(stack);
        int xpToAdd = XP_VALUES[currentMode];
        int beforeXP = collectorEntity.getStoredExperience();

        int actualAdded = collectorEntity.addExperience(xpToAdd);
        int newXP = beforeXP + actualAdded;

        // Mensaje de feedback
        player.sendSystemMessage(Component.literal("Added ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(actualAdded + " XP")
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" to Collector")
                        .withStyle(ChatFormatting.GREEN)));

        if (actualAdded < xpToAdd) {
            player.sendSystemMessage(Component.literal("Collector reached maximum capacity!")
                    .withStyle(ChatFormatting.GOLD));
        }

        // Info del estado actual
        float fillPercent = collectorEntity.getFillPercentage();
        ChatFormatting color = fillPercent >= 100.0f ? ChatFormatting.RED :
                fillPercent >= 75.0f ? ChatFormatting.GOLD : ChatFormatting.GREEN;

        player.sendSystemMessage(Component.literal(String.format("Current: %s / %s XP (%.1f%%)",
                        formatNumber(newXP), formatNumber(collectorEntity.getMaxCapacity()), fillPercent))
                .withStyle(color));
    }

    private void showCollectorInfo(Player player, ExperienceCollectorBlockEntity collectorEntity) {
        player.sendSystemMessage(Component.literal("=== Collector Quick Info ===")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));

        player.sendSystemMessage(Component.literal(""));

        // XP Storage
        int stored = collectorEntity.getStoredExperience();
        int max = collectorEntity.getMaxCapacity();
        float percent = collectorEntity.getFillPercentage();

        player.sendSystemMessage(Component.literal(String.format("Storage: %s / %s (%.1f%%)",
                        formatNumber(stored), formatNumber(max), percent))
                .withStyle(ChatFormatting.YELLOW));

        // Connected Cores Summary
        Map<Integer, Integer> coresByTier = collectorEntity.getConnectedCoresByTier();
        if (coresByTier.isEmpty()) {
            player.sendSystemMessage(Component.literal("No cores connected")
                    .withStyle(ChatFormatting.RED));
        } else {
            StringBuilder coresText = new StringBuilder("Cores: ");
            boolean first = true;
            for (int tier = 1; tier <= 5; tier++) {
                int count = coresByTier.getOrDefault(tier, 0);
                if (count > 0) {
                    if (!first) coresText.append(", ");
                    coresText.append(String.format("MK-%s x%d", toRoman(tier), count));
                    first = false;
                }
            }
            player.sendSystemMessage(Component.literal(coresText.toString())
                    .withStyle(ChatFormatting.GREEN));

            player.sendSystemMessage(Component.literal("Total Production: " +
                            formatNumber(collectorEntity.getTotalProductionRate()) + " XP/h")
                    .withStyle(ChatFormatting.GOLD));
        }

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("Shift + Click to add XP")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    private void addXpToBlock(Player player, ItemStack stack, int beforeXP, int maxCap,
                              XpAdder adder, FillProvider fillProvider, LightProvider lightProvider) {
        int currentMode = getCurrentMode(stack);
        int xpToAdd = XP_VALUES[currentMode];

        int actualAdded = adder.add();
        int newXP = beforeXP + actualAdded;

        // Mensaje de feedback
        player.sendSystemMessage(Component.literal("Added ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(actualAdded + " XP")
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" to Experience Block")
                        .withStyle(ChatFormatting.GREEN)));

        if (actualAdded < xpToAdd) {
            player.sendSystemMessage(Component.literal("Block reached maximum capacity!")
                    .withStyle(ChatFormatting.GOLD));
        }

        // Info del estado actual
        float fillPercent = fillProvider.getFill();
        ChatFormatting color = fillPercent >= 100.0f ? ChatFormatting.RED :
                fillPercent >= 75.0f ? ChatFormatting.GOLD : ChatFormatting.GREEN;

        player.sendSystemMessage(Component.literal(String.format("Current: %d / %d XP (%.1f%%)",
                        newXP, maxCap, fillPercent))
                .withStyle(color));

        player.sendSystemMessage(Component.literal("Light Level: " + lightProvider.getLight() + " / 15")
                .withStyle(ChatFormatting.YELLOW));
    }

    private int getCurrentMode(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.getInt(NBT_XP_MODE);
    }

    private void setMode(ItemStack stack, int mode) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(NBT_XP_MODE, mode);
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

    private String formatNumber(int number) {
        if (number >= 1000000) {
            return String.format("%.1fM", number / 1000000.0);
        } else if (number >= 1000) {
            return String.format("%.1fK", number / 1000.0);
        }
        return String.valueOf(number);
    }

    public static int getXpValue(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        int mode = tag.getInt(NBT_XP_MODE);
        return XP_VALUES[mode];
    }

    @FunctionalInterface
    private interface XpAdder {
        int add();
    }

    @FunctionalInterface
    private interface FillProvider {
        float getFill();
    }

    @FunctionalInterface
    private interface LightProvider {
        int getLight();
    }
}