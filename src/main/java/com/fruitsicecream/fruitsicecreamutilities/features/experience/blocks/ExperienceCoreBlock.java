package com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks;

import com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity.ExperienceCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ExperienceCoreBlock extends BaseEntityBlock {
    private final int tier;
    private final int xpPerHour;

    public ExperienceCoreBlock(Properties properties, int tier, int xpPerHour) {
        super(properties);
        this.tier = tier;
        this.xpPerHour = xpPerHour;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        ExperienceCoreBlockEntity be = new ExperienceCoreBlockEntity(pos, state);
        be.setTierAndRate(tier, xpPerHour);
        return be;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (lvl, pos, st, blockEntity) -> {
            if (blockEntity instanceof ExperienceCoreBlockEntity be) {
                ExperienceCoreBlockEntity.tick(lvl, pos, st, be);
            }
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ExperienceCoreBlockEntity) {
                NetworkHooks.openScreen((ServerPlayer) player, (ExperienceCoreBlockEntity) be, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        // Si el item tiene NBT con experiencia almacenada, restaurarla
        if (stack.hasTag() && stack.getTag().contains("StoredExperience")) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ExperienceCoreBlockEntity coreEntity) {
                int storedXP = stack.getTag().getInt("StoredExperience");
                coreEntity.setStoredExperience(storedXP);
            }
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);

        ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);

        if (tool != null && be instanceof ExperienceCoreBlockEntity coreEntity) {
            boolean hasSilkTouch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0;

            if (hasSilkTouch) {
                // Con Silk Touch: dropear el bloque con XP almacenada
                ItemStack blockDrop = new ItemStack(this);
                int storedXP = coreEntity.getStoredExperience();

                if (storedXP > 0) {
                    CompoundTag tag = blockDrop.getOrCreateTag();
                    tag.putInt("StoredExperience", storedXP);
                }

                drops.clear();
                drops.add(blockDrop);
            } else {
                // Sin Silk Touch: no dropear el bloque, solo XP
                drops.clear();
            }
        }

        return drops;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);

            if (be instanceof ExperienceCoreBlockEntity coreEntity && !level.isClientSide) {
                // Solo dropear XP si NO se usó Silk Touch
                // Verificamos si ya se dropeó el bloque (con Silk Touch)
                // Si no hay drops del bloque, entonces dropeamos la XP
                int storedXP = coreEntity.getStoredExperience();

                if (storedXP > 0) {
                    // Spawnear orbes de experiencia
                    Vec3 spawnPos = new Vec3(
                            pos.getX() + 0.5,
                            pos.getY() + 0.5,
                            pos.getZ() + 0.5
                    );
                    spawnExperienceOrbs((ServerLevel) level, spawnPos, storedXP);
                }
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    private void spawnExperienceOrbs(ServerLevel level, Vec3 pos, int totalXP) {
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
}