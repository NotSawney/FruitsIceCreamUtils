package com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity;

import com.fruitsicecream.fruitsicecreamutilities.core.init.ModBlockEntities;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.ExperienceCoreBlock;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.menu.ExperienceCoreMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ExperienceCoreBlockEntity extends BlockEntity implements MenuProvider {
    private int storedExperience = 0;
    private int tier;
    private int xpPerHour;
    private int maxCapacity;
    private int tickCounter = 0;
    private int pushCounter = 0;

    // Intervalos de push por tier (en ticks)
    private static final int[] PUSH_INTERVALS = {40, 35, 30, 25, 20}; // MK-I a MK-V
    // XP por push por tier
    private static final int[] XP_PER_PUSH = {2, 4, 6, 10, 16}; // MK-I a MK-V

    private static final int TICKS_PER_GENERATION = 200; // 10 segundos

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> ExperienceCoreBlockEntity.this.storedExperience;
                case 1 -> ExperienceCoreBlockEntity.this.tier;
                case 2 -> ExperienceCoreBlockEntity.this.xpPerHour;
                case 3 -> ExperienceCoreBlockEntity.this.maxCapacity;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> ExperienceCoreBlockEntity.this.storedExperience = value;
                case 1 -> ExperienceCoreBlockEntity.this.tier = value;
                case 2 -> ExperienceCoreBlockEntity.this.xpPerHour = value;
                case 3 -> ExperienceCoreBlockEntity.this.maxCapacity = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public ExperienceCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXPERIENCE_CORE_BE.get(), pos, state);
    }

    public void setTierAndRate(int tier, int xpPerHour, int maxCapacity) {
        this.tier = tier;
        this.xpPerHour = xpPerHour;
        this.maxCapacity = maxCapacity;
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ExperienceCoreBlockEntity blockEntity) {
        if (level.isClientSide) return;

        // Generación de XP
        blockEntity.tickCounter++;
        if (blockEntity.tickCounter >= TICKS_PER_GENERATION) {
            blockEntity.tickCounter = 0;
            blockEntity.generateExperience();
        }

        // Push logic (si hay XP almacenada)
        if (blockEntity.storedExperience > 0) {
            blockEntity.pushCounter++;
            int pushInterval = PUSH_INTERVALS[blockEntity.tier - 1];

            if (blockEntity.pushCounter >= pushInterval) {
                blockEntity.pushCounter = 0;
                blockEntity.tryPushToCollector();
            }
        }

        // Actualizar luz cada tick usando blockstate
        blockEntity.updateLightLevel();
    }

    private void generateExperience() {
        // Solo generar si no estamos a capacidad máxima
        if (storedExperience >= maxCapacity) {
            return;
        }

        int xpToAdd = Math.max(1, xpPerHour / 360);
        storedExperience = Math.min(storedExperience + xpToAdd, maxCapacity);
        setChanged();
    }

    private void tryPushToCollector() {
        // TODO: Implementar en la siguiente fase cuando tengamos collectors
        // Por ahora, este método está listo para ser usado

        // Pseudocódigo de lo que hará:
        // 1. Buscar collector arriba o abajo
        // 2. Si encuentra collector válido:
        //    - Calcular XP a transferir
        //    - Verificar si collector puede aceptar
        //    - Transferir y reducir storedExperience
    }

    /**
     * ALTERNATIVA 3: Actualiza el blockstate con el nivel de luz
     * Esta es la forma "vanilla" como lo hace el redstone wire
     */
    private void updateLightLevel() {
        if (level == null || level.isClientSide) return;

        BlockState currentState = getBlockState();
        int currentLightInState = currentState.getValue(ExperienceCoreBlock.LIGHT_LEVEL);
        int newLight = getLightLevel();

        // Solo actualizar si el nivel de luz cambió
        if (currentLightInState != newLight) {
            // Cambiar el blockstate con el nuevo nivel de luz
            BlockState newState = currentState.setValue(ExperienceCoreBlock.LIGHT_LEVEL, newLight);

            // Flag 3 = UPDATE_CLIENTS | UPDATE_NEIGHBORS
            // Esto notifica a clientes y vecinos del cambio
            level.setBlock(worldPosition, newState, 3);
        }
    }

    /**
     * Calcula el nivel de luz basado en el % de llenado
     * 0 XP = 0 luz, máxima capacidad = 15 luz
     */
    public int getLightLevel() {
        if (maxCapacity == 0) return 0;

        float fillPercentage = (float) storedExperience / maxCapacity;
        return (int) (fillPercentage * 15);
    }

    public int getStoredExperience() {
        return storedExperience;
    }

    public void setStoredExperience(int amount) {
        this.storedExperience = Math.min(amount, maxCapacity);
        setChanged();
        updateLightLevel();
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public float getFillPercentage() {
        if (maxCapacity == 0) return 0;
        return (float) storedExperience / maxCapacity * 100;
    }

    public boolean isFull() {
        return storedExperience >= maxCapacity;
    }

    public void collectExperience(Player player) {
        if (storedExperience <= 0 || level == null || level.isClientSide) return;

        Vec3 spawnPos = new Vec3(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 1.0,
                worldPosition.getZ() + 0.5
        );

        spawnExperienceOrbs(level, spawnPos, storedExperience);
        storedExperience = 0;
        setChanged();
        updateLightLevel();
    }

    private void spawnExperienceOrbs(Level level, Vec3 pos, int totalXP) {
        while (totalXP > 0) {
            int orbValue = getExperienceOrbValue(totalXP);
            totalXP -= orbValue;

            ExperienceOrb orb = new ExperienceOrb(level, pos.x, pos.y, pos.z, orbValue);
            level.addFreshEntity(orb);
        }
    }

    private int getExperienceOrbValue(int remaining) {
        if (remaining >= 2477) return 2477;
        if (remaining >= 1237) return 1237;
        if (remaining >= 617) return 617;
        if (remaining >= 307) return 307;
        if (remaining >= 149) return 149;
        if (remaining >= 73) return 73;
        if (remaining >= 37) return 37;
        if (remaining >= 17) return 17;
        if (remaining >= 7) return 7;
        if (remaining >= 3) return 3;
        return 1;
    }

    public int getTier() {
        return tier;
    }

    public int getXpPerHour() {
        return xpPerHour;
    }

    public int getPushInterval() {
        return PUSH_INTERVALS[tier - 1];
    }

    public int getXpPerPush() {
        return XP_PER_PUSH[tier - 1];
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("StoredExperience", storedExperience);
        tag.putInt("Tier", tier);
        tag.putInt("XpPerHour", xpPerHour);
        tag.putInt("MaxCapacity", maxCapacity);
        tag.putInt("TickCounter", tickCounter);
        tag.putInt("PushCounter", pushCounter);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storedExperience = tag.getInt("StoredExperience");
        tier = tag.getInt("Tier");
        xpPerHour = tag.getInt("XpPerHour");
        maxCapacity = tag.getInt("MaxCapacity");
        tickCounter = tag.getInt("TickCounter");
        pushCounter = tag.getInt("PushCounter");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.fruitsicecreamutilities.exp_core_mk" + tier);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ExperienceCoreMenu(containerId, playerInventory, this, this.dataAccess);
    }
}