package com.benji.oasiso.common.block;

import com.benji.oasiso.common.block.entity.WindmillBlockEntity;
import com.benji.oasiso.registry.ModBlocks;
import net.minecraft.core.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.*;

public class WindmillBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public WindmillBlock(Properties p) {
        super(p.pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) {
        BlockPos base = c.getClickedPos();
        Level level = c.getLevel();
        for (int y = 0; y < 7; y++) {
            BlockPos p = base.above(y);
            if (level.isOutsideBuildHeight(p) || !level.hasChunkAt(p) || !level.getWorldBorder().isWithinBounds(p) || !level.getBlockState(p).canBeReplaced(c) || !level.getFluidState(p).isEmpty())
                return null;
            if (c.getPlayer() != null && !level.mayInteract(c.getPlayer(), p)) return null;
        }
        if (!level.isUnobstructed(null, Shapes.create(new AABB(base.getX() + .1875, base.getY(), base.getZ() + .1875, base.getX() + .8125, base.getY() + 6.875, base.getZ() + .8125))))
            return null;
        return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity who, ItemStack stack) {
        if (level.isClientSide) return;
        for (int y = 0; y < 7; y++) {
            if (y == 0) continue;
            level.setBlock(pos.above(y), ModBlocks.WINDMILL_PART.get().defaultBlockState().setValue(WindmillPartBlock.PART, y), 3);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide) {
            for (int y = 0; y < 7; y++) {
                BlockPos p = pos.above(y);
                BlockState s = level.getBlockState(p);
                if (s.getBlock() instanceof WindmillPartBlock && WindmillPartBlock.root(p, s).equals(pos))
                    level.removeBlock(p, false);
            }
        }
        super.onRemove(state, level, pos, next, moving);
    }

    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 16, 13);

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState s) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return new WindmillBlockEntity(p, s);
    }

    @Override
    public BlockState rotate(BlockState s, Rotation r) {
        return s.setValue(FACING, r.rotate(s.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState s, Mirror m) {
        return s.rotate(m.getRotation(s.getValue(FACING)));
    }
}
