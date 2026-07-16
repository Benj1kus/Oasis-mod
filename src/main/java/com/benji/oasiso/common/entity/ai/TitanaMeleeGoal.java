package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.common.entity.TitanaEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.EnumSet;

public class TitanaMeleeGoal extends Goal {
    private final TitanaEntity mob;
    private LivingEntity target;
    private int attackAnimTimer;
    private int attackCooldown;

    public TitanaMeleeGoal(TitanaEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        this.target = this.mob.getTarget();
        return this.target != null && this.target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null && this.target.isAlive();
    }

    @Override
    public void start() {
        this.attackAnimTimer = 0;
        this.attackCooldown = 0;
    }

    @Override
    public void tick() {
        if (this.target == null) return;

        this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);

        double distSqr = this.mob.distanceToSqr(this.target);
        double reachSqr = (this.mob.getBbWidth() * 2.5F * this.mob.getBbWidth() * 2.5F) + this.target.getBbWidth();

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }

        if (this.attackAnimTimer > 0) {
            this.attackAnimTimer++;
            this.mob.getNavigation().stop();

            if (this.attackAnimTimer == 12) {
                if (distSqr <= reachSqr + 9.0D) {
                    boolean didHit = this.target.hurt(this.mob.damageSources().mobAttack(this.mob), (float) this.mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
                    this.mob.playSound(com.benji.oasiso.ModSounds.TITANA_ATTACK.get(), 1.0F, 1.0F);

                    if (didHit && !this.mob.level().isClientSide) {
                        createDenseShockwave();
                    }
                }
            }

            if (this.attackAnimTimer >= 40) {
                this.attackAnimTimer = 0;
                this.mob.setAnimState(0);
                this.attackCooldown = 15;
            }
        } else {
            this.mob.getNavigation().moveTo(this.target, 1.2D);

            if (distSqr <= reachSqr && this.attackCooldown <= 0) {
                this.attackAnimTimer = 1;
                this.mob.setAnimState(this.mob.getRandom().nextBoolean() ? 1 : 2);
            }
        }
    }

    private void createDenseShockwave() {
        if (this.mob.level() instanceof ServerLevel sl) {
            double px = this.target.getX();
            double py = this.target.getY() + 0.1;
            double pz = this.target.getZ();

            for (int i = 0; i < 360; i += 8) {
                double rad = Math.toRadians(i);
                double dx = Math.cos(rad) * 1.5;
                double dz = Math.sin(rad) * 1.5;
                sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, px + dx, py, pz + dz, 2, dx * 0.1, 0, dz * 0.1, 0.05);
            }
        }
    }

    @Override
    public void stop() {
        this.mob.setAnimState(0);
        this.target = null;
    }
}