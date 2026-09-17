package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Explosion;

import java.util.UUID;

public class OsirisSplitEntity extends Monster implements GeoEntity, GlowmaskEntity {

    private static final float EXPLOSION_POWER = 2.5F;
    private static final double GRAVITY = 0.045D;
    private static final double HORIZONTAL_SPEED = 0.70D;
    private static final int MAX_LIFETIME = 120;

    private static final RawAnimation SPIN_ANIMATION = RawAnimation.begin().thenLoop("spin");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private UUID azumaalOwnerId;

    private boolean exploded;


    public OsirisSplitEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);

        this.setNoGravity(true);
        this.noPhysics = true;
    }


    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1.0D).add(Attributes.MOVEMENT_SPEED, 0.0D).add(Attributes.KNOCKBACK_RESISTANCE, 1.0D).add(Attributes.ATTACK_DAMAGE, 0.0D).add(Attributes.FOLLOW_RANGE, 1.0D);
    }


    @Override
    protected void registerGoals() {
    }


    public void setAzumaalOwner(AzumaalEntity boss) {
        this.azumaalOwnerId = boss != null ? boss.getUUID() : null;
    }


    public AzumaalEntity getAzumaalOwner(ServerLevel level) {
        if (this.azumaalOwnerId == null) {
            return null;
        }

        Entity entity = level.getEntity(this.azumaalOwnerId);
        return entity instanceof AzumaalEntity azumaal ? azumaal : null;
    }

    public void launchTowards(Vec3 target) {
        Vec3 origin = this.position();

        double dx = target.x - origin.x;
        double dy = target.y - origin.y;
        double dz = target.z - origin.z;

        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double flightTicks = Mth.clamp(horizontalDistance / HORIZONTAL_SPEED, 14.0D, 38.0D);

        double velocityX = dx / flightTicks;
        double velocityZ = dz / flightTicks;
        double velocityY = (dy + GRAVITY * flightTicks * (flightTicks - 1.0D) * 0.5D) / flightTicks;

        this.setDeltaMovement(velocityX, velocityY, velocityZ);

        this.hasImpulse = true;

        updateRotationFromMotion();
    }


    @Override
    public void tick() {
        super.tick();

        if (this.exploded) {
            return;
        }

        this.setNoGravity(true);

        Vec3 movement = this.getDeltaMovement();

        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {

            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canImpactEntity);

            if (hit.getType() != HitResult.Type.MISS) {
                Vec3 hitPosition = hit.getLocation();
                this.setPos(hitPosition.x, hitPosition.y, hitPosition.z);
                explode(serverLevel);
                return;
            }
        }

        this.move(MoverType.SELF, movement);
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            spawnTrail(serverLevel);
        }
        this.setDeltaMovement(movement.x, movement.y - GRAVITY, movement.z);
        updateRotationFromMotion();

        if (!this.level().isClientSide && this.tickCount >= MAX_LIFETIME && this.level() instanceof ServerLevel serverLevel) {
            explode(serverLevel);
        }
    }


    private boolean canImpactEntity(Entity entity) {
        if (entity == this || !entity.isAlive() || entity.isSpectator() || !entity.isPickable()) {

            return false;
        }

        if (entity instanceof OsirisSplitEntity || entity instanceof OsirisTentacleEntity) {

            return false;
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            AzumaalEntity owner = getAzumaalOwner(serverLevel);
            if (entity == owner) {
                return false;
            }
        }

        return true;
    }


    private void spawnTrail(ServerLevel level) {
        level.sendParticles(Oasiso.MELTED_SPLASH.get(),
                this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(), 7, 0.16D, 0.16D, 0.16D, 0.045D);
    }


    private void explode(ServerLevel level) {
        if (this.exploded) {
            return;
        }

        this.exploded = true;

        Vec3 position = this.position();
        spawnImpactParticles(level, position);

        float impactPitch = 0.50F + level.random.nextFloat() * 0.05F;
        level.playSound(null, position.x, position.y, position.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.15F, impactPitch);

        AzumaalEntity owner = getAzumaalOwner(level);
        Entity explosionSource = owner != null ? owner : this;
        Explosion explosion = new Explosion(level, explosionSource, null, null, position.x, position.y, position.z, EXPLOSION_POWER, false, Explosion.BlockInteraction.KEEP);

        explosion.explode();

        for (var playerEntry : explosion.getHitPlayers().entrySet()) {
            if (!(playerEntry.getKey() instanceof ServerPlayer player)) {
                continue;
            }

            player.hurtMarked = true;
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }

        this.discard();
    }


    public static void spawnLaunchParticles(ServerLevel level, Vec3 position) {
        level.sendParticles(Oasiso.CHAOS_BOMB_CENTER_SMOKE.get(), position.x, position.y, position.z, 4, 0.18D, 0.18D, 0.18D, 0.025D);
        level.sendParticles(Oasiso.CHAOS_BOMB_FIRE_SMOKE.get(), position.x, position.y, position.z, 16, 0.42D, 0.42D, 0.42D, 0.075D);
        level.sendParticles(Oasiso.CHAOS_BOMB_SPARKS.get(), position.x, position.y, position.z, 32, 0.40D, 0.40D, 0.40D, 0.16D);
    }


    private void spawnImpactParticles(ServerLevel level, Vec3 position) {
        level.sendParticles(Oasiso.CHAOS_BOMB_CENTER_SMOKE.get(), position.x, position.y, position.z, 10, 0.45D, 0.60D, 0.45D, 0.04D);
        level.sendParticles(Oasiso.CHAOS_BOMB_FIRE_SMOKE.get(), position.x, position.y, position.z, 38, 1.25D, 1.35D, 1.25D, 0.13D);
        level.sendParticles(Oasiso.CHAOS_BOMB_SPARKS.get(), position.x, position.y, position.z, 80, 1.10D, 1.25D, 1.10D, 0.27D);
    }


    private void updateRotationFromMotion() {
        Vec3 movement = this.getDeltaMovement();

        if (movement.lengthSqr() < 1.0E-7D) {

            return;
        }

        double horizontal = Math.sqrt(movement.x * movement.x + movement.z * movement.z);

        float yaw = (float) (Mth.atan2(movement.z, movement.x) * 180.0D / Math.PI) - 90.0F;

        float pitch = (float) (-Mth.atan2(movement.y, horizontal) * 180.0D / Math.PI);

        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.yBodyRot = yaw;

        this.setXRot(pitch);
    }


    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public void knockback(double strength, double x, double z) {
    }


    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        if (this.azumaalOwnerId != null) {
            tag.putUUID("AzumaalOwner", this.azumaalOwnerId);
        }
    }


    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        this.azumaalOwnerId = tag.hasUUID("AzumaalOwner") ? tag.getUUID("AzumaalOwner") : null;
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> state.setAndContinue(SPIN_ANIMATION)));
    }


    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/osiris_split.png");
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}