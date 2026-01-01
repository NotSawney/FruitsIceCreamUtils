package com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks;

import com.fruitsicecream.fruitsicecreamutilities.features.experience.blocks.base.BasePipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.Direction;

public class GoldPipeBlock extends BasePipeBlock {
    // Propiedades de conexión para cada dirección
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    // Formas del core y brazos para hitbox
    private static final VoxelShape CORE_SHAPE = Block.box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape NORTH_SHAPE = Block.box(6, 6, 0, 10, 10, 5);
    private static final VoxelShape SOUTH_SHAPE = Block.box(6, 6, 11, 10, 10, 16);
    private static final VoxelShape EAST_SHAPE = Block.box(11, 6, 6, 16, 10, 10);
    private static final VoxelShape WEST_SHAPE = Block.box(0, 6, 6, 5, 10, 10);
    private static final VoxelShape UP_SHAPE = Block.box(6, 11, 6, 10, 16, 10);
    private static final VoxelShape DOWN_SHAPE = Block.box(6, 0, 6, 10, 5, 10);

    public GoldPipeBlock() {
        super(Properties.of()
                        .strength(2.0f, 5.0f)
                        .sound(SoundType.METAL)
                        .requiresCorrectToolForDrops()
                        .noOcclusion(), // Importante para transparencia
                1); // Tier 1 (básico)

        registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
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
        return state.setValue(getPropertyForDirection(direction), canConnectTo(neighborState, direction.getOpposite()));
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

    private boolean canConnectTo(BlockState neighborState, Direction connectionSide) {
        Block block = neighborState.getBlock();

        // Conectar a otras tuberías
        if (block instanceof BasePipeBlock) {
            return true;
        }

        // Conectar a Cores
        if (block instanceof ExperienceCoreBlock core) {
            return isCoreTierCompatible(core.getTier());
        }

        // Conectar a Collectors
        if (block instanceof ExperienceCollectorBlock collector) {
            return isCollectorCompatible(collector.getTier());
        }

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

    // Gold Pipes (Tier 1) solo se conectan a cores MK1-MK3 y Basic Collector
    @Override
    protected boolean isCoreTierCompatible(int coreTier) {
        return coreTier >= 1 && coreTier <= 3;
    }

    @Override
    protected boolean isCollectorCompatible(int collectorTier) {
        return collectorTier == 1; // Solo Basic Collector
    }
}