package com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity;

import com.fruitsicecream.fruitsicecreamutilities.core.config.ModConfig;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity.PipeBlockEntity.PipeNetwork;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.ExperienceCollectorBlock;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.ExperienceCoreBlock;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.base.BasePipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class PipeNetworkScanner {

    private static final int MAX_SCAN_SIZE = 5000; // Límite de seguridad

    /**
     * Escanea la red de tuberías desde una posición inicial
     * @param level El mundo
     * @param startPos Posición de inicio (una pipe)
     * @param tier Tier de la pipe inicial
     * @return PipeNetwork con información de la red, o null si no es válida
     */
    public static PipeNetwork scanNetwork(Level level, BlockPos startPos, int tier) {
        Set<BlockPos> visitedPipes = new HashSet<>();
        Set<BlockPos> connectedCores = new HashSet<>();
        BlockPos collectorPos = null;

        Queue<ScanNode> queue = new LinkedList<>();
        queue.add(new ScanNode(startPos, 0));
        visitedPipes.add(startPos);

        int maxDistance = ModConfig.PIPES.getMaxDistance(tier);
        int maxCores = ModConfig.PIPES.getMaxCores(tier);

        while (!queue.isEmpty() && visitedPipes.size() < MAX_SCAN_SIZE) {
            ScanNode current = queue.poll();
            BlockPos pos = current.pos;
            int distance = current.distance;

            // Verificar distancia máxima
            if (maxDistance >= 0 && distance > maxDistance) {
                continue;
            }

            // Explorar vecinos
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);
                BlockState neighborState = level.getBlockState(neighborPos);
                Block neighborBlock = neighborState.getBlock();

                // Si es una pipe, continuar explorando
                if (neighborBlock instanceof BasePipeBlock && !visitedPipes.contains(neighborPos)) {
                    visitedPipes.add(neighborPos);
                    queue.add(new ScanNode(neighborPos, distance + 1));
                }
                // Si es un Core, agregarlo a la lista
                else if (neighborBlock instanceof ExperienceCoreBlock core) {
                    if (isCoreTierCompatible(tier, core.getTier())) {
                        connectedCores.add(neighborPos);
                    }
                }
                // Si es un Collector, guardarlo (solo el primero encontrado)
                else if (neighborBlock instanceof ExperienceCollectorBlock collector) {
                    if (collectorPos == null && isCollectorCompatible(tier, collector.getTier())) {
                        collectorPos = neighborPos;
                    }
                }
            }
        }

        // Aplicar límite de cores si existe
        if (maxCores >= 0 && connectedCores.size() > maxCores) {
            connectedCores = prioritizeCoresByTier(level, connectedCores, maxCores);
        }

        int networkSize = visitedPipes.size() + connectedCores.size() + (collectorPos != null ? 1 : 0);

        return new PipeNetwork(connectedCores, collectorPos, networkSize, tier);
    }

    /**
     * Prioriza cores por tier, manteniendo solo los maxCores de mayor tier
     */
    private static Set<BlockPos> prioritizeCoresByTier(Level level, Set<BlockPos> cores, int maxCores) {
        List<CoreWithTier> coreList = new ArrayList<>();

        for (BlockPos corePos : cores) {
            if (level.getBlockEntity(corePos) instanceof ExperienceCoreBlockEntity core) {
                coreList.add(new CoreWithTier(corePos, core.getTier()));
            }
        }

        // Ordenar por tier descendente (mayor tier primero)
        coreList.sort((a, b) -> Integer.compare(b.tier, a.tier));

        // Tomar solo los primeros maxCores
        Set<BlockPos> result = new HashSet<>();
        for (int i = 0; i < Math.min(maxCores, coreList.size()); i++) {
            result.add(coreList.get(i).pos);
        }

        return result;
    }

    private static boolean isCoreTierCompatible(int pipeTier, int coreTier) {
        if (pipeTier == 1) {
            // Gold pipes: MK1-MK3
            return coreTier >= 1 && coreTier <= 3;
        } else {
            // Diamond pipes: MK1-MK5
            return coreTier >= 1 && coreTier <= 5;
        }
    }

    private static boolean isCollectorCompatible(int pipeTier, int collectorTier) {
        if (pipeTier == 1) {
            // Gold pipes: solo Basic Collector
            return collectorTier == 1;
        } else {
            // Diamond pipes: solo Advanced Collector
            return collectorTier == 2;
        }
    }

    private static class ScanNode {
        final BlockPos pos;
        final int distance;

        ScanNode(BlockPos pos, int distance) {
            this.pos = pos;
            this.distance = distance;
        }
    }

    private static class CoreWithTier {
        final BlockPos pos;
        final int tier;

        CoreWithTier(BlockPos pos, int tier) {
            this.pos = pos;
            this.tier = tier;
        }
    }
}