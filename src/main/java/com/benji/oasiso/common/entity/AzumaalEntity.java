package com.benji.oasiso.common.entity;

import com.benji.oasiso.config.OsirisRealmConfig;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.effect.ChaosChamberManager;
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

import java.util.ArrayDeque;
import java.util.Deque;
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

    private static final int SPAWN_ANIMATION_TIME = 200;

    private static final double HOVER_AMPLITUDE = 0.18D;
    private static final double HOVER_SPEED = 0.08D;
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


    private static final RawAnimation EYES_ANIMATION = RawAnimation.begin().thenPlay("eyes");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlay("death");
    private static final RawAnimation SUMMON_1_ANIMATION = RawAnimation.begin().thenPlay("summon_1");
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

    private int bladeSplashMode = SPLASH_NONE;
    private int spawnTicks;
// DIALOG

    private static final int INTRO_FAILSAFE_TICKS = 20 * 45;

    private boolean introLocked;
    private boolean introPanelFinished;
    private boolean introDialogueStarted;

    private int introDialogueTicks;

    private UUID introPlayerId;

    public AzumaalEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.deathManager = new AzumaalDeathManager(this);
        this.attackController = new AzumaalAttackController(this);
        this.setNoGravity(true);

        if (!level.isClientSide) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(OsirisRealmConfig.AZUMAAL_MAX_HEALTH.get());
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(OsirisRealmConfig.AZUMAAL_ATTACK_DAMAGE.get());
            this.setHealth(this.getMaxHealth());
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1500.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 22.0D)
                .add(Attributes.FOLLOW_RANGE, LOOK_RANGE);
    }

    public void setBossPortal(BossPortalEntity portal) {
        this.deathManager.setPortal(portal);
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

        this.entityData.define(DEATH_VISUAL_TICKS, 0);
        this.entityData.define(PARKOUR_ACTIVE, false);
        this.entityData.define(DEFENDING, false);
        this.entityData.define(ANIM_STATE, STATE_SPAWN);
        this.entityData.define(CLONE_MODE, false);
        this.entityData.define(CLONE_INDEX, 0);
        this.entityData.define(HOVER_BASE_Y, 0.0F);
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
        this.introPanelFinished = false;
        this.introDialogueStarted = false;

        this.introDialogueTicks = 0;
        this.introPlayerId = null;

        this.setDefending(false);
        this.setDeathVisualTicks(0);
        this.setParkourActive(false);

        this.entityData.set(HOVER_BASE_Y, (float) hoverBaseY);

        this.setAnimState(STATE_SPAWN);
        this.setNoGravity(true);
        this.setInvulnerable(true);

        this.setDeltaMovement(Vec3.ZERO);
    }


    @Override
    public void tick() {
        super.tick();


        if (!this.level().isClientSide && !this.isClone()) {
            float progress = this.isDeathSequenceActive() || this.getMaxHealth() <= 0.0F ? 0.0F : this.getHealth() / this.getMaxHealth();
            this.bossEvent.setProgress(Mth.clamp(progress, 0.0F, 1.0F));
        }
        if (!this.level().isClientSide && !this.isClone() && this.level() instanceof ServerLevel serverLevel) {
            ChaosChamberManager.captureNearbyPlayers(serverLevel, this);
        }
        this.setNoGravity(true);
        this.fallDistance = 0.0F;
        if (this.isClone()) {
            tickClone();
            return;
        }
        if (this.isDeathSequenceActive()) {
            this.setDeltaMovement(Vec3.ZERO);
            this.fallDistance = 0.0F;
            if (!this.level().isClientSide && this.level() instanceof ServerLevel level) {
                boolean finished = this.deathManager.tick(level);
                if (finished) {
                    finishCustomDeath(level);
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
                        tryStartIntroDialogue(
                                serverLevel
                        );
                    }
                }
            }
            return;
        }

        if (!this.level().isClientSide
                && this.introLocked
                && this.level() instanceof ServerLevel serverLevel) {

            tickIntroDialogue(
                    serverLevel
            );

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
        double hoverOffset = Math.sin(this.tickCount * HOVER_SPEED) * HOVER_AMPLITUDE;
        double targetY = this.getHoverBaseY() + hoverOffset;

        this.setPos(this.getX(), targetY, this.getZ());
        this.setDeltaMovement(Vec3.ZERO);
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
    if (this.isClone() || !this.introLocked) {
        return;
    }
    this.introPanelFinished = true;
    this.introPlayerId = player.getUUID();

    if (this.level() instanceof ServerLevel level) {
        tryStartIntroDialogue(level);
    }
}


    private void tryStartIntroDialogue(ServerLevel level) {
        if (!this.introLocked || this.introDialogueStarted || !this.introPanelFinished || this.getAnimState() == STATE_SPAWN) {
            return;
        }

        ServerPlayer player = resolveIntroPlayer(level);

        if (player == null) {
            Player nearest = level.getNearestPlayer(this, 96.0D);

            if (nearest instanceof ServerPlayer serverPlayer) {
                player = serverPlayer;
                this.introPlayerId = serverPlayer.getUUID();
            }
        }
        if (player == null) {
            return;
        }

        this.introDialogueStarted = true;
        this.introDialogueTicks = 0;
        this.setInvulnerable(true);
        this.setDeltaMovement(Vec3.ZERO);

        BossDialogueNetwork.startDialogue(player, this.getUUID(), "azumaal");
    }

    private void tickIntroDialogue(ServerLevel level) {
        this.setInvulnerable(true);
        this.setDeltaMovement(Vec3.ZERO);
        this.fallDistance = 0.0F;
        ServerPlayer player = resolveIntroPlayer(level);

        if (player != null) {
            this.lookAtPlayer(player, 5.0F);
        }

        this.introDialogueTicks++;

        if (!this.introPanelFinished && this.introDialogueTicks >= 120) {
            this.introPanelFinished = true;
            tryStartIntroDialogue(level);
            this.introDialogueTicks = 0;
            return;
        }

        if (this.introDialogueStarted && this.introDialogueTicks >= INTRO_FAILSAFE_TICKS) {
            finishIntroDialogueInternal();
        }
    }


    private ServerPlayer resolveIntroPlayer(ServerLevel level) {
        if (this.introPlayerId == null) {
            return null;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(this.introPlayerId);
        if (player == null || player.serverLevel() != level) {
            return null;
        }
        return player;
    }


    public void finishIntroDialogue(ServerPlayer player) {
        if (!this.introLocked || !this.introDialogueStarted) {
            return;
        }
        if (this.introPlayerId != null && !this.introPlayerId.equals(player.getUUID())) {
            return;
        }
        finishIntroDialogueInternal();
    }


    private void finishIntroDialogueInternal() {
        this.introLocked = false;
        this.introPanelFinished = false;
        this.introDialogueStarted = false;

        this.introDialogueTicks = 0;
        this.introPlayerId = null;

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

        this.deathManager.save(tag);

        if (!this.isClone()) {
            this.attackController.save(tag);
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
        this.setNoGravity(true);
        this.setInvulnerable(this.getAnimState() == STATE_SPAWN);
        this.deathManager.load(tag);
        if (!this.isClone()) {
            this.attackController.load(tag);
        }
    }
    public boolean hasBladeParticleAura() {
        int state = this.getAnimState();
        return state == STATE_ATTACK_THROW || state == STATE_AIR_THROW;
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<AzumaalEntity> controller = new AnimationController<>(this,
                "controller",
                0,

                state -> {
                    return switch (this.getAnimState()) {
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
        if (!this.isClone()
                && this.introLocked) {
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

            if (!this.level().isClientSide && damaged && this.level() instanceof ServerLevel level) {
                float actualDamage = healthBefore - this.getHealth();
                if (actualDamage > 0.0F) {
                    DamageNumberSpawner.spawn(level, this, actualDamage);

                    if (!this.isDeathSequenceActive()
                            && source.getEntity() instanceof Player attackingPlayer
                            && !attackingPlayer.isCreative()
                            && !attackingPlayer.isSpectator()) {
                        registerPressureDefenseHit(level, actualDamage);
                    }
                }
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

        while (!this.pressureHitTicks.isEmpty()
                && now - this.pressureHitTicks.peekFirst() > PRESSURE_HIT_WINDOW_TICKS) {
            this.pressureHitTicks.removeFirst();
        }

        while (!this.pressureDamageSamples.isEmpty()
                && now - this.pressureDamageSamples.peekFirst().tick() > PRESSURE_DAMAGE_WINDOW_TICKS) {
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

        level.playSound(
                null,
                this.blockPosition(),
                SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.HOSTILE,
                0.85F,
                1.25F
        );

        double radiusSqr = PRESSURE_SHOCKWAVE_RADIUS * PRESSURE_SHOCKWAVE_RADIUS;

        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
                continue;
            }
            if (this.distanceToSqr(player) > radiusSqr) {
                continue;
            }

            Vec3 away = new Vec3(
                    player.getX() - this.getX(),
                    0.0D,
                    player.getZ() - this.getZ()
            );

            if (away.lengthSqr() < 0.0001D) {
                Vec3 look = this.getLookAngle();
                away = new Vec3(-look.x, 0.0D, -look.z);
            }

            if (away.lengthSqr() < 0.0001D) {
                away = new Vec3(0.0D, 0.0D, 1.0D);
            }

            double push = Mth.lerp(
                    this.getRandom().nextDouble(),
                    PRESSURE_PUSH_MIN,
                    PRESSURE_PUSH_MAX
            );

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
        String baseName = this.isDefending() ? "azumaal_defend" : "azumaal";
        return buildAnimatedTexture("textures/entity/", baseName);
    }

    public ResourceLocation getAnimatedEmissiveTexture() {
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