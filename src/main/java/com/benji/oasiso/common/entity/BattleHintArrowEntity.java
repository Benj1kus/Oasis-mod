package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class BattleHintArrowEntity extends Monster implements GeoEntity, GlowmaskEntity {

    private static final double RADIAL_ARROW_OFFSET = 4.54D;
    private static final int AIM_DURATION = 20 * 8;
    private static final double FLOOR_OFFSET = 0.025D;
    private static final float MAX_ROTATION_PER_TICK = 14.0F;

    private static final int MODE_TRACKING = 0;
    private static final int MODE_FIXED = 1;

    private static final double DESIRED_BOMB_SPACING = 2.75D;

    private static final int MIN_BOMBS = 2;
    private static final int MAX_BOMBS = 16;

    private static final int RADIAL_BOMB_COUNT = 5;
    private static final double RADIAL_BOMB_SPACING = 2.75D;

    private static final double BOMB_START_DISTANCE = 1.5D;
    private static final double BOMB_HEIGHT = 5.0D;

    private static final String OWNER_TAG = "ArrowOwner";
    private static final String TARGET_TAG = "ArrowTarget";
    private static final String AIM_TICKS_TAG = "ArrowAimTicks";
    private static final String MODE_TAG = "ArrowMode";
    private static final String FIXED_YAW_TAG = "ArrowFixedYaw";

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);


    private UUID ownerId;
    private UUID targetId;

    private int aimTicks = AIM_DURATION;
    private int mode = MODE_TRACKING;

    private float fixedYaw;


    public BattleHintArrowEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.noPhysics = true;
    }


    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1000.0D).add(Attributes.MOVEMENT_SPEED, 0.0D).add(Attributes.KNOCKBACK_RESISTANCE, 1.0D).add(Attributes.ATTACK_DAMAGE, 0.0D).add(Attributes.FOLLOW_RANGE, 128.0D);
    }

    @Override
    protected void registerGoals() {
    }

    public void startAiming(AzumaalEntity owner, ServerPlayer target) {
        startTracking(owner, target);
    }


    public void startTracking(AzumaalEntity owner, ServerPlayer target) {
        this.ownerId = owner.getUUID();
        this.targetId = target.getUUID();
        this.mode = MODE_TRACKING;
        this.aimTicks = AIM_DURATION;
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.noPhysics = true;

        if (this.level() instanceof ServerLevel serverLevel) {
            followOwnerOnGround(serverLevel, owner);
        }
        faceTarget(target, 180.0F);
    }

    public void startFixedDirection(AzumaalEntity owner, float yaw) {
        this.ownerId = owner.getUUID();
        this.targetId = null;
        this.mode = MODE_FIXED;
        this.fixedYaw = yaw;
        this.aimTicks = AIM_DURATION;
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.noPhysics = true;

        if (this.level() instanceof ServerLevel serverLevel) {
            followOwnerOnGround(serverLevel, owner);
        }
        setArrowYaw(yaw);
    }

    @Override
    public void tick() {
        super.tick();

        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.noPhysics = true;
        this.setDeltaMovement(Vec3.ZERO);
        this.fallDistance = 0.0F;
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        AzumaalEntity owner = resolveOwner(serverLevel);
        if (owner == null) {
            this.discard();
            return;
        }

        followOwnerOnGround(serverLevel, owner);
        ServerPlayer target = null;
        if (this.mode == MODE_TRACKING) {
            target = resolveTarget(serverLevel);
            if (target == null) {
                this.discard();
                return;
            }
            faceTarget(target, MAX_ROTATION_PER_TICK);
        } else {
            setArrowYaw(this.fixedYaw);
        }
        this.aimTicks--;
        if (this.aimTicks > 0) {
            return;
        }
        if (this.mode == MODE_TRACKING) {
            spawnTrackingBombLine(serverLevel, target);
        } else {
            spawnFixedBombLine(serverLevel);
        }
        this.discard();
    }

    private void followOwnerOnGround(ServerLevel level, AzumaalEntity owner) {
        double x = owner.getX();
        double z = owner.getZ();

        if (this.mode == MODE_FIXED) {
            double yawRadians = Math.toRadians(this.fixedYaw);
            double directionX = -Math.sin(yawRadians);
            double directionZ = Math.cos(yawRadians);

            x += directionX * RADIAL_ARROW_OFFSET;
            z += directionZ * RADIAL_ARROW_OFFSET;
        }
        double floorY = getGroundY(level, x, z);
        this.setPos(x, floorY + FLOOR_OFFSET, z);
    }

    private double getGroundY(ServerLevel level, double x, double z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
    }


    private void faceTarget(ServerPlayer target, float maxStep) {
        double deltaX = target.getX() - this.getX();
        double deltaZ = target.getZ() - this.getZ();

        if (deltaX * deltaX + deltaZ * deltaZ < 0.0001D) {
            return;
        }
        float targetYaw = (float) (Mth.atan2(deltaZ, deltaX) * (180.0D / Math.PI)) - 90.0F;
        float difference = Mth.wrapDegrees(targetYaw - this.getYRot());
        float rotation = Mth.clamp(difference, -maxStep, maxStep);
        setArrowYaw(this.getYRot() + rotation);
    }


    private void setArrowYaw(float yaw) {
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.yBodyRot = yaw;
        this.yBodyRotO = yaw;
    }

    private void spawnTrackingBombLine(ServerLevel level, ServerPlayer target) {
        double deltaX = target.getX() - this.getX();
        double deltaZ = target.getZ() - this.getZ();

        double targetDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double lineDistance = Math.max(2.5D, targetDistance);
        int bombCount = Mth.clamp((int) Math.ceil(lineDistance / DESIRED_BOMB_SPACING), MIN_BOMBS, MAX_BOMBS);

        Vec3 direction = getArrowDirection();
        for (int i = 0; i < bombCount; i++) {
            double progress = bombCount <= 1 ? 0.5D : i / (double) (bombCount - 1);
            double distance = Mth.lerp(progress, BOMB_START_DISTANCE, lineDistance);
            spawnBomb(level, direction, distance);
        }
    }

    private void spawnFixedBombLine(ServerLevel level) {
        Vec3 direction = getArrowDirection();

        double originX = this.getX() - direction.x * RADIAL_ARROW_OFFSET;
        double originZ = this.getZ() - direction.z * RADIAL_ARROW_OFFSET;

        for (int i = 0; i < RADIAL_BOMB_COUNT; i++) {
            double distance = BOMB_START_DISTANCE + i * RADIAL_BOMB_SPACING;
            spawnBombFromOrigin(level, originX, originZ, direction, distance);
        }
    }

    private void spawnBombFromOrigin(ServerLevel level, double originX, double originZ, Vec3 direction, double distance) {
        double bombX = originX + direction.x * distance;
        double bombZ = originZ + direction.z * distance;
        double bombY = getGroundY(level, bombX, bombZ) + BOMB_HEIGHT;

        ChaosBombEntity bomb = Oasiso.CHAOS_BOMB.get().create(level);

        if (bomb == null) {
            return;
        }
        AzumaalEntity owner = resolveOwner(level);
        if (owner != null) {
            bomb.setAzumaalOwner(owner);
        }
        bomb.moveTo(bombX, bombY, bombZ, this.random.nextFloat() * 360.0F, 0.0F);
        bomb.setDeltaMovement(0.0D, -0.08D, 0.0D);
        level.addFreshEntity(bomb);
        level.sendParticles(Oasiso.PURPLE_STARS.get(), bombX, bombY + 0.35D, bombZ, 7, 0.28D, 0.32D, 0.28D, 0.055D);
    }


    private Vec3 getArrowDirection() {
        double yawRadians = Math.toRadians(this.getYRot());
        return new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
    }

    private void spawnBomb(ServerLevel level, Vec3 direction, double distance) {
        double bombX = this.getX() + direction.x * distance;
        double bombZ = this.getZ() + direction.z * distance;
        double bombY = getGroundY(level, bombX, bombZ) + BOMB_HEIGHT;
        ChaosBombEntity bomb = Oasiso.CHAOS_BOMB.get().create(level);
        if (bomb == null) {
            return;
        }

        bomb.moveTo(bombX, bombY, bombZ, this.random.nextFloat() * 360.0F, 0.0F);
        bomb.setDeltaMovement(0.0D, -0.08D, 0.0D);
        level.addFreshEntity(bomb);

        level.sendParticles(Oasiso.PURPLE_STARS.get(), bombX, bombY + 0.35D, bombZ, 7, 0.28D, 0.32D, 0.28D, 0.055D);
    }

    private AzumaalEntity resolveOwner(ServerLevel level) {
        if (this.ownerId == null) {
            return null;
        }

        Entity entity = level.getEntity(this.ownerId);
        if (!(entity instanceof AzumaalEntity owner) || !owner.isAlive()) {
            return null;
        }
        return owner;
    }

    private ServerPlayer resolveTarget(ServerLevel level) {
        if (this.targetId == null) {
            return null;
        }
        Entity entity = level.getEntity(this.targetId);
        if (!(entity instanceof ServerPlayer target) || !target.isAlive() || target.isSpectator() || target.isCreative()) {
            return null;
        }
        return target;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerId != null) {
            tag.putUUID(OWNER_TAG, this.ownerId);
        }
        if (this.targetId != null) {
            tag.putUUID(TARGET_TAG, this.targetId);
        }
        tag.putInt(AIM_TICKS_TAG, this.aimTicks);
        tag.putInt(MODE_TAG, this.mode);
        tag.putFloat(FIXED_YAW_TAG, this.fixedYaw);
    }


    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        this.ownerId = tag.hasUUID(OWNER_TAG) ? tag.getUUID(OWNER_TAG) : null;
        this.targetId = tag.hasUUID(TARGET_TAG) ? tag.getUUID(TARGET_TAG) : null;

        this.aimTicks = tag.contains(AIM_TICKS_TAG) ? tag.getInt(AIM_TICKS_TAG) : AIM_DURATION;
        this.mode = tag.contains(MODE_TAG) ? tag.getInt(MODE_TAG) : MODE_TRACKING;

        this.fixedYaw = tag.getFloat(FIXED_YAW_TAG);

        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.noPhysics = true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> state.setAndContinue(IDLE_ANIMATION)));
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/" + "battle_hint_arrow.png");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}