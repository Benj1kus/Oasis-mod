package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SpearAttackEntity extends Projectile implements GeoEntity, GlowmaskEntity {
    public static final int DISAP_TICKS = 21;
    public static final int MAX_FLIGHT_TICKS = 120;
    public static final double GRAVITY = .085, MAX_SPEED = 1.5;
    private static final EntityDataAccessor<Boolean> LANDED = SynchedEntityData.defineId(SpearAttackEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation DISAP = RawAnimation.begin().thenPlayAndHold("disap");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int flightAge, landedAge;
    private float damage = 20;
    private boolean spent;

    public SpearAttackEntity(EntityType<? extends SpearAttackEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public void prepare(ApollyonEntity owner, float damage) {
        setOwner(owner);
        this.damage = Math.max(0, damage);
        setDeltaMovement(0, -.12, 0);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(LANDED, false);
    }

    public boolean isLanded() {
        return entityData.get(LANDED);
    }

    @Override
    public void tick() {
        super.tick();
        if (isLanded()) {
            setDeltaMovement(Vec3.ZERO);
            if (!level().isClientSide && ++landedAge >= DISAP_TICKS) discard();
            return;
        }
        if (!level().isClientSide) {
            Entity owner = getOwner();
            if (!(owner instanceof ApollyonEntity boss) || !boss.isAlive() || boss.isRemoved() || ++flightAge > MAX_FLIGHT_TICKS || getY() < level().getMinBuildHeight() - 8) {
                discard();
                return;
            }
        }
        Vec3 motion = new Vec3(0, Math.max(-MAX_SPEED, getDeltaMovement().y - GRAVITY), 0);
        setDeltaMovement(motion);
        Vec3 from = position(), to = from.add(motion);
        if (!level().isClientSide) {
            var block = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            Vec3 end = block.getType() == HitResult.Type.BLOCK ? block.getLocation() : to;
            if (!spent) {
                var hit = ProjectileUtil.getEntityHitResult(level(), this, from, end, getBoundingBox().expandTowards(motion).inflate(.3), this::canDamage);
                if (hit != null) {
                    spent = true;
                    LivingEntity owner = (LivingEntity) getOwner();
                    hit.getEntity().hurt(damageSources().mobProjectile(this, owner), damage);
                }
            }
            if (block.getType() == HitResult.Type.BLOCK) {
                setPos(end.x, end.y + .015, end.z);
                setDeltaMovement(Vec3.ZERO);
                entityData.set(LANDED, true);
                landedAge = 0;
                return;
            }
        }
        setPos(to.x, to.y, to.z);
    }

    private boolean canDamage(Entity target) {
        if (!(target instanceof LivingEntity living) || !living.isAlive() || target == getOwner() || target instanceof ApollyonEntity || !target.canBeHitByProjectile())
            return false;
        if (target instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        return getOwner() == null || !getOwner().isAlliedTo(target);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SpearAttackEntity>(this, "controller", 0, event -> event.setAndContinue(isLanded() ? DISAP : IDLE)));
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/spear_attack.png");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("SpearDamage", damage);
        tag.putBoolean("SpearSpent", spent);
        tag.putBoolean("SpearLanded", isLanded());
        tag.putInt("SpearFlightAge", flightAge);
        tag.putInt("SpearLandedAge", landedAge);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SpearDamage")) damage = Math.max(0, tag.getFloat("SpearDamage"));
        spent = tag.getBoolean("SpearSpent");
        entityData.set(LANDED, tag.getBoolean("SpearLanded"));
        flightAge = Math.max(0, tag.getInt("SpearFlightAge"));
        landedAge = Math.max(0, tag.getInt("SpearLandedAge"));
    }
}
