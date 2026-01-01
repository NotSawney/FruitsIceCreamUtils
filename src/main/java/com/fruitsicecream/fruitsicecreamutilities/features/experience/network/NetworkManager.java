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
 * VERSIÓN OPTIMIZADA con límites de seguridad mejorados.
 */
public class NetworkManager {

    // Límites de seguridad más agresivos
    private static final int MAX_SCAN_BLOCKS_LIMITED = 250;   // Para pipes con distancia limitada
    private static final int MAX_SCAN_BLOCKS_UNLIMITED = 750; // Para pipes ilimitadas
    private static final int MAX_CORES_PER_NETWORK = 100;      // Límite absoluto de cores

    /**
     * Representa una red de XP completamente escaneada.
     */
    public static class XPNetwork {
        public final BlockPos collectorPos;
        public final int collectorTier;
        public final List<CoreConnection> cores;
        public final int networkSize;
        public final boolean valid;
        public final boolean hitScanLimit; // Nuevo: indica si se alcanzó el límite

        public XPNetwork(BlockPos collectorPos, int collectorTier,
                         List<CoreConnection> cores, int networkSize, boolean hitScanLimit) {
            this.collectorPos = collectorPos;
            this.collectorTier = collectorTier;
            this.cores = cores;
            this.networkSize = networkSize;
            this.valid = !cores.isEmpty();
            this.hitScanLimit = hitScanLimit;
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
        public final int distance;

        public CoreConnection(BlockPos pos, int tier, int distance) {
            this.pos = pos;
            this.tier = tier;
            this.distance = distance;
        }
    }

    /**
     * Escanea la red completa desde un Collector.
     * VERSIÓN OPTIMIZADA con límites adaptativos.
     */
    public static XPNetwork scanFromCollector(Level level, BlockPos collectorPos, int collectorTier) {
        if (level == null || collectorPos == null) {
            return new XPNetwork(collectorPos, collectorTier, Collections.emptyList(), 0, false);
        }

        // Determinar límite de escaneo según si la distancia es limitada o no
        int maxDistance = getMaxDistanceForTier(collectorTier);
        int maxScanBlocks = maxDistance < 0 ? MAX_SCAN_BLOCKS_UNLIMITED : MAX_SCAN_BLOCKS_LIMITED;

        // Estructuras de escaneo
        Set<BlockPos> visitedPipes = new HashSet<>();
        List<CoreConnection> foundCores = new ArrayList<>();
        Queue<ScanNode> queue = new LinkedList<>();

        // Comenzar desde las pipes adyacentes al Collector
        addInitialPipes(level, collectorPos, collectorTier, queue, visitedPipes);

        int blocksScanned = 0;
        boolean hitLimit = false;

        // BFS optimizado
        while (!queue.isEmpty() && blocksScanned < maxScanBlocks) {
            ScanNode current = queue.poll();
            if (current == null) break;

            blocksScanned++;

            // Límite de distancia (si aplica)
            if (maxDistance >= 0 && current.distance > maxDistance) {
                continue;
            }

            // Límite absoluto de cores
            if (foundCores.size() >= MAX_CORES_PER_NETWORK) {
                hitLimit = true;
                break;
            }

            // Verificar que el chunk esté cargado
            if (!level.isLoaded(current.pos)) {
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
                continue; // Los cores no extienden la red
            }

            // Si es una Pipe, explorar vecinos
            if (block instanceof BasePipeBlock pipeBlock) {
                int pipeTier = pipeBlock.getTier();

                // Solo procesar si la pipe es compatible
                if (!isPipeCompatibleWithCollector(collectorTier, pipeTier)) {
                    continue;
                }

                // Explorar vecinos (optimizado)
                exploreNeighbors(level, current, collectorTier, queue, visitedPipes);
            }
        }

        // Detectar si alcanzamos el límite
        if (blocksScanned >= maxScanBlocks) {
            hitLimit = true;
            if (ModConfig.GENERAL.enableDebugLogging.get()) {
                System.out.println("[NetworkManager] WARNING: Scan limit reached at " + collectorPos +
                        " (scanned " + blocksScanned + " blocks)");
            }
        }

        // Aplicar límite de cores si existe
        List<CoreConnection> finalCores = applyCoreLimits(foundCores, collectorTier);

        int networkSize = visitedPipes.size() + finalCores.size() + 1;

        return new XPNetwork(collectorPos, collectorTier, finalCores, networkSize, hitLimit);
    }

    /**
     * Explora los vecinos de una pipe de forma optimizada.
     */
    private static void exploreNeighbors(Level level, ScanNode current, int collectorTier,
                                         Queue<ScanNode> queue, Set<BlockPos> visited) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = current.pos.relative(dir);

            // Skip si ya visitamos
            if (visited.contains(neighborPos)) {
                continue;
            }

            // Skip si no está cargado
            if (!level.isLoaded(neighborPos)) {
                continue;
            }

            BlockState neighborState = level.getBlockState(neighborPos);
            Block neighborBlock = neighborState.getBlock();

            // Solo añadir si es Pipe o Core compatible
            boolean shouldAdd = false;

            if (neighborBlock instanceof BasePipeBlock) {
                shouldAdd = true;
            } else if (neighborBlock instanceof ExperienceCoreBlock coreBlock) {
                shouldAdd = isCoreTierCompatibleWithCollector(collectorTier, coreBlock.getTier());
            }

            if (shouldAdd) {
                visited.add(neighborPos);
                queue.add(new ScanNode(neighborPos, current.distance + 1));
            }
        }
    }

    /**
     * Añade las pipes iniciales adyacentes al Collector.
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

                if (isPipeCompatibleWithCollector(collectorTier, pipeTier)) {
                    visited.add(pipePos);
                    queue.add(new ScanNode(pipePos, 1));
                }
            }
        }
    }

    /**
     * Aplica límites de cantidad de cores según tier del Collector.
     */
    private static List<CoreConnection> applyCoreLimits(List<CoreConnection> cores, int collectorTier) {
        int maxCores = getMaxCoresForTier(collectorTier);

        // Si no hay límite o no lo excedemos, devolver todos
        if (maxCores < 0 || cores.size() <= maxCores) {
            return cores;
        }

        // Priorizar cores por tier (mayor tier primero), luego por distancia (más cercanos)
        List<CoreConnection> sorted = new ArrayList<>(cores);
        sorted.sort((a, b) -> {
            int tierCompare = Integer.compare(b.tier, a.tier);
            if (tierCompare != 0) return tierCompare;
            return Integer.compare(a.distance, b.distance);
        });

        return sorted.subList(0, maxCores);
    }

    // ========================================
    // MÉTODOS DE COMPATIBILIDAD
    // ========================================

    private static boolean isPipeCompatibleWithCollector(int collectorTier, int pipeTier) {
        return collectorTier == pipeTier;
    }

    private static boolean isCoreTierCompatibleWithCollector(int collectorTier, int coreTier) {
        if (collectorTier == 1) {
            return coreTier >= 1 && coreTier <= 3;
        } else {
            return coreTier >= 1 && coreTier <= 5;
        }
    }

    private static int getMaxDistanceForTier(int collectorTier) {
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