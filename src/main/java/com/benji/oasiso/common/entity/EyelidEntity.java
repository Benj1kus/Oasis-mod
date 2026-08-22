package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import com.benji.oasiso.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class EyelidEntity extends Projectile implements GeoEntity, GlowmaskEntity {

    private static final int OWNER_AZUMAAL = 0;
    private static final int OWNER_CRUSADER_TANK = 1;
    private static final String OWNER_TYPE_TAG = "OwnerType";

    private int ownerType = OWNER_AZUMAAL;

    public static final int MODE_ORBIT = 0;
    public static final int MODE_HOMING = 1;
    public static final int MODE_REFLECTED = 2;

    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(EyelidEntity.class, EntityDataSerializers.INT);

    //orbit
    private static final double MIN_ORBIT_RADIUS = 3.2D;
    private static final double MAX_ORBIT_RADIUS = 4.4D;
    private static final double MIN_ORBIT_SPEED = 0.055D;
    private static final double MAX_ORBIT_SPEED = 0.078D;

    private static final int MIN_LAUNCH_DELAY = 35;
    private static final int RANDOM_LAUNCH_DELAY = 70;
    private static final double HOMING_SPEED = 0.95D;

    private static final double HOMING_TURN_FACTOR = 0.32D;

    private static final double TARGET_PREDICTION = 5.0D;
    private static final double REFLECTED_SPEED = 1.25D;

    private static final int MAX_FLIGHT_TICKS = 20 * 12;
    private static final double MAX_OWNER_DISTANCE = 80.0D;
    private static final int REFLECTION_GRACE = 5;

    private static final float PLAYER_DAMAGE = 15.0F;

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);


    private UUID ownerId;
    private UUID targetId;
    private UUID reflectorId;


    private double orbitAngle;
    private double orbitRadius;
    private double orbitSpeed;
    private double orbitVerticalPhase;
    private int orbitDirection = 1;

    private int launchDelay;
    private int flightTicks;
    private int reflectionGraceTicks;


    public EyelidEntity(EntityType<? extends EyelidEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(MODE, MODE_ORBIT);
    }

    public int getFlightMode() {
        return this.entityData.get(MODE);
    }

    private void setFlightMode(int mode) {
        this.entityData.set(MODE, mode);
    }

    public void initializeOrbit(AzumaalEntity owner, int index) {
        this.ownerType = OWNER_AZUMAAL;
        this.ownerId = owner.getUUID();
        this.setOwner(owner);

        this.orbitAngle = Math.toRadians(index * 60.0D);
        this.orbitRadius = MIN_ORBIT_RADIUS + this.random.nextDouble() * (MAX_ORBIT_RADIUS - MIN_ORBIT_RADIUS);
        this.orbitSpeed = MIN_ORBIT_SPEED + this.random.nextDouble() * (MAX_ORBIT_SPEED - MIN_ORBIT_SPEED);

        this.orbitDirection = this.random.nextBoolean()

                ? 1

                : -1;

        this.orbitVerticalPhase = index * (Math.PI * 2.0D / 6.0D);
        this.launchDelay = MIN_LAUNCH_DELAY + index * 8 + this.random.nextInt(RANDOM_LAUNCH_DELAY);

        this.flightTicks = 0;
        this.setFlightMode(MODE_ORBIT);
        Vec3 start = calculateOrbitPosition(owner);
        this.moveTo(start.x, start.y, start.z, owner.getYRot(), 0.0F);

        this.setDeltaMovement(Vec3.ZERO);
    }

    public void initializeTankShot(CrusaderTankEntity owner, ServerPlayer target, Vec3 spawnPosition) {
        this.ownerType = OWNER_CRUSADER_TANK;
        this.ownerId = owner.getUUID();
        this.targetId = target.getUUID();
        this.reflectorId = null;

        this.setOwner(owner);
        this.flightTicks = 0;
        this.reflectionGraceTicks = 0;

        this.setFlightMode(MODE_HOMING);
        this.moveTo(spawnPosition.x, spawnPosition.y, spawnPosition.z, owner.getYRot(), 0.0F);

        Vec3 direction = getDesiredDirection(target);
        this.setDeltaMovement(direction.scale(HOMING_SPEED));
        updateFacing(this.getDeltaMovement());
    }

    @Override
    public void tick() {
        super.tick();
        this.setNoGravity(true);
        if (this.level().isClientSide) {
            return;
        }
        if (!(this.level() instanceof ServerLevel level)) {

            return;
        }
        if (this.ownerType == OWNER_CRUSADER_TANK) {
//TANK PROJ
            CrusaderTankEntity tankOwner = resolveTankOwner(level);
            if (tankOwner == null || !tankOwner.isAlive()) {
                dissolve(level);
                return;
            }
            if (this.distanceToSqr(tankOwner) > MAX_OWNER_DISTANCE * MAX_OWNER_DISTANCE) {
                dissolve(level);
                return;
            }
        } else {
            //boss proj
            AzumaalEntity azumaalOwner = resolveOwner(level);
            if (azumaalOwner == null || !azumaalOwner.isAlive() || !azumaalOwner.isDefending()) {
                dissolve(level);
                return;
            }
            if (this.distanceToSqr(azumaalOwner) > MAX_OWNER_DISTANCE * MAX_OWNER_DISTANCE) {
                dissolve(level);
                return;
            }
        }
        if (this.reflectionGraceTicks > 0) {
            this.reflectionGraceTicks--;
        }
        switch (this.getFlightMode()) {
            case MODE_HOMING -> tickHoming(level);
            case MODE_REFLECTED -> tickReflected(level);

            default -> {
                AzumaalEntity azumaalOwner = resolveOwner(level);
                if (azumaalOwner == null || !azumaalOwner.isAlive() || !azumaalOwner.isDefending()) {
                    dissolve(level);
                    return;
                }
                tickOrbit(level, azumaalOwner);
            }
        }
    }

    private void tickOrbit(ServerLevel level, AzumaalEntity owner) {
        Vec3 previousPosition = this.position();

        this.orbitAngle += this.orbitSpeed * this.orbitDirection;

        Vec3 newPosition = calculateOrbitPosition(owner);
        Vec3 movement = newPosition.subtract(previousPosition);

        this.setPos(newPosition);
        this.setDeltaMovement(movement);

        updateFacing(movement);

        this.launchDelay--;
        if (this.launchDelay > 0) {
            return;
        }
        ServerPlayer target = findNearestTarget(level);
        if (target == null) {
            this.launchDelay = 20;
            return;
        }
        launchAt(target);
    }

    private Vec3 calculateOrbitPosition(AzumaalEntity owner) {
        double x = owner.getX() + Math.cos(this.orbitAngle) * this.orbitRadius;
        double z = owner.getZ() + Math.sin(this.orbitAngle) * this.orbitRadius;
        double y = owner.getY() + owner.getBbHeight() * 0.52D + Math.sin(this.tickCount * 0.11D + this.orbitVerticalPhase) * 0.85D;

        return new Vec3(x, y, z);
    }

    private void launchAt(ServerPlayer target) {
        this.targetId = target.getUUID();
        this.flightTicks = 0;
        this.setFlightMode(MODE_HOMING);
        Vec3 direction = getDesiredDirection(target);
        this.setDeltaMovement(direction.scale(HOMING_SPEED));
        updateFacing(this.getDeltaMovement());
    }


    private void tickHoming(ServerLevel level) {
        this.flightTicks++;
        if (this.flightTicks >= MAX_FLIGHT_TICKS) {
            dissolve(level);
            return;
        }
        ServerPlayer target = resolveTarget(level);
        if (target == null) {
            target = findNearestTarget(level);

            if (target != null) {
                this.targetId = target.getUUID();
            } else {
                this.setFlightMode(MODE_REFLECTED);
                tickReflected(level);
                return;
            }
        }
        Vec3 desired = getDesiredDirection(target);
        Vec3 current = this.getDeltaMovement();

        if (current.lengthSqr() < 0.0001D) {
            current = desired;
        }

        current = current.normalize();
        Vec3 steering = current.scale(1.0D - HOMING_TURN_FACTOR).add(desired.scale(HOMING_TURN_FACTOR));

        if (steering.lengthSqr() < 0.0001D) {
            steering = desired;
        }
        this.setDeltaMovement(steering.normalize().scale(HOMING_SPEED));
        updateFacing(this.getDeltaMovement());
        flyAndCollide(level);
    }


    private Vec3 getDesiredDirection(ServerPlayer target) {
        Vec3 predicted = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ()).add(target.getDeltaMovement().scale(TARGET_PREDICTION));
        Vec3 direction = predicted.subtract(this.position());
        if (direction.lengthSqr() < 0.0001D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }


        return direction.normalize();
    }

    private void tickReflected(ServerLevel level) {
        this.flightTicks++;
        if (this.flightTicks >= MAX_FLIGHT_TICKS) {
            dissolve(level);
            return;
        }
        updateFacing(this.getDeltaMovement());
        flyAndCollide(level);
    }

    private void flyAndCollide(ServerLevel level) {
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() != HitResult.Type.MISS) {
            this.onHit(hitResult);
            if (this.isRemoved()) {
                return;
            }
        }
        Vec3 movement = this.getDeltaMovement();
        this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
            return false;
        }
        if (this.reflectionGraceTicks > 0 && this.reflectorId != null && this.reflectorId.equals(player.getUUID())) {
            return false;
        }
        return true;
    }


    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (!(result.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (this.ownerType == OWNER_CRUSADER_TANK) {
            CrusaderTankEntity owner = resolveTankOwner(level);
            if (owner != null && owner.isAlive()) {
                player.hurt(owner.damageSources().mobAttack(owner), PLAYER_DAMAGE);
            }
        } else {
            AzumaalEntity owner = resolveOwner(level);
            if (owner != null && owner.isAlive()) {
                player.hurt(owner.damageSources().mobAttack(owner), PLAYER_DAMAGE);
            }
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.EYELID_SHOT.get(), SoundSource.HOSTILE, 1.2F, 1.0F);
        dissolve(level);
    }

    private CrusaderTankEntity resolveTankOwner(ServerLevel level) {
        if (this.ownerId == null) {
            return null;
        }
        Entity entity = level.getEntity(this.ownerId);
        if (entity instanceof CrusaderTankEntity tank) {
            return tank;
        }
        return null;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        dissolve(level);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!(source.getEntity() instanceof Player player) || player.isSpectator()) {
            return false;
        }
        if (this.level().isClientSide) {
            return true;
        }
        Vec3 direction = player.getLookAngle();
        if (direction.lengthSqr() < 0.0001D) {
            return false;
        }

        this.targetId = null;
        this.reflectorId = player.getUUID();
        this.reflectionGraceTicks = REFLECTION_GRACE;
        this.flightTicks = 0;

        this.setFlightMode(MODE_REFLECTED);
        this.setDeltaMovement(direction.normalize().scale(REFLECTED_SPEED));
        updateFacing(this.getDeltaMovement());
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(Oasiso.PURPLE_STARS.get(), this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(), 10, 0.25D, 0.25D, 0.25D, 0.055D);
        }
        return true;
    }

    private void updateFacing(Vec3 movement) {
        if (movement.lengthSqr() < 0.000001D) {
            return;
        }
        float targetYaw = (float) (Mth.atan2(movement.z, movement.x) * (180.0D / Math.PI)) - 90.0F;
        double horizontal = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
        float targetPitch = (float) (-Mth.atan2(movement.y, horizontal) * (180.0D / Math.PI));

        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();

        this.setYRot(targetYaw);
        this.setXRot(targetPitch);
    }

    private AzumaalEntity resolveOwner(ServerLevel level) {
        if (this.ownerId == null) {
            return null;
        }
        Entity entity = level.getEntity(this.ownerId);
        if (entity instanceof AzumaalEntity owner && !owner.isClone()) {
            return owner;
        }
        return null;
    }

    public boolean isOwnedBy(AzumaalEntity owner) {
        return this.ownerType == OWNER_AZUMAAL
                && this.ownerId != null
                && this.ownerId.equals(owner.getUUID());
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

    private ServerPlayer findNearestTarget(ServerLevel level) {
        ServerPlayer nearest = null;
        double nearestDistance = 96.0D * 96.0D;
        for (ServerPlayer player : level.players()) {
            if (!isValidTarget(player)) {
                continue;
            }
            double distance = this.distanceToSqr(player);
            if (distance >= nearestDistance) {
                continue;
            }
            nearest = player;
            nearestDistance = distance;
        }
        return nearest;
    }

    private boolean isValidTarget(ServerPlayer player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator();
    }

    private void dissolve(ServerLevel level) {
        if (this.isRemoved()) {
            return;
        }
        level.sendParticles(Oasiso.PURPLE_STARS.get(), this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(), 30, 0.35D, 0.35D, 0.35D, 0.09D);
        this.discard();
    }

    @Override
    public boolean isPickable() {
        return true;
    }


    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        final double renderDistance = 128.0D;
        return distance < renderDistance * renderDistance;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
        if (this.targetId != null) {
            tag.putUUID("Target", this.targetId);
        }
        if (this.reflectorId != null) {
            tag.putUUID("Reflector", this.reflectorId);
        }
        tag.putInt(OWNER_TYPE_TAG, this.ownerType);
        tag.putInt("Mode", this.getFlightMode());

        tag.putDouble("OrbitAngle", this.orbitAngle);
        tag.putDouble("OrbitRadius", this.orbitRadius);
        tag.putDouble("OrbitSpeed", this.orbitSpeed);
        tag.putDouble("OrbitVerticalPhase", this.orbitVerticalPhase);

        tag.putInt("OrbitDirection", this.orbitDirection);
        tag.putInt("LaunchDelay", this.launchDelay);
        tag.putInt("FlightTicks", this.flightTicks);
        tag.putInt("ReflectionGrace", this.reflectionGraceTicks);
    }


    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.ownerType = tag.contains(OWNER_TYPE_TAG) ? tag.getInt(OWNER_TYPE_TAG) : OWNER_AZUMAAL;
        this.ownerId = tag.hasUUID("Owner")
                ? tag.getUUID("Owner")
                : null;
        this.targetId = tag.hasUUID("Target")
                ? tag.getUUID("Target")
                : null;
        this.reflectorId = tag.hasUUID("Reflector")
                ? tag.getUUID("Reflector")
                : null;

        this.setFlightMode(tag.getInt("Mode"));

        this.orbitAngle = tag.getDouble("OrbitAngle");
        this.orbitRadius = tag.getDouble("OrbitRadius");
        this.orbitSpeed = tag.getDouble("OrbitSpeed");
        this.orbitVerticalPhase = tag.getDouble("OrbitVerticalPhase");

        this.orbitDirection = tag.getInt("OrbitDirection");
        if (this.orbitDirection == 0) {
            this.orbitDirection = 1;
        }
        this.launchDelay = tag.getInt("LaunchDelay");
        this.flightTicks = tag.getInt("FlightTicks");
        this.reflectionGraceTicks = tag.getInt("ReflectionGrace");
        this.setNoGravity(true);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> state.setAndContinue(IDLE_ANIMATION)));
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/" + "eyelid_emissive.png");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}