package com.benji.oasiso.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SoulCloudBlock extends Block {
    public static final BooleanProperty SOULS = BooleanProperty.create("souls");
    public static final BooleanProperty OFF = BooleanProperty.create("off");
    public static final float SOUL_CHANCE = .30F;
    private static final VoxelShape FLOOR = Block.box(0, 0, 0, 16, 32D / 3D, 16);

    public SoulCloudBlock(Properties properties) {
        super(properties.noOcclusion().isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false));
        registerDefaultState(stateDefinition.any().setValue(SOULS, false).setValue(OFF, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SOULS, OFF);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(SOULS, !context.getLevel().isClientSide && context.getLevel().random.nextFloat() < SOUL_CHANCE);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FLOOR;
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FLOOR;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1F;
    }
}
