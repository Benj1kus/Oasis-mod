package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.common.entity.DasherEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class DasherKeepDistanceGoal extends Goal {
    private final DasherEntity mob;

    public DasherKeepDistanceGoal(DasherEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        return target != null && this.mob.getAnimState() == 0 && this.mob.distanceToSqr(target) > 100.0D;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target != null) {
            this.mob.getNavigation().moveTo(target, 1.0D);
        }
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
    }
}