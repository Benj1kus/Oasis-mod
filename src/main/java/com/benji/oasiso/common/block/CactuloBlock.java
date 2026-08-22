package com.benji.oasiso.common.block;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.CactoEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;

public class CactuloBlock extends FlowerBlock {

    public CactuloBlock(MobEffect effect, int duration, Properties properties) {
        super(effect, duration, properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.SAND);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        super.playerWillDestroy(level, pos, state, player);

        if (!level.isClientSide && !player.isCreative()) {

            CactoEntity cacto = Oasiso.CACTO.get().create(level);

            if (cacto != null) {
                cacto.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);

                cacto.setTarget(player);

                level.addFreshEntity(cacto);
            }
        }
    }
}