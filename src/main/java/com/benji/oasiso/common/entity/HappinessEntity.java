package com.benji.oasiso.common.entity;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class HappinessEntity extends PathfinderMob implements GeoEntity, GlowmaskEntity {
    public static final int CAST_TICKS = 47, RELEASE_TICK = 36, COOLDOWN = 100;
    public static final double RANGE = 24;
    public static final TagKey<EntityType<?>> IGNORE = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "happiness_ignored"));
    public static final TagKey<EntityType<?>> HOSTILES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "happiness_hostiles"));
    private static final EntityDataAccessor<Boolean> CASTING = SynchedEntityData.defineId(HappinessEntity.class, EntityDataSerializers.BOOLEAN);
    public static final int SPAWN_TICKS = 60;
    private static final EntityDataAccessor<Boolean> SUMMONING = SynchedEntityData.defineId(HappinessEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation SPAWN = RawAnimation.begin().thenPlay("spawn");
    private int summonTicks;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation CAST = RawAnimation.begin().thenPlay("cast");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int life = 600, castAge = -1, cooldown = 20;
    private boolean vanished;

    public HappinessEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        life = 600 + random.nextInt(601);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10).add(Attributes.MOVEMENT_SPEED, 0).add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.ATTACK_DAMAGE, 0).add(Attributes.FOLLOW_RANGE, RANGE);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SUMMONING, false);
        entityData.define(CASTING, false);
    }

    public void startSummoning() {
        summonTicks = SPAWN_TICKS;
        entityData.set(SUMMONING, true);
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

    public static boolean isEnemy(LivingEntity target, Entity source) {
        if (!target.isAlive() || target.isSpectator() || target instanceof Player || target instanceof HappinessEntity || target.getType().is(IGNORE) || target instanceof TamableAnimal pet && pet.isTame() || source != null && (target == source || source.isAlliedTo(target)))
            return false;
        if (target instanceof Mob mob && mob.getTarget() instanceof Player player && player.isAlive() && !player.isCreative() && !player.isSpectator())
            return true;
        if (target.getType().is(HOSTILES)) return true;
        if (target instanceof NeutralMob) return false;
        return target instanceof Enemy || target.getType().getCategory() == MobCategory.MONSTER;
    }

    public static LivingEntity nearest(Level level, Vec3 pos, Entity source, double range) {
        return level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(pos, pos).inflate(range), e -> isEnemy(e, source) && e.distanceToSqr(pos) <= range * range).stream().filter(e -> level.clip(new net.minecraft.world.level.ClipContext(pos, e.getEyePosition(), net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, source)).getType() == net.minecraft.world.phys.HitResult.Type.MISS).min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(pos))).orElse(null);
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
        if (summonTicks > 0) {
            if (--summonTicks == 0) entityData.set(SUMMONING, false);
            return;
        }
        if (castAge >= 0) {
            if (++castAge == RELEASE_TICK) volley();
            if (castAge >= CAST_TICKS) {
                castAge = -1;
                cooldown = COOLDOWN;
                entityData.set(CASTING, false);
            }
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        LivingEntity enemy = nearest(level(), getEyePosition(), this, RANGE);
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
        entityData.set(CASTING, true);
        level().playSound(null, getX(), getY(), getZ(), com.benji.oasiso.ModSounds.HAPPY_CAST.get(), net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    private void volley() {
        int count = 3 + random.nextInt(3);
        double yaw = Math.toRadians(getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        for (int i = 0; i < count; i++) {
            double spread = (i - (count - 1) / 2.0) * .60;
            Vec3 position = position().add(0, getBbHeight() + .65 + .18 * Math.abs(spread), 0).add(forward.scale(-.55)).add(right.scale(spread));
            HappyKnifeEntity knife = new HappyKnifeEntity(ModEntities.HAPPY_KNIFE.get(), level());
            knife.setOwner(this);
            knife.setPos(position);
            knife.setYRot(getYRot());
            level().addFreshEntity(knife);
            flare((ServerLevel) level(), position, 9, .20);
            level().playSound(null, position.x, position.y, position.z, com.benji.oasiso.ModSounds.BOMB_SPAWN.get(), net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.8F + level().random.nextFloat() * 0.4F);
        }
    }

    public static void flare(ServerLevel level, Vec3 pos, int amount, double spread) {
        level.sendParticles(Oasiso.HAPPY_FLARE.get(), pos.x, pos.y, pos.z, amount, spread, spread, spread, .035);
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
        tag.putInt("CoreremSummonTicks", summonTicks);
        tag.putInt("HappyLife", life);
        tag.putInt("HappyCooldown", cooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        summonTicks = Math.max(0, Math.min(SPAWN_TICKS, tag.getInt("CoreremSummonTicks")));
        entityData.set(SUMMONING, summonTicks > 0);
        if (tag.contains("HappyLife")) life = Math.max(1, tag.getInt("HappyLife"));
        cooldown = Math.max(20, tag.getInt("HappyCooldown"));
        castAge = -1;
        entityData.set(CASTING, false);
    }

    @Override
    protected SoundEvent getAmbientSound() {

        SoundEvent[] sounds = {ModSounds.HAPPY_IDLE1.get(), ModSounds.HAPPY_IDLE2.get(), ModSounds.HAPPY_IDLE3.get()};

        return sounds[this.random.nextInt(sounds.length)];
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return ModSounds.HAPPY_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SOUL_DIE.get();
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> event.setAndContinue(entityData.get(SUMMONING) ? SPAWN : entityData.get(CASTING) ? CAST : IDLE)));
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/happiness.png");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
