package com.benji.oasiso.client.layer;

import com.benji.oasiso.Oasiso;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AzumalitArmSmokeLayer {

    private static final int CUSTOM_SPIRALS_PER_ARM = 3;
    private static final int SOUL_PARTICLES_PER_ARM = 1;
    private static final int SOUL_FLAMES_PER_ARM = 2;
    private static final int EMISSION_INTERVAL_TICKS = 2;
    private static final double NOZZLE_OUTWARD_OFFSET = 0.40D;
    private static final double HORIZONTAL_MOTION_INHERITANCE = 0.82D;
    private static final double VERTICAL_MOTION_INHERITANCE = 0.55D;

    private static final Map<UUID, ArmCapture> CAPTURES = new HashMap<>();

    private AzumalitArmSmokeLayer() {
    }

    public static void captureBone(LivingEntity wearer, String boneName, GeoBone bone, float partialTick) {
        if (wearer == null || bone == null || !wearer.isAlive() || wearer.isInvisible() || !(wearer.level() instanceof ClientLevel level)) {
            return;
        }

        boolean relevant = "smokearm_left".equals(boneName) || "arm_p_left".equals(boneName) || "smokearm_right".equals(boneName) || "arm_p_right".equals(boneName);

        if (!relevant) {
            return;
        }
        Vector3d local = bone.getLocalPosition();
        if (local == null || !Double.isFinite(local.x) || !Double.isFinite(local.y) || !Double.isFinite(local.z)) {
            return;
        }

        Vec3 position = ownerLocalToWorld(wearer, local, partialTick);

        UUID wearerId = wearer.getUUID();
        int tick = wearer.tickCount;

        ArmCapture capture = CAPTURES.computeIfAbsent(wearerId, ignored -> new ArmCapture());

        if (capture.tick != tick) {
            capture.reset(tick);
        }

        switch (boneName) {
            case "smokearm_left" -> capture.leftStart = position;
            case "arm_p_left" -> capture.leftGuide = position;
            case "smokearm_right" -> capture.rightStart = position;
            case "arm_p_right" -> capture.rightGuide = position;
        }

        trySpawn(level, wearer, capture.leftStart, capture.leftGuide, false, capture);
        trySpawn(level, wearer, capture.rightStart, capture.rightGuide, true, capture);
    }

    private static Vec3 ownerLocalToWorld(LivingEntity wearer, Vector3d local, float partialTick) {
        double entityX = Mth.lerp(partialTick, wearer.xOld, wearer.getX());
        double entityY = Mth.lerp(partialTick, wearer.yOld, wearer.getY());
        double entityZ = Mth.lerp(partialTick, wearer.zOld, wearer.getZ());

        float bodyYaw = Mth.rotLerp(partialTick, wearer.yBodyRotO, wearer.yBodyRot);

        double yaw = Math.toRadians(bodyYaw - 180.0F);
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);

        double worldX = entityX + local.x * cos - local.z * sin;
        double worldZ = entityZ + local.x * sin + local.z * cos;
        double worldY = entityY + 1.5D - local.y;

        return new Vec3(worldX, worldY, worldZ);
    }

    private static void trySpawn(ClientLevel level, LivingEntity wearer, Vec3 start, Vec3 guide, boolean right, ArmCapture capture) {
        if (start == null || guide == null) {
            return;
        }

        if (wearer.tickCount % EMISSION_INTERVAL_TICKS != 0) {
            return;
        }

        if (right ? capture.rightEmitted : capture.leftEmitted) {
            return;
        }

        Vec3 direction = guide.subtract(start);

        float bodyYaw = wearer.yBodyRot;
        double yaw = Math.toRadians(bodyYaw - 180.0F);

        double sideOffset = right ? NOZZLE_OUTWARD_OFFSET : -NOZZLE_OUTWARD_OFFSET;

        Vec3 shiftedStart = start.add(Math.cos(yaw) * sideOffset, 0.0D, Math.sin(yaw) * sideOffset);

        if (direction.lengthSqr() < 0.000001D) {
            return;
        }

        if (direction.lengthSqr() > 16.0D) {
            return;
        }

        if (right) {
            capture.rightEmitted = true;
        } else {
            capture.leftEmitted = true;
        }

        spawnJet(level, wearer, shiftedStart, direction.normalize(), right ? Math.PI : 0.0D);
    }

    private static final class ArmCapture {
        private int tick = Integer.MIN_VALUE;

        private Vec3 leftStart;
        private Vec3 leftGuide;
        private Vec3 rightStart;
        private Vec3 rightGuide;

        private boolean leftEmitted;
        private boolean rightEmitted;

        private void reset(int tick) {
            this.tick = tick;

            this.leftStart = null;
            this.leftGuide = null;
            this.rightStart = null;
            this.rightGuide = null;

            this.leftEmitted = false;
            this.rightEmitted = false;
        }
    }

    private static void spawnJet(ClientLevel level, LivingEntity wearer, Vec3 start, Vec3 baseDirection, double phaseOffset) {
        Vec3 direction = baseDirection;
        Vec3 wearerMotion = wearer.getDeltaMovement();
        Vec3 inheritedMotion = new Vec3(wearerMotion.x * HORIZONTAL_MOTION_INHERITANCE, wearerMotion.y * VERTICAL_MOTION_INHERITANCE, wearerMotion.z * HORIZONTAL_MOTION_INHERITANCE);
        Vec3 referenceUp = Math.abs(direction.y) > 0.92D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);

        Vec3 side = direction.cross(referenceUp);

        if (side.lengthSqr() < 0.000001D) {
            return;
        }

        side = side.normalize();

        Vec3 secondAxis = side.cross(direction).normalize();

        double cycle = 0.84D + 0.16D * Math.sin(wearer.tickCount * 0.22D + phaseOffset);
        double basePhase = wearer.tickCount * 0.76D + phaseOffset;

        for (int i = 0; i < CUSTOM_SPIRALS_PER_ARM; i++) {

            double angle = basePhase + i * (Math.PI * 2.0D / CUSTOM_SPIRALS_PER_ARM) + (level.random.nextDouble() - 0.5D) * 0.30D;
            double radius = (0.040D + level.random.nextDouble() * 0.060D) * cycle;

            Vec3 radial = side.scale(Math.cos(angle) * radius).add(secondAxis.scale(Math.sin(angle) * radius));
            Vec3 tangent = side.scale(-Math.sin(angle)).add(secondAxis.scale(Math.cos(angle)));

            Vec3 spawnPosition = start.add(radial);

            double speed = (0.040D + level.random.nextDouble() * 0.030D) * cycle;

            Vec3 velocity = direction.scale(speed).add(tangent.scale(0.012D + level.random.nextDouble() * 0.012D)).add(radial.scale(0.020D)).add(inheritedMotion);

            level.addParticle(Oasiso.ARM_SMOKE.get(), spawnPosition.x, spawnPosition.y, spawnPosition.z, velocity.x, velocity.y, velocity.z);
        }

        for (int i = 0; i < SOUL_PARTICLES_PER_ARM; i++) {

            double spreadX = (level.random.nextDouble() - 0.5D) * 0.022D;
            double spreadY = (level.random.nextDouble() - 0.5D) * 0.022D;
            double spreadZ = (level.random.nextDouble() - 0.5D) * 0.022D;

            Vec3 soulVelocity = direction.scale((0.035D + level.random.nextDouble() * 0.020D) * cycle).add(spreadX, spreadY, spreadZ).add(inheritedMotion);
            level.addParticle(ParticleTypes.SOUL, start.x, start.y, start.z, soulVelocity.x, soulVelocity.y, soulVelocity.z);
        }

        for (int i = 0; i < SOUL_FLAMES_PER_ARM; i++) {

            double angle = basePhase + i * (Math.PI * 2.0D / SOUL_FLAMES_PER_ARM);
            double radius = 0.025D + level.random.nextDouble() * 0.020D;

            Vec3 flameOffset = side.scale(Math.cos(angle) * radius).add(secondAxis.scale(Math.sin(angle) * radius));
            Vec3 flameVelocity = direction.scale(0.025D + level.random.nextDouble() * 0.020D).add(inheritedMotion);

            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, start.x + flameOffset.x, start.y + flameOffset.y, start.z + flameOffset.z, flameVelocity.x, flameVelocity.y, flameVelocity.z);
        }
    }
}
