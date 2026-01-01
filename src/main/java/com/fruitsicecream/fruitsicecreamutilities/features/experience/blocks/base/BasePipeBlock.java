package com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.base;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Clase base para todas las tuberías de experiencia.
 * Maneja la lógica común de conexión y compatibilidad de tiers.
 */
public abstract class BasePipeBlock extends Block {
    private final int tier;

    protected BasePipeBlock(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Determina si esta tubería puede conectarse a un Core del tier especificado.
     * @param coreTier El tier del Core (1-5)
     * @return true si es compatible
     */
    protected abstract boolean isCoreTierCompatible(int coreTier);

    /**
     * Determina si esta tubería puede conectarse a un Collector del tier especificado.
     * @param collectorTier El tier del Collector (1-2)
     * @return true si es compatible
     */
    protected abstract boolean isCollectorCompatible(int collectorTier);
}