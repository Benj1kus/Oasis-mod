package com.benji.oasiso.common.entity;

import com.benji.oasiso.config.OsirisRealmConfig;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.effect.ChaosChamberManager;
import com.benji.oasiso.common.dimension.BossArenaEncounter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import com.benji.oasiso.common.util.DamageNumberSpawner;
import com.benji.oasiso.common.entity.ai.AzumaalAttackController;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import com.benji.oasiso.network.dialogue.BossDialogueNetwork;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import com.benji.oasiso.common.entity.ai.AzumaalStageTwoAI;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import com.benji.oasiso.common.entity.ai.AzumaalDeathManager;

public class AzumaalEntity extends Monster implements GeoEntity, GlowmaskEntity {

    public static final int STATE_SPAWN = 0;
    public static final int STATE_DEATH = 10;
    public static final int STATE_SUMMON_2 = 8;
    public static final int STATE_SUMMON_1 = 7;
    public static final int STATE_ATTACK_DOUBLE = 6;

    private static final float STAGE_TWO_HITBOX_SIZE = 88.0F / 16.0F;
    public static final String BOSS_BAR_STAGE_TWO_KEY = "bossbar.oasiso.azumaal_stage2";

    public static final int SPLASH_NONE = 0;
    public static final int SPLASH_BOTH = 1;
    public static final int SPLASH_RIGHT = 2;
    public static final int SPLASH_LEFT = 3;

    private static final int TEXTURE_FRAME_COUNT = 6;
    private static final int TEXTURE_FRAME_TICKS = 2;

    public static final int STATE_ATTACK_THROW = 4;
    public static final int STATE_AIR_THROW = 5;
    public static final int STATE_IDLE = 1;
    public static final int STATE_ATTACK_1 = 2;
    public static final int STATE_ATTACK_2 = 3;
    public static final int STATE_EYES = 9;

    public static final int STATE_STAGE_TWO_BITE = 11;
    public static final int STATE_STAGE_TWO_DIG = 12;
    public static final int STATE_STAGE_TWO_JUMP = 13;
    public static final int STATE_STAGE_TWO_RUN = 14;
    public static final int STATE_STAGE_TWO_TENTACLE = 15;

    private static final int SPAWN_ANIMATION_TIME = 200;

    private static final double HOVER_AMPLITUDE = 0.18D;
    private static final double HOVER_SPEED = 0.08D;

    private static final double HOVER_GROUND_CLEARANCE = 2.0D;
    private static final double HOVER_GROUND_SCAN = 8.0D;

    private static final double HOVER_FALL_ACCELERATION = 0.018D;
    private static final double HOVER_MAX_FALL_SPEED = 0.22D;

    private static final double LOOK_RANGE = 30.0D;

    //pressure defense
    private static final int PRESSURE_HIT_WINDOW_TICKS = 40;
    private static final int PRESSURE_HIT_THRESHOLD = 6;
    private static final int PRESSURE_DAMAGE_WINDOW_TICKS = 100;
    private static final float PRESSURE_DAMAGE_THRESHOLD = 0.12F;
    private static final int PRESSURE_CHECK_COOLDOWN_TICKS = 80;
    private static final double PRESSURE_SHOCKWAVE_RADIUS = 10.5D;
    private static final double PRESSURE_PUSH_MIN = 6.5D;
    private static final double PRESSURE_PUSH_MAX = 8.5D;
    private static final byte PRESSURE_SHOCKWAVE_EVENT = 67;

    private static final EntityDataAccessor<Integer> ANIM_STATE = SynchedEntityData.defineId(AzumaalEntity.class, EntityDataSerializers.INT);

    private UUID cloneOwnerId;
    private double hoverFallSpeed;
    private double cloneOffsetX;
    private double cloneOffsetZ;

    private int cloneFormationAge;
    private int cloneFormationDuration = 1;

    private static final EntityDataAccessor<Float> HOVER_BASE_Y = SynchedEntityData.defineId(AzumaalEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DEATH_VISUAL_TICKS = SynchedEntityData.defineId(AzumaalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PARKOUR_ACTIVE = SynchedEntityData.defineId(AzumaalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DEFENDING = SynchedEntityData.defineId(AzumaalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CLONE_MODE = SynchedEntityData.defineId(AzumaalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> CLONE_INDEX = SynchedEntityData.defineId(AzumaalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> STAGE_TWO = SynchedEntityData.defineId(AzumaalEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation STAGE_TWO_BITE_ANIMATION = RawAnimation.begin().thenPlay("bite");
    private static final RawAnimation STAGE_TWO_DIG_ANIMATION = RawAnimation.begin().thenPlay("diggin");
    private static final RawAnimation STAGE_TWO_JUMP_ANIMATION = RawAnimation.begin().thenPlay("jump");
    private static final RawAnimation STAGE_TWO_RUN_ANIMATION = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation STAGE_TWO_TENTACLE_ANIMATION = RawAnimation.begin().thenPlay("tentacle");

    private static final RawAnimation EYES_ANIMATION = RawAnimation.begin().thenPlay("eyes");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlay("death");
    private static final RawAnimation SUMMON_1_ANIMATION = RawAnimation.begin().thenPlay("summon_1").thenLoop("idle");
    private static final RawAnimation SUMMON_2_ANIMATION = RawAnimation.begin().thenPlay("summon_2");
    private static final RawAnimation ATTACK_DOUBLE_ANIMATION = RawAnimation.begin().thenPlay("attack_double");
    private static final RawAnimation ATTACK_THROW_ANIMATION = RawAnimation.begin().thenPlay("attack_throw");
    private static final RawAnimation AIR_THROW_ANIMATION = RawAnimation.begin().thenPlay("air_throw");
    private static final RawAnimation ATTACK_1_ANIMATION = RawAnimation.begin().thenPlay("attack_1");
    private static final RawAnimation ATTACK_2_ANIMATION = RawAnimation.begin().thenPlay("attack_2");
    private static final RawAnimation SPAWN_ANIMATION = RawAnimation.begin().thenPlay("spawn");
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");

    private static final String SPAWN_TICKS_TAG = "AzumaalSpawnTicks";
    private static final String ANIM_STATE_TAG = "AzumaalAnimState";
    private static final String HOVER_Y_TAG = "AzumaalHoverY";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private final Deque<Long> pressureHitTicks = new ArrayDeque<>();
    private final Deque<PressureDamageSample> pressureDamageSamples = new ArrayDeque<>();
    private long pressureDefenseCooldownUntil;
    private long clientPressureShockwaveStartNanos;

    //bossbar
    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.oasiso.azumaal"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

    private final AzumaalAttackController attackController;
    private final AzumaalDeathManager deathManager;
    private final AzumaalStageTwoAI stageTwoAI;

    private int bladeSplashMode = SPLASH_NONE;
    private int spawnTicks;
// DIALOG

    private static final int INTRO_FAILSAFE_TICKS = 20 * 45;

    private boolean introLocked;
    private int introDialogueTicks;

    private final Set<UUID> introParticipants = new HashSet<>();
    private final Set<UUID> introPanelFinishedPlayers = new HashSet<>();
    private final Set<UUID> introDialogueStartedPlayers = new HashSet<>();
    private final Set<UUID> introDialogueFinishedPlayers = new HashSet<>();

    private UUID arenaSessionId;

    public AzumaalEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.deathManager = new AzumaalDeathManager(this);
        this.attackController = new AzumaalAttackController(this);
        this.stageTwoAI = new AzumaalStageTwoAI(this);
        this.setNoGravity(true);

        if (!level.isClientSide) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(OsirisRealmConfig.AZUMAAL_MAX_HEALTH.get());
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(OsirisRealmConfig.AZUMAAL_ATTACK_DAMAGE.get());
            this.setHealth(this.getMaxHealth());
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1500.0D).add(Attributes.MOVEMENT_SPEED, 0.4D).add(Attributes.KNOCKBACK_RESISTANCE, 1.0D).add(Attributes.ATTACK_DAMAGE, 22.0D).add(Attributes.FOLLOW_RANGE, LOOK_RANGE);
    }

    public void setBossPortal(BossPortalEntity portal) {
        this.deathManager.setPortal(portal);
    }

    public void setArenaSessionId(UUID sessionId) {
        this.arenaSessionId = sessionId;
    }

    public UUID getArenaSessionId() {
        return this.arenaSessionId;
    }

    public boolean isEncounterParticipant(ServerPlayer player) {
        if (player == null || player.serverLevel() != this.level() || !player.isAlive() || player.isSpectator()) {
            return false;
        }

        if (this.arenaSessionId == null) {
            return true;
        }

        return BossArenaEncounter.isPlayerInSession(player, this.arenaSessionId);
    }

    public boolean isDeathSequenceActive() {
        return this.getAnimState() == STATE_DEATH;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (!this.isClone()) {
            this.bossEvent.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();

        this.entityData.define(STAGE_TWO, false);
        this.entityData.define(DEATH_VISUAL_TICKS, 0);
        this.entityData.define(PARKOUR_ACTIVE, false);
        this.entityData.define(DEFENDING, false);
        this.entityData.define(ANIM_STATE, STATE_SPAWN);
        this.entityData.define(CLONE_MODE, false);
        this.entityData.define(CLONE_INDEX, 0);
        this.entityData.define(HOVER_BASE_Y, 0.0F);
    }

    public boolean isStageTwo() {
        return this.entityData.get(STAGE_TWO);
    }

    public float getStageTwoTriggerHealth() {
        return (float) Math.min(this.getMaxHealth(), Math.max(1.0D, OsirisRealmConfig.AZUMAAL_STAGE_TWO_TRIGGER_HEALTH.get()));
    }

    private void setStageTwo(boolean stageTwo) {
        this.entityData.set(STAGE_TWO, stageTwo);

        this.refreshDimensions();

        if (!this.level().isClientSide) {
            this.bossEvent.setName(Component.translatable(stageTwo ? BOSS_BAR_STAGE_TWO_KEY : "entity.oasiso.azumaal"));
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        if (isStageTwo()) {
            return EntityDimensions.scalable(STAGE_TWO_HITBOX_SIZE, STAGE_TWO_HITBOX_SIZE);
        }
        return super.getDimensions(pose);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (STAGE_TWO.equals(accessor)) {
            this.refreshDimensions();
        }
    }

    private void beginStageTwoTransition(ServerLevel level, DamageSource source) {
        if (this.isStageTwo() || this.deathManager.isActive()) {

            return;
        }
        this.setHealth(getStageTwoTriggerHealth());
        this.attackController.prepareForDeath(level);

        this.pressureHitTicks.clear();
        this.pressureDamageSamples.clear();
        this.pressureDefenseCooldownUntil = 0L;

        this.setDefending(false);
        this.setParkourActive(false);
        this.setDeltaMovement(Vec3.ZERO);

        this.deathManager.beginStageTransition(level, source);
    }

    public boolean isDefending() {
        return this.entityData.get(DEFENDING);
    }

    public int getDeathVisualTicks() {
        return this.entityData.get(DEATH_VISUAL_TICKS);
    }


    public void setDeathVisualTicks(int ticks) {
        this.entityData.set(DEATH_VISUAL_TICKS, ticks);
    }


    public void setDefending(boolean defending) {
        this.entityData.set(DEFENDING, defending);
    }

    public boolean isParkourActive() {
        return this.entityData.get(PARKOUR_ACTIVE);
    }


    public void setParkourActive(boolean active) {
        this.entityData.set(PARKOUR_ACTIVE, active);
    }


    //CLONe
    public boolean isClone() {
        return this.entityData.get(CLONE_MODE);
    }


    public int getCloneIndex() {
        return this.entityData.get(CLONE_INDEX);
    }


    public boolean isCloneOf(AzumaalEntity owner) {
        return this.isClone() && this.cloneOwnerId != null && this.cloneOwnerId.equals(owner.getUUID());
    }

    public void initializeClone(AzumaalEntity owner, int cloneIndex, Vec3 relativeOffset, int formationDuration) {
        this.entityData.set(CLONE_MODE, true);
        this.entityData.set(CLONE_INDEX, cloneIndex);

        this.cloneOwnerId = owner.getUUID();

        this.cloneOffsetX = relativeOffset.x;
        this.cloneOffsetZ = relativeOffset.z;

        this.cloneFormationAge = 0;
        this.cloneFormationDuration = Math.max(1, formationDuration);

        this.setHealth(1.0F);

        this.entityData.set(HOVER_BASE_Y, (float) owner.getHoverBaseY());

        this.setAnimState(owner.getAnimState());
        this.setNoGravity(true);
        this.setInvulnerable(false);
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected void registerGoals() {
    }

    public void startSpawnSequence(double hoverBaseY) {
        this.spawnTicks = 0;
        this.attackController.reset();

        this.introLocked = true;
        this.introDialogueTicks = 0;

        this.introParticipants.clear();
        this.introPanelFinishedPlayers.clear();
        this.introDialogueStartedPlayers.clear();
        this.introDialogueFinishedPlayers.clear();

        this.setDefending(false);
        this.setDeathVisualTicks(0);
        this.setParkourActive(false);

        this.entityData.set(HOVER_BASE_Y, (float) hoverBaseY);

        this.setAnimState(STATE_SPAWN);
        this.setNoGravity(true);
        this.setInvulnerable(true);

        this.setDeltaMovement(Vec3.ZERO);
    }


    private void tickStageTwoEntropyFlames() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.tickCount % 2 != 0) {
            return;
        }

        int particles = 1 + this.random.nextInt(2);

        for (int i = 0; i < particles; i++) {
            double x = this.getX() + (this.random.nextDouble() - 0.5D) * this.getBbWidth() * 1.55D;
            double y = this.getY() + 0.35D + this.random.nextDouble() * (this.getBbHeight() * 0.82D);
            double z = this.getZ() + (this.random.nextDouble() - 0.5D) * this.getBbWidth() * 1.55D;

            double vx = (this.random.nextDouble() - 0.5D) * 0.030D;
            double vy = 0.006D + this.random.nextDouble() * 0.018D;
            double vz = (this.random.nextDouble() - 0.5D) * 0.030D;

            serverLevel.sendParticles(Oasiso.ENTROPY_FLAME.get(), x, y, z, 1, vx, vy, vz, 0.0D);
        }
    }


    @Override
    public void tick() {
        super.tick();


        if (!this.level().isClientSide && !this.isClone()) {
            float progress;

            if (this.getMaxHealth() <= 0.0F) {
                progress = 0.0F;
            } else if (this.isDeathSequenceActive() && !this.deathManager.isStageTransition()) {
                progress = 0.0F;
            } else {
                progress = this.getHealth() / this.getMaxHealth();
            }

            this.bossEvent.setProgress(Mth.clamp(progress, 0.0F, 1.0F));
        }
        if (!this.level().isClientSide && !this.isClone() && this.level() instanceof ServerLevel serverLevel && this.tickCount % 10 == 0) {
            ChaosChamberManager.captureNearbyPlayers(serverLevel, this);
        }

        if (!this.isStageTwo()) {
            this.setNoGravity(true);
            this.fallDistance = 0.0F;
        } else {
            this.setNoGravity(false);
        }

        if (this.isClone()) {
            tickClone();
            return;
        }

        if (this.isDeathSequenceActive()) {
            this.setDeltaMovement(Vec3.ZERO);

            this.fallDistance = 0.0F;

            if (!this.level().isClientSide && this.level() instanceof ServerLevel level) {
                boolean stageTransition = this.deathManager.isStageTransition();
                boolean finished = this.deathManager.tick(level);
                if (finished) {
                    if (stageTransition) {
                        finishStageTwoTransition(level);
                    } else {
                        finishCustomDeath(level);
                    }
                }
            }
            return;
        }

        if (this.getAnimState() == STATE_SPAWN) {

            this.setDeltaMovement(Vec3.ZERO);
            this.setPos(this.getX(), this.getHoverBaseY(), this.getZ());

            if (!this.level().isClientSide) {
                this.setInvulnerable(true);
                this.spawnTicks++;
                if (this.spawnTicks >= SPAWN_ANIMATION_TIME) {
                    this.setAnimState(STATE_IDLE);
                    this.setInvulnerable(true);

                    if (this.level() instanceof ServerLevel serverLevel) {
                        collectIntroParticipants(serverLevel);
                        tryStartIntroDialogues(serverLevel, false);
                    }
                }
            }
            return;
        }

        if (!this.level().isClientSide && this.introLocked && this.level() instanceof ServerLevel serverLevel) {
            tickIntroDialogue(serverLevel);
            return;
        }

        if (this.isStageTwo()) {
            this.setNoGravity(false);

            Vec3 movement = this.getDeltaMovement();
            this.setDeltaMovement(0.0D, movement.y, 0.0D);

            if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
                this.setInvulnerable(false);
                this.stageTwoAI.tick(serverLevel);
                tickStageTwoEntropyFlames();
            }

            return;
        }

        int currentState = this.getAnimState();
        if (!this.isParkourActive() && currentState != STATE_ATTACK_THROW && currentState != STATE_AIR_THROW) {
            tickHover();
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }

        if (this.level().isClientSide && this.getAnimState() != STATE_ATTACK_1 && this.getAnimState() != STATE_ATTACK_2 && this.getAnimState() != STATE_ATTACK_DOUBLE) {
            this.bladeSplashMode = SPLASH_NONE;
        }

        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            this.setInvulnerable(false);
            if (this.attackController.isAttacking()) {
                this.attackController.tickAttack(serverLevel);
            } else {
                lookAtNearestPlayer(serverLevel);
                this.attackController.tickIdle(serverLevel);
            }
        }
    }

    private void tickClone() {
        if (this.level().isClientSide) {
            int state = this.getAnimState();
            if (state != STATE_ATTACK_1 && state != STATE_ATTACK_2 && state != STATE_ATTACK_DOUBLE) {
                this.bladeSplashMode = SPLASH_NONE;
            }
            return;
        }

        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }

        AzumaalEntity owner = resolveCloneOwner(level);
        if (owner == null || !owner.isAlive() || owner.isClone() || owner.isDeathSequenceActive()) {
            dissolveClone(level);
            return;
        }
        this.setDefending(owner.isDefending());
        if (this.cloneFormationAge < this.cloneFormationDuration) {
            this.cloneFormationAge++;
        }

        double progress = Mth.clamp(this.cloneFormationAge / (double) this.cloneFormationDuration, 0.0D, 1.0D);

        double targetX = owner.getX() + this.cloneOffsetX * progress;
        double targetZ = owner.getZ() + this.cloneOffsetZ * progress;

        this.setPos(targetX, owner.getY(), targetZ);
        this.setDeltaMovement(Vec3.ZERO);

        this.setAnimState(owner.getAnimState());
        this.setYRot(owner.getYRot());
        this.setYHeadRot(owner.getYHeadRot());

        this.yBodyRot = owner.yBodyRot;
        this.yBodyRotO = owner.yBodyRotO;
    }

    private AzumaalEntity resolveCloneOwner(ServerLevel level) {
        if (this.cloneOwnerId == null) {
            return null;
        }
        Entity entity = level.getEntity(this.cloneOwnerId);
        if (entity instanceof AzumaalEntity owner) {
            return owner;
        }
        return null;
    }


    private void tickHover() {

        double baseY = this.getHoverBaseY();

        Double groundY = findGroundBelow(baseY);
        if (groundY != null) {

            double desiredBaseY = groundY + HOVER_GROUND_CLEARANCE;
            if (desiredBaseY < baseY - 0.03D) {

                this.hoverFallSpeed = Math.min(HOVER_MAX_FALL_SPEED, this.hoverFallSpeed + HOVER_FALL_ACCELERATION);

                baseY = Math.max(desiredBaseY, baseY - this.hoverFallSpeed);

                this.entityData.set(HOVER_BASE_Y, (float) baseY);

            } else {

                this.hoverFallSpeed = 0.0D;
            }

        } else {
            this.hoverFallSpeed = Math.min(HOVER_MAX_FALL_SPEED, this.hoverFallSpeed + HOVER_FALL_ACCELERATION);

            baseY -= this.hoverFallSpeed;

            this.entityData.set(HOVER_BASE_Y, (float) baseY);
        }

        double hoverOffset = Math.sin(this.tickCount * HOVER_SPEED) * HOVER_AMPLITUDE;
        this.setPos(this.getX(), baseY + hoverOffset, this.getZ());
        this.setDeltaMovement(Vec3.ZERO);
    }

    private Double findGroundBelow(double baseY) {

        Vec3 start = new Vec3(this.getX(), baseY + 0.25D, this.getZ());
        Vec3 end = new Vec3(this.getX(), baseY - HOVER_GROUND_SCAN, this.getZ());

        BlockHitResult hit = this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

        if (hit.getType() == HitResult.Type.MISS) {

            return null;
        }

        return hit.getLocation().y;
    }

    private void lookAtNearestPlayer(ServerLevel level) {
        Player nearestPlayer = level.getNearestPlayer(this, LOOK_RANGE);
        if (nearestPlayer == null || nearestPlayer.isSpectator() || nearestPlayer.isCreative()) {
            return;
        }
        lookAtPlayer(nearestPlayer, 5.0F);
    }
// dialogue methods

    public void onIntroPanelFinished(ServerPlayer player) {
        if (this.isClone() || !this.introLocked || !isIntroCandidate(player)) {

            return;
        }

        this.introParticipants.add(player.getUUID());

        this.introPanelFinishedPlayers.add(player.getUUID());

        if (this.level() instanceof ServerLevel level) {

            tryStartIntroDialogues(level, false);
        }
    }

    private void collectIntroParticipants(ServerLevel level) {
        double rangeSqr = 96.0D * 96.0D;

        for (ServerPlayer player : level.players()) {

            if (!isIntroCandidate(player)) {
                continue;
            }

            if (this.distanceToSqr(player) > rangeSqr) {

                continue;
            }

            this.introParticipants.add(player.getUUID());
        }
    }

    private boolean isIntroCandidate(ServerPlayer player) {
        if (player == null || player.serverLevel() != this.level() || !player.isAlive() || player.isSpectator()) {

            return false;
        }

        if (this.arenaSessionId == null) {
            return true;
        }

        return BossArenaEncounter.isPlayerInSession(player, this.arenaSessionId);
    }

    private void tryStartIntroDialogues(ServerLevel level, boolean forceWithoutPanel) {
        if (!this.introLocked || this.getAnimState() == STATE_SPAWN) {

            return;
        }

        collectIntroParticipants(level);

        for (UUID playerId : new HashSet<>(this.introParticipants)) {

            if (this.introDialogueStartedPlayers.contains(playerId)) {

                continue;
            }

            if (!forceWithoutPanel && !this.introPanelFinishedPlayers.contains(playerId)) {

                continue;
            }

            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);

            if (player == null || !isIntroCandidate(player)) {

                continue;
            }

            this.introDialogueStartedPlayers.add(playerId);

            BossDialogueNetwork.startDialogue(player, this.getUUID(), "azumaal");
        }
    }

    private void tickIntroDialogue(ServerLevel level) {
        this.setInvulnerable(true);
        this.setDeltaMovement(Vec3.ZERO);
        this.fallDistance = 0.0F;

        this.introDialogueTicks++;

        if (this.introDialogueTicks % 10 == 0) {
            collectIntroParticipants(level);
            pruneUnavailableIntroParticipants(level);
        }

        boolean forceStart = this.introDialogueTicks >= 120;

        tryStartIntroDialogues(level, forceStart);

        ServerPlayer lookTarget = findClosestIntroParticipant(level);

        if (lookTarget != null) {
            this.lookAtPlayer(lookTarget, 5.0F);
        }

        if (!this.introParticipants.isEmpty() && allIntroParticipantsFinished()) {

            finishIntroDialogueInternal();
            return;
        }

        if (this.introParticipants.isEmpty() && this.introDialogueTicks >= 120) {

            finishIntroDialogueInternal();
            return;
        }

        if (this.introDialogueTicks >= INTRO_FAILSAFE_TICKS) {

            finishIntroDialogueInternal();
        }
    }

    private void pruneUnavailableIntroParticipants(ServerLevel level) {
        Iterator<UUID> iterator = this.introParticipants.iterator();

        while (iterator.hasNext()) {
            UUID playerId = iterator.next();

            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);

            if (player != null && isIntroCandidate(player)) {

                continue;
            }

            iterator.remove();
            this.introPanelFinishedPlayers.remove(playerId);
            this.introDialogueStartedPlayers.remove(playerId);
            this.introDialogueFinishedPlayers.remove(playerId);
        }
    }

    private ServerPlayer findClosestIntroParticipant(ServerLevel level) {
        ServerPlayer closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (UUID playerId : this.introParticipants) {

            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);

            if (player == null || !isIntroCandidate(player)) {

                continue;
            }

            double distance = this.distanceToSqr(player);

            if (distance >= closestDistance) {
                continue;
            }

            closest = player;
            closestDistance = distance;
        }

        return closest;
    }

    private boolean allIntroParticipantsFinished() {
        for (UUID playerId : this.introParticipants) {
            if (!this.introDialogueStartedPlayers.contains(playerId) || !this.introDialogueFinishedPlayers.contains(playerId)) {

                return false;
            }
        }

        return true;
    }

    public void finishIntroDialogue(ServerPlayer player) {
        if (!this.introLocked || player == null) {

            return;
        }

        UUID playerId = player.getUUID();

        if (!this.introParticipants.contains(playerId) && !this.introDialogueStartedPlayers.contains(playerId)) {

            return;
        }

        this.introDialogueFinishedPlayers.add(playerId);

        if (allIntroParticipantsFinished()) {
            finishIntroDialogueInternal();
        }
    }

    private void finishIntroDialogueInternal() {
        this.introLocked = false;
        this.introDialogueTicks = 0;

        this.introParticipants.clear();
        this.introPanelFinishedPlayers.clear();
        this.introDialogueStartedPlayers.clear();
        this.introDialogueFinishedPlayers.clear();

        this.setInvulnerable(false);
        this.setDeltaMovement(Vec3.ZERO);

        if (this.level() instanceof ServerLevel serverLevel) {

            ChaosChamberManager.captureNearbyPlayers(serverLevel, this);
        }

        this.attackController.beginPostSpawnCooldown();
    }

    public boolean isIntroLocked() {
        return this.introLocked;
    }

    public void lookAtPlayer(Player target, float maxRotationStep) {
        double deltaX = target.getX() - this.getX();
        double deltaZ = target.getZ() - this.getZ();

        float targetYaw = (float) (Mth.atan2(deltaZ, deltaX) * (180.0D / Math.PI)) - 90.0F;
        float yawDifference = Mth.wrapDegrees(targetYaw - this.getYRot());

        float rotationStep = Mth.clamp(yawDifference, -maxRotationStep, maxRotationStep);

        float newYaw = this.getYRot() + rotationStep;

        this.setYRot(newYaw);
        this.setYHeadRot(newYaw);

        this.yBodyRot = newYaw;
        this.yBodyRotO = newYaw;

        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    public int getAnimState() {
        return this.entityData.get(ANIM_STATE);
    }

    public void setAnimState(int state) {
        this.entityData.set(ANIM_STATE, state);
    }

    public double getHoverBaseY() {
        return this.entityData.get(HOVER_BASE_Y);
    }

    public boolean isBladeSplashActive() {
        return this.bladeSplashMode != SPLASH_NONE;
    }

    public int getBladeSplashMode() {
        return this.bladeSplashMode;
    }


    private void playClientBossSound(SoundEvent sound, float volume, float pitch) {
        this.level().playLocalSound(this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(), sound, SoundSource.HOSTILE, volume, pitch, false);
    }

    private void handleClientInstruction(String instruction) {
        if (instruction == null) {
            return;
        }
        String normalized = instruction.replace(";", "").trim();
        switch (normalized) {
            case "ding_1", "ding_2" -> {
                if (!this.isClone()) {
                    playClientBossSound(ModSounds.SCISSORS.get(), 1.35F, 1.0F);
                }
            }
            case "ding_3" -> {
                if (!this.isClone()) {
                    playClientBossSound(ModSounds.SCISSORS.get(), 1.35F, 1.0F);
                    playClientBossSound(ModSounds.BOSS_SPAWN.get(), 1.8F, 1.0F);
                }
            }

            case "attack1_splash_start", "attack2_splash_start" -> this.bladeSplashMode = SPLASH_RIGHT; //ИЗМЕНИЛ ТУТ
            case "attack1_splash_end", "attack2_splash_end" -> this.bladeSplashMode = SPLASH_NONE;

            case "double_splash_start1" -> this.bladeSplashMode = SPLASH_RIGHT;
            case "double_splash_end1" -> this.bladeSplashMode = SPLASH_NONE;

            case "double_splash_start2" -> this.bladeSplashMode = SPLASH_LEFT;
            case "double_splash_end2" -> this.bladeSplashMode = SPLASH_NONE;
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        tag.putBoolean("AzumaalParkourActive", this.isParkourActive());
        tag.putBoolean("AzumaalDefending", this.isDefending());
        tag.putBoolean("AzumaalIsClone", this.isClone());
        tag.putBoolean("AzumaalStageTwo", this.isStageTwo());

        tag.putInt("AzumaalCloneIndex", this.getCloneIndex());
        if (this.cloneOwnerId != null) {
            tag.putUUID("AzumaalCloneOwner", this.cloneOwnerId);
        }

        tag.putDouble("AzumaalCloneOffsetX", this.cloneOffsetX);
        tag.putDouble("AzumaalCloneOffsetZ", this.cloneOffsetZ);

        tag.putInt("AzumaalCloneFormationAge", this.cloneFormationAge);
        tag.putInt("AzumaalCloneFormationDuration", this.cloneFormationDuration);

        tag.putInt(SPAWN_TICKS_TAG, this.spawnTicks);
        tag.putInt(ANIM_STATE_TAG, this.getAnimState());

        tag.putFloat(HOVER_Y_TAG, this.entityData.get(HOVER_BASE_Y));

        if (this.arenaSessionId != null) {
            tag.putUUID("AzumaalArenaSession", this.arenaSessionId);
        }

        tag.putBoolean("AzumaalIntroLocked", this.introLocked);
        tag.putInt("AzumaalIntroTicks", this.introDialogueTicks);

        this.deathManager.save(tag);

        if (!this.isClone()) {
            this.attackController.save(tag);
            this.stageTwoAI.save(tag);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        this.setParkourActive(tag.getBoolean("AzumaalParkourActive"));
        this.entityData.set(CLONE_MODE, tag.getBoolean("AzumaalIsClone"));
        this.setDefending(tag.getBoolean("AzumaalDefending"));
        this.entityData.set(CLONE_INDEX, tag.getInt("AzumaalCloneIndex"));

        this.cloneOwnerId = tag.hasUUID("AzumaalCloneOwner") ? tag.getUUID("AzumaalCloneOwner") : null;

        this.cloneOffsetX = tag.getDouble("AzumaalCloneOffsetX");
        this.cloneOffsetZ = tag.getDouble("AzumaalCloneOffsetZ");

        this.cloneFormationAge = tag.getInt("AzumaalCloneFormationAge");
        this.cloneFormationDuration = Math.max(1, tag.getInt("AzumaalCloneFormationDuration"));

        this.spawnTicks = tag.getInt(SPAWN_TICKS_TAG);

        this.setAnimState(tag.contains(ANIM_STATE_TAG) ? tag.getInt(ANIM_STATE_TAG) : STATE_SPAWN);

        if (tag.contains(HOVER_Y_TAG)) {
            this.entityData.set(HOVER_BASE_Y, tag.getFloat(HOVER_Y_TAG));
        } else {
            this.entityData.set(HOVER_BASE_Y, (float) this.getY());
        }
        this.arenaSessionId = tag.hasUUID("AzumaalArenaSession") ? tag.getUUID("AzumaalArenaSession") : null;

        this.introLocked = tag.getBoolean("AzumaalIntroLocked");

        this.introDialogueTicks = tag.getInt("AzumaalIntroTicks");

        this.entityData.set(STAGE_TWO, tag.getBoolean("AzumaalStageTwo"));

        this.refreshDimensions();
        if (!this.level().isClientSide) {
            this.bossEvent.setName(Component.translatable(this.isStageTwo() ? BOSS_BAR_STAGE_TWO_KEY : "entity.oasiso.azumaal"));
        }

        this.setNoGravity(!this.isStageTwo());

        this.setInvulnerable(this.getAnimState() == STATE_SPAWN || this.introLocked || this.isDeathSequenceActive());

        this.deathManager.load(tag);

        if (!this.isClone()) {
            this.attackController.load(tag);
            this.stageTwoAI.load(tag);
        }
    }

    public boolean hasBladeParticleAura() {
        int state = this.getAnimState();
        return state == STATE_ATTACK_THROW || state == STATE_AIR_THROW;
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<AzumaalEntity> controller = new AnimationController<>(this, "controller", 0,

                state -> {
                    return switch (this.getAnimState()) {
                        case STATE_STAGE_TWO_BITE -> state.setAndContinue(STAGE_TWO_BITE_ANIMATION);
                        case STATE_STAGE_TWO_DIG -> state.setAndContinue(STAGE_TWO_DIG_ANIMATION);
                        case STATE_STAGE_TWO_JUMP -> state.setAndContinue(STAGE_TWO_JUMP_ANIMATION);
                        case STATE_STAGE_TWO_RUN -> state.setAndContinue(STAGE_TWO_RUN_ANIMATION);
                        case STATE_STAGE_TWO_TENTACLE -> state.setAndContinue(STAGE_TWO_TENTACLE_ANIMATION);
                        case STATE_DEATH -> state.setAndContinue(DEATH_ANIMATION);
                        case STATE_EYES -> state.setAndContinue(EYES_ANIMATION);
                        case STATE_SUMMON_2 -> state.setAndContinue(SUMMON_2_ANIMATION);
                        case STATE_SUMMON_1 -> state.setAndContinue(SUMMON_1_ANIMATION);
                        case STATE_ATTACK_DOUBLE -> state.setAndContinue(ATTACK_DOUBLE_ANIMATION);
                        case STATE_ATTACK_THROW -> state.setAndContinue(ATTACK_THROW_ANIMATION);
                        case STATE_AIR_THROW -> state.setAndContinue(AIR_THROW_ANIMATION);
                        case STATE_SPAWN -> state.setAndContinue(SPAWN_ANIMATION);
                        case STATE_ATTACK_1 -> state.setAndContinue(ATTACK_1_ANIMATION);
                        case STATE_ATTACK_2 -> state.setAndContinue(ATTACK_2_ANIMATION);
                        default -> state.setAndContinue(IDLE_ANIMATION);
                    };
                });

        controller.setCustomInstructionKeyframeHandler(event -> {
            if (!this.level().isClientSide) {
                return;
            }
            handleClientInstruction(event.getKeyframeData().getInstructions());
        });
        controllers.add(controller);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.isClone() && this.introLocked) {
            return false;
        }
        if (!this.isClone() && this.isDeathSequenceActive()) {
            return false;
        }
        if (!this.isClone() && this.isDefending()) {
            if (!this.level().isClientSide && this.level() instanceof ServerLevel level && amount > 0.0F) {
                DamageNumberSpawner.spawn(level, this, 0.0F);
            }
            return true;
        }
        if (!this.isClone() && this.isParkourActive() && source.is(DamageTypeTags.IS_PROJECTILE)) {
            if (!this.level().isClientSide && this.level() instanceof ServerLevel level) {
                DamageNumberSpawner.spawn(level, this, 0.0F);
            }
            return true;
        }

        if (!this.isClone()) {
            Player parkourMeleePlayer = this.isParkourActive() && source.getDirectEntity() instanceof Player player ? player : null;
            float healthBefore = this.getHealth();

            boolean damaged = super.hurt(source, amount);

            if (damaged && parkourMeleePlayer != null && !this.level().isClientSide) {
                this.attackController.onParkourMeleeHit(parkourMeleePlayer);
            }

            float stageTwoTriggerHealth = getStageTwoTriggerHealth();

            boolean shouldStartStageTwo = !this.level().isClientSide && damaged && !this.isStageTwo() && !this.deathManager.isActive() && this.getHealth() <= stageTwoTriggerHealth;

            if (shouldStartStageTwo && this.getHealth() < stageTwoTriggerHealth) {
                this.setHealth(stageTwoTriggerHealth);
            }

            if (!this.level().isClientSide && damaged && this.level() instanceof ServerLevel level) {
                float actualDamage = healthBefore - this.getHealth();
                if (actualDamage > 0.0F) {
                    DamageNumberSpawner.spawn(level, this, actualDamage);

                    if (!this.isStageTwo() && !this.isDeathSequenceActive() && source.getEntity() instanceof Player attackingPlayer && !attackingPlayer.isCreative() && !attackingPlayer.isSpectator()) {
                        registerPressureDefenseHit(level, actualDamage);
                    }
                }
            }
            if (shouldStartStageTwo && this.level() instanceof ServerLevel level) {
                beginStageTwoTransition(level, source);
            }
            return damaged;
        }

        if (!(source.getEntity() instanceof Player player) || player.isSpectator()) {
            return false;
        }
        if (!(this.level() instanceof ServerLevel level)) {
            return true;
        }

        dissolveClone(level);
        return true;
    }

    private void registerPressureDefenseHit(ServerLevel level, float actualDamage) {
        long now = level.getGameTime();

        if (now < this.pressureDefenseCooldownUntil) {
            this.pressureHitTicks.clear();
            this.pressureDamageSamples.clear();
            return;
        }

        this.pressureHitTicks.addLast(now);
        this.pressureDamageSamples.addLast(new PressureDamageSample(now, actualDamage));

        while (!this.pressureHitTicks.isEmpty() && now - this.pressureHitTicks.peekFirst() > PRESSURE_HIT_WINDOW_TICKS) {
            this.pressureHitTicks.removeFirst();
        }

        while (!this.pressureDamageSamples.isEmpty() && now - this.pressureDamageSamples.peekFirst().tick() > PRESSURE_DAMAGE_WINDOW_TICKS) {
            this.pressureDamageSamples.removeFirst();
        }

        float recentDamage = 0.0F;
        for (PressureDamageSample sample : this.pressureDamageSamples) {
            recentDamage += sample.damage();
        }

        boolean rapidHits = this.pressureHitTicks.size() >= PRESSURE_HIT_THRESHOLD;
        boolean burstDamage = recentDamage >= this.getMaxHealth() * PRESSURE_DAMAGE_THRESHOLD;

        if (!rapidHits && !burstDamage) {
            return;
        }

        this.pressureHitTicks.clear();
        this.pressureDamageSamples.clear();
        this.pressureDefenseCooldownUntil = now + PRESSURE_CHECK_COOLDOWN_TICKS;

        if (this.getRandom().nextBoolean()) {
            performPressureShockwave(level);
        }
    }

    private void performPressureShockwave(ServerLevel level) {
        level.broadcastEntityEvent(this, PRESSURE_SHOCKWAVE_EVENT);

        level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 0.85F, 1.25F);

        double radiusSqr = PRESSURE_SHOCKWAVE_RADIUS * PRESSURE_SHOCKWAVE_RADIUS;

        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
                continue;
            }
            if (this.distanceToSqr(player) > radiusSqr) {
                continue;
            }

            Vec3 away = new Vec3(player.getX() - this.getX(), 0.0D, player.getZ() - this.getZ());

            if (away.lengthSqr() < 0.0001D) {
                Vec3 look = this.getLookAngle();
                away = new Vec3(-look.x, 0.0D, -look.z);
            }

            if (away.lengthSqr() < 0.0001D) {
                away = new Vec3(0.0D, 0.0D, 1.0D);
            }

            double push = Mth.lerp(this.getRandom().nextDouble(), PRESSURE_PUSH_MIN, PRESSURE_PUSH_MAX);

            Vec3 horizontal = away.normalize().scale(push);
            Vec3 oldMovement = player.getDeltaMovement();
            player.setDeltaMovement(horizontal.x, oldMovement.y, horizontal.z);
            player.hasImpulse = true;
            player.hurtMarked = true;
        }
    }

    public long getClientPressureShockwaveStartNanos() {
        return this.clientPressureShockwaveStartNanos;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == PRESSURE_SHOCKWAVE_EVENT) {
            this.clientPressureShockwaveStartNanos = System.nanoTime();
            return;
        }

        super.handleEntityEvent(id);
    }

    private record PressureDamageSample(long tick, float damage) {
    }

    private void finishStageTwoTransition(ServerLevel level) {
        this.setStageTwo(true);

        this.setHealth(getStageTwoTriggerHealth());

        this.setDeathVisualTicks(0);
        this.setDefending(false);
        this.setParkourActive(false);
        this.setAnimState(STATE_IDLE);
        this.setNoGravity(false);
        this.setInvulnerable(false);
        this.setDeltaMovement(Vec3.ZERO);
        this.hoverFallSpeed = 0.0D;
        this.refreshDimensions();
        this.stageTwoAI.beginStageTwo();

        level.sendParticles(Oasiso.MELTED_SPLASH.get(), this.getX(), this.getY() + this.getBbHeight() * 0.48D, this.getZ(), 180, 2.35D, 2.35D, 2.35D, 0.16D);
    }

    private void finishCustomDeath(ServerLevel level) {
        ChaosChamberManager.releasePlayers(level.getServer(), this.getUUID());
        // credit player
        ServerPlayer killer = this.deathManager.resolveKiller(level);
        DamageSource finalSource;
        if (killer != null) {
            this.setLastHurtByPlayer(killer);
            finalSource = level.damageSources().playerAttack(killer);
        } else {
            finalSource = level.damageSources().generic();
        }
        this.setHealth(0.0F);
        super.die(finalSource);
        this.discard();
    }

    private void dissolveClone(ServerLevel level) {
        level.sendParticles(Oasiso.PURPLE_STARS.get(), this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(), 55, 0.8D, 2.1D, 0.8D, 0.13D);
        this.discard();
    }

    @Override
    public void die(DamageSource source) {
        if (this.isClone()) {
            super.die(source);
            return;
        }

        if (this.isDeathSequenceActive()) {
            return;
        }

        if (!(this.level() instanceof ServerLevel level)) {

            return;
        }
        if (!this.isStageTwo()) {
            beginStageTwoTransition(level, source);
            return;
        }

        this.stageTwoAI.reset();
        this.attackController.prepareForDeath(level);
        this.deathManager.begin(level, source);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        SoundEvent[] sounds = {ModSounds.AZUMAAL_IDLE1.get(), ModSounds.AZUMAAL_IDLE2.get(), ModSounds.AZUMAAL_IDLE3.get()};
        return sounds[this.random.nextInt(sounds.length)];
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return ModSounds.AZUMAAL_HIT.get();
    }

    public int getTextureAnimationFrame() {
        return (this.tickCount / TEXTURE_FRAME_TICKS) % TEXTURE_FRAME_COUNT;
    }

    public ResourceLocation getMainTexture() {
        if (this.isStageTwo()) {
            return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/osiris_stage2.png");
        }
        String baseName = this.isDefending() ? "azumaal_defend" : "azumaal";
        return buildAnimatedTexture("textures/entity/", baseName);
    }

    public ResourceLocation getAnimatedEmissiveTexture() {
        if (this.isStageTwo()) {
            return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/osiris_stage2_emissive.png");
        }
        return buildAnimatedTexture("textures/entity/emissive/", "azumaal_emissive");
    }

    private ResourceLocation buildAnimatedTexture(String folder, String baseName) {
        int frame = getTextureAnimationFrame();
        String textureName = frame == 0 ? baseName : baseName + "_frame" + (frame + 1);
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, folder + textureName + ".png");
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return getAnimatedEmissiveTexture();
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}