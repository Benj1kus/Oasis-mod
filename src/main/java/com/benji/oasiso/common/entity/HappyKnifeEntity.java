package com.benji.oasiso.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class HappyKnifeEntity extends Projectile implements GeoEntity {
    public static final int SPAWN = 0, AIM = 1, FLY = 2, STUCK = 3, FADE = 4;
    public static final float DAMAGE = 5;
    public static final int SPAWN_TICKS = 16, AIM_TICKS = 12, STICK_TICKS = 24, FADE_TICKS = 12;
    public static final double SPEED = .65, STEERING = .16;
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(HappyKnifeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> START = SynchedEntityData.defineId(HappyKnifeEntity.class, EntityDataSerializers.LONG);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation APPEAR = RawAnimation.begin().thenPlayAndHold("spawn"), IDLE = RawAnimation.begin().thenLoop("idle"), SPIN = RawAnimation.begin().thenLoop("spin");
    private LivingEntity target, victim;
    private Vec3 attachment = Vec3.ZERO;
    private double aimU = .5, aimV = .5, aimW = .5;
    private float yawOffset;
    private float pitchOffset;
    private int totalAge;
    private boolean visualSpinning;

    public HappyKnifeEntity(EntityType<? extends HappyKnifeEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(PHASE, SPAWN);
        entityData.define(START, 0L);
    }

    public int phase() {
        return entityData.get(PHASE);
    }

    public float phaseAge(float partial) {
        return Math.max(0, level().getGameTime() - entityData.get(START) + partial);
    }

    public float opacity(float partial) {
        return phase() == FADE ? Mth.clamp(1 - phaseAge(partial) / FADE_TICKS, 0, 1) : 1;
    }

    private void phase(int next) {
        entityData.set(PHASE, next);
        entityData.set(START, level().getGameTime());
    }

    private Entity allegiance() {
        return getOwner() == null ? this : getOwner();
    }

    private boolean valid(LivingEntity e) {
        return e != null && HappinessEntity.isEnemy(e, allegiance()) && distanceToSqr(e) < 32 * 32;
    }

    private void acquire() {
        LivingEntity next = HappinessEntity.nearest(level(), position(), allegiance(), 24);
        if (next != target && next != null) {
            aimU = .15 + random.nextDouble() * .70;
            aimV = .20 + random.nextDouble() * .65;
            aimW = .15 + random.nextDouble() * .70;
        }
        target = next;
    }

    private Vec3 aimPoint() {
        AABB box = target.getBoundingBox();
        return new Vec3(Mth.lerp(aimU, box.minX, box.maxX), Mth.lerp(aimV, box.minY, box.maxY), Mth.lerp(aimW, box.minZ, box.maxZ));
    }

    private EntityHitResult sweep(Vec3 start, Vec3 end, Vec3 movement) {
        EntityHitResult closest = null;
        double best = Double.POSITIVE_INFINITY;
        for (LivingEntity living : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().expandTowards(movement).inflate(.5), e -> HappinessEntity.isEnemy(e, allegiance()))) {
            AABB box = living.getBoundingBox().inflate(.05);
            Vec3 point = box.contains(start) ? start : box.clip(start, end).orElse(null);
            if (point == null) continue;
            double distance = start.distanceToSqr(point);
            if (distance < best) {
                best = distance;
                closest = new EntityHitResult(living, point);
            }
        }
        return closest;
    }

    private void aim(Vec3 delta) {
        if (delta.lengthSqr() < 1e-8) return;
        setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        setXRot((float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance())));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (totalAge++ == 0) phase(SPAWN);
        if (totalAge > 240 && phase() != FADE) phase(FADE);
        int state = phase();
        if (state == SPAWN) {
            if (phaseAge(0) >= SPAWN_TICKS) {
                acquire();
                phase(AIM);
            }
            return;
        }
        if (state == AIM) {
            if (!valid(target)) acquire();
            if (target != null) aim(aimPoint().subtract(position()));
            if (phaseAge(0) >= AIM_TICKS) {
                if (target == null) {
                    if (phaseAge(0) >= 40) phase(FADE);
                } else {
                    phase(FLY);
                    setDeltaMovement(aimPoint().subtract(position()).normalize().scale(.20));
                }
            }
            return;
        }
        if (state == STUCK || state == FADE) {
            if (victim != null && victim.isAlive()) {
                Vec3 off = attachment.yRot((float) -Math.toRadians(victim.getYRot()));
                setPos(victim.position().add(off));
                setYRot(victim.getYRot() + yawOffset);
                setXRot(pitchOffset);
            }
            if (state == STUCK && (victim != null && !victim.isAlive() || phaseAge(0) >= STICK_TICKS)) phase(FADE);
            if (state == FADE && phaseAge(0) >= FADE_TICKS) discard();
            return;
        }

        if (!valid(target) && totalAge % 5 == 0) acquire();
        Vec3 velocity = getDeltaMovement();
        if (valid(target)) {
            Vec3 wanted = aimPoint().subtract(position()).normalize().scale(SPEED);
            velocity = velocity.lerp(wanted, STEERING);
        }
        if (velocity.lengthSqr() < .001) {
            phase(FADE);
            return;
        }
        setDeltaMovement(velocity);
        aim(velocity);
        Vec3 start = position(), end = start.add(velocity);
        BlockHitResult block = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 limit = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        EntityHitResult hit = sweep(start, limit, velocity);
        if (hit != null) {
            LivingEntity living = (LivingEntity) hit.getEntity();
            setPos(hit.getLocation());
            living.hurt(damageSources().thrown(this, getOwner()), DAMAGE);
            level().playSound(null, getX(), getY(), getZ(), com.benji.oasiso.ModSounds.KNIFE_HIT.get(), net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 0.85F + random.nextFloat() * 0.3F);
            victim = living;
            attachment = position().subtract(living.position()).yRot((float) Math.toRadians(living.getYRot()));
            yawOffset = getYRot() - living.getYRot();
            pitchOffset = getXRot();

            setDeltaMovement(Vec3.ZERO);
            phase(STUCK);
        } else if (block.getType() != HitResult.Type.MISS) {
            setPos(block.getLocation());
            setDeltaMovement(Vec3.ZERO);
            phase(STUCK);
        } else setPos(end);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        discard();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "knife", 0, event -> {
            int state = phase();
            if (state == SPAWN) {
                return event.setAndContinue(APPEAR);
            } else if (state == STUCK || state == FADE) {
                return event.setAndContinue(IDLE);
            } else if (state == FLY || state == AIM) {
                return event.setAndContinue(SPIN);
            }
            return event.setAndContinue(IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
