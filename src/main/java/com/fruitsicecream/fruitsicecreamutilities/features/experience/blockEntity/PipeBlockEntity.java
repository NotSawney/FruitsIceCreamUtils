package com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity;

import com.fruitsicecream.fruitsicecreamutilities.core.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * NUEVA VERSIÓN SIMPLIFICADA: Las pipes ahora son PASIVAS.
 *
 * Responsabilidades:
 * - Solo conectar visualmente
 * - Notificar a Collectors vecinos cuando hay cambios
 * - NO mantener cache de red
 * - NO hacer escaneos
 *
 * Los Collectors son los únicos que gestionan redes activamente.
 */
public class PipeBlockEntity extends BlockEntity {

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PIPE_BE.get(), pos, state);
    }

    /**
     * Las pipes ya no tienen lógica de tick.
     * Toda la lógica de red la manejan los Collectors.
     */
    public static void tick(Level level, BlockPos pos, BlockState state, PipeBlockEntity blockEntity) {
        // INTENCIONALMENTE VACÍO
        // Las pipes son completamente pasivas ahora
    }

    /**
     * Cuando se coloca o rompe una pipe, notificamos a los Collectors cercanos
     * para que invaliden su cache de red.
     */
    @Override
    public void setRemoved() {
        super.setRemoved();

        if (level != null && !level.isClientSide) {
            notifyNearbyCollectors();
        }
    }

    /**
     * Notifica a todos los Collectors en un radio de 2 bloques que la topología cambió.
     * Esto es más eficiente que propagar notificaciones por toda la red.
     */
    private void notifyNearbyCollectors() {
        // Buscar en un cubo de 5x5x5 centrado en esta pipe
        BlockPos.betweenClosedStream(
                worldPosition.offset(-2, -2, -2),
                worldPosition.offset(2, 2, 2)
        ).forEach(pos -> {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ExperienceCollectorBlockEntity collector) {
                collector.invalidateNetwork();
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

    // ========================================
    // NBT (Mínimo necesario)
    // ========================================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        // Las pipes no necesitan guardar nada especial
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // Las pipes no necesitan cargar nada especial
    }
}