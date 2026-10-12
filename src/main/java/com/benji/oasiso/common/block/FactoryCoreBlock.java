package com.benji.oasiso.common.block;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.common.block.entity.FactoryCoreBlockEntity;
import com.benji.oasiso.registry.ModBlocks;
import net.minecraft.core.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.*;

public class FactoryCoreBlock extends BaseEntityBlock {
    public FactoryCoreBlock(Properties p) {
        super(p.pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) {
        BlockPos base = c.getClickedPos();
        Level level = c.getLevel();
        for (int y = 0; y < 3; y++)
            for (int x = -1; x <= 1; x++)
                for (int z = -1; z <= 1; z++) {
                    BlockPos p = base.offset(x, y, z);
                    if (level.isOutsideBuildHeight(p) || !level.hasChunkAt(p) || !level.getWorldBorder().isWithinBounds(p) || !level.getBlockState(p).canBeReplaced(c) || !level.getFluidState(p).isEmpty())
                        return null;
                    if (c.getPlayer() != null && !level.mayInteract(c.getPlayer(), p)) return null;
                }
        if (!level.isUnobstructed(null, Shapes.create(new AABB(base.getX() - .25, base.getY(), base.getZ() - .25, base.getX() + 1.25, base.getY() + 2.875, base.getZ() + 1.25))))
            return null;
        return defaultBlockState();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity who, ItemStack stack) {
        if (level.isClientSide) return;
        for (int y = 0; y < 3; y++)
            for (int x = -1; x <= 1; x++)
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    level.setBlock(pos.offset(x, y, z), ModBlocks.FACTORY_CORE_PART.get().defaultBlockState().setValue(FactoryCorePartBlock.X, x + 1).setValue(FactoryCorePartBlock.Y, y).setValue(FactoryCorePartBlock.Z, z + 1), 3);
                }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide) {
            for (int y = 0; y < 3; y++)
                for (int x = -1; x <= 1; x++)
                    for (int z = -1; z <= 1; z++) {
                        BlockPos p = pos.offset(x, y, z);
                        BlockState s = level.getBlockState(p);
                        if (s.getBlock() instanceof FactoryCorePartBlock && FactoryCorePartBlock.root(p, s).equals(pos))
                            level.removeBlock(p, false);
                    }
        }
        super.onRemove(state, level, pos, next, moving);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0) {
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, ModSounds.ENTROPY_LIGHTNING.get(), SoundSource.BLOCKS, 0.6F, 0.85F + random.nextFloat() * 0.3F, false);
        }
    }

    public static VoxelShape slice(int x, int y, int z) {
        return Shapes.box(Math.max(0, -.25 - x), 0, Math.max(0, -.25 - z), Math.min(1, 1.25 - x), Math.min(1, 2.875 - y), Math.min(1, 1.25 - z));
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return slice(0, 0, 0);
    }

    @Override
    public RenderShape getRenderShape(BlockState s) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return new FactoryCoreBlockEntity(p, s);
    }
}
