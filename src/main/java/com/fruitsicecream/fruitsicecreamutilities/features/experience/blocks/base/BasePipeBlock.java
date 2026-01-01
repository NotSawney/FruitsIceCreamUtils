package com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.base;

import net.minecraft.world.level.block.BaseEntityBlock;

/**
 * Clase base para todas las tuberías de experiencia.
 * Maneja la lógica común de conexión y compatibilidad de tiers.
 */
public abstract class BasePipeBlock extends BaseEntityBlock {
    private final int tier;

    protected BasePipeBlock(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public int getTier() {
        return tier;
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