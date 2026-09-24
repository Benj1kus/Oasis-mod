package com.benji.oasiso.common.block;

import com.benji.oasiso.common.block.entity.EntropyLanternBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class EntropyLanternBlock extends Block implements EntityBlock {
    public EntropyLanternBlock(Properties properties) {
        super(properties);
    }
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EntropyLanternBlockEntity(pos, state);
    }
}
