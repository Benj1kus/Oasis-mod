package com.benji.oasiso.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GeyserBigBlock extends Block {

    private static final VoxelShape VASE_SHAPE = box(3, 0, 3, 13, 26, 13);

    public GeyserBigBlock(Properties properties) {
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
        com.benji.oasiso.client.geyser.BigGeyserClient.observe(level, pos);
    }

    @Override
    public void stepOn(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.Entity entity) {
        com.benji.oasiso.common.geyser.BigGeyserContactDamage.touch(entity);
        super.stepOn(level, pos, state, entity);
    }

    public static boolean open(net.minecraft.world.level.Level level, BlockPos pos) {
        double top = pos.getY() + 26.0 / 16.0;
        return level.hasChunkAt(pos.above()) && level.getFluidState(pos).isEmpty() && level.getFluidState(pos.above()).isEmpty() && level.noCollision(new net.minecraft.world.phys.AABB(pos.getX() + .28, top + .01, pos.getZ() + .28, pos.getX() + .72, top + .09, pos.getZ() + .72));
    }
}