package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.common.entity.TitanaEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class TitanaSandHandGoal extends Goal {
    private final TitanaEntity mob;
    private int timer;

    public TitanaSandHandGoal(TitanaEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        return target != null && this.mob.handCooldown <= 0 && this.mob.getModelState() == 0 && this.mob.distanceToSqr(target) < 144.0D;
    }

    @Override
    public boolean canContinueToUse() {
        return this.timer < 30;
    }

    @Override
    public void start() {
        this.timer = 0;
        this.mob.setAnimState(3); // sand_attack
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.timer++;
        this.mob.spawnLeftParticles();
    }

    @Override
    public void stop() {
        this.mob.setAnimState(0);
        this.mob.setModelState(1);

        this.mob.handStrikesLeft = 3;
        this.mob.nextStrikeTimer = 10;
        this.mob.handCooldown = 800;
    }
}