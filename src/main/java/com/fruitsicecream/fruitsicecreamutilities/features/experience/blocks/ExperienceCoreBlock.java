package com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ExperienceCoreBlock extends Block {
    private final int tier;
    private final int xpPerHour;
    private static final int TICKS_PER_HOUR = 72000; // 20 ticks/seg * 3600 seg

    public ExperienceCoreBlock(Properties properties, int tier, int xpPerHour) {
        super(properties);
        this.tier = tier;
        this.xpPerHour = xpPerHour;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            // Programar el primer tick
            level.scheduleTick(pos, this, getTickDelay());
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Generar XP
        generateExperience(level, pos);

        // Programar el siguiente tick
        level.scheduleTick(pos, this, getTickDelay());
    }

    private void generateExperience(ServerLevel level, BlockPos pos) {
        // Por ahora solo log, luego implementaremos la generación real de XP
        // La generación real necesitará buscar jugadores cercanos o almacenar XP
        int xpPerTick = xpPerHour / TICKS_PER_HOUR;

        // TODO: Implementar lógica de generación de XP
        // Opciones:
        // 1. Generar orbes de XP directamente
        // 2. Almacenar en el bloque y que el jugador lo recoja
        // 3. Darlo al jugador más cercano
    }

    private int getTickDelay() {
        // Calcular cada cuántos ticks debe generar XP
        // Para 360 XP/hora = 0.005 XP/tick, generemos 1 XP cada 200 ticks (10 segundos)
        return 200;
    }

    public int getTier() {
        return tier;
    }

    public int getXpPerHour() {
        return xpPerHour;
    }
}