package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class QuiEntity extends PathfinderMob implements GeoEntity, GlowmaskEntity {
    private enum Phase {DRIFT, APPROACH, DESCEND, SLEEP, WAKE}

    private static final ResourceLocation RELAX_WATER = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "relax_water");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("sleep");
    private static final int CYCLE = 40;
    private static final int DASH_TICK = 14;
    private static final int BLEND = 4;
    private static final EntityDataAccessor<Boolean> ASLEEP = SynchedEntityData.defineId(QuiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int TURN_TICKS = 10;
    private static final EntityDataAccessor<Float> FROM_YAW = SynchedEntityData.defineId(QuiEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TO_YAW = SynchedEntityData.defineId(QuiEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FROM_TILT = SynchedEntityData.defineId(QuiEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TO_TILT = SynchedEntityData.defineId(QuiEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Long> TURN_START = SynchedEntityData.defineId(QuiEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> START = SynchedEntityData.defineId(QuiEntity.class, EntityDataSerializers.LONG);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Phase phase = Phase.DRIFT;
    private BlockPos pool;
    private int checkDelay = 80;
    private int phaseTicks;
    private int sleepTicks;
    private double heading;
    private double hoverHeight = 3.5;
    private long lastRenderTime = Long.MIN_VALUE;
    private long lastIdleCycle = Long.MIN_VALUE;
    private boolean needsAlignment = true;

    private Vec3 plannedDash = Vec3.ZERO;
    private boolean turnFromWall;

    public QuiEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
        heading = random.nextDouble() * Math.PI * 2;
        checkDelay += random.nextInt(100);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ASLEEP, false);
        entityData.define(FROM_YAW, 0.0F);
        entityData.define(TO_YAW, 0.0F);
        entityData.define(FROM_TILT, 0.0F);
        entityData.define(TO_TILT, 0.0F);
        entityData.define(TURN_START, -1L);
        entityData.define(START, -1L);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10).add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public void travel(Vec3 input) {
        if (!isEffectiveAi()) return;
        if (entityData.get(START) < 0) restartCycle();
        if (phase == Phase.DRIFT) drift();
        else visitPool();

        move(MoverType.SELF, getDeltaMovement());
        fallDistance = 0;
        calculateEntityAnimation(true);
    }

    private float turnProgress(float partialTick) {
        long elapsed = level().getGameTime() - entityData.get(TURN_START);
        float t = Mth.clamp((elapsed + partialTick) / TURN_TICKS, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    public float getFlightYaw(float partialTick) {
        return Mth.rotLerp(turnProgress(partialTick), entityData.get(FROM_YAW), entityData.get(TO_YAW));
    }

    public float getFlightTilt(float partialTick) {
        return Mth.lerp(turnProgress(partialTick), entityData.get(FROM_TILT), entityData.get(TO_TILT));
    }

    private void aimAlong(Vec3 direction) {
        if (direction.lengthSqr() < 1.0E-10) return;
        double horizontal = direction.horizontalDistance();
        float yaw = horizontal < 1.0E-5 ? getFlightYaw(0) : (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float tilt = (float) Math.toDegrees(Math.atan2(horizontal, direction.y));
        float fromYaw = getFlightYaw(0);
        float fromTilt = getFlightTilt(0);
        entityData.set(FROM_YAW, fromYaw);
        entityData.set(FROM_TILT, fromTilt);
        entityData.set(TO_YAW, yaw);
        entityData.set(TO_TILT, tilt);
        entityData.set(TURN_START, level().getGameTime());
    }

    private void restartCycle() {
        entityData.set(START, level().getGameTime() + BLEND);
    }

    private long animationAge() {
        return Math.max(0, level().getGameTime() - entityData.get(START));
    }

    private void drift() {
        Vec3 pos = position();
        var floor = level().clip(new ClipContext(pos, pos.add(0, -8, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
        double clearance = floor.getType() == HitResult.Type.MISS ? 8 : getY() - floor.getLocation().y;
        int beat = (int) (animationAge() % CYCLE);
        if (horizontalCollision) turnFromWall = true;
        if (beat == DASH_TICK - TURN_TICKS || entityData.get(TURN_START) < 0) {
            if (turnFromWall) {
                heading += Math.PI * (0.8 + random.nextDouble() * 0.4);
                turnFromWall = false;
            } else if (random.nextFloat() < 0.45F) {
                heading += (random.nextDouble() - 0.5) * Math.PI;
            }
            hoverHeight = 2.6 + random.nextDouble() * 1.8;
            double lift = Mth.clamp((hoverHeight - clearance) * 0.12 + 0.05, -0.16, 0.20);
            plannedDash = new Vec3(Math.cos(heading) * 0.31, lift, Math.sin(heading) * 0.31);
            aimAlong(plannedDash);
        }

        if (beat == DASH_TICK && plannedDash.lengthSqr() > 0) {
            setDeltaMovement(plannedDash);
            plannedDash = Vec3.ZERO;
        } else {
            Vec3 speed = getDeltaMovement().scale(0.90);
            double correction = Mth.clamp((hoverHeight - clearance) * 0.008, -0.023, 0.023);
            setDeltaMovement(speed.x, Mth.clamp(speed.y + correction, -0.24, 0.36), speed.z);
        }
        faceMotion();
        if (--checkDelay <= 0) {
            checkDelay = 160 + random.nextInt(81);
            if (random.nextFloat() < 0.30f) {
                pool = findPool();
                if (pool != null) enter(Phase.APPROACH);
            }
        }
    }

    private boolean isRelaxWater(BlockPos p) {
        FluidState state = level().getFluidState(p);
        if (state.isEmpty()) return false;
        var fluid = state.getType();
        if (fluid instanceof FlowingFluid flowing) fluid = flowing.getSource();
        return RELAX_WATER.equals(ForgeRegistries.FLUIDS.getKey(fluid)) || RELAX_WATER.equals(ForgeRegistries.BLOCKS.getKey(state.createLegacyBlock().getBlock()));
    }

    private BlockPos findPool() {
        BlockPos center = blockPosition();
        BlockPos closest = null;
        double best = 100.00001;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-10, -10, -10), center.offset(10, 10, 10))) {
            double distance = position().distanceToSqr(Vec3.atCenterOf(p));
            if (distance >= best || !level().hasChunkAt(p) || !isRelaxWater(p)) continue;
            if (!level().isEmptyBlock(p.above()) || !level().isEmptyBlock(p.above(2))) continue;
            best = distance;
            closest = p.immutable();
        }
        return closest;
    }

    private void enter(Phase next) {
        phase = next;
        phaseTicks = 0;
        plannedDash = Vec3.ZERO;
        turnFromWall = false;
        if (next == Phase.DESCEND) aimAlong(new Vec3(0, 1, 0));
    }

    private void leavePool() {
        if (entityData.get(ASLEEP)) {
            entityData.set(ASLEEP, false);
            restartCycle();
        }
        pool = null;
        checkDelay = 240 + random.nextInt(121);
        enter(Phase.DRIFT);
    }

    private void visitPool() {
        phaseTicks++;
        if (pool == null || !level().hasChunkAt(pool) || !isRelaxWater(pool) || !level().isEmptyBlock(pool.above()) || phaseTicks > 240) {
            leavePool();
            return;
        }
        FluidState water = level().getFluidState(pool);
        double surface = pool.getY() + water.getHeight(level(), pool);
        Vec3 above = new Vec3(pool.getX() + 0.5, surface + 1.15, pool.getZ() + 0.5);
        double depth = Math.min(getBbHeight() * 0.45, (surface - pool.getY()) * 0.85);
        Vec3 bed = new Vec3(above.x, surface - depth, above.z);
        switch (phase) {
            case APPROACH -> {
                if (phaseTicks == 1) aimAlong(above.subtract(position()));
                steer(above, 0.16);
                faceMotion();
                if (position().distanceToSqr(above) < 0.0064) enter(Phase.DESCEND);
            }
            case DESCEND -> {
                steer(bed, 0.045);
                if (position().distanceToSqr(bed) < 0.0016) {
                    sleepTicks = BLEND + CYCLE * (2 + random.nextInt(2));
                    entityData.set(ASLEEP, true);
                    restartCycle();
                    enter(Phase.SLEEP);
                }
            }
            case SLEEP -> {
                steer(bed, 0.025);
                if (phaseTicks >= sleepTicks) {
                    entityData.set(ASLEEP, false);
                    restartCycle();
                    enter(Phase.WAKE);
                }
            }
            case WAKE -> {
                steer(bed, 0.025);
                if (animationAge() == DASH_TICK - TURN_TICKS) {
                    plannedDash = new Vec3(0.12 * Math.cos(heading), 0.49, 0.12 * Math.sin(heading));
                    aimAlong(plannedDash);
                }
                if (animationAge() == DASH_TICK) {
                    setDeltaMovement(plannedDash);
                    leavePool();
                }
            }
            default -> {
            }
        }
    }

    private void steer(Vec3 target, double limit) {
        Vec3 delta = target.subtract(position());
        Vec3 wanted = delta.scale(0.22);
        if (wanted.lengthSqr() > limit * limit) wanted = wanted.normalize().scale(limit);
        setDeltaMovement(getDeltaMovement().lerp(wanted, 0.42));
    }

    private void faceMotion() {
        Vec3 speed = getDeltaMovement();
        if (speed.horizontalDistanceSqr() < 0.0001) return;
        float yaw = (float) Math.toDegrees(Math.atan2(-speed.x, speed.z));
        setYRot(Mth.approachDegrees(getYRot(), yaw, 12));
        setYHeadRot(getYRot());
        setYBodyRot(getYRot());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", BLEND, state -> {
            boolean asleep = entityData.get(ASLEEP);
            long now = level().getGameTime();
            long elapsed = now - entityData.get(START) + BLEND;
            long cycle = Math.floorDiv(elapsed, CYCLE);
            if (lastRenderTime == Long.MIN_VALUE || now - lastRenderTime > 2) needsAlignment = true;
            if (!asleep && needsAlignment && cycle != lastIdleCycle && cycle > 0 && Math.floorMod(elapsed, CYCLE) < 2) {
                state.getController().forceAnimationReset();
                needsAlignment = false;
            }
            lastRenderTime = now;
            lastIdleCycle = cycle;
            return state.setAndContinue(asleep ? SLEEP : IDLE);
        }));
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/qui_emissive.png");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
