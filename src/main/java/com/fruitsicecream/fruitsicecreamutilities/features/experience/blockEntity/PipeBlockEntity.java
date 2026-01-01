package com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity;

import com.fruitsicecream.fruitsicecreamutilities.core.config.ModConfig;
import com.fruitsicecream.fruitsicecreamutilities.core.init.ModBlockEntities;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.ExperienceCollectorBlock;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.ExperienceCoreBlock;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.base.BasePipeBlock;
import com.fruitsicecream.fruitsicecreamutilities.util.SaveStateTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class PipeBlockEntity extends BlockEntity {
    // Cache de red
    private PipeNetwork cachedNetwork = null;
    private boolean needsNetworkUpdate = true;
    private int cooldownTicks = 0;

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PIPE_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PipeBlockEntity blockEntity) {
        if (level.isClientSide) return;
        if (SaveStateTracker.isSaving()) {
            return;
        }

        // Primero verificamos si somos una "Tubería de Salida" (conectada a un Collector).
        boolean isConnectedToCollector = blockEntity.hasAdjacentCollector();

        if (isConnectedToCollector) {
            // Solo si somos útiles, verificamos si necesitamos actualizar la red
            if (blockEntity.needsNetworkUpdate || blockEntity.cachedNetwork == null) {
                if (blockEntity.cooldownTicks <= 0) {
                    try {
                        blockEntity.scanNetwork();
                        blockEntity.needsNetworkUpdate = false;
                        blockEntity.cooldownTicks = 100;
                    } catch (Exception e) {
                        // Si falla el escaneo, no crashear - solo loguear y resetear
                        blockEntity.cachedNetwork = null;
                        blockEntity.needsNetworkUpdate = true;
                        blockEntity.cooldownTicks = 200; // Cooldown más largo tras error
                    }
                } else {
                    blockEntity.cooldownTicks--;
                }
            }

            // Ejecutar lógica de transferencia
            if (blockEntity.cachedNetwork != null && blockEntity.cachedNetwork.isValid()) {
                try {
                    blockEntity.tryTransferExperience();
                } catch (Exception e) {
                    // Si falla la transferencia, invalidar la red para forzar re-escaneo
                    blockEntity.markNetworkDirty();
                }
            }
        } else {
            // Si dejamos de estar conectados a un collector, limpiamos la cache
            if (blockEntity.cachedNetwork != null) {
                blockEntity.cachedNetwork = null;
            }
            blockEntity.needsNetworkUpdate = false;
        }
    }

    private void scanNetwork() {
        BasePipeBlock pipeBlock = (BasePipeBlock) getBlockState().getBlock();
        cachedNetwork = PipeNetworkScanner.scanNetwork(level, worldPosition, pipeBlock.getTier());
    }

    private void tryTransferExperience() {
        if (level == null || cachedNetwork == null) return;

        // Solo transferir si esta pipe está directamente conectada a un Collector
        BlockEntity collectorAbove = level.getBlockEntity(worldPosition.above());
        BlockEntity collectorBelow = level.getBlockEntity(worldPosition.below());

        ExperienceCollectorBlockEntity targetCollector = null;
        boolean isAboveConnection = false;

        if (collectorBelow instanceof ExperienceCollectorBlockEntity) {
            targetCollector = (ExperienceCollectorBlockEntity) collectorBelow;
            isAboveConnection = false;
        } else if (collectorAbove instanceof ExperienceCollectorBlockEntity) {
            targetCollector = (ExperienceCollectorBlockEntity) collectorAbove;
            isAboveConnection = true;
        }

        if (targetCollector == null) return;

        // Si hay conexión arriba Y abajo, solo procesar desde abajo
        BlockEntity otherSide = isAboveConnection ?
                level.getBlockEntity(worldPosition.below()) :
                level.getBlockEntity(worldPosition.above());

        if (otherSide instanceof PipeBlockEntity otherPipe) {
            PipeNetwork otherNetwork = otherPipe.getCachedNetwork();
            if (otherNetwork != null &&
                    otherNetwork.collector != null &&
                    otherNetwork.collector.equals(targetCollector.getBlockPos())) {
                // Están interconectadas - solo procesar desde abajo
                if (isAboveConnection) return;
            }
        }

        // Calcular XP disponible para transferir
        int availableXP = calculateAvailableXP();

        if (availableXP <= 0) return;

        // Aplicar límites según config
        BasePipeBlock pipeBlock = (BasePipeBlock) getBlockState().getBlock();
        int tier = pipeBlock.getTier();

        int xpToTransfer = availableXP;

        if (ModConfig.PIPES.limitedFlowrate.get()) {
            int flowRate = ModConfig.PIPES.getFlowRate(tier);
            xpToTransfer = Math.min(availableXP, flowRate);
        }

        // Intentar agregar al Collector
        int transferred = targetCollector.addExperience(xpToTransfer);

        if (transferred > 0) {
            // Distribuir el XP consumido entre los cores conectados
            distributeXPConsumption(transferred);
        }
    }

    private int calculateAvailableXP() {
        if (cachedNetwork == null || !cachedNetwork.isValid()) return 0;

        int totalXP = 0;
        for (BlockPos corePos : cachedNetwork.connectedCores) {
            if (level.getBlockEntity(corePos) instanceof ExperienceCoreBlockEntity core) {
                totalXP += core.getStoredExperience();
            }
        }
        return totalXP;
    }

    private void distributeXPConsumption(int totalConsumed) {
        if (cachedNetwork == null || cachedNetwork.connectedCores.isEmpty()) return;

        // Ordenar cores por tier (prioridad a tier más alto)
        List<ExperienceCoreBlockEntity> cores = new ArrayList<>();
        for (BlockPos corePos : cachedNetwork.connectedCores) {
            if (level.getBlockEntity(corePos) instanceof ExperienceCoreBlockEntity core) {
                cores.add(core);
            }
        }
        cores.sort((a, b) -> Integer.compare(b.getTier(), a.getTier()));

        int remaining = totalConsumed;

        for (ExperienceCoreBlockEntity core : cores) {
            if (remaining <= 0) break;

            int coreXP = core.getStoredExperience();
            int toTake = Math.min(remaining, coreXP);

            core.setStoredExperience(coreXP - toTake);
            remaining -= toTake;
        }
    }

    public void markNetworkDirty() {
        this.needsNetworkUpdate = true;
        this.cachedNetwork = null;
    }

    public PipeNetwork getCachedNetwork() {
        return cachedNetwork;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Notificar a pipes vecinas que la red cambió
        if (level != null) {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = worldPosition.relative(dir);
                if (level.getBlockEntity(neighborPos) instanceof PipeBlockEntity pipe) {
                    pipe.markNetworkDirty();
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("NeedsUpdate", needsNetworkUpdate);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        needsNetworkUpdate = tag.getBoolean("NeedsUpdate");
    }

    // Método auxiliar nuevo para detección rápida
    private boolean hasAdjacentCollector() {
        // Revisamos Arriba y Abajo
        BlockEntity above = level.getBlockEntity(worldPosition.above());
        BlockEntity below = level.getBlockEntity(worldPosition.below());

        return above instanceof ExperienceCollectorBlockEntity ||
                below instanceof ExperienceCollectorBlockEntity;
    }

    /**
     * Representa una red de tuberías con sus cores y collector conectados
     */
    public static class PipeNetwork {
        public final Set<BlockPos> connectedCores;
        public final BlockPos collector;
        public final int networkSize; // Total de bloques en la red (pipes + cores + collector)
        public final int tier;

        public PipeNetwork(Set<BlockPos> cores, BlockPos collector, int networkSize, int tier) {
            this.connectedCores = new HashSet<>(cores);
            this.collector = collector;
            this.networkSize = networkSize;
            this.tier = tier;
        }

        public boolean isValid() {
            return !connectedCores.isEmpty() && collector != null;
        }

        public int getCoreCount() {
            return connectedCores.size();
        }
    }
}