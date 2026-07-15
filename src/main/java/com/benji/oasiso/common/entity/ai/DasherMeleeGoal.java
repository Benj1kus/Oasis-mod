package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.common.entity.DasherEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class DasherMeleeGoal extends Goal {
    private final DasherEntity mob;
    private LivingEntity target;
    private int attackTimer;
    private int cooldown = 0;

    public DasherMeleeGoal(DasherEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        this.target = this.mob.getTarget();
        return this.target != null && !this.mob.isMagnetic() && this.mob.distanceToSqr(this.target) <= 16.0D && this.mob.getAnimState() == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.attackTimer < 30 && this.mob.getAnimState() == 1;
    }

    @Override
    public void start() {
        this.attackTimer = 0;
        this.mob.setAnimState(1);
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.attackTimer++;
        if (this.target != null) {
            this.mob.getLookControl().setLookAt(this.target, 30F, 30F);

            if (this.attackTimer == 8 && this.mob.distanceToSqr(this.target) <= 25.0D) {
                this.target.hurt(this.mob.damageSources().mobAttack(this.mob), (float) this.mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
            }
        }
    }

    @Override
    public void stop() {
        if (this.mob.getAnimState() == 1) {
            this.mob.setAnimState(0);
        }
        this.target = null;
        this.cooldown = 20;
    }
}