package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ai.PaladinCombatController;
import com.benji.oasiso.common.entity.ai.PaladinDeathManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
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
import com.benji.oasiso.network.dialogue.BossDialogueNetwork;

import java.util.UUID;

public class PaladinEntity extends Monster implements GeoEntity, GlowmaskEntity {

    public static final int STATE_IDLE = 0;
    public static final int STATE_ATTACK_1 = 1;
    public static final int STATE_ATTACK_2 = 2;
    public static final int STATE_SPIN_ATTACK = 3;
    public static final int STATE_GRAB = 4;
    public static final int STATE_SHIELD = 5;

    private static final int INTRO_FAILSAFE_TICKS = 20 * 45;

    private boolean introLocked;
    private boolean introPanelFinished;
    private boolean introDialogueStarted;

    private int introDialogueTicks;

    private UUID introPlayerId;

    //cutscene states
    public static final int STATE_WAIT = 6;
    public static final int STATE_AWAKE = 7;
    public static final int STATE_DEATH = 8;
// 1,25 sec
    private static final int AWAKE_DURATION = 25;

    private static final EntityDataAccessor<Integer> ANIM_STATE = SynchedEntityData.defineId(PaladinEntity.class, EntityDataSerializers.INT);
//anims
    private static final RawAnimation WAIT_ANIMATION = RawAnimation.begin().thenLoop("wait");
    private static final RawAnimation AWAKE_ANIMATION = RawAnimation.begin().thenPlay("awake");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlay("death");
    private static final RawAnimation SHIELD_ANIMATION = RawAnimation.begin().thenPlay("shield");
    private static final RawAnimation SPIN_ATTACK_ANIMATION = RawAnimation.begin().thenPlay("spin_attack");
    private static final RawAnimation GRAB_ANIMATION = RawAnimation.begin().thenPlay("grab");
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK_1_ANIMATION = RawAnimation.begin().thenPlay("palattack_1");
    private static final RawAnimation ATTACK_2_ANIMATION = RawAnimation.begin().thenPlay("palattack_2");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private final PaladinCombatController combatController;
    private final PaladinDeathManager deathManager;

    private int awakeTicks; //intro data
    private boolean swordSplashActive;
    private long clientShockwaveStartNanos;


    public PaladinEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.combatController = new PaladinCombatController(this);
        this.deathManager = new PaladinDeathManager(this);
        this.setNoAi(true);
    }


    public static AttributeSupplier.Builder createAttributes() {

        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 800.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 24.0D)
                .add(Attributes.FOLLOW_RANGE, 30.0D);
    }


    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIM_STATE, STATE_WAIT);
    }

    @Override
    protected void registerGoals() {

        this.targetSelector.addGoal(1,
                new NearestAttackableTargetGoal<>(this, Player.class, false, false));

        this.goalSelector.addGoal(3,
                new WaterAvoidingRandomStrollGoal(this, 0.8D));
    }

    @Override
    public void tick() {
        super.tick();
        int state = this.getAnimState();
        if (this.level().isClientSide) {

            if (state != STATE_ATTACK_1 && state != STATE_ATTACK_2 && state != STATE_SPIN_ATTACK) {
                this.swordSplashActive = false;
            }
        }


        //DEATH

        if (state == STATE_DEATH) {
            this.setDeltaMovement(Vec3.ZERO);
            if (!this.level().isClientSide && this.level() instanceof ServerLevel level) {

                this.getNavigation().stop();
                this.setNoAi(true);

                boolean finished = this.deathManager.tick(level);
                if (finished) {
                    finishCustomDeath(level);
                }
            }
            return;
        }


        //Wait

        if (state == STATE_WAIT) {
            this.setDeltaMovement(Vec3.ZERO);

            if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {

                this.getNavigation().stop();
                this.setNoAi(true);
                // 40 b radius
                Player nearestPlayer = serverLevel.getNearestPlayer(this, 40.0D);

                if (nearestPlayer != null && nearestPlayer.isAlive() && !nearestPlayer.isSpectator()) {
                    this.lookAtPlayer(nearestPlayer, 2.5F);
                }
            }


            return;
        }


        // awake
        if (state == STATE_AWAKE) {

            this.setDeltaMovement(Vec3.ZERO);
            if (!this.level().isClientSide) {

                this.getNavigation().stop();
                this.setNoAi(true);

                if (this.getTarget() instanceof Player player) {

                    this.lookAtPlayer(player, 20.0F);
                }


                this.awakeTicks++;

                if (this.awakeTicks >= AWAKE_DURATION) {
                    this.awakeTicks =
                            AWAKE_DURATION;
                    this.setAnimState(
                            STATE_IDLE
                    );
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

        if (this.level().isClientSide) {
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel) {

            this.setNoAi(false);
            this.combatController.tick(serverLevel);
        }
    }


    // intro

    private void beginAwake(DamageSource source) {
        if (!(this.level() instanceof ServerLevel)) {
            return;
        }
        if (this.getAnimState() != STATE_WAIT) {
            return;
        }

        this.awakeTicks = 0;
        this.introLocked = true;
        this.introPanelFinished = false;
        this.introDialogueStarted = false;

        this.introDialogueTicks = 0;
        this.introPlayerId = null;
        this.setAnimState(STATE_AWAKE);
        this.setNoAi(true);
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        if (source.getEntity() instanceof Player player) {

            this.introPlayerId =
                    player.getUUID();

            this.setTarget(player);
            this.lookAtPlayer(player, 180.0F);
        }
    }

    //dialog methhods

    public void onIntroPanelFinished(ServerPlayer player) {
        if (!this.introLocked) {
            return;
        }

        this.introPanelFinished = true;
        this.introPlayerId = player.getUUID();


        if (this.level() instanceof ServerLevel level) {
            tryStartIntroDialogue(level);
        }
    }


    private void tryStartIntroDialogue(ServerLevel level) {
        if (!this.introLocked || this.introDialogueStarted || !this.introPanelFinished
                || this.getAnimState() != STATE_IDLE) {

            return;
        }

        ServerPlayer player = resolveIntroPlayer(level);


        if (player == null) {
            return;
        }

        this.introDialogueStarted = true;
        this.introDialogueTicks = 0;

        this.setNoAi(true);
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);

        BossDialogueNetwork.startDialogue(player, this.getUUID(), "paladin");
    }


    private void tickIntroDialogue(ServerLevel level) {
        this.setNoAi(true);
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);

        ServerPlayer player = resolveIntroPlayer(level);


        if (player != null) {
            this.lookAtPlayer(player, 5.0F);
        }

        this.introDialogueTicks++;

        if (!this.introPanelFinished && this.introDialogueTicks >= 160) {
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

        this.setAnimState(STATE_IDLE);

        this.setDeltaMovement(Vec3.ZERO);

        this.setNoAi(false);
    }

// anim states

    public int getAnimState() {
        return this.entityData.get(ANIM_STATE);
    }
    public void setAnimState(int state) {
        this.entityData.set(ANIM_STATE, state);
    }
    public boolean isDeathSequenceActive() {

        return this.getAnimState() == STATE_DEATH;
    }

// lookat

    public void lookAtPlayer(Player target, float maxRotationStep) {
        double deltaX = target.getX() - this.getX();
        double deltaZ = target.getZ() - this.getZ();

        float targetYaw = (float) (Mth.atan2(deltaZ, deltaX) * (180.0D / Math.PI)) - 90.0F;
        float difference = Mth.wrapDegrees(targetYaw - this.getYRot());
        float step = Mth.clamp(difference, -maxRotationStep, maxRotationStep);
        float newYaw = this.getYRot() + step;

        this.setYRot(newYaw);
        this.setYHeadRot(newYaw);

        this.yBodyRot = newYaw;
        this.yBodyRotO = newYaw;
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
    }


    //damgae

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isDeathSequenceActive()) {
            return false;
        }
        if (this.introLocked) {
            return true;
        }
        if (this.getAnimState() == STATE_WAIT) {
            if (source.getEntity() instanceof Player) {
                if (!this.level().isClientSide) {
                    beginAwake(source);
                }
                return true;
            }
            return false;
        }

        if (this.getAnimState() == STATE_AWAKE) {
            return true;
        }

        if (this.getAnimState() == STATE_SHIELD) {
            return true;
        }
        return super.hurt(source, amount);
    }


    //death

    @Override
    public void die(DamageSource source) {
        if (this.isDeathSequenceActive()) {
            return;
        }

        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        //stop all
        this.combatController.prepareForDeath(level);
        this.deathManager.begin(level, source);
    }


    private void finishCustomDeath(ServerLevel level) {
        ServerPlayer killer = this.deathManager.resolveKiller(level);
        DamageSource finalSource;


        if (killer != null) {

            // player kill (and later do the same in CoA-better for modpacks!!!!!!!)
            this.setLastHurtByPlayer(killer);
            finalSource = level.damageSources().playerAttack(killer);

        } else {
            finalSource = level.damageSources().generic();
        }
        this.setHealth(0.0F);
        super.die(finalSource);
        this.discard();
    }


    // QTE

    public float applyQteBacklash(float amount) {
        float healthBefore = this.getHealth();
        this.invulnerableTime = 0;
        super.hurt(this.damageSources().magic(),
                amount);
        this.invulnerableTime = 0;
        return Math.max(0.0F,
                healthBefore - this.getHealth());
    }


    public void onSwordHeartDestroyed() {
        if (this.level().isClientSide) {
            return;
        }
        this.combatController.onSwordHeartDestroyed();
    }


    public boolean isQteActive() {
        return this.combatController.isQteActive();
    }


    public Vec3 getQteServerAnchor(int slot) {
        Vec3 local = switch (slot) {
            case 0 -> new Vec3(2.7D, 4.0D, -2.8D);
            case 1 -> new Vec3(1.7D, 3.2D, -2.5D);
            default -> new Vec3(0.8D, 2.5D, -2.2D);
        };
        double radians = Math.toRadians(-this.getYRot());
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double rotatedX = local.x * cos - local.z * sin;
        double rotatedZ = local.x * sin + local.z * cos;

        return new Vec3(this.getX() + rotatedX,
                this.getY() + local.y,
                this.getZ() + rotatedZ);
    }

    public boolean isSwordSplashActive() {
        return this.swordSplashActive;
    }

    public long getClientShockwaveStartNanos() {
        return this.clientShockwaveStartNanos;
    }

    private void handleClientInstruction(String instruction) {
        if (instruction == null) {
            return;
        }
        String normalized = instruction.replace(";", "").trim();
        switch (normalized) {
            case "pal1_splash_start", "pal2_splash_start", "spin_start" -> this.swordSplashActive = true;
            case "pal1_splash_end", "pal2_splash_end", "spin_end" -> this.swordSplashActive = false;
            case "palattack1_damage" -> this.clientShockwaveStartNanos = System.nanoTime();
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<PaladinEntity> controller = new AnimationController<>(this,
                "controller",
                0,
                state -> {
                    return switch (this.getAnimState()) {
                        case STATE_WAIT -> state.setAndContinue(WAIT_ANIMATION);
                        case STATE_AWAKE -> state.setAndContinue(AWAKE_ANIMATION);
                        case STATE_DEATH -> state.setAndContinue(DEATH_ANIMATION);
                        case STATE_SHIELD -> state.setAndContinue(SHIELD_ANIMATION);
                        case STATE_ATTACK_1 -> state.setAndContinue(ATTACK_1_ANIMATION);
                        case STATE_ATTACK_2 -> state.setAndContinue(ATTACK_2_ANIMATION);
                        case STATE_SPIN_ATTACK -> state.setAndContinue(SPIN_ATTACK_ANIMATION);
                        case STATE_GRAB -> state.setAndContinue(GRAB_ANIMATION);
                        default -> {
                            if (state.isMoving()) {
                                yield state.setAndContinue(WALK_ANIMATION);
                            }
                            yield state.setAndContinue(IDLE_ANIMATION);
                        }
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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("AnimState", this.getAnimState());
        tag.putInt("AwakeTicks", this.awakeTicks);

        this.combatController.save(tag);
        this.deathManager.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int state = tag.contains("AnimState")
                ? tag.getInt("AnimState")
                : STATE_WAIT;

        this.setAnimState(state);
        this.awakeTicks = tag.getInt("AwakeTicks");

        this.combatController.load(tag);
        this.deathManager.load(tag);

        this.setNoAi(state == STATE_WAIT || state == STATE_AWAKE || state == STATE_DEATH);
    }


    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }


    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/" + "paladin_emissive.png");
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}