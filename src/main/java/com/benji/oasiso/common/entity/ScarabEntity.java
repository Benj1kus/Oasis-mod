package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.damagesource.DamageSource;

public class ScarabEntity extends Monster implements GeoEntity, GlowmaskEntity, PlayerRideableJumping {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation FLY_UP = RawAnimation.begin().thenLoop("fly_up");
    private static final RawAnimation FLY_DOWN = RawAnimation.begin().thenLoop("fly_down");
    private static final RawAnimation FLY_FORWARD = RawAnimation.begin().thenLoop("fly_forward");
    private static final RawAnimation FLY_LEFT = RawAnimation.begin().thenLoop("fly_left");
    private static final RawAnimation FLY_RIGHT = RawAnimation.begin().thenLoop("fly_right");

    private static final EntityDataAccessor<Boolean> DATA_FLYING = SynchedEntityData.defineId(ScarabEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_FLIGHT_PITCH = SynchedEntityData.defineId(ScarabEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_FLIGHT_ROLL = SynchedEntityData.defineId(ScarabEntity.class, EntityDataSerializers.FLOAT);

    private static final double SEAT_Y = 18.0D / 16.0D;
    private static final double SEAT_Z = 1.5D / 16.0D;

    private static final double NORMAL_JUMP_Y = 0.80D;

    private static final double FLIGHT_TAKEOFF_Y = 1.35D;
    private static final double FLIGHT_TAKEOFF_FORWARD = 0.42D;
    private static final int FLIGHT_CHARGE_THRESHOLD = 80;
    private static final double FLIGHT_SPEED = 0.58D;
    private static final double FLIGHT_ACCELERATION = 0.35D;
    private static final float FLIGHT_TURN_SPEED = 9.0F;
    private static final float FLIGHT_PITCH_THRESHOLD = 15.0F;
    private static final float FLIGHT_YAW_THRESHOLD = 10.0F;

    private static final float MAX_FLIGHT_PITCH = 45.0F;
    private static final float MAX_FLIGHT_ROLL = 36.0F;
    private static final float FLIGHT_ROLL_FACTOR = 0.55F;
    private static final float FLIGHT_TILT_RESPONSE = 0.18F;

    private static final double SCARAB_MOVE_SPEED = 0.205D;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float previousFlightPitch;
    private float previousFlightRoll;

    public ScarabEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setMaxUpStep(2.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 200.0D).add(Attributes.MOVEMENT_SPEED, SCARAB_MOVE_SPEED).add(Attributes.KNOCKBACK_RESISTANCE, 1.0D).add(Attributes.ATTACK_DAMAGE, 0.0D).add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        this.resetFallDistance();

        return false;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();

        this.entityData.define(DATA_FLIGHT_PITCH, 0.0F);
        this.entityData.define(DATA_FLIGHT_ROLL, 0.0F);
        this.entityData.define(DATA_FLYING, false);
    }

    public boolean isFlyingMode() {
        return this.entityData.get(DATA_FLYING);
    }

    public float getFlightPitch() {
        return this.entityData.get(DATA_FLIGHT_PITCH);
    }

    public float getFlightRoll() {
        return this.entityData.get(DATA_FLIGHT_ROLL);
    }

    public float getFlightPitch(float partialTick) {
        return Mth.lerp(partialTick, this.previousFlightPitch, this.getFlightPitch());
    }

    public float getFlightRoll(float partialTick) {
        return Mth.lerp(partialTick, this.previousFlightRoll, this.getFlightRoll());
    }

    private void setFlyingMode(boolean flying) {
        this.entityData.set(DATA_FLYING, flying);
    }

    private void stopFlyingMode() {
        this.setFlyingMode(false);
        this.setNoGravity(false);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (this.isVehicle()) {
            return super.mobInteract(player, hand);
        }

        if (!this.level().isClientSide) {

            this.setTarget(null);
            this.getNavigation().stop();
            player.startRiding(this);
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player && this.getPassengers().isEmpty();
    }

    @Override
    public LivingEntity getControllingPassenger() {
        Entity passenger = this.getFirstPassenger();

        if (passenger instanceof Player player) {
            return player;
        }

        return null;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (!this.hasPassenger(passenger)) {
            return;
        }

        float pitch = (float) Math.toRadians(this.getFlightPitch());
        float roll = (float) Math.toRadians(this.getFlightRoll());

        Vec3 seatOffset = new Vec3(0.0D, SEAT_Y, SEAT_Z).xRot(pitch).zRot(roll).yRot((float) Math.toRadians(-this.getYRot()));

        moveFunction.accept(passenger, this.getX() + seatOffset.x, this.getY() + seatOffset.y, this.getZ() + seatOffset.z);
    }

    @Override
    public void travel(Vec3 travelVector) {
        LivingEntity controller = this.getControllingPassenger();

        if (!(controller instanceof Player player) || !this.isAlive()) {
            super.travel(travelVector);
            return;
        }

        if (this.isFlyingMode()) {

            if (!this.isNoGravity()) {

                super.travel(Vec3.ZERO);
                return;
            }

            rotateTowardsFlightTarget(player);

            if (this.isControlledByLocalInstance()) {

                Vec3 wantedVelocity = player.getLookAngle().normalize().scale(FLIGHT_SPEED);
                Vec3 velocity = this.getDeltaMovement().lerp(wantedVelocity, FLIGHT_ACCELERATION);

                this.setDeltaMovement(velocity);
                this.move(MoverType.SELF, velocity);

            } else {
                this.setDeltaMovement(Vec3.ZERO);
            }
            return;
        }

        this.setYRot(player.getYRot());

        this.yRotO = this.getYRot();
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();

        float strafe = player.xxa * 0.5F;
        float forward = player.zza;

        if (forward <= 0.0F) {
            forward *= 0.25F;
        }

        this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED));

        if (this.isControlledByLocalInstance()) {
            super.travel(new Vec3(strafe, 0.0D, forward));

        } else {

            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    private void rotateTowardsFlightTarget(Player player) {
        float difference = Mth.wrapDegrees(player.getYRot() - this.getYRot());
        float rotationStep = Mth.clamp(difference, -FLIGHT_TURN_SPEED, FLIGHT_TURN_SPEED);
        float yaw = this.getYRot() + rotationStep;
        this.setYRot(yaw);

        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    private void updateFlightTilt(Player player) {
        float targetPitch = 0.0F;
        float targetRoll = 0.0F;

        if (this.isFlyingMode() && this.isNoGravity() && player != null) {
            targetPitch = Mth.clamp(-player.getXRot(), -MAX_FLIGHT_PITCH, MAX_FLIGHT_PITCH);

            float yawDifference = Mth.wrapDegrees(player.getYRot() - this.getYRot());
            targetRoll = Mth.clamp(-yawDifference * FLIGHT_ROLL_FACTOR,
                    -MAX_FLIGHT_ROLL, MAX_FLIGHT_ROLL);
        }

        float pitch = Mth.lerp(FLIGHT_TILT_RESPONSE, this.getFlightPitch(), targetPitch);
        float roll = Mth.lerp(FLIGHT_TILT_RESPONSE, this.getFlightRoll(), targetRoll);

        if (Math.abs(pitch) < 0.025F) {
            pitch = 0.0F;
        }

        if (Math.abs(roll) < 0.025F) {
            roll = 0.0F;
        }

        this.entityData.set(DATA_FLIGHT_PITCH, pitch);
        this.entityData.set(DATA_FLIGHT_ROLL, roll);
    }

    @Override
    public boolean canJump() {
        return this.onGround() && !this.isFlyingMode() && this.getControllingPassenger() instanceof Player;
    }

    @Override
    public void onPlayerJump(int jumpPower) {
        startRiderJump(jumpPower);
    }

    @Override
    public void handleStartJump(int jumpPower) {
        startRiderJump(jumpPower);
    }

    @Override
    public void handleStopJump() {
    }

    private void startRiderJump(int jumpPower) {
        if (jumpPower <= 0 || !this.onGround() || this.isFlyingMode()) {
            return;
        }

        LivingEntity controller = this.getControllingPassenger();

        if (!(controller instanceof Player player)) {
            return;
        }

        if (jumpPower >= FLIGHT_CHARGE_THRESHOLD) {

            this.setFlyingMode(true);
            this.setNoGravity(false);
            Vec3 look = player.getLookAngle();
            Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);

            if (horizontal.lengthSqr() > 0.0001D) {
                horizontal = horizontal.normalize();
            }

            this.setDeltaMovement(horizontal.x * FLIGHT_TAKEOFF_FORWARD, FLIGHT_TAKEOFF_Y, horizontal.z * FLIGHT_TAKEOFF_FORWARD);

            this.setOnGround(false);
            this.resetFallDistance();
            return;
        }

        Vec3 movement = this.getDeltaMovement();
        this.setDeltaMovement(movement.x, NORMAL_JUMP_Y, movement.z);
        this.setOnGround(false);
        this.resetFallDistance();
    }

    @Override
    public void tick() {

        this.previousFlightPitch = this.getFlightPitch();

        this.previousFlightRoll = this.getFlightRoll();

        super.tick();

        this.resetFallDistance();

        Entity passenger = this.getFirstPassenger();
        if (passenger instanceof Player player) {
            player.resetFallDistance();
        }

        if (!this.level().isClientSide && this.isVehicle()) {
            this.getNavigation().stop();
            this.setTarget(null);
        }

        if (!this.isFlyingMode()) {

            if (this.isNoGravity()) {
                this.setNoGravity(false);
            }

            if (!this.level().isClientSide) {
                updateFlightTilt(null);
            }

            return;
        }

        LivingEntity controller = this.getControllingPassenger();

        if (!(controller instanceof Player player)) {

            stopFlyingMode();

            if (!this.level().isClientSide) {
                updateFlightTilt(null);
            }

            return;
        }

        if (!this.isNoGravity()) {

            if (!this.onGround() && this.getDeltaMovement().y <= 0.0D) {

                this.setNoGravity(true);
                Vec3 movement = this.getDeltaMovement();
                this.setDeltaMovement(movement.x, 0.0D, movement.z);
            }
            if (!this.level().isClientSide) {
                updateFlightTilt(player);
            }
            return;
        }

        if (this.onGround()) {
            stopFlyingMode();
        }

        if (!this.level().isClientSide) {
            updateFlightTilt(player);
        }
    }

    @Override
    protected void registerGoals() {

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 3, state -> {

            if (this.isFlyingMode()) {

                if (!this.isNoGravity()) {
                    return state.setAndContinue(FLY_UP);
                }

                LivingEntity controller = this.getControllingPassenger();

                if (controller instanceof Player player) {

                    float pitch = player.getXRot();

                    if (pitch < -FLIGHT_PITCH_THRESHOLD) {
                        return state.setAndContinue(FLY_UP);
                    }

                    if (pitch > FLIGHT_PITCH_THRESHOLD) {
                        return state.setAndContinue(FLY_DOWN);
                    }

                    float yawDifference = Mth.wrapDegrees(player.getYRot() - this.getYRot());

                    if (yawDifference > FLIGHT_YAW_THRESHOLD) {
                        return state.setAndContinue(FLY_RIGHT);
                    }

                    if (yawDifference < -FLIGHT_YAW_THRESHOLD) {
                        return state.setAndContinue(FLY_LEFT);
                    }
                }
                return state.setAndContinue(FLY_FORWARD);
            }

            if (!this.onGround()) {

                if (this.getDeltaMovement().y > 0.05D) {
                    return state.setAndContinue(FLY_UP);
                }

                if (this.getDeltaMovement().y < -0.05D) {
                    return state.setAndContinue(FLY_DOWN);
                }
            }

            if (state.isMoving()) {
                return state.setAndContinue(WALK);
            }
            return state.setAndContinue(IDLE);
        }));
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/scarab_emissive.png");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
