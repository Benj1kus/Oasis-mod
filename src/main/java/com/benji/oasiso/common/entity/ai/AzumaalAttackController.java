package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.config.OsirisRealmConfig;

import com.benji.oasiso.common.entity.AzumaalEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.benji.oasiso.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.BattleHintArrowEntity;
import com.benji.oasiso.common.entity.CircleHintEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AzumaalAttackController {

    private static final int AIR_HOLD_PARTICLE_INTERVAL = 3;


    private static final String DATA_TAG = "AzumaalAttackController";

    private final AzumaalCloneManager cloneManager;
    private final AzumaalEntity boss;
    private final AzumaalDefenseManager defenseManager;
    private final AzumaalParkourManager parkourManager;
    private final AzumaalStunManager stunManager;
    private AttackPhase phase = AttackPhase.NONE;

    private int attackTick;
    private int attackCooldown = OsirisRealmConfig.AZUMAAL_POST_SPAWN_ATTACK_COOLDOWN.get();

    private UUID targetId;
    private UUID lastTargetId;

    private Vec3 prepareStep = Vec3.ZERO;
    private int prepareTicksRemaining;
    private Vec3 dashLanding;
    private int dashTicksRemaining;
    private int airChaseTicksRemaining;
    private int returnTicksRemaining;


    private boolean throwConnected;
    private boolean summonRadialPattern;
    private boolean summonMegaScatteredPattern;

    public AzumaalAttackController(AzumaalEntity boss) {
        this.parkourManager = new AzumaalParkourManager(boss);
        this.defenseManager = new AzumaalDefenseManager(boss);
        this.stunManager = new AzumaalStunManager();
        this.cloneManager = new AzumaalCloneManager(boss);

        this.boss = boss;
    }


    private float getAttackDamage(float multiplier) {
        return (float) boss.getAttributeValue(
                Attributes.ATTACK_DAMAGE
        ) * multiplier;
    }

    public void reset() {
        this.phase = AttackPhase.NONE;

        this.parkourManager.reset();
        this.defenseManager.reset();
        this.stunManager.reset();
        this.cloneManager.resetFormation();
        this.summonMegaScatteredPattern = false;
        this.summonRadialPattern = false;
        this.attackTick = 0;
        this.attackCooldown = OsirisRealmConfig.AZUMAAL_POST_SPAWN_ATTACK_COOLDOWN.get();
        this.targetId = null;
        this.lastTargetId = null;

        clearMovement();

        this.throwConnected = false;
    }

    private void playBossSound(SoundEvent sound, float volume, float pitch) {
        boss.level().playSound(null, boss.getX(), boss.getY() + boss.getBbHeight() * 0.5D, boss.getZ(), sound, SoundSource.HOSTILE, volume, pitch);
    }

    public void beginPostSpawnCooldown() {
        this.attackCooldown = OsirisRealmConfig.AZUMAAL_POST_SPAWN_ATTACK_COOLDOWN.get();
    }


    public boolean isAttacking() {
        return this.phase != AttackPhase.NONE;
    }

    public void tickIdle(ServerLevel level) {
        this.defenseManager.tick(level);
        this.stunManager.tick(level);

        if (boss.getAnimState() != AzumaalEntity.STATE_IDLE) {
            return;
        }

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
            return;
        }
        ServerPlayer target = findNearestTarget(level);

        if (target == null) {
            this.attackCooldown = 20;
            return;
        }
        startRandomAttack(level, target);
    }


    private void startRandomAttack(ServerLevel level, ServerPlayer target) {
        this.targetId = target.getUUID();
        this.lastTargetId = target.getUUID();

        clearMovement();
        this.throwConnected = false;

        boolean clonesAlreadyExist = this.cloneManager.hasActiveClones(level);
        boolean defenseAlreadyActive = this.defenseManager.isActive(level);
        boolean parkourAvailable = !clonesAlreadyExist && !defenseAlreadyActive;

        int availableAttackCount = 7;

        if (!clonesAlreadyExist) {
            availableAttackCount++;
        }

        if (!defenseAlreadyActive) {
            availableAttackCount++;
        }

        if (parkourAvailable) {
            availableAttackCount++;
        }

        int attack = boss.getRandom().nextInt(availableAttackCount);

        if (attack < 7) {

            switch (attack) {

                case 0 -> startMelee(target, AttackPhase.MELEE_1);
                case 1 -> startMelee(target, AttackPhase.MELEE_2);
                case 2 -> startThrowApproach(target);
                case 3 -> startDoubleApproach(target);
                case 4 -> startSummonWeak(target);
                case 5 -> startSummonMega(target);

                default -> startEyesAttack(target);
            }
            return;
        }
        // optional attacks

        int extraIndex = 7;
        //rainbow
        if (!clonesAlreadyExist) {
            if (attack == extraIndex) {
                startRainbowCloneSummon(target);
                return;
            }
            extraIndex++;
        }


        // defense
        if (!defenseAlreadyActive) {
            if (attack == extraIndex) {
                startDefenseCast(target);
                return;
            }
            extraIndex++;
        }
// parkour
        if (parkourAvailable && attack == extraIndex) {
            startParkourAttack(level, target);
        }
    }

    public void tickAttack(ServerLevel level) {

        this.defenseManager.tick(level);
        this.stunManager.tick(level);

        switch (this.phase) {

            case DEFENSE_CAST -> tickDefenseCast(level);
            case PARKOUR -> tickParkourAttack(level);
            case EYES -> tickEyesAttack(level);
            case SUMMON_CLONES -> tickRainbowCloneSummon(level);
            case SUMMON_MEGA -> tickSummonMega(level);
            case SUMMON_WEAK -> tickSummonWeak(level);
            case DOUBLE_APPROACH -> tickDoubleApproach(level);
            case DOUBLE_ATTACK -> tickDoubleAttack(level);
            case MELEE_1 -> tickMelee(level, true);
            case MELEE_2 -> tickMelee(level, false);
            case THROW_APPROACH -> tickThrowApproach(level);
            case THROW_UP -> tickThrowUp(level);
            case AIR_THROW -> tickAirThrow(level);
            case NONE -> {
            }
        }
    }

    public void prepareForDeath(ServerLevel level) {
        this.parkourManager.forceCleanup(level);
        this.defenseManager.reset();
        this.stunManager.reset();
        this.cloneManager.resetFormation();
        this.phase = AttackPhase.NONE;
        this.attackTick = 0;
        this.attackCooldown = 0;
        this.targetId = null;
        this.throwConnected = false;

        clearMovement();

        boss.setDefending(false);
        boss.setParkourActive(false);
        boss.setDeltaMovement(Vec3.ZERO);
    }

    private void startDefenseCast(ServerPlayer target) {
        this.phase = AttackPhase.DEFENSE_CAST;
        this.attackTick = 0;

        boss.setAnimState(AzumaalEntity.STATE_SUMMON_2);

        playBossSound(ModSounds.SUMMON_CAST.get(), 1.35F, 1.0F);

        boss.setDeltaMovement(Vec3.ZERO);
        boss.lookAtPlayer(target, 180.0F);
    }

    private void tickDefenseCast(ServerLevel level) {
        this.attackTick++;

        ServerPlayer target = resolveTarget(level);

        boss.setDeltaMovement(Vec3.ZERO);
        if (target != null) {
            boss.lookAtPlayer(target, 8.0F);
        }

        if (this.attackTick < OsirisRealmConfig.AZUMAAL_DEFENSE_CAST_DURATION.get()) {
            return;
        }
        this.defenseManager.activate(level);
        finishAttack();
    }

    private void startEyesAttack(ServerPlayer target) {
        this.phase = AttackPhase.EYES;
        this.attackTick = 0;

        boss.setAnimState(AzumaalEntity.STATE_EYES);

        playBossSound(ModSounds.EYE_ATTACK.get(), 1.45F, 1.0F);

        boss.setDeltaMovement(Vec3.ZERO);
        boss.lookAtPlayer(target, 180.0F);
    }

    private void tickEyesAttack(ServerLevel level) {
        this.attackTick++;

        ServerPlayer target = resolveTarget(level);

        boss.setDeltaMovement(Vec3.ZERO);

        if (target != null) {
            boss.lookAtPlayer(target, 8.0F);
        }

        if (this.attackTick == OsirisRealmConfig.AZUMAAL_EYES_MAGIC_TICK.get()) {
            performEyesMagic(level);
        }

        if (this.attackTick >= OsirisRealmConfig.AZUMAAL_EYES_DURATION.get()) {
            finishAttack();
        }
    }

    private void performEyesMagic(ServerLevel level) {
        double rangeSqr = OsirisRealmConfig.AZUMAAL_EYES_RANGE.get() * OsirisRealmConfig.AZUMAAL_EYES_RANGE.get();
        for (ServerPlayer player : level.players()) {
            if (!isValidTarget(player)) {
                continue;
            }
            if (boss.distanceToSqr(player) > rangeSqr) {
                continue;
            }
            this.stunManager.stun(player);
        }
    }

    private void startSummonMega(ServerPlayer target) {
        this.phase = AttackPhase.SUMMON_MEGA;
        this.attackTick = 0;
        this.summonMegaScatteredPattern = boss.getRandom().nextBoolean();

        boss.setAnimState(AzumaalEntity.STATE_SUMMON_2);

        playBossSound(ModSounds.SUMMON_CAST.get(), 1.35F, 1.0F);

        boss.setDeltaMovement(Vec3.ZERO);
        boss.lookAtPlayer(target, 180.0F);
    }

    private void tickSummonMega(ServerLevel level) {
        this.attackTick++;

        ServerPlayer target = resolveTarget(level);

        if (target == null && this.attackTick <= OsirisRealmConfig.AZUMAAL_SUMMON_MEGA_TICK.get()) {
            finishAttack();
            return;
        }

        boss.setDeltaMovement(Vec3.ZERO);

        if (target != null) {
            boss.lookAtPlayer(target, 8.0F);
        }

        if (this.attackTick == OsirisRealmConfig.AZUMAAL_SUMMON_MEGA_TICK.get() && target != null) {
            if (this.summonMegaScatteredPattern) {
                spawnScatteredMegaWarnings(level);
            } else {
                spawnTrackingMegaWarning(level, target);
            }
        }

        if (this.attackTick >= OsirisRealmConfig.AZUMAAL_SUMMON_MEGA_DURATION.get()) {
            finishAttack();
        }
    }

    private void spawnTrackingMegaWarning(ServerLevel level, ServerPlayer target) {
        CircleHintEntity circle = Oasiso.CIRCLE_HINT.get().create(level);

        if (circle == null) {
            return;
        }

        circle.moveTo(target.getX(), target.getY(), target.getZ(), 0.0F, 0.0F);
        circle.startTrackingSequence(boss, target, OsirisRealmConfig.AZUMAAL_MEGA_TRACKING_WAVES.get());

        level.addFreshEntity(circle);
    }

    private void spawnScatteredMegaWarnings(ServerLevel level) {

        double baseAngle = boss.getRandom().nextDouble() * Math.PI * 2.0D;
        double angleStep = (Math.PI * 2.0D) / OsirisRealmConfig.AZUMAAL_MEGA_RANDOM_CIRCLES.get();

        for (int i = 0; i < OsirisRealmConfig.AZUMAAL_MEGA_RANDOM_CIRCLES.get(); i++) {

            double jitter = (boss.getRandom().nextDouble() * 2.0D - 1.0D) * OsirisRealmConfig.AZUMAAL_MEGA_RANDOM_ANGLE_JITTER.get();
            double angle = baseAngle + i * angleStep + jitter;
            double minRadius = Math.min(OsirisRealmConfig.AZUMAAL_MEGA_RANDOM_MIN_RADIUS.get(), OsirisRealmConfig.AZUMAAL_MEGA_RANDOM_MAX_RADIUS.get());
            double maxRadius = Math.max(OsirisRealmConfig.AZUMAAL_MEGA_RANDOM_MIN_RADIUS.get(), OsirisRealmConfig.AZUMAAL_MEGA_RANDOM_MAX_RADIUS.get());
            double distance = minRadius + boss.getRandom().nextDouble() * (maxRadius - minRadius);

            double x = boss.getX() + Math.cos(angle) * distance;
            double z = boss.getZ() + Math.sin(angle) * distance;

            spawnFixedMegaWarning(level, x, z);
        }
    }

    private void spawnFixedMegaWarning(ServerLevel level, double x, double z) {
        CircleHintEntity circle = Oasiso.CIRCLE_HINT.get().create(level);

        if (circle == null) {
            return;
        }

        circle.moveTo(x, boss.getY(), z, 0.0F, 0.0F);
        circle.startFixed(boss, x, z);

        level.addFreshEntity(circle);
    }

    private void startSummonWeak(ServerPlayer target) {
        this.summonRadialPattern = boss.getRandom().nextBoolean();
        this.phase = AttackPhase.SUMMON_WEAK;
        this.attackTick = 0;

        boss.setAnimState(AzumaalEntity.STATE_SUMMON_1);

        playBossSound(ModSounds.SUMMON_CAST.get(), 1.35F, 1.0F);

        boss.setDeltaMovement(Vec3.ZERO);
        boss.lookAtPlayer(target, 180.0F);
    }

    private void tickSummonWeak(ServerLevel level) {
        this.attackTick++;

        ServerPlayer target = resolveTarget(level);

        if (target == null && this.attackTick <= OsirisRealmConfig.AZUMAAL_SUMMON_WEAK_TICK.get()) {
            finishAttack();
            return;
        }

        boss.setDeltaMovement(Vec3.ZERO);

        if (target != null) {
            boss.lookAtPlayer(target, 8.0F);
        }

        if (this.attackTick == OsirisRealmConfig.AZUMAAL_SUMMON_WEAK_TICK.get() && target != null) {
            if (this.summonRadialPattern) {
                spawnRadialWarnings(level);
            } else {
                spawnTrackingWarning(level, target);
            }
        }

        if (this.attackTick == OsirisRealmConfig.AZUMAAL_SUMMON_WEAK_ANIMATION_DURATION.get()) {
            boss.setAnimState(AzumaalEntity.STATE_IDLE);
        }

        if (this.attackTick >= (OsirisRealmConfig.AZUMAAL_SUMMON_WEAK_TICK.get() + OsirisRealmConfig.AZUMAAL_SUMMON_WARNING_DURATION.get())) {
            finishAttack();
        }
    }

    private void spawnTrackingWarning(ServerLevel level, ServerPlayer target) {
        BattleHintArrowEntity arrow = Oasiso.BATTLE_HINT_ARROW.get().create(level);

        if (arrow == null) {
            return;
        }

        arrow.moveTo(boss.getX(), boss.getY(), boss.getZ(), boss.getYRot(), 0.0F);
        arrow.startTracking(boss, target);

        level.addFreshEntity(arrow);
    }

    private void spawnRadialWarnings(ServerLevel level) {
        int arrowCount = OsirisRealmConfig.AZUMAAL_RADIAL_WARNING_ARROWS.get();
        for (int i = 0; i < arrowCount; i++) {
            float yaw = i * (360.0F / arrowCount);

            BattleHintArrowEntity arrow = Oasiso.BATTLE_HINT_ARROW.get().create(level);

            if (arrow == null) {
                continue;
            }

            arrow.moveTo(boss.getX(), boss.getY(), boss.getZ(), yaw, 0.0F);
            arrow.startFixedDirection(boss, yaw);

            level.addFreshEntity(arrow);
        }
    }


    private void startDoubleApproach(ServerPlayer target) {
        this.phase = AttackPhase.DOUBLE_APPROACH;

        this.attackTick = 0;

        boss.setAnimState(AzumaalEntity.STATE_IDLE);
        boss.lookAtPlayer(target, 180.0F);

        beginDash(target, OsirisRealmConfig.AZUMAAL_DOUBLE_APPROACH_DISTANCE.get(), OsirisRealmConfig.AZUMAAL_DOUBLE_APPROACH_TICKS.get());
    }

    private void tickDoubleApproach(ServerLevel level) {
        ServerPlayer target = resolveTarget(level);

        if (target == null) {
            finishAttack();
            return;
        }

        boss.lookAtPlayer(target, 20.0F);
        tickDashMovement();

        if (this.dashTicksRemaining <= 0) {
            startDoubleAttack(target);
        }
    }

    private void startDoubleAttack(ServerPlayer target) {
        this.phase = AttackPhase.DOUBLE_ATTACK;

        this.attackTick = 0;

        boss.setAnimState(AzumaalEntity.STATE_ATTACK_DOUBLE);
        boss.setDeltaMovement(Vec3.ZERO);
        boss.lookAtPlayer(target, 180.0F);
    }

    private void tickDoubleAttack(ServerLevel level) {
        this.attackTick++;

        ServerPlayer target = resolveTarget(level);

        if (target != null) {
            boss.lookAtPlayer(target, 12.0F);
        }

        if (this.attackTick == OsirisRealmConfig.AZUMAAL_DOUBLE_DAMAGE_1_TICK.get()) {
            playBossSound(ModSounds.SWING.get(), 2.5F, 1.0F);
            dealDoubleDamage(level, target);
        }

        if (this.attackTick == OsirisRealmConfig.AZUMAAL_DOUBLE_DAMAGE_2_TICK.get()) {
            playBossSound(ModSounds.SWING.get(), 2.5F, 1.0F);
            dealDoubleDamage(level, target);
        }

        if (this.attackTick >= OsirisRealmConfig.AZUMAAL_DOUBLE_ATTACK_DURATION.get()) {
            finishAttack();
        }
    }

    private void dealDoubleDamage(ServerLevel level, ServerPlayer target) {
        if (target == null || !target.isAlive()) {
            return;
        }

        if (!this.cloneManager.isTargetInAttackRange(level, target, OsirisRealmConfig.AZUMAAL_DOUBLE_DAMAGE_RANGE.get())) {
            return;
        }
        target.hurt(boss.damageSources().mobAttack(boss), getAttackDamage(OsirisRealmConfig.AZUMAAL_DOUBLE_DAMAGE_MULTIPLIER.get().floatValue()));
    }

    private void startMelee(ServerPlayer target, AttackPhase attackPhase) {
        this.phase = attackPhase;
        this.attackTick = 0;

        boss.setAnimState(attackPhase == AttackPhase.MELEE_1 ? AzumaalEntity.STATE_ATTACK_1 : AzumaalEntity.STATE_ATTACK_2);

        boss.lookAtPlayer(target, 180.0F);
    }

    private void tickMelee(ServerLevel level, boolean attackOne) {
        this.attackTick++;

        ServerPlayer target = resolveTarget(level);

        if (target != null) {
            boss.lookAtPlayer(target, 12.0F);
        }

        int prepareTick = attackOne ? OsirisRealmConfig.AZUMAAL_ATTACK_1_PREPARE_TICK.get() : OsirisRealmConfig.AZUMAAL_ATTACK_2_PREPARE_TICK.get();
        int damageTick = attackOne ? OsirisRealmConfig.AZUMAAL_ATTACK_1_DAMAGE_TICK.get() : OsirisRealmConfig.AZUMAAL_ATTACK_2_DAMAGE_TICK.get();

        int dashStartTick = damageTick - OsirisRealmConfig.AZUMAAL_DASH_MOVE_TICKS.get() + 1;

        if (this.attackTick == prepareTick && target != null) {
            beginPrepareMovement(target);
        }

        tickPrepareMovement();

        if (this.attackTick == dashStartTick && target != null) {
            beginDash(target, OsirisRealmConfig.AZUMAAL_DASH_STOP_DISTANCE.get(), OsirisRealmConfig.AZUMAAL_DASH_MOVE_TICKS.get());
        }

        tickDashMovement();


        if (this.attackTick == damageTick) {

            playBossSound(ModSounds.SWING.get(), 2.5F, 1.0F);

            dealMeleeDamage(level, target);
        }


        if (this.attackTick >= OsirisRealmConfig.AZUMAAL_MELEE_DURATION.get()) {
            finishAttack();
        }
    }

    private void startThrowApproach(ServerPlayer target) {
        this.phase = AttackPhase.THROW_APPROACH;
        this.attackTick = 0;

        boss.setAnimState(AzumaalEntity.STATE_IDLE);
        boss.lookAtPlayer(target, 180.0F);

        beginDash(target, OsirisRealmConfig.AZUMAAL_THROW_APPROACH_DISTANCE.get(), OsirisRealmConfig.AZUMAAL_THROW_APPROACH_TICKS.get());
    }


    private void tickThrowApproach(ServerLevel level) {
        ServerPlayer target = resolveTarget(level);

        if (target == null) {
            finishAttack();
            return;
        }

        boss.lookAtPlayer(target, 20.0F);

        tickDashMovement();

        if (this.dashTicksRemaining <= 0) {
            startThrowUpAnimation(target);
        }
    }

    private void startThrowUpAnimation(ServerPlayer target) {
        this.phase = AttackPhase.THROW_UP;
        this.attackTick = 0;
        this.throwConnected = false;

        boss.setAnimState(AzumaalEntity.STATE_ATTACK_THROW);
        boss.setDeltaMovement(Vec3.ZERO);
        boss.lookAtPlayer(target, 180.0F);
    }


    private void tickThrowUp(ServerLevel level) {
        this.attackTick++;

        ServerPlayer target = resolveTarget(level);

        if (target != null) {
            boss.lookAtPlayer(target, 10.0F);
        }

        if (this.attackTick == OsirisRealmConfig.AZUMAAL_THROW_UP_TICK.get()) {
            this.throwConnected = launchTarget(level, target);
        }

        if (this.attackTick < OsirisRealmConfig.AZUMAAL_THROW_UP_DURATION.get()) {
            return;
        }

        if (!this.throwConnected || target == null) {
            finishAttack();
            return;
        }
        startAirThrow(target);
    }


    private boolean launchTarget(ServerLevel level, ServerPlayer target) {
        if (target == null || !target.isAlive()) {
            return false;
        }

        if (!this.cloneManager.isTargetInAttackRange(level, target, OsirisRealmConfig.AZUMAAL_THROW_UP_RANGE.get())) {
            return false;
        }

        target.setDeltaMovement(0.0D, OsirisRealmConfig.AZUMAAL_THROW_UP_VELOCITY.get(), 0.0D);

        target.hurtMarked = true;
        target.fallDistance = 0.0F;

        target.serverLevel().sendParticles(Oasiso.PURPLE_STARS.get(), target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 28, 0.55D, 0.75D, 0.55D, 0.08D);
        return true;
    }


    private void startAirThrow(ServerPlayer target) {
        this.phase = AttackPhase.AIR_THROW;
        this.attackTick = 0;
        this.airChaseTicksRemaining = OsirisRealmConfig.AZUMAAL_AIR_CHASE_TICKS.get();
        this.returnTicksRemaining = 0;

        boss.setAnimState(AzumaalEntity.STATE_AIR_THROW);
        boss.setDeltaMovement(Vec3.ZERO);
        boss.lookAtPlayer(target, 180.0F);
    }


    private void tickAirThrow(ServerLevel level) {
        this.attackTick++;

        ServerPlayer target = resolveTarget(level);

        if (target != null && this.attackTick < OsirisRealmConfig.AZUMAAL_THROW_DOWN_TICK.get()) {
            holdTargetInAir(level, target);
        }

        if (target != null) {
            boss.lookAtPlayer(target, 20.0F);

            if (this.attackTick <= OsirisRealmConfig.AZUMAAL_AIR_CHASE_TICKS.get()) {
                tickAirChase(target);
            } else if (this.attackTick < OsirisRealmConfig.AZUMAAL_THROW_DOWN_TICK.get()) {
                followAirTarget(target);
            }
        }

        if (this.attackTick == OsirisRealmConfig.AZUMAAL_THROW_DOWN_TICK.get()) {

            playBossSound(ModSounds.SWING.get(), 2.5F, 0.92F);

            throwTargetDown(level, target);

            this.returnTicksRemaining = OsirisRealmConfig.AZUMAAL_AIR_THROW_DURATION.get() - OsirisRealmConfig.AZUMAAL_THROW_DOWN_TICK.get();
        }

        if (this.attackTick >= OsirisRealmConfig.AZUMAAL_THROW_DOWN_TICK.get()) {
            tickReturnToHover();
        }


        if (this.attackTick >= OsirisRealmConfig.AZUMAAL_AIR_THROW_DURATION.get()) {
            finishAttack();
        }
    }

    private void holdTargetInAir(ServerLevel level, ServerPlayer target) {
        Vec3 movement = target.getDeltaMovement();

        target.setDeltaMovement(movement.x * OsirisRealmConfig.AZUMAAL_AIR_HOLD_HORIZONTAL_DAMPING.get(), 0.0D, movement.z * OsirisRealmConfig.AZUMAAL_AIR_HOLD_HORIZONTAL_DAMPING.get());

        target.hurtMarked = true;
        target.fallDistance = 0.0F;

        if (this.attackTick % AIR_HOLD_PARTICLE_INTERVAL == 0) {
            level.sendParticles(Oasiso.PURPLE_STARS.get(), target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 5, 0.4D, 0.65D, 0.4D, 0.025D);
        }
    }

    private void tickAirChase(ServerPlayer target) {
        int remaining = Math.max(1, this.airChaseTicksRemaining);

        double targetX = target.getX();
        double targetY = target.getY() + OsirisRealmConfig.AZUMAAL_AIR_HEIGHT_OFFSET.get();
        double targetZ = target.getZ();

        double nextX = boss.getX() + (targetX - boss.getX()) / remaining;
        double nextY = boss.getY() + (targetY - boss.getY()) / remaining;
        double nextZ = boss.getZ() + (targetZ - boss.getZ()) / remaining;

        boss.setPos(nextX, nextY, nextZ);

        boss.setDeltaMovement(Vec3.ZERO);

        if (this.airChaseTicksRemaining > 0) {
            this.airChaseTicksRemaining--;
        }
    }

    private void followAirTarget(ServerPlayer target) {
        double desiredX = target.getX();
        double desiredY = target.getY() + OsirisRealmConfig.AZUMAAL_AIR_HEIGHT_OFFSET.get();
        double desiredZ = target.getZ();

        final double follow = OsirisRealmConfig.AZUMAAL_AIR_FOLLOW_FACTOR.get();

        boss.setPos(boss.getX() + (desiredX - boss.getX()) * follow, boss.getY() + (desiredY - boss.getY()) * follow, boss.getZ() + (desiredZ - boss.getZ()) * follow);

        boss.setDeltaMovement(Vec3.ZERO);
    }


    private void throwTargetDown(ServerLevel level, ServerPlayer target) {
        if (target == null || !target.isAlive()) {
            return;
        }

        if (!this.cloneManager.isTargetInAttackRange(level, target, OsirisRealmConfig.AZUMAAL_THROW_DOWN_RANGE.get())) {
            return;
        }

        target.serverLevel().sendParticles(Oasiso.PURPLE_STARS.get(), target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 38, 0.65D, 0.55D, 0.65D, 0.12D);

        target.hurt(boss.damageSources().mobAttack(boss), getAttackDamage(OsirisRealmConfig.AZUMAAL_THROW_DOWN_DAMAGE_MULTIPLIER.get().floatValue()));

        Vec3 attackOrigin = this.cloneManager.getClosestMemberPosition(level, target);
        Vec3 horizontal = new Vec3(target.getX() - attackOrigin.x, 0.0D, target.getZ() - attackOrigin.z);

        if (horizontal.lengthSqr() < 0.01D) {
            Vec3 look = boss.getLookAngle();
            horizontal = new Vec3(look.x, 0.0D, look.z);
        }

        if (horizontal.lengthSqr() < 0.0001D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        }

        horizontal = horizontal.normalize().scale(OsirisRealmConfig.AZUMAAL_THROW_DOWN_HORIZONTAL.get());

        target.setDeltaMovement(horizontal.x, OsirisRealmConfig.AZUMAAL_THROW_DOWN_VERTICAL.get(), horizontal.z);
        target.hurtMarked = true;
        target.fallDistance = 0.0F;
    }

    private void tickReturnToHover() {
        if (this.returnTicksRemaining <= 0) {
            return;
        }

        double destinationY = boss.getHoverBaseY();
        double fraction = 1.0D / this.returnTicksRemaining;
        double newY = boss.getY() + (destinationY - boss.getY()) * fraction;

        boss.setPos(boss.getX(), newY, boss.getZ());
        boss.setDeltaMovement(Vec3.ZERO);

        this.returnTicksRemaining--;
    }

    private void beginPrepareMovement(ServerPlayer target) {
        Vec3 toTarget = new Vec3(target.getX() - boss.getX(), 0.0D, target.getZ() - boss.getZ());

        if (toTarget.lengthSqr() < 0.0001D) {
            return;
        }

        this.prepareStep = toTarget.normalize().scale(-OsirisRealmConfig.AZUMAAL_PREPARE_BACK_DISTANCE.get() / OsirisRealmConfig.AZUMAAL_PREPARE_MOVE_TICKS.get());
        this.prepareTicksRemaining = OsirisRealmConfig.AZUMAAL_PREPARE_MOVE_TICKS.get();
    }


    private void tickPrepareMovement() {
        if (this.prepareTicksRemaining <= 0) {
            return;
        }

        boss.setPos(boss.getX() + this.prepareStep.x, boss.getY(), boss.getZ() + this.prepareStep.z);
        this.prepareTicksRemaining--;
    }


    private void beginDash(ServerPlayer target, double stopDistance, int movementTicks) {
        Vec3 horizontal = new Vec3(target.getX() - boss.getX(), 0.0D, target.getZ() - boss.getZ());
        double distance = horizontal.length();

        if (distance < 0.0001D) {

            this.dashLanding = boss.position();
            this.dashTicksRemaining = movementTicks;

            return;
        }

        Vec3 direction = horizontal.scale(1.0D / distance);
        double travelDistance = Math.max(0.0D, distance - stopDistance);

        this.dashLanding = new Vec3(boss.getX() + direction.x * travelDistance, boss.getY(), boss.getZ() + direction.z * travelDistance);
        this.dashTicksRemaining = movementTicks;
    }


    private void tickDashMovement() {
        if (this.dashTicksRemaining <= 0 || this.dashLanding == null) {
            return;
        }

        double fraction = 1.0D / this.dashTicksRemaining;

        double newX = boss.getX() + (this.dashLanding.x - boss.getX()) * fraction;
        double newZ = boss.getZ() + (this.dashLanding.z - boss.getZ()) * fraction;

        boss.setPos(newX, boss.getY(), newZ);

        this.dashTicksRemaining--;
    }

    private void dealMeleeDamage(ServerLevel level, ServerPlayer target) {
        if (target == null || !target.isAlive()) {
            return;
        }
        if (!this.cloneManager.isTargetInAttackRange(level, target, OsirisRealmConfig.AZUMAAL_MELEE_DAMAGE_RANGE.get())) {
            return;
        }

        target.hurt(boss.damageSources().mobAttack(boss), getAttackDamage(OsirisRealmConfig.AZUMAAL_MELEE_DAMAGE_MULTIPLIER.get().floatValue()));
    }

    private void finishAttack() {
        this.phase = AttackPhase.NONE;
        this.attackTick = 0;
        this.targetId = null;
        this.throwConnected = false;

        clearMovement();

        boss.setDeltaMovement(Vec3.ZERO);
        boss.setAnimState(AzumaalEntity.STATE_IDLE);

        this.attackCooldown = randomBetween(OsirisRealmConfig.AZUMAAL_ATTACK_COOLDOWN_MIN.get(), OsirisRealmConfig.AZUMAAL_ATTACK_COOLDOWN_MAX.get());
    }


    private void clearMovement() {
        this.prepareStep = Vec3.ZERO;
        this.prepareTicksRemaining = 0;
        this.dashLanding = null;
        this.dashTicksRemaining = 0;
        this.airChaseTicksRemaining = 0;
        this.returnTicksRemaining = 0;
    }

    private ServerPlayer findNearestTarget(
            ServerLevel level
    ) {
        double rangeSqr =
                OsirisRealmConfig
                        .AZUMAAL_TARGET_SEARCH_RANGE
                        .get()
                        * OsirisRealmConfig
                        .AZUMAAL_TARGET_SEARCH_RANGE
                        .get();

        List<ServerPlayer> candidates =
                new ArrayList<>();

        for (ServerPlayer player :
                level.players()) {

            if (!isValidTarget(player)) {
                continue;
            }

            if (!boss.isEncounterParticipant(
                    player
            )) {
                continue;
            }

            if (boss.distanceToSqr(player)
                    > rangeSqr) {

                continue;
            }

            candidates.add(player);
        }

        if (candidates.isEmpty()) {
            return null;
        }

        /*
         * Multiplayer fairness:
         * if at least two valid players are in the encounter, never select the
         * exact same player for two attacks in a row. Among the remaining
         * players the closest one still wins, so the boss keeps natural
         * positioning instead of choosing somebody across the arena randomly.
         */
        ServerPlayer best = null;
        double bestDistance =
                Double.MAX_VALUE;

        boolean canAvoidPrevious =
                candidates.size() > 1
                        && this.lastTargetId != null;

        for (ServerPlayer player :
                candidates) {

            if (canAvoidPrevious
                    && player.getUUID()
                    .equals(
                            this.lastTargetId
                    )) {

                continue;
            }

            double distance =
                    boss.distanceToSqr(
                            player
                    );

            if (distance >= bestDistance) {
                continue;
            }

            best = player;
            bestDistance = distance;
        }

        if (best != null) {
            return best;
        }

        /*
         * Fallback for the one-player case.
         */
        for (ServerPlayer player :
                candidates) {

            double distance =
                    boss.distanceToSqr(
                            player
                    );

            if (distance >= bestDistance) {
                continue;
            }

            best = player;
            bestDistance = distance;
        }

        return best;
    }


    private ServerPlayer resolveTarget(ServerLevel level) {
        if (this.targetId == null) {
            return null;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(this.targetId);
        if (player == null || player.serverLevel() != level || !isValidTarget(player)) {
            return null;
        }
        return player;
    }

    private boolean isValidTarget(ServerPlayer player) {
        return player.isAlive()
                && !player.isSpectator()
                && !player.isCreative()
                && boss.isEncounterParticipant(player);
    }

    private int randomBetween(int minimum, int maximum) {
        int min = Math.min(minimum, maximum);
        int max = Math.max(minimum, maximum);
        return min + boss.getRandom().nextInt(max - min + 1);
    }

    private void startRainbowCloneSummon(ServerPlayer target) {
        this.phase = AttackPhase.SUMMON_CLONES;
        this.attackTick = 0;

        boss.setAnimState(AzumaalEntity.STATE_SUMMON_1);

        playBossSound(ModSounds.SUMMON_CAST.get(), 1.35F, 1.0F);

        boss.setDeltaMovement(Vec3.ZERO);
        boss.lookAtPlayer(target, 180.0F);
    }

    private void tickRainbowCloneSummon(ServerLevel level) {
        this.attackTick++;

        ServerPlayer target = resolveTarget(level);

        if (target == null && this.attackTick <= OsirisRealmConfig.AZUMAAL_CLONE_SUMMON_DURATION.get()) {
            finishAttack();
            return;
        }

        boss.setDeltaMovement(Vec3.ZERO);

        if (target != null) {
            boss.lookAtPlayer(target, 8.0F);
        }

        if (this.attackTick == OsirisRealmConfig.AZUMAAL_CLONE_SUMMON_DURATION.get()) {

            boss.setAnimState(AzumaalEntity.STATE_IDLE);
            boolean created = this.cloneManager.beginFormation(level);

            if (created) {
                playBossSound(ModSounds.CLONES.get(), 2.45F, 1.0F);
            }

            if (!created) {
                finishAttack();
                return;
            }
        }

        if (this.attackTick > OsirisRealmConfig.AZUMAAL_CLONE_SUMMON_DURATION.get()) {
            this.cloneManager.tickFormation();
        }


        if (this.attackTick >= (OsirisRealmConfig.AZUMAAL_CLONE_SUMMON_DURATION.get() + OsirisRealmConfig.AZUMAAL_CLONE_FORMATION_TICKS.get())) {
            finishAttack();
        }
    }

    private void startParkourAttack(ServerLevel level, ServerPlayer target) {
        this.phase = AttackPhase.PARKOUR;


        this.attackTick = 0;

        boss.setAnimState(AzumaalEntity.STATE_IDLE);
        boss.setDeltaMovement(Vec3.ZERO);
        boss.lookAtPlayer(target, 180.0F);

        boolean started = this.parkourManager.begin(level);

        if (!started) {
            finishAttack();
        }
    }

    private void tickParkourAttack(ServerLevel level) {
        this.attackTick++;


        ServerPlayer target = resolveTarget(level);

        if (target != null) {
            boss.lookAtPlayer(target, 8.0F);
        }


        boolean finished = this.parkourManager.tick(level);

        if (finished) {
            finishAttack();
        }
    }

    public void onParkourMeleeHit(net.minecraft.world.entity.player.Player player) {
        this.parkourManager.registerMeleeHit(player);
    }

    public void forceParkourCleanup(ServerLevel level) {
        this.parkourManager.forceCleanup(level);
    }

    public void save(CompoundTag parent) {
        CompoundTag tag = new CompoundTag();

        tag.putBoolean("SummonMegaScatteredPattern", this.summonMegaScatteredPattern);
        tag.putBoolean("SummonRadialPattern", this.summonRadialPattern);
        tag.putString("Phase", this.phase.name());
        tag.putInt("AttackTick", this.attackTick);
        tag.putInt("AttackCooldown", this.attackCooldown);
        tag.putBoolean("ThrowConnected", this.throwConnected);
        tag.putInt("PrepareTicks", this.prepareTicksRemaining);
        tag.putDouble("PrepareX", this.prepareStep.x);
        tag.putDouble("PrepareZ", this.prepareStep.z);
        tag.putInt("DashTicks", this.dashTicksRemaining);
        tag.putInt("AirChaseTicks", this.airChaseTicksRemaining);
        tag.putInt("ReturnTicks", this.returnTicksRemaining);

        if (this.targetId != null) {
            tag.putUUID("Target", this.targetId);
        }

        if (this.lastTargetId != null) {
            tag.putUUID("LastTarget", this.lastTargetId);
        }

        if (this.dashLanding != null) {

            tag.putBoolean("HasDashLanding", true);
            tag.putDouble("DashX", this.dashLanding.x);
            tag.putDouble("DashY", this.dashLanding.y);
            tag.putDouble("DashZ", this.dashLanding.z);
        }
        this.cloneManager.save(tag);
        this.stunManager.save(tag);
        this.parkourManager.save(tag);

        parent.put(DATA_TAG, tag);
    }

    public void load(CompoundTag parent) {
        if (!parent.contains(DATA_TAG, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag tag = parent.getCompound(DATA_TAG);

        if (tag.contains("Phase", Tag.TAG_STRING)) {

            try {
                this.phase = AttackPhase.valueOf(tag.getString("Phase"));
            } catch (IllegalArgumentException ignored) {
                this.phase = AttackPhase.NONE;
            }
        } else {
            this.phase = inferPhaseFromState();
        }

        this.parkourManager.load(tag);
        this.cloneManager.load(tag);
        this.stunManager.load(tag);
        this.summonMegaScatteredPattern = tag.getBoolean("SummonMegaScatteredPattern");
        this.summonRadialPattern = tag.getBoolean("SummonRadialPattern");
        this.attackTick = tag.getInt("AttackTick");
        this.attackCooldown = tag.getInt("AttackCooldown");
        this.throwConnected = tag.getBoolean("ThrowConnected");
        this.prepareTicksRemaining = tag.getInt("PrepareTicks");
        this.prepareStep = new Vec3(tag.getDouble("PrepareX"), 0.0D, tag.getDouble("PrepareZ"));
        this.dashTicksRemaining = tag.getInt("DashTicks");
        this.airChaseTicksRemaining = tag.getInt("AirChaseTicks");
        this.returnTicksRemaining = tag.getInt("ReturnTicks");
        this.targetId = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
        this.lastTargetId = tag.hasUUID("LastTarget") ? tag.getUUID("LastTarget") : null;
        if (tag.getBoolean("HasDashLanding")) {
            this.dashLanding = new Vec3(tag.getDouble("DashX"), tag.getDouble("DashY"), tag.getDouble("DashZ"));
        } else {
            this.dashLanding = null;
        }
    }

    private AttackPhase inferPhaseFromState() {
        return switch (boss.getAnimState()) {

            case AzumaalEntity.STATE_EYES -> AttackPhase.EYES;
            case AzumaalEntity.STATE_SUMMON_2 -> AttackPhase.SUMMON_MEGA;
            case AzumaalEntity.STATE_SUMMON_1 -> AttackPhase.SUMMON_WEAK;
            case AzumaalEntity.STATE_ATTACK_DOUBLE -> AttackPhase.DOUBLE_ATTACK;
            case AzumaalEntity.STATE_ATTACK_1 -> AttackPhase.MELEE_1;
            case AzumaalEntity.STATE_ATTACK_2 -> AttackPhase.MELEE_2;
            case AzumaalEntity.STATE_ATTACK_THROW -> AttackPhase.THROW_UP;
            case AzumaalEntity.STATE_AIR_THROW -> AttackPhase.AIR_THROW;

            default -> AttackPhase.NONE;
        };
    }

    private enum AttackPhase {

        NONE,
        PARKOUR,
        DEFENSE_CAST,
        EYES,
        SUMMON_CLONES,
        SUMMON_WEAK,
        SUMMON_MEGA,
        MELEE_1,
        MELEE_2,
        THROW_APPROACH,
        THROW_UP,
        AIR_THROW,
        DOUBLE_APPROACH,
        DOUBLE_ATTACK
    }
}
