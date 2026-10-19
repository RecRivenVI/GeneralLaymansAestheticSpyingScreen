package io.github.recrivenvi.glass.content.block;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 八像素投影核心；相邻有线节点各自贡献从核心到方块边界的六像素连接。 */
abstract class GlassConnectedBlock extends GlassStateBlock {
    static final Map<Direction, EnumProperty<Connection>> PROPERTY_BY_DIRECTION =
            connectionProperties();

    private static final VoxelShape CORE = Block.box(4.0, 4.0, 4.0, 12.0, 12.0, 12.0);
    private static final Map<Direction, VoxelShape> CONNECTORS =
            Map.of(
                    Direction.NORTH, Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 4.0),
                    Direction.SOUTH, Block.box(5.0, 5.0, 12.0, 11.0, 11.0, 16.0),
                    Direction.WEST, Block.box(0.0, 5.0, 5.0, 4.0, 11.0, 11.0),
                    Direction.EAST, Block.box(12.0, 5.0, 5.0, 16.0, 11.0, 11.0),
                    Direction.DOWN, Block.box(5.0, 0.0, 5.0, 11.0, 4.0, 11.0),
                    Direction.UP, Block.box(5.0, 12.0, 5.0, 11.0, 16.0, 11.0));
    private static final VoxelShape[] SHAPES = createShapes();

    protected GlassConnectedBlock(BlockBehaviour.Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (EnumProperty<Connection> property : PROPERTY_BY_DIRECTION.values()) {
            state = state.setValue(property, Connection.NONE);
        }
        registerDefaultState(state);
    }

    protected final BlockState withConnectionsAndSupports(
            BlockState state, LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            state =
                    state.setValue(
                            PROPERTY_BY_DIRECTION.get(direction),
                            connectionAt(state, level, pos, direction));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnectionsAndSupports(
                defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction directionToNeighbour,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random) {
        return state.setValue(
                PROPERTY_BY_DIRECTION.get(directionToNeighbour),
                connectionAt(state, level, pos, directionToNeighbour));
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return cachedShape(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PROPERTY_BY_DIRECTION.values().toArray(EnumProperty[]::new));
    }

    static VoxelShape coreShape() {
        return CORE;
    }

    static VoxelShape connectorShape(Direction direction) {
        return CONNECTORS.get(direction);
    }

    static VoxelShape attachmentSupportShape() {
        return Shapes.block();
    }

    protected boolean connectsTo(BlockState state, Direction direction, BlockState neighbourState) {
        return neighbourState.getBlock() instanceof GlassConnectedBlock;
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        // 视觉与碰撞保持小型模型；承载判定仍提供完整六面，让拉杆、按钮、火把等
        // 可附着方块能够合法放置，随后由六像素支撑杆表达连接。
        return attachmentSupportShape();
    }

    static VoxelShape cachedShape(BlockState state) {
        return SHAPES[shapeKey(state)];
    }

    static VoxelShape cachedShape(Map<Direction, Connection> connections) {
        int result = 0;
        for (Direction direction : Direction.values()) {
            if (connections.getOrDefault(direction, Connection.NONE) != Connection.NONE) {
                result |= 1 << direction.ordinal();
            }
        }
        return SHAPES[result];
    }

    static int cachedShapeCount() {
        return SHAPES.length;
    }

    private Connection connectionAt(
            BlockState state, LevelReader level, BlockPos pos, Direction direction) {
        BlockPos neighbourPos = pos.relative(direction);
        if (connectsTo(state, direction, level.getBlockState(neighbourPos))) {
            return Connection.WIRED;
        }
        return GlassAttachmentSupport.needsSupport(level, neighbourPos, direction.getOpposite())
                ? Connection.SUPPORT
                : Connection.NONE;
    }

    private static int shapeKey(BlockState state) {
        int result = 0;
        for (Direction direction : Direction.values()) {
            if (state.getValue(PROPERTY_BY_DIRECTION.get(direction)) != Connection.NONE) {
                result |= 1 << direction.ordinal();
            }
        }
        return result;
    }

    private static VoxelShape[] createShapes() {
        VoxelShape[] result = new VoxelShape[1 << Direction.values().length];
        for (int mask = 0; mask < result.length; mask++) {
            result[mask] = shapeForMask(mask);
        }
        return result;
    }

    private static VoxelShape shapeForMask(int mask) {
        VoxelShape shape = CORE;
        for (Direction direction : Direction.values()) {
            if ((mask & 1 << direction.ordinal()) != 0) {
                shape = Shapes.or(shape, CONNECTORS.get(direction));
            }
        }
        return shape;
    }

    private static Map<Direction, EnumProperty<Connection>> connectionProperties() {
        EnumMap<Direction, EnumProperty<Connection>> result = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            result.put(
                    direction,
                    EnumProperty.create(direction.getSerializedName(), Connection.class));
        }
        return Map.copyOf(result);
    }

    enum Connection implements StringRepresentable {
        NONE("none"),
        WIRED("wired"),
        SUPPORT("support");

        private final String serializedName;

        Connection(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
