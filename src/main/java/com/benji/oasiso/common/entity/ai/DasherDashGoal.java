package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.common.entity.DasherEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class DasherDashGoal extends Goal {
    private final DasherEntity mob;
    private LivingEntity target;
    private int timer;
    private int dashCount;
    private Vec3 dashVector;
    private float lockedRot;

    public DasherDashGoal(DasherEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        this.target = this.mob.getTarget();
        return this.target != null && this.mob.dashCooldown <= 0 && this.mob.getAnimState() == 0 && this.mob.getRandom().nextInt(40) == 0;
    }

    @Override
    public void start() {
        this.dashCount = 0;
        this.timer = 0;
        this.mob.setAnimState(2);
    }

    @Override
    public boolean canContinueToUse() {
        return this.dashCount < 3 && this.target != null && this.target.isAlive() && this.mob.getAnimState() != 4;
    }

    @Override
    public void tick() {
        this.timer++;
        this.mob.getNavigation().stop();

        if (this.timer < 15) {
            this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);

            Vec3 dir = this.target.position().subtract(this.mob.position()).normalize();
            this.dashVector = dir.scale(10.0D);

            this.lockedRot = (float)(Math.atan2(dir.z, dir.x) * (180D / Math.PI)) - 90.0F;
            this.mob.setYRot(this.lockedRot);
            this.mob.yBodyRot = this.lockedRot;
        }
        else if (this.timer == 15) {
            this.mob.setDashing(true);
            this.mob.setDeltaMovement(this.dashVector.x, 0, this.dashVector.z);
            this.mob.setMaxUpStep(2.0F);
        }

        if (this.mob.isDashing()) {
            this.mob.setYRot(this.lockedRot);
            this.mob.yBodyRot = this.lockedRot;

            for (LivingEntity e : this.mob.level().getEntitiesOfClass(LivingEntity.class, this.mob.getBoundingBox().inflate(5.0D))) {
                if (e != this.mob && e instanceof Player player && !player.isCreative()) {
                    boolean hit = player.hurt(this.mob.damageSources().mobAttack(this.mob), (float) this.mob.getAttributeValue(Attributes.ATTACK_DAMAGE));

                    if (hit && this.mob.isMagnetic()) {
                        Vec3 kb = player.position().subtract(this.mob.position()).normalize().scale(1.5D);
                        player.setDeltaMovement(player.getDeltaMovement().add(kb.x, 0.1D, kb.z));
                        player.hasImpulse = true;
                    }
                }
            }
        }


        if (this.timer == 33) {
            this.mob.setDashing(false);
            this.mob.setDeltaMovement(0, this.mob.getDeltaMovement().y, 0);
            this.mob.setMaxUpStep(0.6F);
        }

        if (this.timer == 35) {
            this.mob.setAnimState(0);
        }

        if (this.timer >= 47) {
            this.dashCount++;
            if (this.dashCount < 3) {
                this.timer = 0;
                this.mob.setAnimState(2);
            }
        }
    }

    @Override
    public void stop() {
        this.mob.setDashing(false);
        this.mob.setMaxUpStep(0.6F);

        if (this.mob.getAnimState() == 2 || this.mob.getAnimState() == 0) {
            if (this.mob.getAnimState() != 4) {
                this.mob.setAnimState(0);
            }
        }
        this.mob.dashCooldown = 200;
    }
}