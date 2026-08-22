package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.CrusaderAssasinEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;


public final class CrusaderAssassinCombatGoal extends Goal {


    private static final double CHASE_SPEED = 1.0D;

    private static final double ATTACK_RANGE = 2.25D;
    private static final int ATTACK_COOLDOWN = 20;
    private static final int ATTACK_LOCK_TICKS = 14;

    private static final int INVIS_ANIMATION_TICKS = 12;

    private static final int MIN_INVISIBLE_WAIT = 50;
    private static final int RANDOM_INVISIBLE_WAIT = 90;

    private static final int MIN_INVIS_COOLDOWN = 120;
    private static final int RANDOM_INVIS_COOLDOWN = 100;

    private static final int INVIS_PARTICLE_COUNT = 45;

    private final CrusaderAssasinEntity assassin;


    private int attackCooldown;
    private int attackLockTicks;
    private int invisAnimationTicks;
    private int invisibleWaitTicks;
    private int invisCooldown;
    private boolean playingInvisAnimation;


    public CrusaderAssassinCombatGoal(CrusaderAssasinEntity assassin) {
        this.assassin = assassin;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.invisCooldown = randomInvisibilityCooldown();
    }

    @Override
    public boolean canUse() {
        return getTarget() != null;
    }


    @Override
    public boolean canContinueToUse() {
        return getTarget() != null;
    }


    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }


    @Override
    public void stop() {
        this.assassin.getNavigation().stop();
        this.playingInvisAnimation = false;
        this.invisAnimationTicks = 0;
        this.invisibleWaitTicks = 0;
        this.attackLockTicks = 0;
        this.assassin.setInvisible(false);
        this.assassin.finishAction();
    }

    @Override
    public void tick() {
        if (!(this.assassin.level() instanceof ServerLevel level)) {
            return;
        }

        ServerPlayer target = getTarget();
        if (target == null) {
            return;
        }
        lookAt(target);

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        if (this.invisCooldown > 0 && !this.assassin.isInvisible() && !this.playingInvisAnimation) {
            this.invisCooldown--;
        }

        if (this.attackLockTicks > 0) {
            this.attackLockTicks--;
            this.assassin.getNavigation().stop();
            this.assassin.setDeltaMovement(Vec3.ZERO);
            if (this.attackLockTicks <= 0) {
                this.assassin.finishAction();
            }
            return;
        }

        if (this.playingInvisAnimation) {
            tickInvisibilityAnimation(level);
            return;
        }

        if (this.assassin.isInvisible()) {
            tickInvisible(target);
            return;
        }
        tickVisible(target);
    }

    private void tickVisible(ServerPlayer target) {
        double distanceSqr = this.assassin.distanceToSqr(target);
        if (distanceSqr <= ATTACK_RANGE * ATTACK_RANGE && this.attackCooldown <= 0) {
            performVisibleAttack(target);
            return;
        }
        if (this.invisCooldown <= 0) {
            beginInvisibility();
            return;
        }
        chase(target);
    }
    private void performVisibleAttack(ServerPlayer target) {
        this.assassin.getNavigation().stop();
        this.assassin.setDeltaMovement(Vec3.ZERO);
        this.assassin.playAttackState();
        this.assassin.doHurtTarget(target);

        this.attackCooldown = ATTACK_COOLDOWN;
        this.attackLockTicks = ATTACK_LOCK_TICKS;
    }

    private void beginInvisibility() {

        this.playingInvisAnimation = true;
        this.invisAnimationTicks = INVIS_ANIMATION_TICKS;

        this.assassin.getNavigation().stop();
        this.assassin.setDeltaMovement(Vec3.ZERO);
        this.assassin.setInvisible(false);
        this.assassin.playInvisibilityAnimation();
    }

    private void tickInvisibilityAnimation(ServerLevel level) {
        this.assassin.getNavigation().stop();
        this.assassin.setDeltaMovement(Vec3.ZERO);

        this.invisAnimationTicks--;
        if (this.invisAnimationTicks > 0) {
            return;
        }

        this.playingInvisAnimation = false;
        this.assassin.finishAction();
        this.assassin.setInvisible(true);

        spawnInvisibilityParticles(level);
        this.invisibleWaitTicks = MIN_INVISIBLE_WAIT + this.assassin.getRandom().nextInt(RANDOM_INVISIBLE_WAIT);
    }

    private void tickInvisible(ServerPlayer target) {

        if (this.invisibleWaitTicks > 0) {
            this.invisibleWaitTicks--;
            chase(target);
            return;
        }
        double distanceSqr = this.assassin.distanceToSqr(target);

        if (distanceSqr > ATTACK_RANGE * ATTACK_RANGE) {
            chase(target);
            return;
        }
        performAmbushAttack(target);
    }

    private void performAmbushAttack(ServerPlayer target) {
        this.assassin.getNavigation().stop();
        this.assassin.setDeltaMovement(Vec3.ZERO);
        this.assassin.setInvisible(false);
        this.assassin.playAttackState();
        this.assassin.doHurtTarget(target);

        this.attackCooldown = ATTACK_COOLDOWN;
        this.attackLockTicks = ATTACK_LOCK_TICKS;

        this.invisCooldown = randomInvisibilityCooldown();
        this.invisibleWaitTicks = 0;
    }

    private void chase(ServerPlayer target) {
        this.assassin.getNavigation().moveTo(target, CHASE_SPEED);
    }


    private void lookAt(ServerPlayer target) {
        this.assassin.getLookControl().setLookAt(target, 35.0F, 35.0F);
    }

    private void spawnInvisibilityParticles(ServerLevel level) {
        level.sendParticles(Oasiso.PURPLE_STARS.get(), this.assassin.getX(), this.assassin.getY() + this.assassin.getBbHeight() * 0.5D, this.assassin.getZ(), INVIS_PARTICLE_COUNT, Math.max(0.35D, this.assassin.getBbWidth() * 0.65D), this.assassin.getBbHeight() * 0.55D, Math.max(0.35D, this.assassin.getBbWidth() * 0.65D), 0.075D);
    }

    private ServerPlayer getTarget() {
        if (!(this.assassin.getTarget() instanceof ServerPlayer player)) {
            return null;
        }
        if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
            return null;
        }
        return player;
    }

    private int randomInvisibilityCooldown() {
        return MIN_INVIS_COOLDOWN + this.assassin.getRandom().nextInt(RANDOM_INVIS_COOLDOWN);
    }
}