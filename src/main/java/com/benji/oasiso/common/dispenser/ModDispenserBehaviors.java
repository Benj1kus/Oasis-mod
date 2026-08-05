package com.benji.oasiso.common.dispenser;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DispenserBlock;

public final class ModDispenserBehaviors {

    private ModDispenserBehaviors() {
    }

    public static void register() {
        DispenserBlock.registerBehavior(
                Items.BUCKET,
                new BallCactusDispenserBehavior()
        );
    }
}