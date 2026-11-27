package com.benji.oasiso.common.entity;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class KarakWormEntity extends PathfinderMob implements GeoEntity, GlowmaskEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public KarakWormEntity(EntityType<? extends KarakWormEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public static final double BREACH_HEIGHT = 4.5;
    public static final double BURROW_DEPTH = 1.65;
    public static final int WAVE_TICKS = 56;
    public static final double FORWARD_SPEED = 0.24;

    private static final int PLAYER_RADIUS = 15;
    private static final float THEFT_CHANCE = 0.30F;
    private static final int SEARCH_INTERVAL = 100;
    private static final int THEFT_COOLDOWN = 300;
    private static final double CHASE_SPEED = 1.5;

    private long nextSearch;
    private long chaseUntil;
    private ServerPlayer theftTarget;

    private boolean swimming;
    private double phase;
    private double course;
    private double turnRate;
    private double groundY;
    private int steerTicks;

    private final Vec3[] trail = new Vec3[96];
    private int trailIndex;

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0D).add(Attributes.MOVEMENT_SPEED, 0.25D).add(Attributes.KNOCKBACK_RESISTANCE, 0.2D).add(Attributes.ATTACK_DAMAGE, 0.0D).add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    @Override
    public void travel(Vec3 input) {
        if (!(level() instanceof ServerLevel server) || !isAlive() || isNoAi()) return;
        Vec3 from = position();
        if (!swimming || Math.abs(getY() - groundY) > 9) {
            double surface = findSurface(getX(), getZ(), getY());
            if (!Double.isFinite(surface)) {
                double dy = insideGround(from) ? 0.22 : -0.22;
                if (getY() + dy > level().getMinBuildHeight() && getY() + dy < level().getMaxBuildHeight()) {
                    move(MoverType.SELF, new Vec3(0, dy, 0));
                }
                return;
            }
            groundY = surface;
            course = Math.toRadians(getYRot());
            double wave = (2 * (getY() - surface) - BREACH_HEIGHT + BURROW_DEPTH) / (BREACH_HEIGHT + BURROW_DEPTH);
            phase = Math.asin(Mth.clamp(wave, -1.0, 1.0));
            swimming = true;
        }
        updateTheftTarget(server);
        if (theftTarget != null) {
            double targetYaw = Math.atan2(getX() - theftTarget.getX(), theftTarget.getZ() - getZ());
            double error = Math.atan2(Math.sin(targetYaw - course), Math.cos(targetYaw - course));
            course += Mth.clamp(error, -0.24, 0.24);
        } else {
            if (--steerTicks <= 0) {
                turnRate = (random.nextDouble() - 0.5) * 0.038;
                steerTicks = 30 + random.nextInt(55);
            }
            course += turnRate;
        }
        double yaw = course + 0.17 * Math.sin(phase * 0.8);
        double pace = theftTarget == null ? 1.0 : CHASE_SPEED;
        double speed = FORWARD_SPEED * (theftTarget == null && getHurtTimeBoost() ? 1.4 : pace);
        double dx = -Math.sin(yaw) * speed;
        double dz = Math.cos(yaw) * speed;
        double surface = findSurface(getX() + dx, getZ() + dz, groundY);
        if (!Double.isFinite(surface) || Math.abs(surface - groundY) > 2.5) {
            course += Math.PI * 0.65;
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        groundY += Mth.clamp(surface - groundY, -0.18, 0.18);
        phase += pace * 2 * Math.PI / WAVE_TICKS;
        double height = (BREACH_HEIGHT - BURROW_DEPTH + (BREACH_HEIGHT + BURROW_DEPTH) * Math.sin(phase)) * 0.5;
        Vec3 step = new Vec3(dx, Mth.clamp(groundY + height - getY(), -0.65, 0.65), dz);
        setDeltaMovement(step);
        move(MoverType.SELF, step);
        fallDistance = 0;
        setYRot((float) Math.toDegrees(yaw));
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
        scatterAtCrossing(server, from, position());
        if (theftTarget != null) trySnatch(server, theftTarget);
    }

    private void updateTheftTarget(ServerLevel server) {
        long now = server.getGameTime();
        if (theftTarget != null) {
            if (now < chaseUntil && theftTarget.level() == server && eligible(theftTarget) && distanceToSqr(theftTarget) <= PLAYER_RADIUS * PLAYER_RADIUS)
                return;
            theftTarget = null;
            nextSearch = now + SEARCH_INTERVAL;
        }
        if (now < nextSearch) return;
        nextSearch = now + SEARCH_INTERVAL;
        if (random.nextFloat() >= THEFT_CHANCE) return;

        double closest = PLAYER_RADIUS * PLAYER_RADIUS;
        for (ServerPlayer player : server.players()) {
            double distance = distanceToSqr(player);
            if (distance > closest || !eligible(player) || player.getInventory().items.stream().allMatch(ItemStack::isEmpty))
                continue;
            theftTarget = player;
            closest = distance;
        }
        chaseUntil = now + 160;
    }

    private boolean eligible(ServerPlayer player) {
        return player.isAlive() && !player.isRemoved() && !player.isCreative() && !player.isSpectator();
    }

    private void trySnatch(ServerLevel server, ServerPlayer player) {
        Vec3 chest = player.getBoundingBox().getCenter();
        if (position().distanceToSqr(chest) > 2.0 * 2.0 || insideGround(position())) return;
        if (server.clip(new ClipContext(position(), chest, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() != HitResult.Type.MISS)
            return;

        int count = 0;
        ItemStack chosen = ItemStack.EMPTY;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && random.nextInt(++count) == 0) chosen = stack;
        }
        theftTarget = null;
        nextSearch = server.getGameTime() + SEARCH_INTERVAL;
        if (chosen.isEmpty()) return;

        var dropped = new ItemEntity(server, chest.x, chest.y, chest.z, chosen.split(1));
        double angle = random.nextDouble() * 2 * Math.PI;
        double outward = 0.082 + random.nextDouble() * 0.020;
        dropped.setDeltaMovement(Math.cos(angle) * outward, 0.23, Math.sin(angle) * outward);
        dropped.setPickUpDelay(20);
        if (!server.addFreshEntity(dropped)) {
            chosen.grow(1);
            return;
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        nextSearch = server.getGameTime() + THEFT_COOLDOWN;
        server.playSound(null, chest.x, chest.y, chest.z, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.75F, 1.0F + random.nextFloat() * 0.25F);
        server.sendParticles(Oasiso.KR_SAND_DUST.get(), chest.x, chest.y, chest.z, 8, 0.22, 0.18, 0.22, 0.035);
    }

    private boolean getHurtTimeBoost() {
        return getLastHurtByMob() != null && tickCount - getLastHurtByMobTimestamp() < 60;
    }

    private double findSurface(double x, double z, double around) {
        var pos = BlockPos.containing(x, around, z).mutable();
        if (!level().hasChunkAt(pos) || !level().getWorldBorder().isWithinBounds(pos)) return Double.NaN;
        int base = pos.getY();
        double best = Double.NaN;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (int y = Math.max(level().getMinBuildHeight(), base - 8); y <= Math.min(level().getMaxBuildHeight() - 2, base + 5); y++) {
            pos.setY(y);
            var shape = level().getBlockState(pos).getCollisionShape(level(), pos);
            if (shape.isEmpty()) continue;
            var above = pos.above();
            if (!level().getBlockState(above).getCollisionShape(level(), above).isEmpty() || !level().getFluidState(above).isEmpty())
                continue;
            double top = y + shape.max(net.minecraft.core.Direction.Axis.Y);
            double distance = Math.abs(around - top);
            if (distance < bestDistance) {
                best = top;
                bestDistance = distance;
            }
        }
        return best;
    }

    private boolean insideGround(Vec3 point) {
        var pos = BlockPos.containing(point);
        var shape = level().getBlockState(pos).getCollisionShape(level(), pos);
        for (var box : shape.toAabbs()) {
            if (box.contains(point.x - pos.getX(), point.y - pos.getY(), point.z - pos.getZ())) return true;
        }
        return false;
    }

    private void scatterAtCrossing(ServerLevel level, Vec3 from, Vec3 to) {
        boolean emerging = insideGround(from);
        if (emerging == insideGround(to)) return;
        var hit = level.clip(new ClipContext(emerging ? to : from, emerging ? from : to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() != HitResult.Type.BLOCK) return;
        var pos = hit.getBlockPos();
        var point = hit.getLocation();
        float pitch = (emerging ? 1.08F : 0.78F) + random.nextFloat() * 0.14F;
        level.playSound(null, point.x, point.y, point.z, ModSounds.KWORM_DIRT.get(), SoundSource.NEUTRAL, 0.9F, pitch);
        var particle = new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(pos)).setPos(pos);
        level.sendParticles(particle, point.x, point.y + 0.08, point.z, emerging ? 28 : 22, 0.32, 0.12, 0.32, 0.12);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypes.IN_WALL) || source.is(DamageTypeTags.IS_FALL) || super.isInvulnerableTo(source);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) return;
        Vec3 current = position();
        if (trail[trailIndex] == null || current.distanceToSqr(trail[trailIndex]) > 64) Arrays.fill(trail, current);
        trailIndex = (trailIndex + 1) % trail.length;
        trail[trailIndex] = current;
    }

    @Override
    public Vec3 getLightProbePosition(float partialTick) {
        Vec3 probe = super.getLightProbePosition(partialTick);
        BlockPos.MutableBlockPos pos = BlockPos.containing(probe).mutable();

        if (level().getBlockState(pos).getCollisionShape(level(), pos).isEmpty()) {
            return probe;
        }

        int searchHeight = (int) Math.ceil(BURROW_DEPTH) + 2;
        for (int i = 0; i < searchHeight; i++) {
            pos.move(0, 1, 0);
            if (pos.getY() >= level().getMaxBuildHeight()) break;

            if (level().getBlockState(pos).getCollisionShape(level(), pos).isEmpty()) {
                return new Vec3(probe.x, pos.getY() + 0.5, probe.z);
            }
        }

        return probe;
    }

    public Vec3 trailPoint(double behind, float partial) {
        Vec3 point = getPosition(partial);
        Vec3 backward = Vec3.directionFromRotation(0, getYRot()).scale(-1);
        for (int i = 1; i < trail.length && behind > 0; i++) {
            Vec3 older = trail[Math.floorMod(trailIndex - i, trail.length)];
            if (older == null) break;
            Vec3 step = older.subtract(point);
            double length = step.length();
            if (length < 0.0001) continue;
            backward = step.scale(1 / length);
            if (behind <= length) return point.add(backward.scale(behind));
            behind -= length;
            point = older;
        }
        return point.add(backward.scale(behind));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("KarakNextTheftSearch", nextSearch);
        if (!swimming) return;
        tag.putDouble("KarakFloor", groundY);
        tag.putDouble("KarakPhase", phase);
        tag.putDouble("KarakCourse", course);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        nextSearch = tag.getLong("KarakNextTheftSearch");
        swimming = tag.contains("KarakFloor") && Math.abs(tag.getDouble("KarakFloor") - getY()) < 9;
        groundY = tag.getDouble("KarakFloor");
        phase = tag.getDouble("KarakPhase");
        course = tag.getDouble("KarakCourse");
    }

    @Override
    protected SoundEvent getAmbientSound() {

        SoundEvent[] sounds = {ModSounds.KWORM_IDLE1.get(), ModSounds.KWORM_IDLE2.get(), ModSounds.KWORM_IDLE3.get()};

        return sounds[this.random.nextInt(sounds.length)];
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return ModSounds.KWORM_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.KWORM_DEATH.get();
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/karak_worm_emissive.png");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
