package com.benji.oasiso.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GeyserSmallBlock extends Block {

    private static final VoxelShape VASE_SHAPE = box(3, 0, 3, 13, 13, 13);

    public GeyserSmallBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VASE_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VASE_SHAPE;
    }

    @Override
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public void animateTick(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        com.benji.oasiso.client.geyser.SmallGeyserClient.observe(level, pos);
    }

    public static boolean open(net.minecraft.world.level.Level level, BlockPos pos) {
        BlockPos above = pos.above();
        return level.hasChunkAt(above) && level.getFluidState(pos).isEmpty() && level.getFluidState(above).isEmpty() && level.getBlockState(above).getCollisionShape(level, above).isEmpty();
    }
}