package com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity;

import com.fruitsicecream.fruitsicecreamutilities.core.init.ModBlockEntities;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ExperienceCoreBlockEntity extends BlockEntity implements MenuProvider {
    private int storedExperience = 0;
    private int tier;
    private int xpPerHour;
    private int tickCounter = 0;
    private static final int TICKS_PER_GENERATION = 200; // Generar cada 10 segundos (200 ticks)

    // ContainerData para sincronización cliente-servidor
    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> ExperienceCoreBlockEntity.this.storedExperience;
                case 1 -> ExperienceCoreBlockEntity.this.tier;
                case 2 -> ExperienceCoreBlockEntity.this.xpPerHour;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> ExperienceCoreBlockEntity.this.storedExperience = value;
                case 1 -> ExperienceCoreBlockEntity.this.tier = value;
                case 2 -> ExperienceCoreBlockEntity.this.xpPerHour = value;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public ExperienceCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXPERIENCE_CORE_BE.get(), pos, state);
    }

    public void setTierAndRate(int tier, int xpPerHour) {
        this.tier = tier;
        this.xpPerHour = xpPerHour;
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ExperienceCoreBlockEntity blockEntity) {
        if (level.isClientSide) return;

        blockEntity.tickCounter++;

        if (blockEntity.tickCounter >= TICKS_PER_GENERATION) {
            blockEntity.tickCounter = 0;
            blockEntity.generateExperience();
        }
    }

    private void generateExperience() {
        // Calcular XP por generación (cada 200 ticks = 10 segundos)
        // xpPerHour / 72000 * 200 = xpPerHour / 360
        int xpToAdd = Math.max(1, xpPerHour / 360);
        storedExperience += xpToAdd;
        setChanged();
    }

    public int getStoredExperience() {
        return storedExperience;
    }

    public void setStoredExperience(int amount) {
        this.storedExperience = amount;
        setChanged();
    }

    public void collectExperience(Player player) {
        if (storedExperience <= 0 || level == null || level.isClientSide) return;

        // Calcular posición de spawn (encima del bloque)
        Vec3 spawnPos = new Vec3(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 1.0,
                worldPosition.getZ() + 0.5
        );

        // Spawnear orbes de experiencia
        spawnExperienceOrbs(level, spawnPos, storedExperience);

        // Limpiar el almacenamiento
        storedExperience = 0;
        setChanged();
    }

    private void spawnExperienceOrbs(Level level, Vec3 pos, int totalXP) {
        // Vanilla Minecraft spawns orbs in chunks to avoid too many entities
        // Los orbes tienen valores estándar: 2477, 1237, 617, 307, 149, 73, 37, 17, 7, 3, 1

        while (totalXP > 0) {
            int orbValue = getExperienceOrbValue(totalXP);
            totalXP -= orbValue;

            ExperienceOrb orb = new ExperienceOrb(level, pos.x, pos.y, pos.z, orbValue);
            level.addFreshEntity(orb);
        }
    }

    private int getExperienceOrbValue(int remaining) {
        // Usar los valores estándar de Minecraft para orbes de XP
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

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("StoredExperience", storedExperience);
        tag.putInt("Tier", tier);
        tag.putInt("XpPerHour", xpPerHour);
        tag.putInt("TickCounter", tickCounter);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storedExperience = tag.getInt("StoredExperience");
        tier = tag.getInt("Tier");
        xpPerHour = tag.getInt("XpPerHour");
        tickCounter = tag.getInt("TickCounter");
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