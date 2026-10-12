package com.benji.oasiso.common.block;

import com.benji.oasiso.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class EntropyWellPartBlock extends Block {
    public static final IntegerProperty X = IntegerProperty.create("offset_x", 0, 2), Y = IntegerProperty.create("offset_y", 0, 4), Z = IntegerProperty.create("offset_z", 0, 2);

    public EntropyWellPartBlock(Properties p) {
        super(p.pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(X, 1).setValue(Y, 0).setValue(Z, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(X, Y, Z);
    }

    public static BlockPos root(BlockPos p, BlockState s) {
        return p.offset(1 - s.getValue(X), -s.getValue(Y), 1 - s.getValue(Z));
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return EntropyWellBlock.slice(s.getValue(X) - 1, s.getValue(Z) - 1);
    }

    @Override
    public RenderShape getRenderShape(BlockState s) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public void playerWillDestroy(Level l, BlockPos p, BlockState s, Player player) {
        if (!l.isClientSide) {
            BlockPos r = root(p, s);
            BlockState main = l.getBlockState(r);
            if (main.is(ModBlocks.ENTROPY_WELL.get()))
                l.destroyBlock(r, !player.isCreative() && player.hasCorrectToolForDrops(main), player);
        }
        super.playerWillDestroy(l, p, s, player);
    }

    @Override
    public void onRemove(BlockState s, Level l, BlockPos p, BlockState next, boolean moving) {
        if (!s.is(next.getBlock()) && !l.isClientSide) {
            BlockPos r = root(p, s);
            if (l.getBlockState(r).is(ModBlocks.ENTROPY_WELL.get())) l.destroyBlock(r, true);
        }
        super.onRemove(s, l, p, next, moving);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(ModBlocks.ENTROPY_WELL.get());
    }
}
