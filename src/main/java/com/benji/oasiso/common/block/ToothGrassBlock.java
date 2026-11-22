package com.benji.oasiso.common.block;

import net.minecraft.world.level.block.state.StateDefinition;
import com.benji.oasiso.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ToothGrassBlock extends Block implements BonemealableBlock {
    public ToothGrassBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FeralDrying.DRY, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FeralDrying.DRY);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean client) {
        return level.isEmptyBlock(pos.above());
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos origin = pos.above();
        for (int attempt = 0; attempt < 128; attempt++) {
            BlockPos target = origin;
            boolean reachable = true;

            for (int step = 0; step < attempt / 16; step++) {
                int dx = random.nextInt(3) - 1;
                int dy = (random.nextInt(3) - 1) * random.nextInt(3) / 2;
                int dz = random.nextInt(3) - 1;
                target = target.offset(dx, dy, dz);

                if (!level.getBlockState(target.below()).is(this) || level.getBlockState(target).isCollisionShapeFullBlock(level, target)) {
                    reachable = false;
                    break;
                }
            }

            if (!reachable || !level.isEmptyBlock(target)) continue;

            Block plant = switch (random.nextInt(10)) {
                case 0, 1, 2, 3 -> ModBlocks.TOOTH_BUSH.get();
                case 4, 5, 6, 7 -> ModBlocks.TOOTH_SMALLGRASS.get();
                case 8 -> ModBlocks.TOOTHY_PLANT.get();
                default -> ModBlocks.TONGUE_PLANT.get();
            };

            BlockState sprout = plant.defaultBlockState();
            if (sprout.canSurvive(level, target)) {
                level.setBlockAndUpdate(target, sprout);
            }
        }
    }
}
