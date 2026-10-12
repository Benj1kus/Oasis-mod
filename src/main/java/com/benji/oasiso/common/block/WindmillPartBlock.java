package com.benji.oasiso.common.block;

import com.benji.oasiso.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.*;

public final class WindmillPartBlock extends Block {
    public static final IntegerProperty PART = IntegerProperty.create("part", 1, 6);
    private static final VoxelShape FULL = Block.box(3, 0, 3, 13, 16, 13);
    private static final VoxelShape TOP = Block.box(3, 0, 3, 13, 14, 13);

    public WindmillPartBlock(Properties p) {
        super(p.pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(PART, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(PART);
    }

    public static BlockPos root(BlockPos pos, BlockState state) {
        return pos.below(state.getValue(PART));
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return s.getValue(PART) == 6 ? TOP : FULL;
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
            if (main.is(ModBlocks.WINDMILL.get()))
                l.destroyBlock(r, !player.isCreative() && player.hasCorrectToolForDrops(main), player);
        }
        super.playerWillDestroy(l, p, s, player);
    }

    @Override
    public void onRemove(BlockState s, Level l, BlockPos p, BlockState next, boolean moving) {
        if (!s.is(next.getBlock()) && !l.isClientSide) {
            BlockPos r = root(p, s);
            if (l.getBlockState(r).is(ModBlocks.WINDMILL.get())) l.destroyBlock(r, true);
        }
        super.onRemove(s, l, p, next, moving);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(ModBlocks.WINDMILL.get());
    }
}
