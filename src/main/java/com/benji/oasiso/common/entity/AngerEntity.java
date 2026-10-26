package com.benji.oasiso.common.entity;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.sounds.SoundSource;

public class AngerEntity extends PathfinderMob implements GeoEntity, GlowmaskEntity {
    public static final int JUMP_TICK = 5, SHOCK_TICK = 20, ATTACK_TICKS = 30, COOLDOWN = 100;
    public static final double RANGE = 15, SHOCK_RADIUS = 6, LAUNCH_SPEED = .8;
    public static final float DAMAGE = 6;
    private static final EntityDataAccessor<CompoundTag> SHOCK = SynchedEntityData.defineId(AngerEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(AngerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int life, attackAge = -1, cooldown = 20, arcStep;
    private boolean vanished, arc;
    private Vec3 arcStart = Vec3.ZERO, arcEnd = Vec3.ZERO;
    private double arcHeight;

    public AngerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        life = 600 + random.nextInt(601);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 80).add(Attributes.MOVEMENT_SPEED, 0).add(Attributes.KNOCKBACK_RESISTANCE, .20).add(Attributes.ATTACK_DAMAGE, DAMAGE).add(Attributes.FOLLOW_RANGE, RANGE);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ATTACKING, false);
        entityData.define(SHOCK, new CompoundTag());
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void travel(Vec3 input) {
        if (level().isClientSide && entityData.get(ATTACKING)) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (arc && !level().isClientSide) {
            double t = Math.min(1, ++arcStep / (double) (SHOCK_TICK - JUMP_TICK));
            Vec3 desired = arcStart.lerp(arcEnd, t).add(0, 4 * arcHeight * t * (1 - t), 0);
            move(MoverType.SELF, desired.subtract(position()));
            setDeltaMovement(Vec3.ZERO);
            fallDistance = 0;
            if (t >= 1 || horizontalCollision || verticalCollision && t < .5) arc = false;
            return;
        }
        setDeltaMovement(0, getDeltaMovement().y, 0);
        super.travel(Vec3.ZERO);
        setDeltaMovement(0, getDeltaMovement().y, 0);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
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
        if (attackAge >= 0) {
            ++attackAge;
            if (attackAge == JUMP_TICK) jumpToTarget();
            if (attackAge == SHOCK_TICK) shock();
            if (attackAge >= ATTACK_TICKS) {
                attackAge = -1;
                arc = false;
                cooldown = COOLDOWN;
                entityData.set(ATTACKING, false);
            }
            return;
        }
        if (cooldown > 0) {
            --cooldown;
            return;
        }
        if (!onGround()) return;
        LivingEntity enemy = HappinessEntity.nearest(level(), getEyePosition(), this, RANGE);
        if (enemy == null) {
            cooldown = 10;
            return;
        }
        setTarget(enemy);
        double dx = enemy.getX() - getX(), dz = enemy.getZ() - getZ();
        setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        attackAge = 0;
        entityData.set(ATTACKING, true);
    }

    private void jumpToTarget() {
        LivingEntity enemy = getTarget();
        if (enemy == null || !HappinessEntity.isEnemy(enemy, this)) return;

        level().playSound(null, getX(), getY(), getZ(), ModSounds.ANGER_ATTACK.get(), SoundSource.HOSTILE, 1.0F, 1.0F);

        arcStart = position();
        Vec3 difference = enemy.position().subtract(arcStart);
        if (difference.length() > RANGE) difference = difference.normalize().scale(RANGE);
        Vec3 end = arcStart.add(difference);
        Vec3 back = new Vec3(arcStart.x - end.x, 0, arcStart.z - end.z);
        if (back.lengthSqr() > 1) end = end.add(back.normalize().scale(.8));
        BlockHitResult floor = level().clip(new ClipContext(end.add(0, 1.5, 0), end.add(0, -4, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (floor.getType() != HitResult.Type.MISS) end = floor.getLocation().add(0, .01, 0);
        else end = new Vec3(end.x, arcStart.y, end.z);
        arcEnd = end;
        arcHeight = Math.min(3.5, 1.1 + arcStart.distanceTo(arcEnd) * .16);
        arcStep = 0;
        arc = true;
        setDeltaMovement(Vec3.ZERO);
    }

    private void shock() {
        arc = false;
        setDeltaMovement(Vec3.ZERO);

        level().playSound(null, getX(), getY(), getZ(), ModSounds.ANGER_SHOCK.get(), SoundSource.HOSTILE, 1.0F, 1.0F);

        ServerLevel server = (ServerLevel) level();
        CompoundTag wave = new CompoundTag();
        wave.putLong("Time", level().getGameTime());
        wave.putDouble("X", getX());
        wave.putDouble("Y", getY());
        wave.putDouble("Z", getZ());
        entityData.set(SHOCK, wave);
        burst(server, 36, .45);
        for (Mob mob : server.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(SHOCK_RADIUS), e -> e != this && e.isAlive() && !e.isSpectator())) {
            if (distanceToSqr(mob) > SHOCK_RADIUS * SHOCK_RADIUS) continue;
            if (HappinessEntity.isEnemy(mob, this))
                mob.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
            double push = Math.max(0, LAUNCH_SPEED - mob.getDeltaMovement().y);
            if (push > 0) {
                mob.push(0, push, 0);
                mob.hasImpulse = true;
            }
        }
    }

    public long getClientShockTime() {
        CompoundTag tag = entityData.get(SHOCK);
        return tag.contains("Time") ? tag.getLong("Time") : Long.MIN_VALUE;
    }

    public Vec3 getClientShockOrigin() {
        CompoundTag tag = entityData.get(SHOCK);
        return new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
    }

    private void burst(ServerLevel server, int count, double spread) {
        server.sendParticles(Oasiso.ANGER_PART.get(), getX(), getY() + getBbHeight() * .5, getZ(), count, spread, spread, spread, .08);
    }

    private void vanish() {
        if (!vanished && level() instanceof ServerLevel server) {
            vanished = true;
            level().playSound(null, getX(), getY(), getZ(), ModSounds.SOUL_DIE.get(), net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.0F);
            burst(server, 36, .4);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide) {
            vanish();
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("AngerLife", life);
        tag.putInt("AngerCooldown", cooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("AngerLife")) life = Math.max(1, tag.getInt("AngerLife"));
        cooldown = Math.max(20, tag.getInt("AngerCooldown"));
        attackAge = -1;
        arc = false;
        entityData.set(ATTACKING, false);
    }

    @Override
    protected SoundEvent getAmbientSound() {

        SoundEvent[] sounds = {ModSounds.ANGER_IDLE1.get(), ModSounds.ANGER_IDLE2.get(), ModSounds.ANGER_IDLE3.get()};

        return sounds[this.random.nextInt(sounds.length)];
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return ModSounds.ANGER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SOUL_DIE.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> event.setAndContinue(entityData.get(ATTACKING) ? ATTACK : IDLE)).setCustomInstructionKeyframeHandler(event -> {
        }));
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/anger.png");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
