package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.common.entity.SandGolemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class SandGolemAttackGoal extends Goal {
    private final SandGolemEntity golem;
    private int attackTimer;
    private long lastAttackTime;

    public SandGolemAttackGoal(SandGolemEntity golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.golem.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        this.attackTimer = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = this.golem.getTarget();
        if (target == null) return;

        this.golem.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (this.attackTimer > 0) {
            this.attackTimer--;
            this.golem.getNavigation().stop();

            if (this.attackTimer == 20) {
                this.golem.performAoEAttack();
            }

            if (this.attackTimer == 0) {
                this.lastAttackTime = this.golem.level().getGameTime();
            }
        }
        else {
            double distanceToTarget = this.golem.distanceToSqr(target);
            boolean isCooldownOver = this.golem.level().getGameTime() >= this.lastAttackTime + 20;

            if (distanceToTarget <= 16.0D && isCooldownOver) {
                this.attackTimer = 25;
                this.golem.getNavigation().stop();
                this.golem.triggerAnim("action_controller", "attack");
            }
            else {
                this.golem.getNavigation().moveTo(target, 1.0D);
            }
        }
    }

    @Override
    public void stop() {
        this.attackTimer = 0;
        this.golem.getNavigation().stop();
    }
}