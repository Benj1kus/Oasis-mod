package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.common.entity.MonkiEntity;
import com.benji.oasiso.common.entity.projectile.DesertBallEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class MonkiSpecialAttackGoal extends Goal {
    private final MonkiEntity mob;
    private LivingEntity target;
    private int timer;
    private int shotsFired;
    private int maxShots;

    public MonkiSpecialAttackGoal(MonkiEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.mob.hitCount >= 3 && this.mob.getTarget() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null && this.target.isAlive() && (this.mob.getAnimState() != 0);
    }

    @Override
    public void start() {
        this.target = this.mob.getTarget();
        this.mob.setAnimState(1);
        this.timer = 0;
        this.shotsFired = 0;
        this.maxShots = 2 + this.mob.getRandom().nextInt(2);
    }

    @Override
    public void stop() {
        this.mob.hitCount = 0;
        this.mob.setAnimState(0);
        this.target = null;
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.target == null) return;

        this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);

        this.mob.setYRot(this.mob.yHeadRot);
        this.mob.yBodyRot = this.mob.yHeadRot;

        double distSqr = this.mob.distanceToSqr(this.target);
        int state = this.mob.getAnimState();

        if (state == 1) {
            this.timer++;

            if (distSqr < 9.0) {
                Vec3 moveDir = this.mob.position().subtract(this.target.position()).normalize();
                this.mob.getMoveControl().setWantedPosition(this.mob.getX() + moveDir.x, this.mob.getY(), this.mob.getZ() + moveDir.z, 1.2D);
            } else if (distSqr > 36.0) {
                this.mob.getNavigation().moveTo(this.target, 1.2D);
            } else {
                double dx = this.mob.getX() - this.target.getX();
                double dz = this.mob.getZ() - this.target.getZ();
                double currentAngle = Math.atan2(dz, dx);

                double newAngle = currentAngle + 0.15;

                double radius = 4.5D;
                double targetX = this.target.getX() + radius * Math.cos(newAngle);
                double targetZ = this.target.getZ() + radius * Math.sin(newAngle);

                this.mob.getMoveControl().setWantedPosition(targetX, this.mob.getY(), targetZ, 1.2D);
            }

            if (this.timer >= (40 + this.mob.getRandom().nextInt(40))) {
                this.mob.setAnimState(2);
                this.timer = 0;
            }

        } else if (state == 2) {
            this.mob.getNavigation().stop();
            this.mob.setDeltaMovement(0, this.mob.getDeltaMovement().y, 0);

            this.timer++;

            if (this.timer == 15) {
                shootProjectile();
                this.shotsFired++;
            }

            if (this.timer >= 30) {
                if (this.shotsFired >= this.maxShots) {
                    this.stop();
                } else {
                    this.mob.setAnimState(1);
                    this.timer = 0;
                }
            }
        }
    }

    private void shootProjectile() {
        if (!this.mob.level().isClientSide()) {
            DesertBallEntity projectile = new DesertBallEntity(this.mob.level(), this.mob);

            double d0 = this.target.getEyeY() - (double)1.1F;
            double d1 = this.target.getX() - this.mob.getX();
            double d2 = d0 - projectile.getY();
            double d3 = this.target.getZ() - this.mob.getZ();
            double d4 = Math.sqrt(d1 * d1 + d3 * d3) * 0.2D;

            projectile.shoot(d1, d2 + d4, d3, 1.6F, 12.0F);
            this.mob.level().addFreshEntity(projectile);
        }
    }
}