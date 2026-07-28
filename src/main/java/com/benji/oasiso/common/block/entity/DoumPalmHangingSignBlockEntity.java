package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DoumPalmHangingSignBlockEntity extends HangingSignBlockEntity {
    public DoumPalmHangingSignBlockEntity(BlockPos pos, BlockState state) {
        super(Oasiso.DOUM_PALM_HANGING_SIGN_BE.get(), pos, state);
    }
}