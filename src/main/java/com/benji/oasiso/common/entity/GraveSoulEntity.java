package com.benji.oasiso.common.entity;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.gravesoul.GraveSoulScenes;
import com.benji.oasiso.registry.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

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
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class GraveSoulEntity extends Monster implements GeoEntity, GlowmaskEntity {
    private static final EntityDataAccessor<Boolean> GRABBING = SynchedEntityData.defineId(GraveSoulEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> POSE_ENDED = SynchedEntityData.defineId(GraveSoulEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SPAWNING = SynchedEntityData.defineId(GraveSoulEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> CAPTURE_AGE = SynchedEntityData.defineId(GraveSoulEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> CAPTURE_PLAYER = SynchedEntityData.defineId(GraveSoulEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public boolean isCapturing() {
        return entityData.get(CAPTURE_AGE) >= 0;
    }

    public boolean isDissolving() {
        return isCapturing() || isDeadOrDying();
    }

    public UUID getCapturePlayer() {
        return entityData.get(CAPTURE_PLAYER).orElse(null);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack empty = player.getItemInHand(hand);
        if (!empty.is(Oasiso.BOMBUL_BOTTLE_EMPTY.get())) return super.mobInteract(player, hand);
        if (!isAlive() || isCapturing() || isGrabbing() || player.isSpectator() || !player.isAlive())
            return InteractionResult.FAIL;
        if (level().isClientSide) return InteractionResult.SUCCESS;

        ItemStack filled = new ItemStack(ModItems.SOUL_BOTTLE.get());
        entityData.set(CAPTURE_PLAYER, Optional.of(player.getUUID()));
        entityData.set(CAPTURE_AGE, 0);
        spawnTicksRemaining = 0;
        entityData.set(SPAWNING, false);
        setTarget(null);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        setJumping(false);
        playSound(ModSounds.SOUL_DEATH.get(), getSoundVolume(), getVoicePitch());

        empty.shrink(1);
        if (empty.isEmpty()) player.setItemInHand(hand, filled);
        else if (!player.getInventory().add(filled)) player.drop(filled, false);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !isCapturing() && super.hurt(source, amount);
    }

    public static final int SPAWN_TICKS = 42;
    private static final int MOVE_TRANSITION_TICKS = 6;
    private static final RawAnimation SPAWN = RawAnimation.begin().thenPlayAndHold("spawn");
    private int spawnTicksRemaining = SPAWN_TICKS;

    public boolean isSpawning() {
        return entityData.get(SPAWNING);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("GraveSoulSpawnTicks", spawnTicksRemaining);
        tag.putInt("GraveSoulCaptureAge", entityData.get(CAPTURE_AGE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        spawnTicksRemaining = Math.max(0, Math.min(SPAWN_TICKS, tag.getInt("GraveSoulSpawnTicks")));
        entityData.set(SPAWNING, spawnTicksRemaining > 0);
        if (tag.contains("GraveSoulCaptureAge") && tag.getInt("GraveSoulCaptureAge") >= 0) {
            entityData.set(CAPTURE_AGE, DISSOLVE_TICKS - 1);
            spawnTicksRemaining = 0;
            entityData.set(SPAWNING, false);
        }
    }

    public void stopAttackPose() {
        entityData.set(POSE_ENDED, true);
    }

    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlayAndHold("attack");

    public boolean isGrabbing() {
        return entityData.get(GRABBING);
    }

    public void setGrabbing(boolean value) {
        entityData.set(POSE_ENDED, false);
        entityData.set(GRABBING, value);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(GRABBING, false);
        entityData.define(POSE_ENDED, false);
        entityData.define(SPAWNING, true);
        entityData.define(CAPTURE_AGE, -1);
        entityData.define(CAPTURE_PLAYER, Optional.empty());
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return !isSpawning() && !isGrabbing() && !isDissolving() && super.isPushable();
    }

    @Override
    public void travel(Vec3 input) {
        if (isSpawning() || isGrabbing() || isDissolving()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        super.travel(input);
    }

    public static final int DISSOLVE_TICKS = 40;
    private int dissolveAge;
    private int captureVisualAge;

    public int getDissolveAge() {
        if (!isCapturing()) return dissolveAge;
        return level().isClientSide ? Math.max(captureVisualAge, entityData.get(CAPTURE_AGE)) : entityData.get(CAPTURE_AGE);
    }

    @Override
    protected void tickDeath() {
        dissolveAge++;
        if (dissolveAge >= DISSOLVE_TICKS) {
            deathTime = 19;
            super.tickDeath();
        } else deathTime = Math.min(19, dissolveAge);
    }

    @Override
    public void handleEntityEvent(byte event) {
        if (event == 60) return;
        super.handleEntityEvent(event);
    }

    @Override
    public void tick() {
        if (isSpawning() || isCapturing()) {
            getNavigation().stop();
            setDeltaMovement(Vec3.ZERO);
            setJumping(false);
        }
        super.tick();
        if (level().isClientSide && isCapturing()) {
            captureVisualAge = Math.max(captureVisualAge + 1, entityData.get(CAPTURE_AGE));
        }
        if (!level().isClientSide && isCapturing()) {
            int age = entityData.get(CAPTURE_AGE) + 1;
            if (age >= DISSOLVE_TICKS) {
                discard();
                return;
            }
            entityData.set(CAPTURE_AGE, age);
        }
        if (!level().isClientSide && isSpawning()) {
            if (spawnTicksRemaining > 0) spawnTicksRemaining--;
            if (spawnTicksRemaining == 0) entityData.set(SPAWNING, false);
        }
        if (level().isClientSide && isAlive() && !isCapturing() && random.nextInt(35) == 0) {
            level().addParticle(Oasiso.ENT_SOUL.get(), getX(), getY() + getBbHeight() * (.2 + random.nextDouble() * .7), getZ(), 0, .025, 0);
        }
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GraveSoulEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 10.0D).add(Attributes.MOVEMENT_SPEED, 0.23D).add(Attributes.KNOCKBACK_RESISTANCE, 0.2D).add(Attributes.ATTACK_DAMAGE, 0.0D).add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new Goal() {
            private ServerPlayer victim;

            {
                setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
            }

            @Override
            public boolean canUse() {
                if (isSpawning() || isGrabbing() || isDissolving()) return false;
                victim = level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(30), p -> GraveSoulScenes.eligible(p) && distanceToSqr(p) <= 900 && hasLineOfSight(p)).stream().min(java.util.Comparator.comparingDouble(GraveSoulEntity.this::distanceToSqr)).orElse(null);
                return victim != null;
            }

            @Override
            public boolean canContinueToUse() {
                return !isSpawning() && !isGrabbing() && !isDissolving() && victim != null && GraveSoulScenes.eligible(victim) && distanceToSqr(victim) < 1024;
            }

            @Override
            public boolean requiresUpdateEveryTick() {
                return true;
            }

            @Override
            public void tick() {
                getLookControl().setLookAt(victim, 30, 30);
                getNavigation().moveTo(victim, 1.0);
                double reach = (getBbWidth() + victim.getBbWidth()) * .5 + .8;
                if (onGround() && victim.onGround() && distanceToSqr(victim) <= reach * reach && hasLineOfSight(victim))
                    GraveSoulScenes.begin(GraveSoulEntity.this, victim);
            }

            @Override
            public void stop() {
                victim = null;
                getNavigation().stop();
            }
        });
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, .8) {
            @Override
            public boolean canUse() {
                return !isSpawning() && !isGrabbing() && !isDissolving() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !isSpawning() && !isGrabbing() && !isDissolving() && super.canContinueToUse();
            }
        });
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (isCapturing()) return null;

        SoundEvent[] sounds = {ModSounds.SOUL_IDLE1.get(), ModSounds.SOUL_IDLE2.get(), ModSounds.SOUL_IDLE3.get()};

        return sounds[this.random.nextInt(sounds.length)];
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return ModSounds.SOUL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SOUL_DEATH.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", MOVE_TRANSITION_TICKS, event -> {
            event.getController().transitionLength(isSpawning() || isGrabbing() || isDissolving() ? 0 : MOVE_TRANSITION_TICKS);
            if (isDissolving()) return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
            if (isSpawning()) return event.setAndContinue(SPAWN);
            if (isGrabbing())
                return event.setAndContinue(entityData.get(POSE_ENDED) ? RawAnimation.begin().thenLoop("idle") : ATTACK);
            if (event.isMoving()) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }
            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/grave_soul.png");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
