package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.CrusaderTankEntity;
import com.benji.oasiso.common.entity.EyelidEntity;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;


public final class CrusaderTankCombatGoal extends Goal {

    private static final int MELEE_DURATION = 25;
    private static final int MELEE_DAMAGE_TICK = 18;
    private static final double MELEE_DAMAGE_RADIUS = 6.0D;

    private static final int LONG_DURATION = 55;
    private static final int CANNON_SHOT_TICK = 35;
    private static final double LONG_ATTACK_RANGE = 30.0D;

    private static final int MELEE_COOLDOWN = 20;
    private static final int LONG_COOLDOWN = 50;

    private static final double CHASE_SPEED = 1.0D;

    private final CrusaderTankEntity tank;
    private AttackPhase phase = AttackPhase.NONE;

    private int attackTick;
    private int attackCooldown;

    private ServerPlayer attackTarget;

    public CrusaderTankCombatGoal(CrusaderTankEntity tank) {
        this.tank = tank;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.phase != AttackPhase.NONE) {
            return true;
        }
        return getCurrentTarget() != null;
    }


    @Override
    public boolean canContinueToUse() {
        if (this.phase != AttackPhase.NONE) {
            return true;
        }
        return getCurrentTarget() != null;
    }


    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }


    @Override
    public void stop() {
        this.phase = AttackPhase.NONE;

        this.attackTick = 0;
        this.attackTarget = null;

        this.tank.setAnimState(CrusaderTankEntity.STATE_IDLE);
        this.tank.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (!(this.tank.level() instanceof ServerLevel level)) {
            return;
        }

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }

        if (this.phase != AttackPhase.NONE) {
            tickAttack(level);
            return;
        }

        ServerPlayer target = getCurrentTarget();
        if (target == null) {
            return;
        }

        lookAt(target);
        double distanceSqr = this.tank.distanceToSqr(target);

        if (this.attackCooldown > 0) {
            chaseTarget(target);
            return;
        }
        // melee
        if (distanceSqr <= MELEE_DAMAGE_RADIUS * MELEE_DAMAGE_RADIUS) {
            startMelee(target);
            return;
        }
//cannon
        if (distanceSqr <= LONG_ATTACK_RANGE * LONG_ATTACK_RANGE && this.tank.getSensing().hasLineOfSight(target)) {
            startLongAttack(target);
            return;
        }
        chaseTarget(target);
    }

    private void chaseTarget(ServerPlayer target) {
        this.tank.getNavigation().moveTo(target, CHASE_SPEED);
    }

    private void lookAt(ServerPlayer target) {
        this.tank.getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    private void startMelee(ServerPlayer target) {
        this.phase = AttackPhase.MELEE;

        this.attackTick = 0;
        this.attackTarget = target;

        this.tank.getNavigation().stop();
        this.tank.setDeltaMovement(Vec3.ZERO);
        this.tank.setAnimState(CrusaderTankEntity.STATE_ATTACK_MELEE);

        lookAt(target);
    }


    private void startLongAttack(ServerPlayer target) {
        this.phase = AttackPhase.LONG;

        this.attackTick = 0;
        this.attackTarget = target;

        this.tank.getNavigation().stop();
        this.tank.setDeltaMovement(Vec3.ZERO);
        this.tank.setAnimState(CrusaderTankEntity.STATE_ATTACK_LONG);

        lookAt(target);
    }

    private void tickAttack(ServerLevel level) {
        this.attackTick++;

        this.tank.getNavigation().stop();
        this.tank.setDeltaMovement(Vec3.ZERO);

        if (isValidTarget(this.attackTarget)) {
            lookAt(this.attackTarget);
        }

        switch (this.phase) {
            case MELEE -> tickMelee(level);
            case LONG -> tickLongAttack(level);
            case NONE -> {
            }
        }
    }

    private void tickMelee(ServerLevel level) {
        if (this.attackTick == MELEE_DAMAGE_TICK) {
            dealAreaDamage(level);
        }
        if (this.attackTick >= MELEE_DURATION) {
            finishAttack(MELEE_COOLDOWN);
        }
    }

    private void dealAreaDamage(ServerLevel level) {
        AABB area = this.tank.getBoundingBox().inflate(MELEE_DAMAGE_RADIUS);
        float damage = (float) this.tank.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double radiusSqr = MELEE_DAMAGE_RADIUS * MELEE_DAMAGE_RADIUS;

        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area, this::isValidTarget)) {
            if (this.tank.distanceToSqr(player) > radiusSqr) {
                continue;
            }
            player.hurt(this.tank.damageSources().mobAttack(this.tank), damage);
        }
    }

    private void tickLongAttack(ServerLevel level) {
        if (this.attackTick == CANNON_SHOT_TICK) {
            fireCannon(level);
        }
        if (this.attackTick >= LONG_DURATION) {
            finishAttack(LONG_COOLDOWN);
        }
    }


    private void fireCannon(ServerLevel level) {
        if (!isValidTarget(this.attackTarget)) {
            return;
        }
        Vec3 muzzle = this.tank.getCannonMuzzlePosition();
        Vec3 targetCenter = new Vec3(this.attackTarget.getX(), this.attackTarget.getY() + this.attackTarget.getBbHeight() * 0.55D, this.attackTarget.getZ());
        Vec3 direction = targetCenter.subtract(muzzle);

        if (direction.lengthSqr() < 0.0001D) {
            direction = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            direction = direction.normalize();
        }
        spawnCannonParticles(level, muzzle, direction);

        level.playSound(null, muzzle.x, muzzle.y, muzzle.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.35F, 0.92F);

        EyelidEntity projectile = Oasiso.EYELID.get().create(level);
        if (projectile == null) {
            return;
        }
        projectile.initializeTankShot(this.tank, this.attackTarget, muzzle);
        level.addFreshEntity(projectile);
    }

    private void spawnCannonParticles(ServerLevel level, Vec3 muzzle, Vec3 direction) {
        spawnDirectionalParticles(level, Oasiso.CHAOS_BOMB_CENTER_SMOKE.get(), muzzle, direction, 5, 0.04D, 0.11D, 0.18D);

        spawnDirectionalParticles(level, Oasiso.CHAOS_BOMB_FIRE_SMOKE.get(), muzzle, direction, 14, 0.08D, 0.24D, 0.30D);

        spawnDirectionalParticles(level, Oasiso.CHAOS_BOMB_SPARKS.get(), muzzle, direction, 28, 0.20D, 0.55D, 0.48D);
    }


    private void spawnDirectionalParticles(ServerLevel level, ParticleOptions particle, Vec3 origin, Vec3 direction, int count, double minSpeed, double maxSpeed, double spread) {
        for (int i = 0; i < count; i++) {

            Vec3 randomized = direction.add((this.tank.getRandom().nextDouble() - 0.5D) * spread, (this.tank.getRandom().nextDouble() - 0.5D) * spread, (this.tank.getRandom().nextDouble() - 0.5D) * spread);

            if (randomized.lengthSqr() < 0.0001D) {
                randomized = direction;
            }

            double speed = Mth.lerp(this.tank.getRandom().nextDouble(), minSpeed, maxSpeed);
            Vec3 velocity = randomized.normalize().scale(speed);
            level.sendParticles(particle, origin.x, origin.y, origin.z, 0, velocity.x, velocity.y, velocity.z, 1.0D);
        }
    }

    private void finishAttack(int cooldown) {
        this.phase = AttackPhase.NONE;

        this.attackTick = 0;
        this.attackTarget = null;
        this.attackCooldown = cooldown;

        this.tank.setAnimState(CrusaderTankEntity.STATE_IDLE);
        this.tank.setDeltaMovement(Vec3.ZERO);
    }

    private ServerPlayer getCurrentTarget() {
        if (!(this.tank.getTarget() instanceof ServerPlayer player)) {
            return null;
        }
        return isValidTarget(player) ? player : null;
    }

    private boolean isValidTarget(ServerPlayer player) {
        return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator() && player.level() == this.tank.level();
    }

    private enum AttackPhase {
        NONE, MELEE, LONG
    }
}