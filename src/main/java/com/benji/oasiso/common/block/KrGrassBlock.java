package com.benji.oasiso.common.block;

import com.benji.oasiso.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;

public class KrGrassBlock extends Block implements BonemealableBlock {

    public KrGrassBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean isClient) {
        return level.getBlockState(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos basePos = pos.above();

        for (int i = 0; i < 16; i++) {
            BlockPos targetPos = basePos.offset(random.nextInt(3) - random.nextInt(3), random.nextInt(2) - random.nextInt(2), random.nextInt(3) - random.nextInt(3));

            if (level.getBlockState(targetPos).isAir() && level.getBlockState(targetPos.below()).is(this)) {

                boolean isBig = random.nextFloat() < 0.25F;

                if (isBig) {
                    BlockPos upperPos = targetPos.above();
                    if (level.getBlockState(upperPos).isAir()) {
                        DoublePlantBlock.placeAt(level, ModBlocks.KR_BIGGRASS.get().defaultBlockState(), targetPos, Block.UPDATE_ALL);
                    }
                }
            } else {
                BlockState smallState = ModBlocks.KR_SMALLGRASS.get().defaultBlockState();
                if (smallState.canSurvive(level, targetPos)) {
                    level.setBlock(targetPos, smallState, Block.UPDATE_ALL);
                }
            }
        }
    }
}