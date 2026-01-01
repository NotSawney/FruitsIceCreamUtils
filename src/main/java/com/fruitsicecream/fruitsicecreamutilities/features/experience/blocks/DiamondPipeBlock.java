package com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks;

import com.fruitsicecream.fruitsicecreamutilities.core.config.ModConfig;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity.ExperienceCollectorBlockEntity;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity.PipeBlockEntity;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.base.BasePipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.TierSortingRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * Diamond-Studded Pipeline (Tier 2)
 * - Completamente pasivo, no requiere ticking
 * - La lógica de red está centralizada en NetworkManager
 * - Solo se encarga de conexiones visuales y notificaciones
 */
public class DiamondPipeBlock extends BasePipeBlock {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    private static final VoxelShape CORE_SHAPE = Block.box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape NORTH_SHAPE = Block.box(6, 6, 0, 10, 10, 5);
    private static final VoxelShape SOUTH_SHAPE = Block.box(6, 6, 11, 10, 10, 16);
    private static final VoxelShape EAST_SHAPE = Block.box(11, 6, 6, 16, 10, 10);
    private static final VoxelShape WEST_SHAPE = Block.box(0, 6, 6, 5, 10, 10);
    private static final VoxelShape UP_SHAPE = Block.box(6, 11, 6, 10, 16, 10);
    private static final VoxelShape DOWN_SHAPE = Block.box(6, 0, 6, 10, 5, 10);

    public DiamondPipeBlock() {
        super(Properties.of()
                        .strength(3.5f, 8.0f)
                        .sound(SoundType.METAL)
                        .requiresCorrectToolForDrops()
                        .noOcclusion(),
                2);

        registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeBlockEntity(pos, state);
    }

    // ========================================
    // NOTA: NO HAY getTicker()
    // Las pipes son pasivas y no necesitan tick
    // Toda la lógica está en NetworkManager
    // ========================================

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE_SHAPE;

        if (state.getValue(NORTH)) shape = Shapes.or(shape, NORTH_SHAPE);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, SOUTH_SHAPE);
        if (state.getValue(EAST)) shape = Shapes.or(shape, EAST_SHAPE);
        if (state.getValue(WEST)) shape = Shapes.or(shape, WEST_SHAPE);
        if (state.getValue(UP)) shape = Shapes.or(shape, UP_SHAPE);
        if (state.getValue(DOWN)) shape = Shapes.or(shape, DOWN_SHAPE);

        return shape;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // Notificar a Collectors cercanos cuando cambia la topología
        if (!level.isClientSide()) {
            notifyNearbyCollectors(level, pos);
        }

        return state.setValue(getPropertyForDirection(direction), canConnectTo(neighborState, direction.getOpposite()));
    }

    /**
     * Notifica a los Collectors cercanos de forma SEGURA.
     * Solo accede a chunks cargados y collectors que no estén siendo removidos.
     */
    private void notifyNearbyCollectors(net.minecraft.world.level.LevelAccessor level, BlockPos pos) {
        BlockPos.betweenClosedStream(
                pos.offset(-2, -2, -2),
                pos.offset(2, 2, 2)
        ).forEach(checkPos -> {
            // SEGURIDAD: Verificar que el chunk esté cargado
            if (level.hasChunkAt(checkPos)) {
                BlockEntity be = level.getBlockEntity(checkPos);
                if (be instanceof ExperienceCollectorBlockEntity collector) {
                    // Solo invalidar si el collector no está siendo removido
                    if (!collector.isRemoved()) {
                        collector.invalidateNetwork();
                    }
                }
            }
        });
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        BlockGetter level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        return this.defaultBlockState()
                .setValue(NORTH, canConnectTo(level.getBlockState(pos.north()), Direction.SOUTH))
                .setValue(SOUTH, canConnectTo(level.getBlockState(pos.south()), Direction.NORTH))
                .setValue(EAST, canConnectTo(level.getBlockState(pos.east()), Direction.WEST))
                .setValue(WEST, canConnectTo(level.getBlockState(pos.west()), Direction.EAST))
                .setValue(UP, canConnectTo(level.getBlockState(pos.above()), Direction.DOWN))
                .setValue(DOWN, canConnectTo(level.getBlockState(pos.below()), Direction.UP));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos,
                        BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);

        // Notificar cuando se coloca
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PipeBlockEntity pipe) {
            pipe.onPlaced();
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        // La notificación se maneja automáticamente en PipeBlockEntity.setRemoved()
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            if (!player.isCreative()) {
                ItemStack tool = player.getMainHandItem();
                if (hasCorrectTool(tool)) {
                    popResource(level, pos, new ItemStack(this));
                }
            }
        }

        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity blockEntity, ItemStack tool) {
        player.awardStat(net.minecraft.stats.Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
    }

    private boolean hasCorrectTool(ItemStack tool) {
        if (tool.isEmpty()) return false;

        String requiredTierName = ModConfig.PIPES.getRequiredTool(2); // Tier 2 para Diamond
        Tier requiredTier = getTierFromName(requiredTierName);

        if (requiredTier == null) return true;
        if (!(tool.getItem() instanceof TieredItem tieredItem)) return false;

        Tier toolTier = tieredItem.getTier();
        return toolTier == requiredTier || TierSortingRegistry.getTiersLowerThan(toolTier).contains(requiredTier);
    }

    private Tier getTierFromName(String name) {
        return switch (name.toUpperCase()) {
            case "WOOD" -> Tiers.WOOD;
            case "STONE" -> Tiers.STONE;
            case "IRON" -> Tiers.IRON;
            case "DIAMOND" -> Tiers.DIAMOND;
            case "NETHERITE" -> Tiers.NETHERITE;
            case "GOLD" -> Tiers.GOLD;
            default -> Tiers.DIAMOND;
        };
    }

    private boolean canConnectTo(BlockState neighborState, Direction connectionSide) {
        Block block = neighborState.getBlock();

        if (block instanceof BasePipeBlock) return true;
        if (block instanceof ExperienceCoreBlock core) return isCoreTierCompatible(core.getTier());
        if (block instanceof ExperienceCollectorBlock collector) return isCollectorCompatible(collector.getTier());

        return false;
    }

    private BooleanProperty getPropertyForDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    @Override
    protected boolean isCoreTierCompatible(int coreTier) {
        return coreTier >= 1 && coreTier <= 5;
    }

    @Override
    protected boolean isCollectorCompatible(int collectorTier) {
        return collectorTier == 2;
    }
}