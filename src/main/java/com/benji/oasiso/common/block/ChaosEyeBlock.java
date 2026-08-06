package com.benji.oasiso.common.block;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ChaosEyeBlock extends Block {

    public ChaosEyeBlock(
            Properties properties
    ) {
        super(properties);
    }

    @Override
    public void tick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {
        if (!state.is(this)) {
            return;
        }
        
        if (!level.removeBlock(pos, false)) {
            return;
        }

        level.sendParticles(
                Oasiso.GOLDEN_STARS.get(),
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                18,
                0.38D,
                0.38D,
                0.38D,
                0.055D
        );
    }
}