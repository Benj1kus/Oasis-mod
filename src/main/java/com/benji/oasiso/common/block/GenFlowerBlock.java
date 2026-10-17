package com.benji.oasiso.common.block;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class GenFlowerBlock extends FlowerBlock {

    public GenFlowerBlock(MobEffect effect, int duration, Properties properties) {
        super(effect, duration, properties);
    }

    private static boolean maybePlaced (BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(ModBlocks.KR_GRASS.get());
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return maybePlaced(state);
    }
}