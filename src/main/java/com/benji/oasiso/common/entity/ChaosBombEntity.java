package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import com.benji.oasiso.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ChaosBombEntity extends Monster implements GeoEntity, GlowmaskEntity {

    private boolean spawnSoundPlayed;

    public static final int STATE_AIR = 0;
    public static final int STATE_BOUNCE = 1;
    public static final int STATE_GROUND = 2;

    private static final String AZUMAAL_OWNER_TAG = "AzumaalOwner";

    private UUID azumaalOwnerId;


    private static final EntityDataAccessor<Integer> ANIM_STATE = SynchedEntityData.defineId(ChaosBombEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation BOUNCE_ANIMATION = RawAnimation.begin().thenPlay("bounce");
    private static final RawAnimation NONE_ANIMATION = RawAnimation.begin().thenLoop("none");

    private static final int BOUNCE_ANIMATION_TICKS = 10;
    private static final double BOUNCE_VELOCITY = 0.58D;
    private static final double BOUNCE_HORIZONTAL_DAMPING = 0.72D;


    private static final int MIN_EXPLOSION_TIME = 80;
    private static final int MAX_EXPLOSION_TIME = 160;

    private static final float EXPLOSION_POWER = 4.0F;
    private static final byte EXPLOSION_VISUAL_EVENT = 67;

    private static final String EXPLOSION_TICKS_TAG = "ChaosBombExplosionTicks";
    private static final String HAS_BOUNCED_TAG = "ChaosBombHasBounced";
    private static final String BOUNCE_TICKS_TAG = "ChaosBombBounceAnimTicks";
    private static final String ANIM_STATE_TAG = "ChaosBombAnimState";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int explosionTicks = -1;
    private int bounceAnimationTicks;

    private boolean hasBounced;
    private boolean wasOnGround;

    public ChaosBombEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (this.level().isClientSide || this.spawnSoundPlayed) {
            return;
        }
        this.spawnSoundPlayed = true;
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.BOMB_SPAWN.get(), SoundSource.HOSTILE, 0.65F, 0.95F + this.random.nextFloat() * 0.10F);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIM_STATE, STATE_AIR);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        this.fallDistance = 0.0F;
        if (this.level().isClientSide) {
            return;
        }
        if (this.explosionTicks < 0) {
            this.explosionTicks = randomBetween(MIN_EXPLOSION_TIME, MAX_EXPLOSION_TIME);
        }
        tickBounce();
        this.explosionTicks--;
        if (this.explosionTicks <= 0 && this.level() instanceof ServerLevel serverLevel) {
            explode(serverLevel);
        }
    }

    public void setAzumaalOwner(AzumaalEntity owner) {
        this.azumaalOwnerId = owner.getUUID();
    }

    @Nullable
    public AzumaalEntity getAzumaalOwner(ServerLevel level) {
        if (this.azumaalOwnerId == null) {
            return null;
        }
        Entity entity = level.getEntity(this.azumaalOwnerId);
        if (entity instanceof AzumaalEntity boss) {
            return boss;
        }
        return null;
    }

    private void tickBounce() {
        boolean grounded = this.onGround();

        if (!this.hasBounced && grounded && !this.wasOnGround) {
            this.hasBounced = true;
            this.bounceAnimationTicks = BOUNCE_ANIMATION_TICKS;
            this.setAnimState(STATE_BOUNCE);

            Vec3 movement = this.getDeltaMovement();
            this.setDeltaMovement(movement.x * BOUNCE_HORIZONTAL_DAMPING, BOUNCE_VELOCITY, movement.z * BOUNCE_HORIZONTAL_DAMPING);
            this.setOnGround(false);
            this.wasOnGround = false;
            return;
        }
        if (this.bounceAnimationTicks > 0) {
            this.bounceAnimationTicks--;
            if (this.bounceAnimationTicks <= 0) {
                this.setAnimState(this.onGround() ? STATE_GROUND : STATE_AIR);
            }
            this.wasOnGround = grounded;
            return;
        }
        this.setAnimState(grounded ? STATE_GROUND : STATE_AIR);
        this.wasOnGround = grounded;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        return false;
    }

    private void explode(ServerLevel level) {
        double explosionX = this.getX();
        double explosionY = this.getY() + 0.35D;
        double explosionZ = this.getZ();

        level.broadcastEntityEvent(this, EXPLOSION_VISUAL_EVENT);
        level.playSound(null, explosionX, explosionY, explosionZ, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 4.0F, 0.88F + this.random.nextFloat() * 0.16F);
        level.explode(this, null, null, explosionX, explosionY, explosionZ, EXPLOSION_POWER, false, Level.ExplosionInteraction.NONE, false);

        this.discard();
    }

    @Override
    public void handleEntityEvent(byte eventId) {
        if (eventId == EXPLOSION_VISUAL_EVENT) {
            spawnExplosionParticles();
            return;
        }
        super.handleEntityEvent(eventId);
    }

    private void spawnExplosionParticles() {
        double centerX = this.getX();
        double centerY = this.getY() + 0.35D;
        double centerZ = this.getZ();

        for (int i = 0; i < 18; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double horizontalSpeed = this.random.nextDouble() * 0.10D;

            double velocityX = Math.cos(angle) * horizontalSpeed;
            double velocityY = 0.035D + this.random.nextDouble() * 0.10D;
            double velocityZ = Math.sin(angle) * horizontalSpeed;

            this.level().addParticle(Oasiso.CHAOS_BOMB_CENTER_SMOKE.get(), centerX + (this.random.nextDouble() - 0.5D) * 0.18D, centerY + (this.random.nextDouble() - 0.5D) * 0.15D, centerZ + (this.random.nextDouble() - 0.5D) * 0.18D, velocityX, velocityY, velocityZ);
        }
        for (int i = 0; i < 30; i++) {
            Vec3 direction = randomExplosionDirection();
            double speed = 0.10D + this.random.nextDouble() * 0.22D;
            this.level().addParticle(Oasiso.CHAOS_BOMB_FIRE_SMOKE.get(), centerX, centerY, centerZ, direction.x * speed, direction.y * speed + 0.035D, direction.z * speed);
        }
        for (int i = 0; i < 58; i++) {

            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double horizontalSpeed = 0.12D + this.random.nextDouble() * 0.48D;

            double velocityX = Math.cos(angle) * horizontalSpeed;
            double velocityZ = Math.sin(angle) * horizontalSpeed;
            double velocityY = 0.16D + this.random.nextDouble() * 0.48D;

            this.level().addParticle(Oasiso.CHAOS_BOMB_SPARKS.get(), centerX + (this.random.nextDouble() - 0.5D) * 0.12D, centerY + (this.random.nextDouble() - 0.5D) * 0.12D, centerZ + (this.random.nextDouble() - 0.5D) * 0.12D, velocityX, velocityY, velocityZ);
        }
    }


    private Vec3 randomExplosionDirection() {
        Vec3 direction = new Vec3(this.random.nextDouble() - 0.5D,
                this.random.nextDouble() - 0.35D,
                this.random.nextDouble() - 0.5D);
        if (direction.lengthSqr() < 0.0001D) {
            return new Vec3(0.0D, 1.0D, 0.0D);
        }
        return direction.normalize();
    }

    private int randomBetween(int minimum, int maximum) {
        return minimum + this.random.nextInt(maximum - minimum + 1);
    }

    public int getAnimState() {
        return this.entityData.get(ANIM_STATE);
    }

    private void setAnimState(int state) {
        if (this.getAnimState() == state) {
            return;
        }
        this.entityData.set(ANIM_STATE, state);
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            return switch (this.getAnimState()) {
                case STATE_BOUNCE -> state.setAndContinue(BOUNCE_ANIMATION);
                case STATE_GROUND -> state.setAndContinue(NONE_ANIMATION);
                default -> state.setAndContinue(IDLE_ANIMATION);
            };
        }));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.azumaalOwnerId != null) {
            tag.putUUID(AZUMAAL_OWNER_TAG, this.azumaalOwnerId);
        }

        tag.putInt(EXPLOSION_TICKS_TAG, this.explosionTicks);

        tag.putBoolean("BombSpawnSoundPlayed", this.spawnSoundPlayed);
        tag.putBoolean(HAS_BOUNCED_TAG, this.hasBounced);

        tag.putInt(BOUNCE_TICKS_TAG, this.bounceAnimationTicks);
        tag.putInt(ANIM_STATE_TAG, this.getAnimState());
    }


    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        this.azumaalOwnerId = tag.hasUUID(AZUMAAL_OWNER_TAG) ? tag.getUUID(AZUMAAL_OWNER_TAG) : null;
        this.explosionTicks = tag.contains(EXPLOSION_TICKS_TAG) ? tag.getInt(EXPLOSION_TICKS_TAG) : -1;
        this.spawnSoundPlayed = tag.getBoolean("BombSpawnSoundPlayed");
        this.hasBounced = tag.getBoolean(HAS_BOUNCED_TAG);
        this.bounceAnimationTicks = tag.getInt(BOUNCE_TICKS_TAG);

        this.setAnimState(tag.contains(ANIM_STATE_TAG) ? tag.getInt(ANIM_STATE_TAG) : STATE_AIR);
    }


    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/" + "chaos_bomb_emissive.png");
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}