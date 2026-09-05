package com.benji.oasiso.common.entity;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
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
import com.benji.oasiso.registry.ModItems;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import com.benji.oasiso.common.item.ScarabCoreItem;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

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

    private static final EntityDataAccessor<Integer> DATA_UPGRADE = SynchedEntityData.defineId(ScarabEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_POWERED = SynchedEntityData.defineId(ScarabEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> DATA_SURFACE = SynchedEntityData.defineId(ScarabEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_WALL_FACING_DOWN = SynchedEntityData.defineId(ScarabEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_CEILING_ENTRY_DIRECTION = SynchedEntityData.defineId(ScarabEntity.class, EntityDataSerializers.INT);

    private static final int POWER_DURATION = 20 * 40;
    private static final double POWER_STAT_MULTIPLIER = 2.0D;
    private static final float POWER_TILT_ANGLE_MULTIPLIER = 1.08F;
    private static final float POWER_TILT_RESPONSE_MULTIPLIER = 1.30F;
    private static final int POWER_FX_DURATION = 30;

    private static final double SEAT_Y = 18.0D / 16.0D;
    private static final double SEAT_Z = 1.5D / 16.0D;

    private static final double WALL_VISUAL_INSET = 1.5D;
    private static final double CEILING_VISUAL_INSET = 1.8D;

    private static final int CEILING_ENTRY_TICKS = 14;

    private static final int STEP_SOUND_INTERVAL = 3;

    private int stepSoundCooldown;
    private int stepSoundIndex;

    private int ceilingEntryTicks;

    private double lastSoundX;
    private double lastSoundZ;
    private boolean soundPositionInitialized;

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

    private static final double SURFACE_SPEED_MULTIPLIER = 0.90D;
    private static final double SURFACE_ADHESION = 0.075D;
    private static final double SURFACE_PROBE_DEPTH = 0.12D;
    private static final int WALL_REQUIRED_HEIGHT = 4;
    private static final double AUTO_WALL_SPEED_MULTIPLIER = 0.72D;
    private static final double AUTO_CEILING_SPEED_MULTIPLIER = 0.64D;
    private static final float SURFACE_TILT_RESPONSE = 0.20F;
    private static final int NORMAL_DROP_HEIGHT = 3;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float previousFlightPitch;
    private float previousFlightRoll;
    private int powerTicksRemaining;
    private int powerFxTicksRemaining;

    private int autoWallDirection = 1;
    private int autoSurfaceDecisionTicks;

    @Nullable
    private UUID summoningCoreId;

    @Nullable
    private UUID summoningPlayerId;

    private boolean coreDeathHandled;

    private double lastSoundY;

    public ScarabEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setMaxUpStep(2.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 100.0D).add(Attributes.MOVEMENT_SPEED, SCARAB_MOVE_SPEED).add(Attributes.KNOCKBACK_RESISTANCE, 0.05D).add(Attributes.ATTACK_DAMAGE, 0.0D).add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        this.resetFallDistance();

        return false;
    }

    public void bindToScarabCore(UUID coreId, UUID playerId) {
        this.summoningCoreId = coreId;
        this.summoningPlayerId = playerId;
        this.setPersistenceRequired();
    }

    public void startCoreSummonEffect() {
        if (this.level().isClientSide) {
            return;
        }
        this.powerFxTicksRemaining = POWER_FX_DURATION;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();

        this.entityData.define(DATA_FLIGHT_PITCH, 0.0F);
        this.entityData.define(DATA_FLIGHT_ROLL, 0.0F);
        this.entityData.define(DATA_FLYING, false);
        this.entityData.define(DATA_UPGRADE, ScarabUpgrade.NONE.id());
        this.entityData.define(DATA_POWERED, false);
        this.entityData.define(DATA_SURFACE, ScarabSurface.FLOOR.id());
        this.entityData.define(DATA_WALL_FACING_DOWN, false);
        this.entityData.define(DATA_CEILING_ENTRY_DIRECTION, -1);
    }

    public boolean isFlyingMode() {
        return this.entityData.get(DATA_FLYING);
    }

    public boolean isWallFacingDown() {
        return this.entityData.get(DATA_WALL_FACING_DOWN);
    }

    private void setWallFacingDown(boolean facingDown) {
        this.entityData.set(DATA_WALL_FACING_DOWN, facingDown);
    }

    public ScarabUpgrade getUpgrade() {
        return ScarabUpgrade.byId(this.entityData.get(DATA_UPGRADE));
    }

    public boolean isPowered() {
        return this.entityData.get(DATA_POWERED);
    }

    private void setUpgrade(ScarabUpgrade upgrade) {
        this.entityData.set(DATA_UPGRADE, upgrade.id());
    }

    private void setPowered(boolean powered) {
        this.entityData.set(DATA_POWERED, powered);
    }

    public String getScarabTexture() {
        if (this.isPowered()) {
            return "scarab_powered.png";
        }

        return this.getUpgrade().texture();
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

    public ScarabSurface getSurface() {
        return ScarabSurface.byId(this.entityData.get(DATA_SURFACE));
    }

    public boolean isSurfaceAttached() {
        return this.getSurface() != ScarabSurface.FLOOR;
    }

    private void setSurface(ScarabSurface surface) {
        this.entityData.set(DATA_SURFACE, surface.id());

        if (surface != ScarabSurface.CEILING) {
            clearCeilingEntry();
        }

        if (!surface.isWall()) {
            this.setWallFacingDown(false);
        }

        if (surface != ScarabSurface.FLOOR) {

            this.setNoGravity(true);
            this.resetFallDistance();

        } else if (!this.isFlyingMode()) {

            this.setNoGravity(false);
        }
    }

    private Direction getHorizontalDirection(float yaw) {
        int index = Mth.floor(yaw / 90.0F + 0.5F) & 3;

        return switch (index) {
            case 0 -> Direction.SOUTH;
            case 1 -> Direction.WEST;
            case 2 -> Direction.NORTH;
            default -> Direction.EAST;
        };
    }

    private float getYawForDirection(Direction direction) {
        return switch (direction) {
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            case NORTH -> 180.0F;
            case EAST -> -90.0F;

            default -> this.getYRot();
        };
    }

    private void faceDirection(Direction direction) {
        float yaw = getYawForDirection(direction);
        this.setYRot(yaw);

        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    private Vec3 directionVector(Direction direction) {
        return new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    private boolean isTouchingSurface(Direction normal) {
        return isTouchingSurfaceAt(this.getBoundingBox(), normal);
    }

    private boolean isTouchingSurfaceAt(AABB box, Direction normal) {
        double depth = SURFACE_PROBE_DEPTH;

        double horizontalInset = Math.min(0.12D, this.getBbWidth() * 0.08D);

        double verticalInset = 0.10D;

        AABB probe = switch (normal) {

            case UP ->
                    new AABB(box.minX + horizontalInset, box.minY - depth, box.minZ + horizontalInset, box.maxX - horizontalInset, box.minY + 0.02D, box.maxZ - horizontalInset);
            case DOWN ->
                    new AABB(box.minX + horizontalInset, box.maxY - 0.02D, box.minZ + horizontalInset, box.maxX - horizontalInset, box.maxY + depth, box.maxZ - horizontalInset);
            case NORTH ->
                    new AABB(box.minX + horizontalInset, box.minY + verticalInset, box.maxZ - 0.02D, box.maxX - horizontalInset, box.maxY - verticalInset, box.maxZ + depth);
            case SOUTH ->
                    new AABB(box.minX + horizontalInset, box.minY + verticalInset, box.minZ - depth, box.maxX - horizontalInset, box.maxY - verticalInset, box.minZ + 0.02D);
            case WEST ->
                    new AABB(box.maxX - 0.02D, box.minY + verticalInset, box.minZ + horizontalInset, box.maxX + depth, box.maxY - verticalInset, box.maxZ - horizontalInset);
            case EAST ->
                    new AABB(box.minX - depth, box.minY + verticalInset, box.minZ + horizontalInset, box.minX + 0.02D, box.maxY - verticalInset, box.maxZ - horizontalInset);
        };

        return !this.level().noCollision(this, probe);
    }

    @Nullable
    private Vec3 findSurfaceStepOffset(Direction normal, Vec3 tangentMovement) {
        if (tangentMovement.lengthSqr() < 0.000001D) {
            return null;
        }

        Vec3 travel = tangentMovement.normalize().scale(Math.max(tangentMovement.length(), 0.24D));
        Vec3 awayFromSurface = directionVector(normal);

        for (int blocks = 1; blocks <= 2; blocks++) {

            Vec3 offset = awayFromSurface.scale(blocks + 0.05D).add(travel);

            AABB candidate = this.getBoundingBox().move(offset);

            if (!this.level().noCollision(this, candidate)) {
                continue;
            }
            if (!isTouchingSurfaceAt(candidate, normal)) {

                continue;
            }
            return offset;
        }
        return null;
    }

    private boolean canSurfaceStep(Direction normal, Vec3 tangentMovement) {
        return findSurfaceStepOffset(normal, tangentMovement) != null;
    }

    private boolean trySurfaceStep(Direction normal, Vec3 tangentMovement) {
        Vec3 offset = findSurfaceStepOffset(normal, tangentMovement);

        if (offset == null) {
            return false;
        }

        this.setPos(this.getX() + offset.x, this.getY() + offset.y, this.getZ() + offset.z);
        this.setDeltaMovement(tangentMovement);
        this.resetFallDistance();

        return true;
    }

    private boolean isTallWall(Direction travelDirection) {
        if (travelDirection == Direction.UP || travelDirection == Direction.DOWN) {

            return false;
        }

        double distance = this.getBbWidth() * 0.5D + 0.35D;
        double checkX = this.getX() + travelDirection.getStepX() * distance;
        double checkZ = this.getZ() + travelDirection.getStepZ() * distance;

        int baseY = Mth.floor(this.getBoundingBox().minY + 0.15D);

        for (int y = 0; y < WALL_REQUIRED_HEIGHT; y++) {

            BlockPos pos = BlockPos.containing(checkX, baseY + y, checkZ);
            BlockState state = this.level().getBlockState(pos);

            if (state.getCollisionShape(this.level(), pos).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private void tryStartWallClimb(@Nullable Player rider) {
        if (this.isFlyingMode() || this.isSurfaceAttached() || !this.onGround()) {

            return;
        }

        boolean wantsWall;

        if (rider != null) {
            wantsWall = rider.zza > 0.05F;

        } else {
            wantsWall = this.horizontalCollision;
        }


        if (!wantsWall) {
            return;
        }

        Direction travelDirection = getHorizontalDirection(this.getYRot());

        if (!isTallWall(travelDirection)) {
            return;
        }

        Direction wallNormal = travelDirection.getOpposite();
        this.setWallFacingDown(false);
        this.setSurface(ScarabSurface.fromWallNormal(wallNormal));

        faceDirection(travelDirection);

        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);

        this.autoWallDirection = 1;
        this.autoSurfaceDecisionTicks = 80 + this.random.nextInt(100);
    }

    private void travelOnAttachedSurface(Player player) {
        if (!this.isControlledByLocalInstance()) {

            this.setDeltaMovement(Vec3.ZERO);
            return;
        }

        ScarabSurface surface = this.getSurface();

        double speed = this.getAttributeValue(Attributes.MOVEMENT_SPEED) * SURFACE_SPEED_MULTIPLIER;

        float forward = player.zza;
        float strafe = player.xxa;

        Vec3 tangentMovement = Vec3.ZERO;

        if (surface.isWall()) {

            Direction normal = surface.normal();
            updateWallBodyYaw(normal);

            Vec3 normalVector = directionVector(normal);
            Vec3 worldUp = new Vec3(0.0D, 1.0D, 0.0D);
            Vec3 wallLeft = normalVector.cross(worldUp);

            double verticalDirection = this.isWallFacingDown() ? -1.0D : 1.0D;
            tangentMovement = worldUp.scale(forward * verticalDirection).add(wallLeft.scale(strafe));

        } else if (surface == ScarabSurface.CEILING) {

            Direction entryDirection = getCeilingEntryDirection();

            if (entryDirection != null) {
                faceDirection(entryDirection);

                Vec3 forwardVector = directionVector(entryDirection);
                Vec3 leftVector = new Vec3(forwardVector.z, 0.0D, -forwardVector.x);

                tangentMovement = forwardVector.scale(forward).add(leftVector.scale(strafe));

            } else {

                this.setYRot(player.getYRot());

                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.getYRot();

                float radians = (float) Math.toRadians(this.getYRot());

                Vec3 forwardVector = new Vec3(-Mth.sin(radians), 0.0D, Mth.cos(radians));
                Vec3 leftVector = new Vec3(Mth.cos(radians), 0.0D, Mth.sin(radians));

                tangentMovement = forwardVector.scale(forward).add(leftVector.scale(strafe));
            }
        }

        if (tangentMovement.lengthSqr() > 1.0D) {
            tangentMovement = tangentMovement.normalize();
        }

        tangentMovement = tangentMovement.scale(speed);
        moveAlongSurface(tangentMovement, surface.normal());
    }

    private void moveAlongSurface(Vec3 tangentMovement, Direction normal) {
        Vec3 normalVector = directionVector(normal);

        if (tangentMovement.lengthSqr() > 0.000001D) {

            AABB directCandidate = this.getBoundingBox().move(tangentMovement);

            if (!this.level().noCollision(this, directCandidate)) {

                if (trySurfaceStep(normal, tangentMovement)) {
                    return;
                }
            }
        }

        Vec3 adhesion = normalVector.scale(-SURFACE_ADHESION);

        this.setDeltaMovement(tangentMovement);
        this.move(MoverType.SELF, tangentMovement.add(adhesion));
        this.resetFallDistance();
    }

    private boolean tryReturnToBaseSurface(Direction normal) {
        Vec3 towardSurface = directionVector(normal).scale(-1.0D);

        for (int blocks = 1; blocks <= 2; blocks++) {

            Vec3 offset = towardSurface.scale(blocks);
            AABB candidate = this.getBoundingBox().move(offset);

            if (!this.level().noCollision(this, candidate)) {
                continue;
            }

            if (!isTouchingSurfaceAt(candidate, normal)) {
                continue;
            }

            this.setPos(this.getX() + offset.x, this.getY() + offset.y, this.getZ() + offset.z);

            this.setDeltaMovement(Vec3.ZERO);
            return true;
        }
        return false;
    }

    private void moveAutonomousOnWall() {
        double speed = this.getAttributeValue(Attributes.MOVEMENT_SPEED) * AUTO_WALL_SPEED_MULTIPLIER;

        Vec3 movement = new Vec3(0.0D, speed * this.autoWallDirection, 0.0D);
        moveAlongSurface(movement, this.getSurface().normal());
    }

    private void moveAutonomousOnCeiling() {
        double speed = this.getAttributeValue(Attributes.MOVEMENT_SPEED) * AUTO_CEILING_SPEED_MULTIPLIER;

        this.autoSurfaceDecisionTicks--;

        if (this.autoSurfaceDecisionTicks <= 0) {

            if (this.random.nextFloat() < 0.45F) {

                float turn = this.random.nextBoolean() ? 90.0F : -90.0F;
                this.setYRot(this.getYRot() + turn);

                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.getYRot();
            }

            this.autoSurfaceDecisionTicks = 70 + this.random.nextInt(100);
        }

        float radians = (float) Math.toRadians(this.getYRot());

        Vec3 forward = new Vec3(-Mth.sin(radians), 0.0D, Mth.cos(radians));

        moveAlongSurface(forward.scale(speed), Direction.DOWN);
    }

    private void updateAutoWallDecision() {
        this.autoSurfaceDecisionTicks--;

        if (this.autoSurfaceDecisionTicks > 0) {
            return;
        }

        if (this.random.nextFloat() < 0.30F) {
            this.autoWallDirection *= -1;
        }

        this.autoSurfaceDecisionTicks = 80 + this.random.nextInt(120);
    }

    private boolean tryWrapWallOntoTop(Direction wallNormal) {
        Vec3 inward = directionVector(wallNormal).scale(-1.0D);

        double horizontalShift = this.getBbWidth() * 0.5D + 0.35D;
        double verticalShift = 0.18D;

        Vec3 offset = new Vec3(inward.x * horizontalShift, verticalShift, inward.z * horizontalShift);

        AABB candidate = this.getBoundingBox().move(offset);

        if (!this.level().noCollision(this, candidate)) {
            return false;
        }

        AABB floorProbe = candidate.move(0.0D, -0.22D, 0.0D);

        if (this.level().noCollision(this, floorProbe)) {
            return false;
        }

        this.setPos(this.getX() + offset.x, this.getY() + offset.y, this.getZ() + offset.z);

        this.setSurface(ScarabSurface.FLOOR);
        this.setNoGravity(false);
        this.setOnGround(true);
        this.setDeltaMovement(Vec3.ZERO);

        return true;
    }

    private void tickSurfaceState(@Nullable Player rider) {

        if (this.isFlyingMode()) {
            if (this.getSurface() != ScarabSurface.FLOOR) {
                this.setSurface(ScarabSurface.FLOOR);
            }

            return;
        }

        ScarabSurface surface = this.getSurface();

        if (surface == ScarabSurface.FLOOR) {

            if (tryStartDownwardWall(rider)) {
                return;
            }

            tryStartWallClimb(rider);
            return;
        }


        this.getNavigation().stop();
        this.setNoGravity(true);
        this.resetFallDistance();

        if (surface.isWall()) {

            Direction normal = surface.normal();

            boolean wantsUp;

            boolean wantsDown;

            if (rider != null) {

                if (this.isWallFacingDown()) {

                    wantsDown = rider.zza > 0.05F;
                    wantsUp = rider.zza < -0.05F;

                } else {
                    wantsUp = rider.zza > 0.05F;
                    wantsDown = rider.zza < -0.05F;
                }

            } else {

                updateAutoWallDecision();
                wantsUp = this.autoWallDirection > 0;
                wantsDown = this.autoWallDirection < 0;
                this.setWallFacingDown(wantsDown);
            }

            updateWallBodyYaw(normal);

            if (wantsDown && isTouchingSurface(Direction.UP)) {

                double speed = this.getAttributeValue(Attributes.MOVEMENT_SPEED) * SURFACE_SPEED_MULTIPLIER;
                Vec3 downwardMovement = new Vec3(0.0D, -speed, 0.0D);

                if (!canSurfaceStep(normal, downwardMovement)) {

                    this.setSurface(ScarabSurface.FLOOR);
                    this.setDeltaMovement(Vec3.ZERO);

                    return;
                }
            }

            if (wantsUp && isTouchingSurface(Direction.DOWN)) {

                double speed = this.getAttributeValue(Attributes.MOVEMENT_SPEED) * SURFACE_SPEED_MULTIPLIER;
                Vec3 upwardMovement = new Vec3(0.0D, speed, 0.0D);

                if (!canSurfaceStep(normal, upwardMovement)) {

                    this.setSurface(ScarabSurface.CEILING);
                    faceDirection(normal);
                    startCeilingEntry(normal);

                    this.autoSurfaceDecisionTicks = 70 + this.random.nextInt(100);
                    this.setDeltaMovement(Vec3.ZERO);
                    return;
                }
            }

            if (!isTouchingSurface(normal)) {

                if (tryReturnToBaseSurface(normal)) {
                    return;
                }

                if (wantsUp && tryWrapWallOntoTop(normal)) {
                    return;
                }
                this.setSurface(ScarabSurface.FLOOR);
                return;
            }

            if (rider == null) {
                moveAutonomousOnWall();
            }

            return;
        }

        if (surface == ScarabSurface.CEILING) {

            if (!isTouchingSurface(Direction.DOWN)) {

                if (tryReturnToBaseSurface(Direction.DOWN)) {
                    return;
                }

                this.setSurface(ScarabSurface.FLOOR);
                return;
            }

            Direction ceilingEntryDirection = getCeilingEntryDirection();

            if (ceilingEntryDirection != null) {

                if (this.ceilingEntryTicks > 0) {
                    this.ceilingEntryTicks--;
                }

                if (rider != null && rider.zza <= 0.05F) {
                    clearCeilingEntry();
                    ceilingEntryDirection = null;

                } else if (this.ceilingEntryTicks <= 0) {
                    clearCeilingEntry();
                    ceilingEntryDirection = null;
                }
            }

            if (ceilingEntryDirection == null) {

                Direction travelDirection = getDesiredCeilingDirection(rider);

                if (travelDirection != null) {
                    Direction wallNormal = travelDirection.getOpposite();

                    if (isTouchingSurface(wallNormal)) {

                        double speed = this.getAttributeValue(Attributes.MOVEMENT_SPEED) * SURFACE_SPEED_MULTIPLIER;

                        Vec3 desiredMovement = directionVector(travelDirection).scale(speed);

                        if (!canSurfaceStep(Direction.DOWN, desiredMovement)) {
                            this.setSurface(ScarabSurface.fromWallNormal(wallNormal));
                            this.setWallFacingDown(true);

                            updateWallBodyYaw(wallNormal);

                            this.autoWallDirection = -1;
                            this.autoSurfaceDecisionTicks = 80 + this.random.nextInt(100);

                            return;
                        }
                    }
                }
            }

            if (rider == null) {
                moveAutonomousOnCeiling();
            }
        }
    }

    @Nullable
    private Direction getDesiredCeilingDirection(@Nullable Player rider) {
        double x;
        double z;

        if (rider != null) {

            if (Math.abs(rider.zza) < 0.05F && Math.abs(rider.xxa) < 0.05F) {

                return null;
            }

            float radians = (float) Math.toRadians(rider.getYRot());

            Vec3 forward = new Vec3(-Mth.sin(radians), 0.0D, Mth.cos(radians));
            Vec3 left = new Vec3(Mth.cos(radians), 0.0D, Mth.sin(radians));
            Vec3 movement = forward.scale(rider.zza).add(left.scale(rider.xxa));

            x = movement.x;
            z = movement.z;

        } else {

            if (!this.horizontalCollision) {
                return null;
            }

            float radians = (float) Math.toRadians(this.getYRot());

            x = -Mth.sin(radians);
            z = Mth.cos(radians);
        }

        if (Math.abs(x) > Math.abs(z)) {
            return x > 0.0D ? Direction.EAST : Direction.WEST;
        }
        return z > 0.0D ? Direction.SOUTH : Direction.NORTH;
    }

    public Vec3 getSurfaceVisualOffset() {
        ScarabSurface surface = this.getSurface();

        if (surface == ScarabSurface.FLOOR) {
            return Vec3.ZERO;
        }

        double inset = surface == ScarabSurface.CEILING ? CEILING_VISUAL_INSET : WALL_VISUAL_INSET;

        Direction normal = surface.normal();

        return new Vec3(-normal.getStepX() * inset, -normal.getStepY() * inset, -normal.getStepZ() * inset);
    }

    private void tickScarabMovementSounds() {

        if (!this.soundPositionInitialized) {
            this.lastSoundX = this.getX();
            this.lastSoundY = this.getY();
            this.lastSoundZ = this.getZ();
            this.soundPositionInitialized = true;
        }

        double dx = this.getX() - this.lastSoundX;
        double dy = this.getY() - this.lastSoundY;
        double dz = this.getZ() - this.lastSoundZ;

        double movedSq = dx * dx + dy * dy + dz * dz;

        this.lastSoundX = this.getX();
        this.lastSoundY = this.getY();
        this.lastSoundZ = this.getZ();

        boolean walking = !this.isFlyingMode() && (this.onGround() || this.isSurfaceAttached()) && movedSq > 0.0004D;

        if (walking) {
            if (this.stepSoundCooldown <= 0) {
                playScarabStep();
                this.stepSoundCooldown = STEP_SOUND_INTERVAL;

            } else {
                this.stepSoundCooldown--;
            }

        } else {
            this.stepSoundCooldown = 0;
            this.stepSoundIndex = 0;
        }
    }

    private void playScarabStep() {

        float volume;
        float pitch;

        switch (this.stepSoundIndex) {

            case 0 -> {
                volume = 0.30F;
                pitch = 0.94F;
            }

            case 1 -> {
                volume = 0.23F;
                pitch = 1.04F;
            }

            case 2 -> {
                volume = 0.28F;
                pitch = 0.98F;
            }

            default -> {
                volume = 0.24F;
                pitch = 1.08F;
            }
        }

        pitch += (this.getRandom().nextFloat() - 0.5F) * 0.035F;

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.SCARAB_STEP.get(), SoundSource.NEUTRAL, volume, pitch);

        this.stepSoundIndex = (this.stepSoundIndex + 1) % 4;
    }

    private void updateWallBodyYaw(Direction normal) {
        Direction yawDirection = this.isWallFacingDown() ? normal : normal.getOpposite();
        faceDirection(yawDirection);
    }

    @Nullable
    private Direction getCeilingEntryDirection() {
        int value = this.entityData.get(DATA_CEILING_ENTRY_DIRECTION);

        if (value < 0) {
            return null;
        }

        return Direction.from3DDataValue(value);
    }

    private void startCeilingEntry(Direction direction) {
        if (direction == Direction.UP || direction == Direction.DOWN) {

            return;
        }

        this.entityData.set(DATA_CEILING_ENTRY_DIRECTION, direction.get3DDataValue());

        this.ceilingEntryTicks = CEILING_ENTRY_TICKS;
    }

    private void clearCeilingEntry() {
        this.entityData.set(DATA_CEILING_ENTRY_DIRECTION, -1);

        this.ceilingEntryTicks = 0;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ScarabUpgrade materialUpgrade = getUpgradeForItem(stack.getItem());

        boolean isAzumalit = stack.is(ModItems.AZUMALIT_SHARD.get());
        boolean isNephritis = stack.is(ModItems.NEPHRITIS.get());

        if (materialUpgrade != null || isAzumalit || isNephritis) {
            if (!this.level().isClientSide) {
                boolean used;
                if (isNephritis) {
                    used = resetScarabUpgrade();
                } else if (isAzumalit) {
                    used = activateAzumalitBoost();
                } else {
                    used = applyPermanentUpgrade(materialUpgrade);
                }
                if (used && !player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

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
    public void die(DamageSource source) {
        if (!this.level().isClientSide && !this.coreDeathHandled && this.summoningCoreId != null && this.summoningPlayerId != null) {
            this.coreDeathHandled = true;
            if (this.level().getServer() != null) {
                ScarabCoreItem.onBoundScarabDeath(this.level().getServer(), this.summoningPlayerId, this.summoningCoreId, this.getUUID());
            }
        }

        super.die(source);
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

    private double getPowerMultiplier() {
        return this.isPowered() ? POWER_STAT_MULTIPLIER : 1.0D;
    }

    private double getCurrentFlightSpeed() {
        return FLIGHT_SPEED * this.getUpgrade().flightSpeedMultiplier() * getPowerMultiplier();
    }

    private double getCurrentFlightAcceleration() {
        return Math.min(0.85D, FLIGHT_ACCELERATION * getPowerMultiplier());
    }

    private float getCurrentFlightTurnSpeed() {
        return (float) (FLIGHT_TURN_SPEED * getPowerMultiplier());
    }

    private double getCurrentNormalJumpY() {
        double heightMultiplier = this.getUpgrade().jumpHeightMultiplier() * getPowerMultiplier();

        return NORMAL_JUMP_Y * Math.sqrt(heightMultiplier);
    }

    private double getCurrentTakeoffY() {
        return FLIGHT_TAKEOFF_Y * Math.sqrt(getPowerMultiplier());
    }

    private double getCurrentTakeoffForward() {
        return FLIGHT_TAKEOFF_FORWARD * getPowerMultiplier();
    }

    private void applyUpgradeAttributes() {
        AttributeInstance maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance movementSpeed = this.getAttribute(Attributes.MOVEMENT_SPEED);

        if (maxHealth == null || movementSpeed == null) {

            return;
        }

        float oldMaximum = this.getMaxHealth();
        float healthRatio = oldMaximum > 0.0F ? Mth.clamp(this.getHealth() / oldMaximum,

                0.0F, 1.0F) : 1.0F;

        ScarabUpgrade upgrade = this.getUpgrade();
        double power = getPowerMultiplier();
        maxHealth.setBaseValue(100.0D * upgrade.healthMultiplier() * power);
        movementSpeed.setBaseValue(SCARAB_MOVE_SPEED * upgrade.walkSpeedMultiplier() * power);
        if (this.isAlive()) {
            this.setHealth(Math.max(0.01F, this.getMaxHealth() * healthRatio));
        }
    }

    private ScarabUpgrade getUpgradeForItem(Item item) {
        if (item == Items.IRON_INGOT) {
            return ScarabUpgrade.IRON;
        }

        if (item == Items.AMETHYST_SHARD) {
            return ScarabUpgrade.AMETHYST;
        }

        if (item == Items.COPPER_INGOT) {
            return ScarabUpgrade.COPPER;
        }

        if (item == Items.GOLD_INGOT) {
            return ScarabUpgrade.GOLD;
        }

        if (item == Items.DIAMOND) {
            return ScarabUpgrade.DIAMOND;
        }

        if (item == Items.EMERALD) {
            return ScarabUpgrade.EMERALD;
        }

        if (item == Items.NETHERITE_INGOT) {
            return ScarabUpgrade.NETHERITE;
        }

        return null;
    }

    private BlockState getParticleBlock(ScarabUpgrade upgrade) {
        return switch (upgrade) {

            case IRON -> Blocks.IRON_BLOCK.defaultBlockState();

            case AMETHYST -> Blocks.AMETHYST_BLOCK.defaultBlockState();

            case COPPER -> Blocks.COPPER_BLOCK.defaultBlockState();

            case GOLD -> Blocks.GOLD_BLOCK.defaultBlockState();

            case DIAMOND -> Blocks.DIAMOND_BLOCK.defaultBlockState();

            case EMERALD -> Blocks.EMERALD_BLOCK.defaultBlockState();

            case NETHERITE -> Blocks.NETHERITE_BLOCK.defaultBlockState();

            default -> Oasiso.NEPHRITIS_BLOCK.get().defaultBlockState();
        };
    }

    private void playUpgradeEffect(BlockState particleBlock) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {

            return;
        }

        serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, particleBlock), this.getX(), this.getY() + this.getBbHeight() * 0.55D, this.getZ(), 65, this.getBbWidth() * 0.55D, this.getBbHeight() * 0.38D, this.getBbWidth() * 0.55D, 0.12D);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SMITHING_TABLE_USE, SoundSource.NEUTRAL, 1.15F, 0.92F + this.getRandom().nextFloat() * 0.12F);
    }

    private boolean applyPermanentUpgrade(ScarabUpgrade upgrade) {
        if (upgrade == null || upgrade == ScarabUpgrade.NONE || upgrade == this.getUpgrade()) {

            return false;
        }

        this.setUpgrade(upgrade);
        applyUpgradeAttributes();
        playUpgradeEffect(getParticleBlock(upgrade));

        return true;
    }

    private boolean resetScarabUpgrade() {
        if (this.getUpgrade() == ScarabUpgrade.NONE && !this.isPowered()) {

            return false;
        }

        this.powerTicksRemaining = 0;
        this.powerFxTicksRemaining = 0;

        this.setPowered(false);
        this.setUpgrade(ScarabUpgrade.NONE);

        applyUpgradeAttributes();
        playUpgradeEffect(Oasiso.NEPHRITIS_BLOCK.get().defaultBlockState());

        return true;
    }

    private boolean activateAzumalitBoost() {
        boolean alreadyPowered = this.isPowered();
        this.powerTicksRemaining = POWER_DURATION;
        this.powerFxTicksRemaining = POWER_FX_DURATION;

        this.setPowered(true);

        if (!alreadyPowered) {
            applyUpgradeAttributes();
        }

        if (this.level() instanceof ServerLevel) {
            playAzumalitMagicSounds();
        }

        return true;
    }

    private void spawnPowerSpiral() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        int age = POWER_FX_DURATION - this.powerFxTicksRemaining;

        for (int arm = 0; arm < 3; arm++) {

            double angle = age * 0.48D + arm * (Math.PI * 2.0D / 3.0D);
            double radius = this.getBbWidth() * 0.60D + 0.15D * Math.sin(age * 0.28D);
            double heightProgress = (age * 0.14D + arm * 0.85D) % (this.getBbHeight() + 0.65D);

            double x = this.getX() + Math.cos(angle) * radius;
            double y = this.getY() + 0.15D + heightProgress;
            double z = this.getZ() + Math.sin(angle) * radius;

            serverLevel.sendParticles(Oasiso.ARM_SMOKE.get(), x, y, z, 2, 0.025D, 0.045D, 0.025D, 0.015D);
        }
    }

    @Override
    public void travel(Vec3 travelVector) {
        LivingEntity controller = this.getControllingPassenger();

        if (!this.isAlive()) {

            super.travel(travelVector);

            return;
        }

        if (!this.isFlyingMode() && this.isSurfaceAttached()) {

            if (controller instanceof Player player) {

                travelOnAttachedSurface(player);

            } else {
                this.setDeltaMovement(Vec3.ZERO);
            }
            return;
        }

        if (!(controller instanceof Player player)) {
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

                Vec3 wantedVelocity = player.getLookAngle().normalize().scale(getCurrentFlightSpeed());
                Vec3 velocity = this.getDeltaMovement().lerp(wantedVelocity, getCurrentFlightAcceleration());

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
        float turnSpeed = getCurrentFlightTurnSpeed();
        float rotationStep = Mth.clamp(difference, -turnSpeed, turnSpeed);
        float yaw = this.getYRot() + rotationStep;
        this.setYRot(yaw);

        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    private void updateFlightTilt(Player player) {
        float targetPitch = 0.0F;
        float targetRoll = 0.0F;

        ScarabUpgrade upgrade = this.getUpgrade();

        float response;

        if (!this.isFlyingMode() && this.isSurfaceAttached()) {

            ScarabSurface surface = this.getSurface();

            if (surface.isWall()) {
                targetPitch = this.isWallFacingDown() ? -90.0F : 90.0F;
                targetRoll = 0.0F;

            } else {
                targetPitch = surface.pitch();
                targetRoll = surface.roll();
            }

            response = SURFACE_TILT_RESPONSE * upgrade.tiltResponseMultiplier();
            response = Mth.clamp(response, 0.06F, 0.45F);

        } else {

            float poweredAngle = this.isPowered() ? POWER_TILT_ANGLE_MULTIPLIER : 1.0F;
            float pitchScale = upgrade.pitchMultiplier() * poweredAngle;
            float rollScale = upgrade.rollMultiplier() * poweredAngle;

            response = FLIGHT_TILT_RESPONSE * upgrade.tiltResponseMultiplier() * (this.isPowered() ? POWER_TILT_RESPONSE_MULTIPLIER : 1.0F);

            response = Mth.clamp(response, 0.035F, 0.65F);

            if (this.isFlyingMode() && this.isNoGravity() && player != null) {

                targetPitch = Mth.clamp(-player.getXRot() * pitchScale, -MAX_FLIGHT_PITCH * pitchScale, MAX_FLIGHT_PITCH * pitchScale);
                float yawDifference = Mth.wrapDegrees(player.getYRot() - this.getYRot());
                targetRoll = Mth.clamp(-yawDifference * FLIGHT_ROLL_FACTOR * rollScale, -MAX_FLIGHT_ROLL * rollScale, MAX_FLIGHT_ROLL * rollScale);
            }
        }

        float pitch = Mth.lerp(response, this.getFlightPitch(), targetPitch);
        float roll = Mth.lerp(response, this.getFlightRoll(), targetRoll);

        if (Math.abs(pitch) < 0.025F) {
            pitch = 0.0F;
        }

        if (Math.abs(roll) < 0.025F) {
            roll = 0.0F;
        }

        this.entityData.set(DATA_FLIGHT_PITCH, pitch);
        this.entityData.set(DATA_FLIGHT_ROLL, roll);
    }

    private boolean hasFloorWithinDrop(Direction direction, int maxDrop) {
        double distance = this.getBbWidth() * 0.5D + 0.18D;

        double x = this.getX() + direction.getStepX() * distance;
        double z = this.getZ() + direction.getStepZ() * distance;

        int startY = Mth.floor(this.getBoundingBox().minY - 0.12D);
        for (int drop = 0; drop <= maxDrop; drop++) {
            BlockPos pos = BlockPos.containing(x, startY - drop, z);
            BlockState state = this.level().getBlockState(pos);
            if (!state.getCollisionShape(this.level(), pos).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private boolean tryStartDownwardWall(@Nullable Player rider) {
        if (this.isFlyingMode() || this.isSurfaceAttached() || !this.onGround()) {

            return false;
        }

        boolean wantsForward;

        float yaw;

        if (rider != null) {

            wantsForward = rider.zza > 0.05F;
            yaw = rider.getYRot();

        } else {

            Vec3 movement = this.getDeltaMovement();
            wantsForward = movement.x * movement.x + movement.z * movement.z > 0.0020D;
            yaw = this.getYRot();
        }

        if (!wantsForward) {
            return false;
        }

        Direction travelDirection = getHorizontalDirection(yaw);

        if (this.horizontalCollision) {
            return false;
        }

        if (hasFloorWithinDrop(travelDirection, NORMAL_DROP_HEIGHT)) {
            return false;
        }

        Vec3 forward = directionVector(travelDirection);

        double maxForward = this.getBbWidth() + 0.75D;
        double maxDown = Math.max(NORMAL_DROP_HEIGHT + 3.0D, this.getBbHeight() + 1.5D);

        for (double down = 0.20D; down <= maxDown; down += 0.20D) {
            for (double forwardDistance = 0.20D; forwardDistance <= maxForward; forwardDistance += 0.20D) {

                Vec3 offset = forward.scale(forwardDistance).add(0.0D, -down, 0.0D);
                AABB candidate = this.getBoundingBox().move(offset);

                if (!this.level().noCollision(this, candidate)) {

                    continue;
                }

                if (!isTouchingSurfaceAt(candidate, travelDirection)) {
                    continue;
                }

                this.setPos(this.getX() + offset.x, this.getY() + offset.y, this.getZ() + offset.z);
                this.setSurface(ScarabSurface.fromWallNormal(travelDirection));
                this.setWallFacingDown(true);
                updateWallBodyYaw(travelDirection);

                this.setOnGround(false);
                this.setNoGravity(true);
                this.setDeltaMovement(Vec3.ZERO);
                this.getNavigation().stop();
                this.autoWallDirection = -1;
                this.autoSurfaceDecisionTicks = 80 + this.random.nextInt(100);

                return true;
            }
        }

        return false;
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

            double takeoffForward = getCurrentTakeoffForward();
            this.setDeltaMovement(horizontal.x * takeoffForward, getCurrentTakeoffY(), horizontal.z * takeoffForward);
            this.setOnGround(false);
            this.resetFallDistance();
            return;
        }

        Vec3 movement = this.getDeltaMovement();
        this.setDeltaMovement(movement.x, getCurrentNormalJumpY(), movement.z);
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

        if (!this.level().isClientSide) {

            if (this.powerTicksRemaining > 0) {
                this.powerTicksRemaining--;

                if (this.powerTicksRemaining == 0 && this.isPowered()) {

                    this.setPowered(false);

                    applyUpgradeAttributes();

                    this.powerFxTicksRemaining = POWER_FX_DURATION;
                    playAzumalitMagicSounds();
                }
            }

            if (this.powerFxTicksRemaining > 0) {
                spawnPowerSpiral();
                this.powerFxTicksRemaining--;
            }
            tickNetheriteProtection(passenger);
            Player surfaceRider = passenger instanceof Player player ? player : null;
            tickSurfaceState(surfaceRider);
            tickScarabMovementSounds();
        }

        if (!this.level().isClientSide && this.isVehicle()) {
            this.getNavigation().stop();
            this.setTarget(null);
        }

        if (!this.isFlyingMode()) {

            if (this.isSurfaceAttached()) {

                if (!this.isNoGravity()) {
                    this.setNoGravity(true);
                }

            } else if (this.isNoGravity()) {
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
    public boolean isInvulnerableTo(DamageSource source) {
        if (this.getUpgrade() == ScarabUpgrade.NETHERITE && source.is(DamageTypeTags.IS_FIRE)) {
            return true;
        }

        return super.isInvulnerableTo(source);
    }

    private void tickNetheriteProtection(Entity passenger) {
        if (this.getUpgrade() != ScarabUpgrade.NETHERITE) {
            return;
        }

        this.clearFire();
        if (!(passenger instanceof Player player)) {

            return;
        }

        player.clearFire();
        MobEffectInstance existing = player.getEffect(MobEffects.FIRE_RESISTANCE);
        if (existing == null || existing.getDuration() < 8) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 12, 0, false, false, false));
        }
    }

    private void playAzumalitMagicSounds() {

        if (!(this.level() instanceof ServerLevel)) {

            return;
        }

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.NEUTRAL, 1.30F, 0.85F);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.85F, 0.72F);
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

            if (this.isSurfaceAttached()) {

                if (this.getDeltaMovement().lengthSqr() > 0.0004D) {
                    return state.setAndContinue(WALK);
                }
                return state.setAndContinue(IDLE);
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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        tag.putInt("ScarabSurface", this.getSurface().id());
        tag.putBoolean("ScarabWallFacingDown", this.isWallFacingDown());

        if (this.summoningCoreId != null) {
            tag.putUUID("SummoningCoreId", this.summoningCoreId);
        }
        if (this.summoningPlayerId != null) {
            tag.putUUID("SummoningPlayerId", this.summoningPlayerId);
        }

        tag.putInt("ScarabUpgrade", this.getUpgrade().id());
        tag.putInt("ScarabPowerTicks", this.powerTicksRemaining);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        this.setSurface(ScarabSurface.byId(tag.getInt("ScarabSurface")));
        this.setWallFacingDown(tag.getBoolean("ScarabWallFacingDown"));

        this.setUpgrade(ScarabUpgrade.byId(tag.getInt("ScarabUpgrade")));
        this.powerTicksRemaining = Math.max(0, tag.getInt("ScarabPowerTicks"));

        if (tag.hasUUID("SummoningCoreId")) {
            this.summoningCoreId = tag.getUUID("SummoningCoreId");
        }

        if (tag.hasUUID("SummoningPlayerId")) {
            this.summoningPlayerId = tag.getUUID("SummoningPlayerId");
            this.setPersistenceRequired();
        }

        this.setPowered(this.powerTicksRemaining > 0);

        if (!this.level().isClientSide) {
            applyUpgradeAttributes();
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {

        SoundEvent[] sounds = {ModSounds.SCARAB_IDLE.get(), ModSounds.SCARAB_IDLE2.get(), ModSounds.SCARAB_IDLE3.get()};

        return sounds[this.random.nextInt(sounds.length)];
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return ModSounds.SCARAB_IDLE3.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SCARAB_DEATH.get();
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        String texture = this.isPowered() ? "scarab_powered_emissive.png" : "scarab_emissive.png";
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/" + texture);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
