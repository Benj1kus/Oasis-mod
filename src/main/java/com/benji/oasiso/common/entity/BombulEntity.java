package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.support.BombulBuffHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BombulEntity extends Monster implements GeoEntity, GlowmaskEntity {

    public static final int STATE_SLEEP = 0;
    public static final int STATE_WAKE_UP = 1;
    public static final int STATE_IDLE = 2;
    public static final int STATE_SUPPORT = 3;
    private static final int FORCED_SLEEP_TIME = 20 * 60 * 2; // 2 минуты

    private int forcedSleepTicks;

    private static final EntityDataAccessor<Integer> STATE =
            SynchedEntityData.defineId(
                    BombulEntity.class,
                    EntityDataSerializers.INT
            );

    private static final double WAKE_RADIUS = 8.0D;

    private static final int WAKE_UP_TIME = 30;
    private static final int SUPPORT_ANIMATION_TIME = 20;
    private static final int SUPPORT_COOLDOWN_TIME = 40;

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    private int stateTimer;
    private int supportCooldown;

    private boolean explodedOnDeath;

    public BombulEntity(
            EntityType<? extends Monster> type,
            Level level
    ) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();

        this.entityData.define(STATE, STATE_SLEEP);
    }

    public int getAnimState() {
        return this.entityData.get(STATE);
    }

    public void setAnimState(int state) {
        this.entityData.set(STATE, state);
    }

    public boolean isAwake() {
        return this.getAnimState() == STATE_IDLE
                || this.getAnimState() == STATE_SUPPORT;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 30.0D);
    }


    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            return;
        }

        int state = this.getAnimState();

        if (state == STATE_SLEEP) {
            freezeSleepingRotation();


            if (this.forcedSleepTicks > 0) {
                this.forcedSleepTicks--;
                return;
            }

            if (hasWakeUpPlayer()) {
                this.setAnimState(STATE_WAKE_UP);
                this.stateTimer = WAKE_UP_TIME;
            }

            return;
        }

        if (state == STATE_WAKE_UP) {
            this.getNavigation().stop();

            if (this.stateTimer > 0) {
                this.stateTimer--;
            }

            if (this.stateTimer <= 0) {
                this.setAnimState(STATE_IDLE);
                this.supportCooldown = 0;
            }

            return;
        }

        boolean buffedSomething = false;


        if (this.tickCount % 10 == 0) {
            buffedSomething =
                    BombulBuffHandler.applyBuffAround(this);
        }

        if (this.supportCooldown > 0) {
            this.supportCooldown--;
        }

        if (state == STATE_SUPPORT) {
            if (this.stateTimer > 0) {
                this.stateTimer--;
            }

            if (this.stateTimer <= 0) {
                this.setAnimState(STATE_IDLE);
                this.supportCooldown =
                        SUPPORT_COOLDOWN_TIME;
            }

            return;
        }

        if (state == STATE_IDLE
                && buffedSomething
                && this.supportCooldown <= 0) {

            this.setAnimState(STATE_SUPPORT);
            this.stateTimer = SUPPORT_ANIMATION_TIME;
        }
    }

    private boolean hasWakeUpPlayer() {
        return !this.level()
                .getEntitiesOfClass(
                        Player.class,
                        this.getBoundingBox()
                                .inflate(WAKE_RADIUS),
                        player ->
                                player.isAlive()
                                        && !player.isCreative()
                                        && !player.isSpectator()
                )
                .isEmpty();
    }

    private void freezeSleepingRotation() {
        this.getNavigation().stop();

        Vec3 movement = this.getDeltaMovement();

        this.setDeltaMovement(
                0.0D,
                movement.y,
                0.0D
        );

        float bodyRotation = this.getYRot();

        this.yBodyRot = bodyRotation;
        this.yBodyRotO = bodyRotation;

        this.setYHeadRot(bodyRotation);
        this.yHeadRotO = bodyRotation;
    }

    @Override
    protected InteractionResult mobInteract(
            Player player,
            InteractionHand hand
    ) {
        ItemStack heldStack = player.getItemInHand(hand);

        if (heldStack.is(Oasiso.NEPHRITIS_CORE.get())) {
            if (!this.level().isClientSide) {
                putToSleep(FORCED_SLEEP_TIME);
                spawnGoldenBurst(35);

                if (!player.getAbilities().instabuild) {
                    heldStack.shrink(1);
                }
            }

            return InteractionResult.sidedSuccess(
                    this.level().isClientSide
            );
        }


        if (heldStack.is(Oasiso.BOMBUL_BOTTLE_EMPTY.get())) {
            if (!this.level().isClientSide) {
                spawnGoldenBurst(45);

                player.setItemInHand(
                        hand,
                        new ItemStack(Oasiso.BOMBUL_BOTTLE.get())
                );

                this.discard();
            }

            return InteractionResult.sidedSuccess(
                    this.level().isClientSide
            );
        }

        return super.mobInteract(player, hand);
    }

    private void putToSleep(int sleepTicks) {
        this.setAnimState(STATE_SLEEP);

        this.forcedSleepTicks = sleepTicks;
        this.stateTimer = 0;
        this.supportCooldown = 0;

        this.setTarget(null);
        this.getNavigation().stop();

        Vec3 movement = this.getDeltaMovement();

        this.setDeltaMovement(
                0.0D,
                movement.y,
                0.0D
        );
    }

    public void spawnGoldenBurst(int particleCount) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(
                Oasiso.GOLDEN_STARS.get(),

                this.getX(),
                this.getY() + this.getBbHeight() * 0.5D,
                this.getZ(),

                particleCount,

                this.getBbWidth() * 0.65D,
                this.getBbHeight() * 0.45D,
                this.getBbWidth() * 0.65D,

                0.08D
        );
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);

        if (!this.level().isClientSide
                && !this.explodedOnDeath) {

            this.explodedOnDeath = true;

            this.level().explode(
                    this,
                    this.getX(),
                    this.getY() + this.getBbHeight() * 0.5D,
                    this.getZ(),
                    4.0F,
                    false,
                    Level.ExplosionInteraction.NONE
            );
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        tag.putInt("BombulState", this.getAnimState());
        tag.putInt("BombulStateTimer", this.stateTimer);
        tag.putInt(
                "BombulSupportCooldown",
                this.supportCooldown
        );
        tag.putInt(
                "BombulForcedSleepTicks",
                this.forcedSleepTicks
        );
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        this.setAnimState(
                tag.getInt("BombulState")
        );
        this.forcedSleepTicks =
                tag.getInt("BombulForcedSleepTicks");

        this.stateTimer =
                tag.getInt("BombulStateTimer");

        this.supportCooldown =
                tag.getInt("BombulSupportCooldown");
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers
    ) {
        controllers.add(
                new AnimationController<>(
                        this,
                        "controller",
                        0,
                        event -> {
                            int state = this.getAnimState();

                            if (state == STATE_SLEEP) {
                                return event.setAndContinue(
                                        RawAnimation.begin()
                                                .thenLoop("sleep")
                                );
                            }

                            if (state == STATE_WAKE_UP) {
                                return event.setAndContinue(
                                        RawAnimation.begin()
                                                .thenPlayAndHold("wake_up")
                                );
                            }

                            if (state == STATE_SUPPORT) {
                                return event.setAndContinue(
                                        RawAnimation.begin()
                                                .thenPlayAndHold("support")
                                );
                            }

                            return event.setAndContinue(
                                    RawAnimation.begin()
                                            .thenLoop("idle")
                            );
                        }
                )
        );
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(
                Oasiso.MODID,
                "textures/entity/emissive/bombul_emissive.png"
        );
    }

    @Override
    public AnimatableInstanceCache
    getAnimatableInstanceCache() {
        return this.cache;
    }
}