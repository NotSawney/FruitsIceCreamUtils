package com.fruitsicecream.fruitsicecreamutilities.features.debug.items;

import com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity.ExperienceCoreBlockEntity;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity.ExperienceCollectorBlockEntity;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity.PipeBlockEntity;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity.PipeBlockEntity.PipeNetwork;
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
            // Pipe
            else if (be instanceof PipeBlockEntity pipeEntity) {
                showPipeNetworkInfo(player, pipeEntity);
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

        player.sendSystemMessage(Component.literal(""));

        // Buscar si está conectado a una red de pipes
        PipeNetwork network = findNetworkForCore(coreEntity);

        if (network != null && network.isValid()) {
            player.sendSystemMessage(Component.literal("Network Status: CONNECTED")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

            player.sendSystemMessage(Component.literal("Connected Cores: " + network.getCoreCount())
                    .withStyle(ChatFormatting.AQUA));

            player.sendSystemMessage(Component.literal("Network Size: " + network.networkSize + " blocks")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            player.sendSystemMessage(Component.literal("Network Status: ISOLATED")
                    .withStyle(ChatFormatting.RED));
            player.sendSystemMessage(Component.literal("Not connected to a pipe network")
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

        player.sendSystemMessage(Component.literal(""));

        // Buscar redes conectadas
        PipeNetwork networkAbove = findNetworkAboveCollector(collectorEntity);
        PipeNetwork networkBelow = findNetworkBelowCollector(collectorEntity);

        int totalCores = 0;
        int totalRate = 0;
        boolean hasNetwork = false;

        if (networkAbove != null && networkAbove.isValid()) {
            totalCores += networkAbove.getCoreCount();
            totalRate += calculateNetworkRate(collectorEntity.getLevel(), networkAbove);
            hasNetwork = true;
        }

        if (networkBelow != null && networkBelow.isValid()) {
            // Si no están interconectadas, contar ambas
            if (networkAbove == null || !networkAbove.collector.equals(networkBelow.collector)) {
                totalCores += networkBelow.getCoreCount();
                totalRate += calculateNetworkRate(collectorEntity.getLevel(), networkBelow);
            }
            hasNetwork = true;
        }

        if (hasNetwork) {
            player.sendSystemMessage(Component.literal("Network Status: CONNECTED")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

            player.sendSystemMessage(Component.literal("Total XP Rate: " + formatNumber(totalRate) + " XP/h")
                    .withStyle(ChatFormatting.GREEN));

            player.sendSystemMessage(Component.literal("Connected Cores: " + totalCores)
                    .withStyle(ChatFormatting.AQUA));

            // Límite de cores
            int maxCores = getCollectorMaxCores(tier);
            if (maxCores > 0) {
                int remaining = maxCores - totalCores;
                if (remaining > 0) {
                    player.sendSystemMessage(Component.literal("Capacity: " + remaining + " slots remaining")
                            .withStyle(ChatFormatting.YELLOW));
                } else {
                    player.sendSystemMessage(Component.literal("Capacity: AT MAXIMUM")
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                }
            } else {
                player.sendSystemMessage(Component.literal("Capacity: UNLIMITED")
                        .withStyle(ChatFormatting.GOLD));
            }
        } else {
            player.sendSystemMessage(Component.literal("Network Status: NO PIPES")
                    .withStyle(ChatFormatting.RED));
            player.sendSystemMessage(Component.literal("No pipe networks connected")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }

        player.sendSystemMessage(Component.literal(""));
    }

    private void showPipeNetworkInfo(Player player, PipeBlockEntity pipeEntity) {
        player.sendSystemMessage(Component.literal("=== Pipe Network Info ===")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

        player.sendSystemMessage(Component.literal(""));

        PipeNetwork network = pipeEntity.getCachedNetwork();

        if (network != null && network.isValid()) {
            player.sendSystemMessage(Component.literal("Network Status: ACTIVE")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

            player.sendSystemMessage(Component.literal("Connected Cores: " + network.getCoreCount())
                    .withStyle(ChatFormatting.AQUA));

            player.sendSystemMessage(Component.literal("Network Size: " + network.networkSize + " blocks")
                    .withStyle(ChatFormatting.GRAY));

            int totalRate = calculateNetworkRate(pipeEntity.getLevel(), network);
            player.sendSystemMessage(Component.literal("Total Production: " + formatNumber(totalRate) + " XP/h")
                    .withStyle(ChatFormatting.GREEN));
        } else {
            player.sendSystemMessage(Component.literal("Network Status: INVALID")
                    .withStyle(ChatFormatting.RED));
            player.sendSystemMessage(Component.literal("Missing Collector or Cores")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }

        player.sendSystemMessage(Component.literal(""));
    }

    private PipeNetwork findNetworkForCore(ExperienceCoreBlockEntity coreEntity) {
        Level level = coreEntity.getLevel();
        if (level == null) return null;

        // Buscar pipes adyacentes
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            net.minecraft.core.BlockPos neighborPos = coreEntity.getBlockPos().relative(dir);
            if (level.getBlockEntity(neighborPos) instanceof PipeBlockEntity pipe) {
                PipeNetwork network = pipe.getCachedNetwork();
                if (network != null && network.connectedCores.contains(coreEntity.getBlockPos())) {
                    return network;
                }
            }
        }

        return null;
    }

    private PipeNetwork findNetworkAboveCollector(ExperienceCollectorBlockEntity collector) {
        Level level = collector.getLevel();
        if (level == null) return null;

        net.minecraft.core.BlockPos abovePos = collector.getBlockPos().above();
        if (level.getBlockEntity(abovePos) instanceof PipeBlockEntity pipe) {
            return pipe.getCachedNetwork();
        }

        return null;
    }

    private PipeNetwork findNetworkBelowCollector(ExperienceCollectorBlockEntity collector) {
        Level level = collector.getLevel();
        if (level == null) return null;

        net.minecraft.core.BlockPos belowPos = collector.getBlockPos().below();
        if (level.getBlockEntity(belowPos) instanceof PipeBlockEntity pipe) {
            return pipe.getCachedNetwork();
        }

        return null;
    }

    private int calculateNetworkRate(Level level, PipeNetwork network) {
        if (level == null || network == null) return 0;

        int totalRate = 0;
        for (net.minecraft.core.BlockPos corePos : network.connectedCores) {
            if (level.getBlockEntity(corePos) instanceof ExperienceCoreBlockEntity core) {
                totalRate += core.getXpPerHour();
            }
        }

        return totalRate;
    }

    private int getCollectorMaxCores(int tier) {
        // Obtener del config (esto depende de tu configuración de collectors)
        // Por ahora usamos valores hardcodeados que coinciden con los defaults
        return tier == 1 ? 10 : -1; // Basic: 10, Advanced: ilimitado
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