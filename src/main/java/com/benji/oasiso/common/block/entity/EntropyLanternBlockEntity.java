package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class EntropyLanternBlockEntity extends BlockEntity {
    public EntropyLanternBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENTROPY_LANTERN_BE.get(), pos, state);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(2.7D);
    }
}
