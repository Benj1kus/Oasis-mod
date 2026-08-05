package com.benji.oasiso.common.block;

import com.benji.oasiso.common.block.entity.LieBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class NephritisBlock extends Block {

    public static final int MIMIC_EFFECT_RADIUS = 40;
    private static final int RADIUS_SQR =
            MIMIC_EFFECT_RADIUS * MIMIC_EFFECT_RADIUS;

    public NephritisBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!level.isClientSide
                && state.getBlock() != oldState.getBlock()) {
            updateNearbyMimics(level, pos, true);
        }
    }

    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (!level.isClientSide
                && state.getBlock() != newState.getBlock()) {
            updateNearbyMimics(level, pos, false);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private void updateNearbyMimics(
            Level level,
            BlockPos nephritisPos,
            boolean placed
    ) {
        BlockPos.MutableBlockPos mutablePos =
                new BlockPos.MutableBlockPos();

        for (int x = -MIMIC_EFFECT_RADIUS;
             x <= MIMIC_EFFECT_RADIUS;
             x++) {

            for (int y = -MIMIC_EFFECT_RADIUS;
                 y <= MIMIC_EFFECT_RADIUS;
                 y++) {

                for (int z = -MIMIC_EFFECT_RADIUS;
                     z <= MIMIC_EFFECT_RADIUS;
                     z++) {

                    if (x * x + y * y + z * z > RADIUS_SQR) {
                        continue;
                    }

                    mutablePos.set(
                            nephritisPos.getX() + x,
                            nephritisPos.getY() + y,
                            nephritisPos.getZ() + z
                    );

                    BlockEntity blockEntity =
                            level.getBlockEntity(mutablePos);

                    if (!(blockEntity
                            instanceof LieBlockEntity lieBlockEntity)) {
                        continue;
                    }

                    if (placed) {
                        lieBlockEntity.considerNephritisSource(
                                nephritisPos
                        );
                    } else {
                        lieBlockEntity.onNephritisRemoved(
                                nephritisPos
                        );
                    }
                }
            }
        }
    }
}