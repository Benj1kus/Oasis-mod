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
    private static final EntityDataAccessor<Boolean> ARTILLERY = SynchedEntityData.defineId(SpearAttackEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LAUNCHED = SynchedEntityData.defineId(SpearAttackEntity.class, EntityDataSerializers.BOOLEAN);
    private Vec3 formation = Vec3.ZERO, aim = Vec3.ZERO, arcStart = Vec3.ZERO, control1 = Vec3.ZERO, control2 = Vec3.ZERO;
    private java.util.UUID artilleryTarget;
    private int artilleryAge, launchDelay;
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
        entityData.define(ARTILLERY, false);
        entityData.define(LAUNCHED, false);
    }

    public boolean isLanded() {
        return entityData.get(LANDED);
    }

    @Override
    public void tick() {
        super.tick();
        if (isArtillery()) {
            tickArtillery();
            return;
        }
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

    public boolean isArtillery() {
        return entityData.get(ARTILLERY);
    }

    public boolean isArtilleryLaunched() {
        return entityData.get(LAUNCHED);
    }

    public void prepareArtillery(Player owner, java.util.UUID target, Vec3 ground, int slot) {
        setOwner(owner);
        entityData.set(ARTILLERY, true);
        artilleryTarget = target;
        formation = position();
        aim = ground;
        damage = com.benji.oasiso.common.item.ApolSpearVolley.DAMAGE;
        launchDelay = slot * 2;
        artilleryAge = 0;
        setDeltaMovement(Vec3.ZERO);
        setXRot(90);
        xRotO = 90;
    }

    private void tickArtillery() {
        if (isLanded()) {
            setDeltaMovement(Vec3.ZERO);
            if (!level().isClientSide && ++landedAge >= 8) discard();
            return;
        }
        if (level().isClientSide) {
            Vec3 next = position().add(getDeltaMovement());
            setPos(next.x, next.y, next.z);
            return;
        }
        var level = (net.minecraft.server.level.ServerLevel) level();
        Entity owner = getOwner();

        if (!(owner instanceof Player player) || !player.isAlive() || player.isRemoved() || ++artilleryAge > 140) {
            discard();
            return;
        }

        int windup = SpearArtilleryPath.WINDUP_TICKS + launchDelay;
        Vec3 from = position(), next;

        if (artilleryAge <= windup) {
            double t = Math.max(0, artilleryAge - launchDelay) / (double) SpearArtilleryPath.WINDUP_TICKS;
            next = SpearArtilleryPath.recoil(formation, aim, t);
            if (artilleryAge % 4 == 0)
                level.sendParticles(Oasiso.ENTROPY_LIGHTNING.get(), getX(), getY() - .5, getZ(), 1, .2, .5, .2, 0);

            if (artilleryAge == windup) {
                Entity target = artilleryTarget == null ? null : level.getEntity(artilleryTarget);
                if (target instanceof LivingEntity living && living.isAlive()) {
                    Vec3 ground = com.benji.oasiso.common.item.ApolSpearVolley.ground(level, target.position(), this);
                    if (ground != null) {
                        aim = ground;
                    } else {
                        aim = target.position();
                    }
                }
                if (aim == null || Double.isNaN(aim.x)) {
                    aim = from.add(0, -5, 0);
                }

                arcStart = next;
                aim = aim.add(0, -.06, 0);
                control1 = SpearArtilleryPath.control1(arcStart, aim, level.getMaxBuildHeight() - 1);
                control2 = SpearArtilleryPath.control2(arcStart, aim, level.getMaxBuildHeight() - 1);
                entityData.set(LAUNCHED, true);
            }
        } else {
            double progress = (artilleryAge - windup) / (double) SpearArtilleryPath.FLIGHT_TICKS;

            if (progress < 0.85 && artilleryTarget != null) {
                Entity target = level.getEntity(artilleryTarget);
                if (target instanceof LivingEntity living && living.isAlive()) {
                    Vec3 ground = com.benji.oasiso.common.item.ApolSpearVolley.ground(level, living.position(), this);

                    if (ground != null) {
                        aim = aim.lerp(ground.add(0, -.06, 0), 0.25);
                    }
                    control1 = SpearArtilleryPath.control1(arcStart, aim, level.getMaxBuildHeight() - 1);
                    control2 = SpearArtilleryPath.control2(arcStart, aim, level.getMaxBuildHeight() - 1);
                }
            }

            next = progress <= 1.0 ? SpearArtilleryPath.point(arcStart, control1, control2, aim, progress) : from.add(0, -MAX_SPEED, 0);
        }

        var collision = level.clip(new ClipContext(from, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 motion = next.subtract(from);

        if (artilleryAge > windup && motion.lengthSqr() > 1e-10) {
            yRotO = getYRot();
            xRotO = getXRot();
            setYRot((float) Math.toDegrees(Math.atan2(motion.x, motion.z)));
            setXRot((float) Math.toDegrees(Math.atan2(motion.y, Math.sqrt(motion.x * motion.x + motion.z * motion.z))));
        }

        if (collision.getType() == HitResult.Type.BLOCK) {
            impactArtillery(level, player, collision.getLocation(), Vec3.atLowerCornerOf(collision.getDirection().getNormal()));
            return;
        }

        setDeltaMovement(motion);
        setPos(next.x, next.y, next.z);
        hasImpulse = true;
    }

    private void impactArtillery(net.minecraft.server.level.ServerLevel level, Player player, Vec3 point, Vec3 normal) {
        if (spent) return;
        spent = true;
        Vec3 center = point.add(normal.scale(.015));
        setPos(center.x, center.y, center.z);
        setDeltaMovement(Vec3.ZERO);
        entityData.set(LANDED, true);
        landedAge = 0;
        level.playSound(null, point.x, point.y, point.z, com.benji.oasiso.ModSounds.APOL_SWING.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.9F + level.random.nextFloat() * 0.2F);

        var area = new net.minecraft.world.phys.AABB(point.x - 1.5, point.y - .25, point.z - 1.5, point.x + 1.5, point.y + 2.75, point.z + 1.5);
        Vec3 eye = point.add(normal.scale(.12)).add(0, .15, 0);
        for (var mob : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, area, m -> m.isAlive() && !m.isInvulnerable() && !m.isAlliedTo(player) && !player.isAlliedTo(m))) {
            var visible = level.clip(new ClipContext(eye, mob.getBoundingBox().getCenter(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (visible.getType() != HitResult.Type.MISS) continue;
            int oldInvulnerability = mob.invulnerableTime;
            try {
                mob.invulnerableTime = 0;
                mob.hurt(damageSources().mobProjectile(this, player), damage);
            } finally {
                mob.invulnerableTime = Math.max(oldInvulnerability, mob.invulnerableTime);
            }
        }
        com.benji.oasiso.common.item.ApolSpearVolleyNetwork.impact(level, point, normal, getId());
        level.sendParticles(Oasiso.ENTROPY_LIGHTNING.get(), point.x, point.y + .2, point.z, 3, .7, .15, .7, 0);
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
        controllers.add(new AnimationController<SpearAttackEntity>(this, "controller", 0, event -> isArtillery() ? software.bernie.geckolib.core.object.PlayState.STOP : event.setAndContinue(isLanded() ? DISAP : IDLE)));
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
        tag.putBoolean("Artillery", isArtillery());
        tag.putBoolean("ArtilleryLaunched", isArtilleryLaunched());
        tag.putInt("ArtilleryAge", artilleryAge);
        tag.putInt("ArtilleryDelay", launchDelay);
        if (artilleryTarget != null) tag.putUUID("ArtilleryTarget", artilleryTarget);
        saveVec(tag, "Formation", formation);
        saveVec(tag, "Aim", aim);
        saveVec(tag, "ArcStart", arcStart);
        saveVec(tag, "Control1", control1);
        saveVec(tag, "Control2", control2);
        tag.putFloat("SpearDamage", damage);
        tag.putBoolean("SpearSpent", spent);
        tag.putBoolean("SpearLanded", isLanded());
        tag.putInt("SpearFlightAge", flightAge);
        tag.putInt("SpearLandedAge", landedAge);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ARTILLERY, tag.getBoolean("Artillery"));
        entityData.set(LAUNCHED, tag.getBoolean("ArtilleryLaunched"));
        artilleryAge = Math.max(0, tag.getInt("ArtilleryAge"));
        launchDelay = Math.max(0, Math.min(10, tag.getInt("ArtilleryDelay")));
        if (tag.hasUUID("ArtilleryTarget")) artilleryTarget = tag.getUUID("ArtilleryTarget");
        formation = readVec(tag, "Formation");
        aim = readVec(tag, "Aim");
        arcStart = readVec(tag, "ArcStart");
        control1 = readVec(tag, "Control1");
        control2 = readVec(tag, "Control2");
        if (tag.contains("SpearDamage")) damage = Math.max(0, tag.getFloat("SpearDamage"));
        spent = tag.getBoolean("SpearSpent");
        entityData.set(LANDED, tag.getBoolean("SpearLanded"));
        flightAge = Math.max(0, tag.getInt("SpearFlightAge"));
        landedAge = Math.max(0, tag.getInt("SpearLandedAge"));
    }

    private static void saveVec(CompoundTag tag, String key, Vec3 v) {
        CompoundTag data = new CompoundTag();
        data.putDouble("X", v.x);
        data.putDouble("Y", v.y);
        data.putDouble("Z", v.z);
        tag.put(key, data);
    }

    private static Vec3 readVec(CompoundTag tag, String key) {
        CompoundTag v = tag.getCompound(key);
        return new Vec3(v.getDouble("X"), v.getDouble("Y"), v.getDouble("Z"));
    }
}
