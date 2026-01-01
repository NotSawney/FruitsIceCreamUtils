package com.fruitsicecream.fruitsicecreamutilities.features.experience.network;

import com.fruitsicecream.fruitsicecreamutilities.core.config.ModConfig;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity.ExperienceCollectorBlockEntity;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity.ExperienceCoreBlockEntity;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.ExperienceCollectorBlock;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.ExperienceCoreBlock;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.base.BasePipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Sistema centralizado de gestión de redes de tuberías.
 *
 * FILOSOFÍA:
 * - Solo los Collectors mantienen redes activas
 * - Las Pipes son pasivas, solo conectan
 * - Escaneo único por red por tick
 * - Sin notificaciones en cascada
 */
public class NetworkManager {

    private static final int MAX_SCAN_BLOCKS = 1000; // Límite de seguridad

    /**
     * Representa una red de XP completamente escaneada.
     */
    public static class XPNetwork {
        public final BlockPos collectorPos;
        public final int collectorTier;
        public final List<CoreConnection> cores;
        public final int networkSize;
        public final boolean valid;

        public XPNetwork(BlockPos collectorPos, int collectorTier,
                         List<CoreConnection> cores, int networkSize) {
            this.collectorPos = collectorPos;
            this.collectorTier = collectorTier;
            this.cores = cores;
            this.networkSize = networkSize;
            this.valid = !cores.isEmpty();
        }

        public int getTotalStoredXP(Level level) {
            int total = 0;
            for (CoreConnection core : cores) {
                BlockEntity be = level.getBlockEntity(core.pos);
                if (be instanceof ExperienceCoreBlockEntity coreEntity) {
                    total += coreEntity.getStoredExperience();
                }
            }
            return total;
        }

        public int getTotalProductionRate(Level level) {
            int total = 0;
            for (CoreConnection core : cores) {
                BlockEntity be = level.getBlockEntity(core.pos);
                if (be instanceof ExperienceCoreBlockEntity coreEntity) {
                    total += coreEntity.getXpPerHour();
                }
            }
            return total;
        }
    }

    /**
     * Información de un Core conectado.
     */
    public static class CoreConnection {
        public final BlockPos pos;
        public final int tier;
        public final int distance; // Distancia en bloques desde el Collector

        public CoreConnection(BlockPos pos, int tier, int distance) {
            this.pos = pos;
            this.tier = tier;
            this.distance = distance;
        }
    }

    /**
     * Escanea la red completa desde un Collector.
     *
     * @param level El mundo
     * @param collectorPos Posición del Collector
     * @param collectorTier Tier del Collector (1 o 2)
     * @return La red escaneada, o una red inválida si hay error
     */
    public static XPNetwork scanFromCollector(Level level, BlockPos collectorPos, int collectorTier) {
        if (level == null || collectorPos == null) {
            return new XPNetwork(collectorPos, collectorTier, Collections.emptyList(), 0);
        }

        // Estructuras de escaneo
        Set<BlockPos> visitedPipes = new HashSet<>();
        List<CoreConnection> foundCores = new ArrayList<>();
        Queue<ScanNode> queue = new LinkedList<>();

        // Comenzar desde las pipes adyacentes al Collector (arriba y abajo)
        addInitialPipes(level, collectorPos, collectorTier, queue, visitedPipes);

        int blocksScanned = 0;

        // BFS para encontrar todas las pipes y cores conectadas
        while (!queue.isEmpty() && blocksScanned < MAX_SCAN_BLOCKS) {
            ScanNode current = queue.poll();
            if (current == null) break;

            blocksScanned++;

            // Verificar límite de distancia por tier
            int maxDistance = getMaxDistanceForTier(collectorTier);
            if (maxDistance >= 0 && current.distance > maxDistance) {
                continue;
            }

            BlockState state = level.getBlockState(current.pos);
            Block block = state.getBlock();

            // Si es un Core, agregarlo
            if (block instanceof ExperienceCoreBlock coreBlock) {
                int coreTier = coreBlock.getTier();
                if (isCoreTierCompatibleWithCollector(collectorTier, coreTier)) {
                    foundCores.add(new CoreConnection(current.pos, coreTier, current.distance));
                }
                // Los cores no extienden la red
                continue;
            }

            // Si es una Pipe, explorar vecinos
            if (block instanceof BasePipeBlock pipeBlock) {
                int pipeTier = pipeBlock.getTier();

                // Solo procesar si la pipe es compatible con este Collector
                if (!isPipeCompatibleWithCollector(collectorTier, pipeTier)) {
                    continue;
                }

                // Explorar todas las direcciones
                for (Direction dir : Direction.values()) {
                    BlockPos neighborPos = current.pos.relative(dir);

                    // Skip si ya visitamos esta posición
                    if (visitedPipes.contains(neighborPos)) {
                        continue;
                    }

                    // Skip si no está cargado
                    if (!level.isLoaded(neighborPos)) {
                        continue;
                    }

                    BlockState neighborState = level.getBlockState(neighborPos);
                    Block neighborBlock = neighborState.getBlock();

                    // Solo añadir a la cola si es Pipe o Core compatible
                    if (neighborBlock instanceof BasePipeBlock ||
                            (neighborBlock instanceof ExperienceCoreBlock coreBlock &&
                                    isCoreTierCompatibleWithCollector(collectorTier, coreBlock.getTier()))) {

                        visitedPipes.add(neighborPos);
                        queue.add(new ScanNode(neighborPos, current.distance + 1));
                    }
                }
            }
        }

        // Aplicar límite de cores si existe
        List<CoreConnection> finalCores = applyCoreLimits(foundCores, collectorTier);

        int networkSize = visitedPipes.size() + finalCores.size() + 1; // +1 por el Collector

        return new XPNetwork(collectorPos, collectorTier, finalCores, networkSize);
    }

    /**
     * Añade las pipes iniciales adyacentes al Collector a la cola de escaneo.
     */
    private static void addInitialPipes(Level level, BlockPos collectorPos, int collectorTier,
                                        Queue<ScanNode> queue, Set<BlockPos> visited) {
        // Solo buscar arriba y abajo del Collector
        for (Direction dir : new Direction[]{Direction.UP, Direction.DOWN}) {
            BlockPos pipePos = collectorPos.relative(dir);

            if (!level.isLoaded(pipePos)) continue;

            BlockState state = level.getBlockState(pipePos);
            Block block = state.getBlock();

            if (block instanceof BasePipeBlock pipeBlock) {
                int pipeTier = pipeBlock.getTier();

                // Verificar compatibilidad de tier
                if (isPipeCompatibleWithCollector(collectorTier, pipeTier)) {
                    visited.add(pipePos);
                    queue.add(new ScanNode(pipePos, 1)); // Distancia 1 desde Collector
                }
            }
        }
    }

    /**
     * Aplica límites de cantidad de cores según tier del Collector.
     */
    private static List<CoreConnection> applyCoreLimits(List<CoreConnection> cores, int collectorTier) {
        int maxCores = getMaxCoresForTier(collectorTier);

        // Si no hay límite, devolver todos
        if (maxCores < 0 || cores.size() <= maxCores) {
            return cores;
        }

        // Priorizar cores por tier (mayor tier primero)
        List<CoreConnection> sorted = new ArrayList<>(cores);
        sorted.sort((a, b) -> {
            // Primero por tier descendente
            int tierCompare = Integer.compare(b.tier, a.tier);
            if (tierCompare != 0) return tierCompare;

            // Luego por distancia ascendente (más cercanos primero)
            return Integer.compare(a.distance, b.distance);
        });

        // Tomar solo los primeros maxCores
        return sorted.subList(0, Math.min(maxCores, sorted.size()));
    }

    // ========================================
    // MÉTODOS DE COMPATIBILIDAD
    // ========================================

    private static boolean isPipeCompatibleWithCollector(int collectorTier, int pipeTier) {
        // Basic Collector (1) solo acepta Gold Pipes (1)
        // Advanced Collector (2) solo acepta Diamond Pipes (2)
        return collectorTier == pipeTier;
    }

    private static boolean isCoreTierCompatibleWithCollector(int collectorTier, int coreTier) {
        if (collectorTier == 1) {
            // Basic: MK-I, MK-II, MK-III
            return coreTier >= 1 && coreTier <= 3;
        } else {
            // Advanced: MK-I a MK-V
            return coreTier >= 1 && coreTier <= 5;
        }
    }

    private static int getMaxDistanceForTier(int collectorTier) {
        // Asumiendo que collectorTier 1 = Gold Pipe, 2 = Diamond Pipe
        return ModConfig.PIPES.getMaxDistance(collectorTier);
    }

    private static int getMaxCoresForTier(int collectorTier) {
        return ModConfig.PIPES.getMaxCores(collectorTier);
    }

    // ========================================
    // CLASE AUXILIAR
    // ========================================

    private static class ScanNode {
        final BlockPos pos;
        final int distance;

        ScanNode(BlockPos pos, int distance) {
            this.pos = pos;
            this.distance = distance;
        }
    }
}