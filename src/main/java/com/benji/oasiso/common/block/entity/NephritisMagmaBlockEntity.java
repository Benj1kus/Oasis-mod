package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class NephritisMagmaBlockEntity extends BlockEntity {
    public NephritisMagmaBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NEPHRITIS_MAGMA_BE.get(), pos, state);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1.2, 0, 1.2).expandTowards(0, 9, 0);
    }
}
