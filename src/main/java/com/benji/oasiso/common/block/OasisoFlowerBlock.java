package com.benji.oasiso.common.block;

import com.benji.oasiso.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Random;

public class OasisoFlowerBlock extends FlowerBlock {
    private static final float FLOWERY_SECRET_CHANCE = 0.3F;
    private static final List<net.minecraft.sounds.SoundEvent> FLOWERY_SOUNDS = List.of(
            ModSounds.FLOWERY1.get(),
            ModSounds.FLOWERY2.get(),
            ModSounds.FLOWERY3.get(),
            ModSounds.FLOWERY4.get(),
            ModSounds.FLOWERY5.get(),
            ModSounds.FLOWERY6.get(),
            ModSounds.FLOWERY7.get(),
            ModSounds.FLOWERY8.get(),
            ModSounds.FLOWERY9.get(),
            ModSounds.FLOWERY10.get(),
            ModSounds.FLOWERY11.get(),
            ModSounds.FLOWERY12.get(),
            ModSounds.FLOWERY13.get()
    );

    public OasisoFlowerBlock(MobEffect effect, int duration, Properties properties) {
        super(effect, duration, properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.GRASS_BLOCK);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              net.minecraft.world.level.block.entity.BlockEntity blockEntity,
                              net.minecraft.world.item.ItemStack tool) {

        super.playerDestroy(level, player, pos, state, blockEntity, tool);

        if (!level.isClientSide && level.random.nextFloat() < FLOWERY_SECRET_CHANCE) {

            net.minecraft.sounds.SoundEvent sound =
                    FLOWERY_SOUNDS.get(level.random.nextInt(FLOWERY_SOUNDS.size()));

            level.playSound(
                    null,
                    pos,
                    sound,
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    1.0F,
                    1.0F
            );
        }
    }
}