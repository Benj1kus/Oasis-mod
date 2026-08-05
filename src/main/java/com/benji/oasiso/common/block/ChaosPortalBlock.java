package com.benji.oasiso.common.block;

import com.benji.oasiso.common.dimension.ChaosPortalTeleporter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ChaosPortalBlock extends Block {

    public static final EnumProperty<Direction.Axis> AXIS =
            BlockStateProperties.HORIZONTAL_AXIS;

    private static final VoxelShape X_SHAPE =
            Block.box(
                    0.0D,
                    0.0D,
                    7.0D,
                    16.0D,
                    16.0D,
                    9.0D
            );

    private static final VoxelShape Z_SHAPE =
            Block.box(
                    7.0D,
                    0.0D,
                    0.0D,
                    9.0D,
                    16.0D,
                    16.0D
            );

    public ChaosPortalBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(
                                AXIS,
                                Direction.Axis.X
                        )
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(AXIS);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return state.getValue(AXIS)
                == Direction.Axis.X
                ? X_SHAPE
                : Z_SHAPE;
    }


    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return Shapes.empty();
    }

    @Override
    public float getShadeBrightness(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        return true;
    }


    @Override
    public void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity
    ) {
        ChaosPortalTeleporter.handleInside(
                level,
                pos,
                state.getValue(AXIS),
                entity
        );
    }


    @Override
    public void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston
    ) {
        super.onPlace(
                state,
                level,
                pos,
                oldState,
                movedByPiston
        );

        if (!level.isClientSide
                && state.getBlock()
                != oldState.getBlock()) {

            level.scheduleTick(
                    pos,
                    this,
                    20
            );
        }
    }

    @Override
    public void tick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {
        Direction.Axis axis =
                state.getValue(AXIS);


        if (!ChaosPortalShape.hasValidPortal(
                level,
                pos,
                axis
        )) {
            level.removeBlock(pos, false);
            return;
        }

        level.scheduleTick(
                pos,
                this,
                20
        );
    }
}