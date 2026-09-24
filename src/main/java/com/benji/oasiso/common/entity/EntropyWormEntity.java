package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class EntropyWormEntity extends Monster implements GeoEntity, GlowmaskEntity {
    private static final EntityDataAccessor<Boolean> DASH = SynchedEntityData.defineId(EntropyWormEntity.class, EntityDataSerializers.BOOLEAN);

    private static final double CRUISE_SPEED = .33, DASH_SPEED = .54, ORBIT_SPEED = .40;
    private static final double PLAYER_RANGE = 16, ORBIT_RADIUS = 3.5;
    private static final int MAX_ORBITERS = 2, ORBIT_TICKS = 120;
    private static final float ORBIT_CHANCE = .10F;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Vec3 destination;
    private UUID orbitPlayer;
    private int approachTicks, orbitTicks, orbitCooldown = 100, dashTicks, dashCooldown = 100;
    private boolean circling;
    private double orbitAngle;
    private int orbitDirection = 1;

    private final float[] yawHistory = new float[128], pitchHistory = new float[128];
    private int historyIndex;
    private boolean historyReady;
    private Vec3 lastVisualPosition;
    private float visualPitch, visualSpeed = .13F, wavePhase, oldWavePhase;

    public EntropyWormEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
        dashCooldown = 80 + random.nextInt(160);
        orbitCooldown = 100 + random.nextInt(200);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10)
                .add(Attributes.MOVEMENT_SPEED, .45)
                .add(Attributes.KNOCKBACK_RESISTANCE, .25)
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.FOLLOW_RANGE, 30);
    }

    @Override
    protected void registerGoals() { }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DASH, false);
    }

    public boolean isDashing() {
        return entityData.get(DASH);
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean ground, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
        fallDistance = 0;
    }

    @Override
    public void travel(Vec3 input) {
        if (isEffectiveAi()) {
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(.96));
        }
        calculateEntityAnimation(false);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel server) || !isAlive()) return;
        if (orbitCooldown > 0) orbitCooldown--;
        if (dashCooldown > 0) dashCooldown--;
        if (dashTicks > 0) dashTicks--;
        if (orbitPlayer == null && orbitCooldown == 0 && tickCount % 100 == 0 && random.nextFloat() < ORBIT_CHANCE) {
            tryOrbit(server);
        }
        if (orbitPlayer != null) {
            ServerPlayer player = server.getServer().getPlayerList().getPlayer(orbitPlayer);
            if (!eligible(player) || distanceToSqr(player) > 32 * 32 || ++approachTicks > 280) {
                endOrbit();
            } else {
                if (!circling && distanceToSqr(player) <= 5 * 5 && getSensing().hasLineOfSight(player)) {
                    circling = true;
                    orbitTicks = ORBIT_TICKS;
                    orbitAngle = Math.atan2(getZ() - player.getZ(), getX() - player.getX());
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, true, true));
                }
                if (circling) {
                    if (--orbitTicks <= 0) {
                        endOrbit();
                    } else {
                        orbitAngle += orbitDirection * .055;
                        destination = player.position().add(Math.cos(orbitAngle) * ORBIT_RADIUS, 1.4 + .45 * Math.sin(tickCount * .09), Math.sin(orbitAngle) * ORBIT_RADIUS);
                        if (tickCount % 8 == 0) {
                            double angle = random.nextDouble() * Math.PI * 2;
                            server.sendParticles(Oasiso.ENTROPY_EYE.get(), player.getX() + Math.cos(angle) * 1.2, player.getY() + .5 + random.nextDouble() * 1.6, player.getZ() + Math.sin(angle) * 1.2, 1, .08, .08, .08, .015);
                        }
                    }
                } else destination = player.position().add(0, 1.7, 0);
            }
        }
        if (orbitPlayer == null) {
            if (destination == null || position().distanceToSqr(destination) < 2 || tickCount % 100 == 0 || horizontalCollision) {
                destination = chooseDestination(server);
            }
            if (dashCooldown == 0) {
                dashTicks = 22 + random.nextInt(15);
                dashCooldown = 140 + random.nextInt(160);
            }
        } else dashTicks = 0;
        entityData.set(DASH, dashTicks > 0);
        steer(server);
    }

    private boolean eligible(ServerPlayer p) {
        return p != null && p.level() == level() && p.isAlive() && !p.isCreative() && !p.isSpectator();
    }

    private void tryOrbit(ServerLevel server) {
        ServerPlayer best = null;
        double nearest = PLAYER_RANGE * PLAYER_RANGE;
        for (ServerPlayer p : server.players()) {
            if (!eligible(p) || distanceToSqr(p) >= nearest || !getSensing().hasLineOfSight(p)) continue;
            long reserved = server.getEntitiesOfClass(EntropyWormEntity.class, p.getBoundingBox().inflate(48), worm -> worm.isAlive() && p.getUUID().equals(worm.orbitPlayer)).size();
            if (reserved >= MAX_ORBITERS) continue;
            best = p;
            nearest = distanceToSqr(p);
        }
        if (best != null) {
            orbitPlayer = best.getUUID();
            approachTicks = 0;
            circling = false;
            orbitDirection = random.nextBoolean() ? 1 : -1;
        }
    }

    private void endOrbit() {
        orbitPlayer = null;
        circling = false;
        destination = null;
        orbitCooldown = 400 + random.nextInt(401);
    }

    private Vec3 chooseDestination(ServerLevel server) {
        for (int i = 0; i < 18; i++) {
            Vec3 p = position().add((random.nextDouble() - .5) * 16, (random.nextDouble() - .35) * 8, (random.nextDouble() - .5) * 16);
            if (!server.hasChunkAt(BlockPos.containing(p))) continue;
            var floor = server.clip(new ClipContext(p, p.add(0, -12, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (floor.getType() != HitResult.Type.MISS)
                p = new Vec3(p.x, floor.getLocation().y + 2 + random.nextDouble() * 3, p.z);
            if (free(server, p) && clearLine(server, position(), p)) return p;
        }
        return position().add(0, .7, 0);
    }

    private boolean free(ServerLevel server, Vec3 point) {
        var box = getBoundingBox().move(point.subtract(position()));
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ)))
            if (!server.hasChunkAt(p)) return false;
        return server.getWorldBorder().isWithinBounds(box) && server.noCollision(this, box) && !server.containsAnyLiquid(box);
    }

    private boolean clearLine(ServerLevel server, Vec3 from, Vec3 to) {
        int steps = Math.max(1, (int) Math.ceil(from.distanceTo(to)));
        for (int i = 0; i <= steps; i++)
            if (!server.hasChunkAt(BlockPos.containing(from.lerp(to, i / (double) steps)))) return false;
        return server.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    private void steer(ServerLevel server) {
        if (destination == null) return;
        Vec3 delta = destination.subtract(position());
        if (delta.lengthSqr() < .01) return;
        Vec3 direction = delta.normalize();
        double speed = orbitPlayer != null ? ORBIT_SPEED : isDashing() ? DASH_SPEED : CRUISE_SPEED;
        Vec3 side = new Vec3(-direction.z, 0, direction.x);
        double phase = tickCount * (isDashing() ? .40 : .16) + getId();
        Vec3 wanted = direction.scale(speed).add(side.scale(Math.sin(phase) * speed * .20)).add(0, Math.sin(phase * .8) * speed * .12, 0);
        Vec3 lookAhead = position().add(wanted.normalize().scale(1.8));
        if (!free(server, lookAhead) || !clearLine(server, position(), lookAhead)) {
            wanted = Vec3.ZERO;
            for (Vec3 avoid : new Vec3[]{new Vec3(0, .18, 0), side.scale(.16), side.scale(-.16), direction.scale(-.12)}) {
                Vec3 point = position().add(avoid.scale(8));
                if (free(server, point) && clearLine(server, position(), point)) {
                    wanted = avoid;
                    break;
                }
            }
            if (orbitPlayer == null) destination = null;
        }
        Vec3 velocity = getDeltaMovement().lerp(wanted, isDashing() ? .16 : .09);
        setDeltaMovement(velocity);
        if (velocity.horizontalDistanceSqr() > .0001) {
            float yaw = (float) (Math.atan2(-velocity.x, velocity.z) * Mth.RAD_TO_DEG);
            setYRot(getYRot() + Mth.clamp(Mth.wrapDegrees(yaw - getYRot()), -9F, 9F));
            yBodyRot = yHeadRot = getYRot();
        }
    }

    @Override
    public void tick() {
        super.tick();
        yBodyRot = yHeadRot = getYRot();
        if (level().isClientSide) recordVisualHistory();
    }

    private void recordVisualHistory() {
        Vec3 movement = lastVisualPosition == null ? Vec3.ZERO : position().subtract(lastVisualPosition);
        if (!historyReady || movement.lengthSqr() > 64) {
            java.util.Arrays.fill(yawHistory, getYRot());
            java.util.Arrays.fill(pitchHistory, 0);
            historyReady = true;
            visualPitch = 0;
        }
        if (movement.lengthSqr() > .00001 && movement.lengthSqr() < 64) {
            float pitch = (float) Math.atan2(movement.y, Math.max(.001, movement.horizontalDistance()));
            visualPitch = Mth.lerp(.18F, visualPitch, Mth.clamp(pitch, -.65F, .65F));
            visualSpeed = Mth.lerp(.12F, visualSpeed, (float) movement.length());
        }
        historyIndex = (historyIndex + 1) & 127;
        yawHistory[historyIndex] = getYRot();
        pitchHistory[historyIndex] = visualPitch;
        lastVisualPosition = position();
        oldWavePhase = wavePhase;
        wavePhase += isDashing() ? .42F : .18F;
    }

    public float historyYaw(float delay, float partial) {
        if (!historyReady) return getYRot();
        float offset = Mth.clamp(delay + 1 - partial, 0, 125);
        int n = Mth.floor(offset);
        return Mth.rotLerp(offset - n, yawHistory[(historyIndex - n) & 127], yawHistory[(historyIndex - n - 1) & 127]);
    }

    public float historyPitch(float delay, float partial) {
        float offset = Mth.clamp(delay + 1 - partial, 0, 125);
        int n = Mth.floor(offset);
        return Mth.lerp(offset - n, pitchHistory[(historyIndex - n) & 127], pitchHistory[(historyIndex - n - 1) & 127]);
    }

    public float segmentDelay() {
        return Mth.clamp(.94F / Math.max(.08F, visualSpeed), 3F, 11F);
    }

    public float wavePhase(float partial) {
        return Mth.lerp(partial, oldWavePhase, wavePhase) + (getId() & 255);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "procedural", 0, state -> PlayState.STOP));
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/entropy_worm_emissive.png");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
