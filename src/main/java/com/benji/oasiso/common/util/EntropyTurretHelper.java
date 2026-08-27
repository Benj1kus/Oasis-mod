package com.benji.oasiso.common.util;

import com.benji.oasiso.common.entity.SandGolemEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class EntropyTurretHelper {

    public static final double RANGE = 15.0D;

    private static final double MUZZLE_SIDE_OFFSET = 7.75D / 16.0D;
    private static final double MUZZLE_FORWARD_OFFSET = 0.08D;
    private static final double MUZZLE_HEIGHT = 1.72D;

    private EntropyTurretHelper() {
    }

    public enum Side {
        LEFT, RIGHT
    }

    public static TargetPair findTargets(Player player) {
        AABB search = player.getBoundingBox().inflate(RANGE);
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class, search, entity -> isValidEnemy(player, entity));

        LivingEntity nearest = null;
        LivingEntity secondNearest = null;

        double nearestDistance = RANGE * RANGE;
        double secondNearestDistance = RANGE * RANGE;

        for (LivingEntity entity : candidates) {
            double distanceSqr = player.distanceToSqr(entity);

            if (distanceSqr > RANGE * RANGE) {
                continue;
            }

            if (!player.hasLineOfSight(entity)) {
                continue;
            }

            if (nearest == null || distanceSqr < nearestDistance) {
                secondNearest = nearest;
                secondNearestDistance = nearestDistance;

                nearest = entity;
                nearestDistance = distanceSqr;
                continue;
            }

            if (entity != nearest && (secondNearest == null || distanceSqr < secondNearestDistance)) {
                secondNearest = entity;
                secondNearestDistance = distanceSqr;
            }
        }

        if (nearest == null) {
            return new TargetPair(null, null);
        }
        return new TargetPair(nearest, secondNearest != null ? secondNearest : nearest);
    }

    public static LivingEntity findTarget(Player player, Side side) {
        TargetPair targets = findTargets(player);
        return side == Side.LEFT ? targets.left() : targets.right();
    }

    private static boolean isValidEnemy(Player player, LivingEntity entity) {
        if (entity == player || !entity.isAlive()) {
            return false;
        }

        if (!(entity instanceof Enemy)) {
            return false;
        }

        if (entity.isAlliedTo(player)) {
            return false;
        }

        if (entity instanceof SandGolemEntity golem && golem.isPlayerCreated()) {
            return false;
        }

        return true;
    }

    public static Vec3 getMuzzlePosition(Player player, Side side) {
        Vec3 forward = getBodyForward(player);
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);

        double sideOffset = side == Side.LEFT ? -MUZZLE_SIDE_OFFSET : MUZZLE_SIDE_OFFSET;
        return player.position().add(0.0D, MUZZLE_HEIGHT, 0.0D).add(forward.scale(MUZZLE_FORWARD_OFFSET)).add(right.scale(sideOffset));
    }

    public static Vec3 getAimDirection(Vec3 muzzle, LivingEntity target) {
        Vec3 aimPoint = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ());

        Vec3 direction = aimPoint.subtract(muzzle);

        return direction.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
    }

    public static float getRelativeYawDegrees(Player player, LivingEntity target) {
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();

        float targetYaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;

        return Mth.wrapDegrees(targetYaw - player.yBodyRot);
    }

    public static float getPitchDegrees(Player player, LivingEntity target) {
        Vec3 muzzle = player.position().add(0.0D, MUZZLE_HEIGHT, 0.0D);

        double dx = target.getX() - muzzle.x;
        double dz = target.getZ() - muzzle.z;
        double dy = target.getY() + target.getBbHeight() * 0.55D - muzzle.y;

        double horizontal = Math.sqrt(dx * dx + dz * dz);

        return (float) -Math.toDegrees(Math.atan2(dy, horizontal));
    }

    private static Vec3 getBodyForward(Player player) {
        double yaw = Math.toRadians(player.yBodyRot);
        return new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
    }

    public record TargetPair(LivingEntity left, LivingEntity right) {
    }
}
