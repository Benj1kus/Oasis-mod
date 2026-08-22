package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.common.entity.KrombulEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class KrombulRandomFlyGoal extends Goal {

    private final KrombulEntity krombul;
    private final double speedModifier;

    private double wantedX;
    private double wantedY;
    private double wantedZ;

    private int remainingTicks;

    public KrombulRandomFlyGoal(KrombulEntity krombul, double speedModifier) {
        this.krombul = krombul;
        this.speedModifier = speedModifier;

        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.krombul.isTeleporting() || this.krombul.getTarget() != null || !this.krombul.getNavigation().isDone()) {
            return false;
        }

        if (this.krombul.getRandom().nextInt(30) != 0) {
            return false;
        }

        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = this.krombul.getRandom().nextDouble() * Math.PI * 2.0D;

            double distance = 2.0D + this.krombul.getRandom().nextDouble() * 6.0D;

            double x = this.krombul.getX() + Math.cos(angle) * distance;
            double z = this.krombul.getZ() + Math.sin(angle) * distance;
            double y = this.krombul.findHoverY(x, z);


            if (Math.abs(y - this.krombul.getY()) > 5.0D) {
                continue;
            }

            this.wantedX = x;
            this.wantedY = y;
            this.wantedZ = z;

            return true;
        }

        return false;
    }

    @Override
    public void start() {
        this.remainingTicks = 100;

        this.krombul.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, this.speedModifier);
    }

    @Override
    public boolean canContinueToUse() {
        return this.remainingTicks > 0 && !this.krombul.getNavigation().isDone() && !this.krombul.isTeleporting() && this.krombul.getTarget() == null;
    }

    @Override
    public void tick() {
        this.remainingTicks--;
    }

    @Override
    public void stop() {
        this.remainingTicks = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}