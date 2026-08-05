package com.benji.oasiso.common.dispenser;

import com.benji.oasiso.common.block.BallCactusBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class BallCactusDispenserBehavior
        extends DefaultDispenseItemBehavior {

    @Override
    protected ItemStack execute(
            BlockSource source,
            ItemStack bucketStack
    ) {
        ServerLevel level =
                source.getLevel();

        Direction facing =
                source.getBlockState()
                        .getValue(DispenserBlock.FACING);

        BlockPos cactusPos =
                source.getPos().relative(facing);

        BlockState cactusState =
                level.getBlockState(cactusPos);

        if (!(cactusState.getBlock()
                instanceof BallCactusBlock)) {
            return super.execute(
                    source,
                    bucketStack
            );
        }

        int currentState =
                cactusState.getValue(
                        BallCactusBlock.STATE
                );

        ItemStack filledBucket;

        if (currentState == 3) {
            filledBucket =
                    new ItemStack(
                            Items.WATER_BUCKET
                    );
        } else if (currentState == 4) {
            filledBucket =
                    new ItemStack(
                            Items.MILK_BUCKET
                    );
        } else {

            return super.execute(
                    source,
                    bucketStack
            );
        }


        level.setBlock(
                cactusPos,
                cactusState.setValue(
                        BallCactusBlock.STATE,
                        2
                ),
                3
        );

        level.playSound(
                null,
                cactusPos,
                SoundEvents.BUCKET_FILL,
                SoundSource.BLOCKS,
                1.0F,
                currentState == 4
                        ? 0.9F
                        : 1.0F
        );


        bucketStack.shrink(1);


        super.execute(
                source,
                filledBucket
        );

        return bucketStack;
    }
}