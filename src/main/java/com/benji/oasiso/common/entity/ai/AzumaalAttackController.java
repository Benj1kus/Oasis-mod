package com.benji.oasiso.common.entity.ai;

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

import java.util.UUID;

public final class AzumaalAttackController {

    private static final double AIR_HOLD_HORIZONTAL_DAMPING = 0.82D;
    private static final int AIR_HOLD_PARTICLE_INTERVAL = 3;

    private static final int MELEE_DURATION = 40;

    private static final int ATTACK_1_PREPARE_TICK = 8;
    private static final int ATTACK_1_DAMAGE_TICK = 22;

    private static final int ATTACK_2_PREPARE_TICK = 6;
    private static final int ATTACK_2_DAMAGE_TICK = 24;

    private static final int DASH_MOVE_TICKS = 3;
    private static final int PREPARE_MOVE_TICKS = 6;
    private static final double PREPARE_BACK_DISTANCE = 0.95D;
    private static final double DASH_STOP_DISTANCE = 2.0D;

    private static final double DAMAGE_RANGE = 8.0D;
    private static final float MELEE_DAMAGE_MULTIPLIER = 1.0F;

    private static final int SUMMON_1_DURATION = 60;
    private static final int SUMMON_WEAK_TICK = 36;
    private static final int SUMMON_WARNING_DURATION = 20 * 8;
    private static final int SUMMON_FINISH_TICK = SUMMON_WEAK_TICK + SUMMON_WARNING_DURATION;

    private static final int SUMMON_2_DURATION = 60;
    private static final int SUMMON_MEGA_TICK = 36;
    private static final int MEGA_TRACKING_WAVES = 3;
    private static final int MEGA_RANDOM_CIRCLES = 5;
    private static final double MEGA_RANDOM_MIN_RADIUS = 6.0D;
    private static final double MEGA_RANDOM_MAX_RADIUS = 20.0D;
    private static final double MEGA_RANDOM_ANGLE_JITTER = 0.35D;

    private static final int DOUBLE_APPROACH_TICKS = 5;
    private static final double DOUBLE_APPROACH_DISTANCE = 2.0D;
    private static final int DOUBLE_ATTACK_DURATION = 55;
    private static final int DOUBLE_DAMAGE_1_TICK = 23;
    private static final int DOUBLE_DAMAGE_2_TICK = 38;
    private static final double DOUBLE_DAMAGE_RANGE = 8.0D;
    private static final float DOUBLE_DAMAGE_MULTIPLIER = 0.80F;

    private static final int THROW_APPROACH_TICKS = 5;
    private static final double THROW_APPROACH_DISTANCE = 2.0D;
    private static final int THROW_UP_DURATION = 45;
    private static final int THROW_UP_TICK = 22;
    private static final double THROW_UP_RANGE = 10.0D;
    private static final double THROW_UP_VELOCITY = 1.60D;
    private static final int AIR_THROW_DURATION = 50;
    private static final int AIR_CHASE_TICKS = 15;
    private static final double AIR_HEIGHT_OFFSET = 1.75D;
    private static final int THROW_DOWN_TICK = 24;
    private static final double THROW_DOWN_RANGE = 5.0D;
    private static final float THROW_DOWN_DAMAGE_MULTIPLIER = 1.10F;
    private static final double THROW_DOWN_HORIZONTAL = 1.15D;
    private static final double THROW_DOWN_VERTICAL = -1.55D;

    private static final double TARGET_SEARCH_RANGE = 96.0D;
    private static final int MIN_ATTACK_COOLDOWN = 35;
    private static final int MAX_ATTACK_COOLDOWN = 70;

    private static final String DATA_TAG = "AzumaalAttackController";

    private final AzumaalCloneManager cloneManager;
    private final AzumaalEntity boss;
    private final AzumaalDefenseManager defenseManager;
    private final AzumaalParkourManager parkourManager;
    private final AzumaalStunManager stunManager;
    private AttackPhase phase = AttackPhase.NONE;

    private int attackTick;
    private int attackCooldown = 40;

    private UUID targetId;

    private Vec3 prepareStep = Vec3.ZERO;
    private int prepareTicksRemaining;
    private Vec3 dashLanding;
    private int dashTicksRemaining;
    private int airChaseTicksRemaining;
    private int returnTicksRemaining;

    private static final int CLONE_SUMMON_DURATION = 60;
    private static final int CLONE_ATTACK_DURATION = CLONE_SUMMON_DURATION + AzumaalCloneManager.FORMATION_TICKS;

    private static final int EYES_DURATION = 45;
    private static final int EYES_MAGIC_TICK = 25;
    private static final double EYES_RANGE = 20.0D;

    private static final int DEFENSE_CAST_DURATION = 60;

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
        this.attackCooldown = 40;
        this.targetId = null;

        clearMovement();

        this.throwConnected = false;
    }

    private void playBossSound(SoundEvent sound, float volume, float pitch) {
        boss.level().playSound(null, boss.getX(), boss.getY() + boss.getBbHeight() * 0.5D, boss.getZ(), sound, SoundSource.HOSTILE, volume, pitch);
    }

    public void beginPostSpawnCooldown() {
        this.attackCooldown = 40;
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

        if (this.attackTick < DEFENSE_CAST_DURATION) {
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

        if (this.attackTick == EYES_MAGIC_TICK) {
            performEyesMagic(level);
        }

        if (this.attackTick >= EYES_DURATION) {
            finishAttack();
        }
    }

    private void performEyesMagic(ServerLevel level) {
        double rangeSqr = EYES_RANGE * EYES_RANGE;
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

        if (target == null && this.attackTick <= SUMMON_MEGA_TICK) {
            finishAttack();
            return;
        }

        boss.setDeltaMovement(Vec3.ZERO);

        if (target != null) {
            boss.lookAtPlayer(target, 8.0F);
        }

        if (this.attackTick == SUMMON_MEGA_TICK && target != null) {
            if (this.summonMegaScatteredPattern) {
                spawnScatteredMegaWarnings(level);
            } else {
                spawnTrackingMegaWarning(level, target);
            }
        }

        if (this.attackTick >= SUMMON_2_DURATION) {
            finishAttack();
        }
    }

    private void spawnTrackingMegaWarning(ServerLevel level, ServerPlayer target) {
        CircleHintEntity circle = Oasiso.CIRCLE_HINT.get().create(level);

        if (circle == null) {
            return;
        }

        circle.moveTo(target.getX(), target.getY(), target.getZ(), 0.0F, 0.0F);
        circle.startTrackingSequence(boss, target, MEGA_TRACKING_WAVES);

        level.addFreshEntity(circle);
    }

    private void spawnScatteredMegaWarnings(ServerLevel level) {

        double baseAngle = boss.getRandom().nextDouble() * Math.PI * 2.0D;
        double angleStep = (Math.PI * 2.0D) / MEGA_RANDOM_CIRCLES;

        for (int i = 0; i < MEGA_RANDOM_CIRCLES; i++) {

            double jitter = (boss.getRandom().nextDouble() * 2.0D - 1.0D) * MEGA_RANDOM_ANGLE_JITTER;
            double angle = baseAngle + i * angleStep + jitter;
            double distance = MEGA_RANDOM_MIN_RADIUS + boss.getRandom().nextDouble() * (MEGA_RANDOM_MAX_RADIUS - MEGA_RANDOM_MIN_RADIUS);

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

        if (target == null && this.attackTick <= SUMMON_WEAK_TICK) {
            finishAttack();
            return;
        }

        boss.setDeltaMovement(Vec3.ZERO);

        if (target != null) {
            boss.lookAtPlayer(target, 8.0F);
        }

        if (this.attackTick == SUMMON_WEAK_TICK && target != null) {
            if (this.summonRadialPattern) {
                spawnRadialWarnings(level);
            } else {
                spawnTrackingWarning(level, target);
            }
        }

        if (this.attackTick == SUMMON_1_DURATION) {
            boss.setAnimState(AzumaalEntity.STATE_IDLE);
        }

        if (this.attackTick >= SUMMON_FINISH_TICK) {
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
        for (int i = 0; i < 8; i++) {
            float yaw = i * 45.0F;

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

        beginDash(target, DOUBLE_APPROACH_DISTANCE, DOUBLE_APPROACH_TICKS);
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

        if (this.attackTick == DOUBLE_DAMAGE_1_TICK) {
            playBossSound(ModSounds.SWING.get(), 2.5F, 1.0F);
            dealDoubleDamage(level, target);
        }

        if (this.attackTick == DOUBLE_DAMAGE_2_TICK) {
            playBossSound(ModSounds.SWING.get(), 2.5F, 1.0F);
            dealDoubleDamage(level, target);
        }

        if (this.attackTick >= DOUBLE_ATTACK_DURATION) {
            finishAttack();
        }
    }

    private void dealDoubleDamage(ServerLevel level, ServerPlayer target) {
        if (target == null || !target.isAlive()) {
            return;
        }

        if (!this.cloneManager.isTargetInAttackRange(level, target, DOUBLE_DAMAGE_RANGE)) {
            return;
        }
        target.hurt(boss.damageSources().mobAttack(boss), getAttackDamage(DOUBLE_DAMAGE_MULTIPLIER));
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

        int prepareTick = attackOne ? ATTACK_1_PREPARE_TICK : ATTACK_2_PREPARE_TICK;
        int damageTick = attackOne ? ATTACK_1_DAMAGE_TICK : ATTACK_2_DAMAGE_TICK;

        int dashStartTick = damageTick - DASH_MOVE_TICKS + 1;

        if (this.attackTick == prepareTick && target != null) {
            beginPrepareMovement(target);
        }

        tickPrepareMovement();

        if (this.attackTick == dashStartTick && target != null) {
            beginDash(target, DASH_STOP_DISTANCE, DASH_MOVE_TICKS);
        }

        tickDashMovement();


        if (this.attackTick == damageTick) {

            playBossSound(ModSounds.SWING.get(), 2.5F, 1.0F);

            dealMeleeDamage(level, target);
        }


        if (this.attackTick >= MELEE_DURATION) {
            finishAttack();
        }
    }

    private void startThrowApproach(ServerPlayer target) {
        this.phase = AttackPhase.THROW_APPROACH;
        this.attackTick = 0;

        boss.setAnimState(AzumaalEntity.STATE_IDLE);
        boss.lookAtPlayer(target, 180.0F);

        beginDash(target, THROW_APPROACH_DISTANCE, THROW_APPROACH_TICKS);
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

        if (this.attackTick == THROW_UP_TICK) {
            this.throwConnected = launchTarget(level, target);
        }

        if (this.attackTick < THROW_UP_DURATION) {
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

        if (!this.cloneManager.isTargetInAttackRange(level, target, THROW_UP_RANGE)) {
            return false;
        }

        target.setDeltaMovement(0.0D, THROW_UP_VELOCITY, 0.0D);

        target.hurtMarked = true;
        target.fallDistance = 0.0F;

        target.serverLevel().sendParticles(Oasiso.PURPLE_STARS.get(), target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 28, 0.55D, 0.75D, 0.55D, 0.08D);
        return true;
    }


    private void startAirThrow(ServerPlayer target) {
        this.phase = AttackPhase.AIR_THROW;
        this.attackTick = 0;
        this.airChaseTicksRemaining = AIR_CHASE_TICKS;
        this.returnTicksRemaining = 0;

        boss.setAnimState(AzumaalEntity.STATE_AIR_THROW);
        boss.setDeltaMovement(Vec3.ZERO);
        boss.lookAtPlayer(target, 180.0F);
    }


    private void tickAirThrow(ServerLevel level) {
        this.attackTick++;

        ServerPlayer target = resolveTarget(level);

        if (target != null && this.attackTick < THROW_DOWN_TICK) {
            holdTargetInAir(level, target);
        }

        if (target != null) {
            boss.lookAtPlayer(target, 20.0F);

            if (this.attackTick <= AIR_CHASE_TICKS) {
                tickAirChase(target);
            } else if (this.attackTick < THROW_DOWN_TICK) {
                followAirTarget(target);
            }
        }

        if (this.attackTick == THROW_DOWN_TICK) {

            playBossSound(ModSounds.SWING.get(), 2.5F, 0.92F);

            throwTargetDown(level, target);

            this.returnTicksRemaining = AIR_THROW_DURATION - THROW_DOWN_TICK;
        }

        if (this.attackTick >= THROW_DOWN_TICK) {
            tickReturnToHover();
        }


        if (this.attackTick >= AIR_THROW_DURATION) {
            finishAttack();
        }
    }

    private void holdTargetInAir(ServerLevel level, ServerPlayer target) {
        Vec3 movement = target.getDeltaMovement();

        target.setDeltaMovement(movement.x * AIR_HOLD_HORIZONTAL_DAMPING, 0.0D, movement.z * AIR_HOLD_HORIZONTAL_DAMPING);

        target.hurtMarked = true;
        target.fallDistance = 0.0F;

        if (this.attackTick % AIR_HOLD_PARTICLE_INTERVAL == 0) {
            level.sendParticles(Oasiso.PURPLE_STARS.get(), target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 5, 0.4D, 0.65D, 0.4D, 0.025D);
        }
    }

    private void tickAirChase(ServerPlayer target) {
        int remaining = Math.max(1, this.airChaseTicksRemaining);

        double targetX = target.getX();
        double targetY = target.getY() + AIR_HEIGHT_OFFSET;
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
        double desiredY = target.getY() + AIR_HEIGHT_OFFSET;
        double desiredZ = target.getZ();

        final double follow = 0.28D;

        boss.setPos(boss.getX() + (desiredX - boss.getX()) * follow, boss.getY() + (desiredY - boss.getY()) * follow, boss.getZ() + (desiredZ - boss.getZ()) * follow);

        boss.setDeltaMovement(Vec3.ZERO);
    }


    private void throwTargetDown(ServerLevel level, ServerPlayer target) {
        if (target == null || !target.isAlive()) {
            return;
        }

        if (!this.cloneManager.isTargetInAttackRange(level, target, THROW_DOWN_RANGE)) {
            return;
        }

        target.serverLevel().sendParticles(Oasiso.PURPLE_STARS.get(), target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 38, 0.65D, 0.55D, 0.65D, 0.12D);

        target.hurt(boss.damageSources().mobAttack(boss), getAttackDamage(THROW_DOWN_DAMAGE_MULTIPLIER));

        Vec3 attackOrigin = this.cloneManager.getClosestMemberPosition(level, target);
        Vec3 horizontal = new Vec3(target.getX() - attackOrigin.x, 0.0D, target.getZ() - attackOrigin.z);

        if (horizontal.lengthSqr() < 0.01D) {
            Vec3 look = boss.getLookAngle();
            horizontal = new Vec3(look.x, 0.0D, look.z);
        }

        if (horizontal.lengthSqr() < 0.0001D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        }

        horizontal = horizontal.normalize().scale(THROW_DOWN_HORIZONTAL);

        target.setDeltaMovement(horizontal.x, THROW_DOWN_VERTICAL, horizontal.z);
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

        this.prepareStep = toTarget.normalize().scale(-PREPARE_BACK_DISTANCE / PREPARE_MOVE_TICKS);
        this.prepareTicksRemaining = PREPARE_MOVE_TICKS;
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
        if (!this.cloneManager.isTargetInAttackRange(level, target, DAMAGE_RANGE)) {
            return;
        }

        target.hurt(boss.damageSources().mobAttack(boss), getAttackDamage(MELEE_DAMAGE_MULTIPLIER));
    }

    private void finishAttack() {
        this.phase = AttackPhase.NONE;
        this.attackTick = 0;
        this.targetId = null;
        this.throwConnected = false;

        clearMovement();

        boss.setDeltaMovement(Vec3.ZERO);
        boss.setAnimState(AzumaalEntity.STATE_IDLE);

        this.attackCooldown = randomBetween(MIN_ATTACK_COOLDOWN, MAX_ATTACK_COOLDOWN);
    }


    private void clearMovement() {
        this.prepareStep = Vec3.ZERO;
        this.prepareTicksRemaining = 0;
        this.dashLanding = null;
        this.dashTicksRemaining = 0;
        this.airChaseTicksRemaining = 0;
        this.returnTicksRemaining = 0;
    }

    private ServerPlayer findNearestTarget(ServerLevel level) {
        ServerPlayer nearest = null;

        double nearestDistance = TARGET_SEARCH_RANGE * TARGET_SEARCH_RANGE;

        for (ServerPlayer player : level.players()) {
            if (!isValidTarget(player)) {
                continue;
            }

            double distance = boss.distanceToSqr(player);
            if (distance >= nearestDistance) {
                continue;
            }
            nearest = player;
            nearestDistance = distance;
        }
        return nearest;
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
        return player.isAlive() && !player.isSpectator() && !player.isCreative();
    }

    private int randomBetween(int minimum, int maximum) {
        return minimum + boss.getRandom().nextInt(maximum - minimum + 1);
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

        if (target == null && this.attackTick <= CLONE_SUMMON_DURATION) {
            finishAttack();
            return;
        }

        boss.setDeltaMovement(Vec3.ZERO);

        if (target != null) {
            boss.lookAtPlayer(target, 8.0F);
        }

        if (this.attackTick == CLONE_SUMMON_DURATION) {

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

        if (this.attackTick > CLONE_SUMMON_DURATION) {
            this.cloneManager.tickFormation();
        }


        if (this.attackTick >= CLONE_ATTACK_DURATION) {
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