package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.common.entity.MonkiBigEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class MonkiBigTornadoGoal extends Goal {
    private final MonkiBigEntity mob;
    private LivingEntity target;
    private Vec3 moveDir;
    private float lockedRot;
    private int timer;
    private int cooldown;

    public MonkiBigTornadoGoal(MonkiBigEntity mob) {
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
        return this.target != null && this.mob.getRandom().nextInt(40) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.timer < 80;
    }

    @Override
    public void start() {
        this.mob.setAnimState(1);
        this.timer = 0;

        Vec3 diff = this.target.position().subtract(this.mob.position());

        this.moveDir = diff.normalize().scale(0.6D);

        this.lockedRot = (float)(Math.atan2(diff.z, diff.x) * (180D / Math.PI)) - 90.0F;
        this.mob.setYRot(this.lockedRot);
        this.mob.yBodyRot = this.lockedRot;
        this.mob.yHeadRot = this.lockedRot;
    }

    @Override
    public void tick() {
        this.timer++;

        this.mob.setYRot(this.lockedRot);
        this.mob.yBodyRot = this.lockedRot;
        this.mob.yHeadRot = this.lockedRot;

        if (this.timer > 40) {
            this.moveDir = this.moveDir.scale(0.85D);
        }

        this.mob.setDeltaMovement(this.moveDir.x, this.mob.getDeltaMovement().y, this.moveDir.z);
    }

    @Override
    public void stop() {
        this.mob.setAnimState(0);

        this.mob.setDeltaMovement(0, this.mob.getDeltaMovement().y, 0);

        this.mob.incrementAttackCount();
        this.cooldown = 40 + this.mob.getRandom().nextInt(40);

        if (this.mob.getAttackCount() >= 3) {
            this.mob.splitBack();
        }
    }
}