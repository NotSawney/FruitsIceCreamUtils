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
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class ExperienceCoreBlock extends BaseEntityBlock {
    // Property para el nivel de luz (0-15) como la redstone
    public static final IntegerProperty LIGHT_LEVEL = BlockStateProperties.LEVEL;

    private final int tier;

    public ExperienceCoreBlock(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any().setValue(LIGHT_LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIGHT_LEVEL);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        ExperienceCoreBlockEntity be = new ExperienceCoreBlockEntity(pos, state);
        return be;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public int getLightEmission(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        // Ahora la luz viene directamente del blockstate
        return state.getValue(LIGHT_LEVEL);
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

        if (stack.hasTag() && stack.getTag().contains("StoredExperience")) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ExperienceCoreBlockEntity coreEntity) {
                int storedXP = stack.getTag().getInt("StoredExperience");
                coreEntity.setStoredExperience(storedXP);
            }
        }
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            BlockEntity be = level.getBlockEntity(pos);

            if (be instanceof ExperienceCoreBlockEntity coreEntity) {
                int storedXP = coreEntity.getStoredExperience();

                if (player.isCreative()) {
                    if (storedXP > 0) {
                        Vec3 spawnPos = new Vec3(
                                pos.getX() + 0.5,
                                pos.getY() + 0.5,
                                pos.getZ() + 0.5
                        );
                        spawnExperienceOrbs(serverLevel, spawnPos, storedXP);
                    }
                    // No dropear el bloque en creativo
                } else {
                    // En SURVIVAL
                    ItemStack heldItem = player.getMainHandItem();
                    boolean hasSilkTouch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, heldItem) > 0;
                    boolean hasCorrectTool = hasCorrectToolForTier(heldItem);

                    // Solo dropear el bloque con NBT si tiene silk touch Y la herramienta correcta
                    if (hasSilkTouch && hasCorrectTool) {
                        if (storedXP > 0) {
                            ItemStack drop = new ItemStack(this);
                            CompoundTag tag = drop.getOrCreateTag();
                            tag.putInt("StoredExperience", storedXP);
                            popResource(level, pos, drop);
                        } else {
                            popResource(level, pos, new ItemStack(this));
                        }
                    } else if (hasCorrectTool) {
                        // Herramienta correcta pero sin silk touch: dropea bloque vacío + XP
                        if (storedXP > 0) {
                            Vec3 spawnPos = new Vec3(
                                    pos.getX() + 0.5,
                                    pos.getY() + 0.5,
                                    pos.getZ() + 0.5
                            );
                            spawnExperienceOrbs(serverLevel, spawnPos, storedXP);
                        }
                    } else {
                        // Herramienta incorrecta: no dropea bloque, solo XP
                        if (storedXP > 0) {
                            Vec3 spawnPos = new Vec3(
                                    pos.getX() + 0.5,
                                    pos.getY() + 0.5,
                                    pos.getZ() + 0.5
                            );
                            spawnExperienceOrbs(serverLevel, spawnPos, storedXP);
                        }
                    }
                }
            }
        }

        super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * Verifica si la herramienta es del tier correcto para este core
     * MK1-3: Requieren pico de hierro o superior
     * MK4-5: Requieren pico de diamante o superior
     */
    private boolean hasCorrectToolForTier(ItemStack tool) {
        if (tool.isEmpty()) return false;

        // Verificar si es un pico
        if (!tool.is(net.minecraft.tags.ItemTags.PICKAXES)) {
            return false;
        }

        // MK1-3: Requieren tier de hierro o superior
        if (tier >= 1 && tier <= 3) {
            return tool.isCorrectToolForDrops(this.defaultBlockState()) ||
                    canHarvestWithIronOrBetter(tool);
        }

        // MK4-5: Requieren tier de diamante o superior
        if (tier >= 4 && tier <= 5) {
            return canHarvestWithDiamondOrBetter(tool);
        }

        return false;
    }

    private boolean canHarvestWithIronOrBetter(ItemStack tool) {
        if (!(tool.getItem() instanceof TieredItem tieredItem)) return false;

        Tier tier = tieredItem.getTier();
        return TierSortingRegistry.getTiersLowerThan(tier).contains(Tiers.IRON) || tier == Tiers.IRON;
    }

    private boolean canHarvestWithDiamondOrBetter(ItemStack tool) {
        if (!(tool.getItem() instanceof TieredItem tieredItem)) return false;

        Tier tier = tieredItem.getTier();
        return TierSortingRegistry.getTiersLowerThan(tier).contains(Tiers.DIAMOND) || tier == Tiers.DIAMOND;
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity blockEntity, ItemStack tool) {
        player.awardStat(net.minecraft.stats.Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
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
}