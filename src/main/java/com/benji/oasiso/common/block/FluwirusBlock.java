package com.benji.oasiso.common.block;

import com.benji.oasiso.common.block.entity.FluwirusBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiFunction;

public class FluwirusBlock extends BaseEntityBlock {
    private final BiFunction<BlockPos, BlockState, BlockEntity> blockEntityProvider;

    public FluwirusBlock(Properties properties, BiFunction<BlockPos, BlockState, BlockEntity> blockEntityProvider) {
        super(properties);
        this.blockEntityProvider = blockEntityProvider;
    }

    public FluwirusBlock(Properties properties) {
        super(properties);
        this.blockEntityProvider = FluwirusBlockEntity::new;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return blockEntityProvider.apply(pos, state);
    }
}