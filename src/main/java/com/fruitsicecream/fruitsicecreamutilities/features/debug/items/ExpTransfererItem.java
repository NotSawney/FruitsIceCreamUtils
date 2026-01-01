package com.fruitsicecream.fruitsicecreamutilities.features.debug.items;

import com.fruitsicecream.fruitsicecreamutilities.core.config.ModConfig;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity.ExperienceCoreBlockEntity;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity.ExperienceCollectorBlockEntity;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.network.NetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * ACTUALIZADO para Sistema de Redes v2
 *
 * Ahora usa NetworkManager y ExperienceCollectorBlockEntity para obtener info de red.
 * Ya no depende de PipeBlockEntity.PipeNetwork.
 */
public class ExpTransfererItem extends Item {

    public ExpTransfererItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        BlockEntity be = level.getBlockEntity(context.getClickedPos());

        if (!level.isClientSide) {
            // Experience Core
            if (be instanceof ExperienceCoreBlockEntity coreEntity) {
                showCoreNetworkInfo(player, coreEntity);
                return InteractionResult.SUCCESS;
            }
            // Experience Collector
            else if (be instanceof ExperienceCollectorBlockEntity collectorEntity) {
                showCollectorNetworkInfo(player, collectorEntity);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    private void showCoreNetworkInfo(Player player, ExperienceCoreBlockEntity coreEntity) {
        player.sendSystemMessage(Component.literal("=== Experience Core Network Info ===")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        player.sendSystemMessage(Component.literal(""));

        // Info básica del core
        int tier = coreEntity.getTier();
        player.sendSystemMessage(Component.literal("Tier: MK-" + toRoman(tier))
                .withStyle(ChatFormatting.YELLOW));

        int xpPerHour = coreEntity.getXpPerHour();
        player.sendSystemMessage(Component.literal("Production: " + formatNumber(xpPerHour) + " XP/h")
                .withStyle(ChatFormatting.GREEN));

        int stored = coreEntity.getStoredExperience();
        int max = coreEntity.getMaxCapacity();
        player.sendSystemMessage(Component.literal("Storage: " + formatNumber(stored) + " / " + formatNumber(max) + " XP")
                .withStyle(ChatFormatting.AQUA));

        player.sendSystemMessage(Component.literal(""));

        // Buscar si está conectado a un Collector
        ExperienceCollectorBlockEntity collector = findConnectedCollector(coreEntity);

        if (collector != null) {
            player.sendSystemMessage(Component.literal("Network Status: CONNECTED")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

            String collectorType = collector.getTier() == 1 ? "Basic" : "Advanced";
            player.sendSystemMessage(Component.literal("Connected to: " + collectorType + " Collector")
                    .withStyle(ChatFormatting.YELLOW));

            player.sendSystemMessage(Component.literal("Total Network Cores: " + collector.getTotalConnectedCores())
                    .withStyle(ChatFormatting.AQUA));
        } else {
            player.sendSystemMessage(Component.literal("Network Status: ISOLATED")
                    .withStyle(ChatFormatting.RED));
            player.sendSystemMessage(Component.literal("Not connected to a Collector")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }

        player.sendSystemMessage(Component.literal(""));
    }

    private void showCollectorNetworkInfo(Player player, ExperienceCollectorBlockEntity collectorEntity) {
        player.sendSystemMessage(Component.literal("=== Experience Collector Network Info ===")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));

        player.sendSystemMessage(Component.literal(""));

        // Info básica
        int tier = collectorEntity.getTier();
        String tierName = tier == 1 ? "Basic" : "Advanced";
        player.sendSystemMessage(Component.literal("Type: " + tierName + " Collector")
                .withStyle(ChatFormatting.YELLOW));

        // Capacidad
        int stored = collectorEntity.getStoredExperience();
        int max = collectorEntity.getMaxCapacity();
        float percent = collectorEntity.getFillPercentage();

        ChatFormatting capacityColor = percent >= 100.0f ? ChatFormatting.RED :
                percent >= 75.0f ? ChatFormatting.GOLD : ChatFormatting.GREEN;

        player.sendSystemMessage(Component.literal(String.format("Storage: %s / %s XP (%.1f%%)",
                        formatNumber(stored), formatNumber(max), percent))
                .withStyle(capacityColor));

        player.sendSystemMessage(Component.literal(""));

        // Información de red
        int totalCores = collectorEntity.getTotalConnectedCores();
        int totalRate = collectorEntity.getTotalProductionRate();

        if (totalCores > 0) {
            player.sendSystemMessage(Component.literal("Network Status: ACTIVE")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

            player.sendSystemMessage(Component.literal("Connected Cores: " + totalCores)
                    .withStyle(ChatFormatting.AQUA));

            // Desglose por tier
            var coresByTier = collectorEntity.getConnectedCoresByTier();
            StringBuilder breakdown = new StringBuilder("  Breakdown: ");
            boolean first = true;
            for (int coreTier = 1; coreTier <= 5; coreTier++) {
                int count = coresByTier.getOrDefault(coreTier, 0);
                if (count > 0) {
                    if (!first) breakdown.append(", ");
                    breakdown.append(String.format("MK-%s x%d", toRoman(coreTier), count));
                    first = false;
                }
            }
            player.sendSystemMessage(Component.literal(breakdown.toString())
                    .withStyle(ChatFormatting.GRAY));

            player.sendSystemMessage(Component.literal("Total Production: " + formatNumber(totalRate) + " XP/h")
                    .withStyle(ChatFormatting.GREEN));

            int avgRate = collectorEntity.getAverageProductionPerCore();
            player.sendSystemMessage(Component.literal("Avg per Core: " + formatNumber(avgRate) + " XP/h")
                    .withStyle(ChatFormatting.YELLOW));

            player.sendSystemMessage(Component.literal(""));

            // Límites de la red
            int maxCores = ModConfig.PIPES.getMaxCores(tier);
            int maxDistance = ModConfig.PIPES.getMaxDistance(tier);

            player.sendSystemMessage(Component.literal("Network Limits:")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));

            if (maxCores > 0) {
                int remaining = maxCores - totalCores;
                if (remaining > 0) {
                    player.sendSystemMessage(Component.literal("  Max Cores: " + totalCores + " / " + maxCores +
                                    " (" + remaining + " slots remaining)")
                            .withStyle(ChatFormatting.YELLOW));
                } else {
                    player.sendSystemMessage(Component.literal("  Max Cores: AT MAXIMUM (" + maxCores + ")")
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                }
            } else {
                player.sendSystemMessage(Component.literal("  Max Cores: UNLIMITED")
                        .withStyle(ChatFormatting.GOLD));
            }

            if (maxDistance > 0) {
                player.sendSystemMessage(Component.literal("  Max Distance: " + maxDistance + " blocks")
                        .withStyle(ChatFormatting.GRAY));
            } else {
                player.sendSystemMessage(Component.literal("  Max Distance: UNLIMITED")
                        .withStyle(ChatFormatting.GOLD));
            }

            // Flow rate
            boolean limitedFlow = ModConfig.PIPES.limitedFlowrate.get();
            if (limitedFlow) {
                int flowRate = ModConfig.PIPES.getFlowRate(tier);
                player.sendSystemMessage(Component.literal("  Flow Rate: " + flowRate + " XP/tick")
                        .withStyle(ChatFormatting.GRAY));
            } else {
                player.sendSystemMessage(Component.literal("  Flow Rate: UNLIMITED")
                        .withStyle(ChatFormatting.GOLD));
            }

        } else {
            player.sendSystemMessage(Component.literal("Network Status: NO CORES")
                    .withStyle(ChatFormatting.RED));
            player.sendSystemMessage(Component.literal("No cores connected to this Collector")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }

        player.sendSystemMessage(Component.literal(""));
    }

    /**
     * Busca un Collector conectado a este Core escaneando la red.
     */
    /**
     * Busca un Collector conectado a este Core escaneando la red.
     */
    private ExperienceCollectorBlockEntity findConnectedCollector(ExperienceCoreBlockEntity coreEntity) {
        Level level = coreEntity.getLevel();
        if (level == null) return null;

        // Buscar Collectors en un radio razonable
        // Usando el máximo posible entre ambos tipos de pipes
        int searchRadius = Math.max(
                ModConfig.PIPES.goldMaxDistance.get() > 0 ? ModConfig.PIPES.goldMaxDistance.get() : 15,
                ModConfig.PIPES.diamondMaxDistance.get() > 0 ? ModConfig.PIPES.diamondMaxDistance.get() : 15
        );

        BlockPos corePos = coreEntity.getBlockPos();

        // Iterar sobre todas las posiciones en el radio de búsqueda
        for (BlockPos pos : BlockPos.betweenClosed(
                corePos.offset(-searchRadius, -searchRadius, -searchRadius),
                corePos.offset(searchRadius, searchRadius, searchRadius))) {

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ExperienceCollectorBlockEntity collector) {
                // Verificar si este collector tiene cores conectados
                // (Si tiene cores, es un colector activo)
                if (collector.getTotalConnectedCores() > 0) {
                    // Verificar si el core podría estar en la red de este collector
                    // comprobando la distancia y compatibilidad de tier
                    double distance = Math.sqrt(corePos.distSqr(pos));
                    int collectorTier = collector.getTier();
                    int coreTier = coreEntity.getTier();

                    // Verificar compatibilidad de tier
                    if (ModConfig.COLLECTORS.isCoreTierCompatible(collectorTier, coreTier)) {
                        // Verificar distancia máxima según el tier del collector
                        int maxDistance = ModConfig.PIPES.getMaxDistance(collectorTier);
                        if (maxDistance < 0 || distance <= maxDistance) {
                            // Este collector es compatible y está en rango
                            return collector;
                        }
                    }
                }
            }
        }

        return null;
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
}