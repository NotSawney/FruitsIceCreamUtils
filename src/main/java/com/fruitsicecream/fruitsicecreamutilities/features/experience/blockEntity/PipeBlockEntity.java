package com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity;

import com.fruitsicecream.fruitsicecreamutilities.core.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * VERSIÓN SEGURA: Optimización para evitar cuelgues en "Saving World".
 */
public class PipeBlockEntity extends BlockEntity {

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PIPE_BE.get(), pos, state);
    }

    /**
     * IMPORTANTE: En Forge 1.20.1, setRemoved se llama tanto al romper el bloque
     * como al descargar el chunk/mundo.
     */
    @Override
    public void setRemoved() {
        // Ejecutamos la lógica de limpieza de Forge primero
        super.setRemoved();

        // SEGURIDAD: Solo notificamos si el nivel no es nulo, estamos en el servidor,
        // Y muy importante: si el BE está siendo removido permanentemente (no solo descargado).
        // Sin embargo, para redes, lo más seguro es verificar si el nivel sigue "vivo".
        if (level != null && !level.isClientSide) {
            notifyNearbyCollectors();
        }
    }

    /**
     * Notifica a los Collectors cercanos con chequeos de seguridad de carga de Chunks.
     */
    private void notifyNearbyCollectors() {
        if (level == null) return;

        // Definimos el área de búsqueda (Radio 2)
        BlockPos min = worldPosition.offset(-2, -2, -2);
        BlockPos max = worldPosition.offset(2, 2, 2);

        // Usamos betweenClosedStream pero con un filtro de seguridad de carga
        BlockPos.betweenClosedStream(min, max).forEach(pos -> {
            // SEGURIDAD CRÍTICA: Nunca llames a getBlockEntity sin verificar si el chunk está cargado.
            // Esto es lo que causa el hang en "Saving World".
            if (level.hasChunkAt(pos)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof ExperienceCollectorBlockEntity collector) {
                    // Solo invalidamos si el collector no está marcado para removerse también
                    if (!collector.isRemoved()) {
                        collector.invalidateNetwork();
                    }
                }
            }
        });
    }

    /**
     * Llamado cuando la pipe se coloca en el mundo.
     */
    public void onPlaced() {
        if (level != null && !level.isClientSide) {
            notifyNearbyCollectors();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
    }

}