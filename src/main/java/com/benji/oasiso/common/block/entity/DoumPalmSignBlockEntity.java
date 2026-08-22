package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DoumPalmSignBlockEntity extends SignBlockEntity {
    public DoumPalmSignBlockEntity(BlockPos pos, BlockState state) {
        super(Oasiso.DOUM_PALM_SIGN_BE.get(), pos, state);
    }
}