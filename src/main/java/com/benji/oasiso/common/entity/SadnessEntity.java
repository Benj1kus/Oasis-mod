package com.benji.oasiso.common.entity;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.effect.SadnessStun;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SadnessEntity extends PathfinderMob implements GeoEntity, GlowmaskEntity {
    public static final int CAST_TICKS = 35, RELEASE_TICK = 20, COOLDOWN = 200;
    public static final double RANGE = 15;
    private static final EntityDataAccessor<Boolean> CASTING = SynchedEntityData.defineId(SadnessEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation CAST = RawAnimation.begin().thenPlay("cast");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int life = 600, castAge = -1, cooldown = 20;
    private boolean vanished;

    public SadnessEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        life = 600 + random.nextInt(601);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.MOVEMENT_SPEED, 0).add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.ATTACK_DAMAGE, 0).add(Attributes.FOLLOW_RANGE, RANGE);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CASTING, false);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void travel(Vec3 input) {
        setDeltaMovement(0, getDeltaMovement().y, 0);
        super.travel(Vec3.ZERO);
        setDeltaMovement(0, getDeltaMovement().y, 0);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !isAlive()) return;
        getNavigation().stop();
        if (--life <= 0) {
            vanish();
            discard();
            return;
        }
        if (SadnessStun.stunned(this)) {
            castAge = -1;
            entityData.set(CASTING, false);
            return;
        }
        if (cooldown > 0) cooldown--;
        if (castAge >= 0) {
            if (++castAge == RELEASE_TICK) pulse();
            if (castAge >= CAST_TICKS) {
                castAge = -1;
                entityData.set(CASTING, false);
            }
            return;
        }
        if (cooldown > 0) return;
        LivingEntity enemy = level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(RANGE), e -> e.getMaxHealth() < 200 && distanceToSqr(e) <= RANGE * RANGE && HappinessEntity.isEnemy(e, this)).stream().findFirst().orElse(null);
        if (enemy == null) {
            cooldown = 10;
            return;
        }
        setTarget(enemy);
        double dx = enemy.getX() - getX(), dz = enemy.getZ() - getZ();
        setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        castAge = 0;
        cooldown = COOLDOWN;
        entityData.set(CASTING, true);
        level().playSound(null, getX(), getY(), getZ(), com.benji.oasiso.ModSounds.SAD_CAST.get(), net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    private void pulse() {
        for (Mob mob : level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(RANGE), e -> e != this && e.isAlive() && !e.isSpectator() && e.getMaxHealth() < 200)) {
            if (distanceToSqr(mob) <= RANGE * RANGE) SadnessStun.apply(mob);
            level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), com.benji.oasiso.ModSounds.SAD_STUN.get(), net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.8F + random.nextFloat() * 0.4F);
        }
        flare((ServerLevel) level(), position().add(0, getBbHeight() * .5, 0), 18, .35);
    }

    private static void flare(ServerLevel level, Vec3 pos, int amount, double spread) {
        level.sendParticles(Oasiso.SAD_BOP.get(), pos.x, pos.y, pos.z, amount, spread, spread, spread, .035);
    }

    private void vanish() {
        if (!vanished && level() instanceof ServerLevel server) {
            vanished = true;
            level().playSound(null, getX(), getY(), getZ(), ModSounds.SOUL_DIE.get(), net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.0F);
            flare(server, position().add(0, getBbHeight() * .5, 0), 32, .35);
        }
    }

    @Override
    public void die(DamageSource damage) {
        super.die(damage);
        if (!level().isClientSide) {
            vanish();
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SadnessLife", life);
        tag.putInt("SadnessCooldown", cooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SadnessLife")) life = Math.max(1, tag.getInt("SadnessLife"));
        cooldown = Math.max(20, tag.getInt("SadnessCooldown"));
        castAge = -1;
        entityData.set(CASTING, false);
    }

    @Override
    protected SoundEvent getAmbientSound() {

        SoundEvent[] sounds = {ModSounds.SAD_IDLE1.get(), ModSounds.SAD_IDLE2.get(), ModSounds.SAD_IDLE3.get()};

        return sounds[this.random.nextInt(sounds.length)];
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return ModSounds.SAD_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SOUL_DIE.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> event.setAndContinue(entityData.get(CASTING) ? CAST : IDLE)));
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/sadness.png");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
