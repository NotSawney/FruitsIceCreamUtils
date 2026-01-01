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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.TierSortingRegistry;
import org.jetbrains.annotations.Nullable;

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
                        .noOcclusion(),
                1);

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

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, blockEntity) -> {
            if (blockEntity instanceof PipeBlockEntity pipe) {
                PipeBlockEntity.tick(lvl, pos, st, pipe);
            }
        };
    }

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

    // Añadir este método helper en la clase del bloque
    private void notifyNearbyCollectors(net.minecraft.world.level.LevelAccessor level, BlockPos pos) {
        BlockPos.betweenClosedStream(
                pos.offset(-2, -2, -2),
                pos.offset(2, 2, 2)
        ).forEach(checkPos -> {
            if (level.getBlockEntity(checkPos) instanceof ExperienceCollectorBlockEntity collector) {
                collector.invalidateNetwork();
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

        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PipeBlockEntity pipe) {
            pipe.onPlaced();
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        // La notificación se hace automáticamente en setRemoved()
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            // Reglas de dropeo:
            // - En creativo: NO dropear nada
            // - Herramienta incorrecta: NO dropear nada
            // - Herramienta correcta: dropear normalmente

            if (!player.isCreative()) {
                ItemStack tool = player.getMainHandItem();
                if (hasCorrectTool(tool)) {
                    // Dropear normalmente (Minecraft se encarga)
                    popResource(level, pos, new ItemStack(this));
                }
                // Si no tiene la herramienta correcta, no dropea nada
            }
            // Si está en creativo, no dropea nada
        }

        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity blockEntity, ItemStack tool) {
        player.awardStat(net.minecraft.stats.Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
        // No llamamos a super.playerDestroy para evitar drops duplicados
    }

    private boolean hasCorrectTool(ItemStack tool) {
        if (tool.isEmpty()) return false;

        // Obtenemos el Tier requerido desde Config
        String requiredTierName = ModConfig.PIPES.getRequiredTool(2);
        Tier requiredTier = getTierFromName(requiredTierName);

        // Si el config está mal o no requiere tier, se pica con cualquier cosa
        if (requiredTier == null) return true;

        // Verificamos si es una herramienta con Tier (Picos, Hachas, etc.)
        if (!(tool.getItem() instanceof TieredItem tieredItem)) return false;

        Tier toolTier = tieredItem.getTier();
        // Verificamos si el tier de la herramienta es igual al requerido
        // O si el requerido está en la lista de tiers "inferiores" (lógica de jerarquía de Forge)
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
            default -> Tiers.IRON;
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
        return coreTier >= 1 && coreTier <= 3;
    }

    @Override
    protected boolean isCollectorCompatible(int collectorTier) {
        return collectorTier == 1;
    }
}