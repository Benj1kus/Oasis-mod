package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.common.entity.ApollyonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Только сервер: выбор цели/места и скрытый перенос в середине визуального эффекта. */
public final class ApollyonTeleportController {
    public static final double TRIGGER_DISTANCE = 10; // По горизонтали: парение не считается.
    public static final double MIN_RADIUS = 4;        // Насколько близко появится к игроку.
    public static final double MAX_RADIUS = 7;
    public static final int COOLDOWN = 40;            // 2 секунды после завершения.
    private final ApollyonEntity mob;
    private int cooldown = 20;
    private Vec3 destination;
    private boolean moved;

    public ApollyonTeleportController(ApollyonEntity mob) { this.mob = mob; }

    /** true: обычное управление высотой/движением в этот тик нужно пропустить. */
    public boolean tick() {
        if (!(mob.level() instanceof ServerLevel level)) return false;
        if (mob.isTeleporting()) {
            mob.getNavigation().stop();
            mob.setDeltaMovement(Vec3.ZERO);
            float age = mob.teleportAge(0);
            if (!moved && age >= ApollyonEntity.TELEPORT_OUT) {
                Player target = target();
                // За время распада игрок мог уйти, а место могли застроить.
                if (target == null) destination = null;
                else if (destination == null || horizontalDistanceSqr(destination, target.position()) > 81
                        || !isSafe(level, destination, target)) destination = findDestination(level, target);
                if (destination != null) {
                    mob.teleportTo(destination.x, destination.y, destination.z);
                    mob.setDeltaMovement(Vec3.ZERO);
                    mob.fallDistance = 0;
                }
                // При отмене собирается обратно на старой позиции, не застревает невидимым.
                moved = true;
            }
            if (age >= ApollyonEntity.TELEPORT_TOTAL) {
                mob.endTeleport();
                destination = null;
                cooldown = COOLDOWN;
            }
            return true;
        }
        if (cooldown > 0) { cooldown--; return false; }
        if (mob.isAttackControllingMovement() || mob.isNoAi() || mob.isPassenger() || mob.isVehicle()) return false;
        if (mob.tickCount % 5 != 0) return false;
        Player target = target();
        if (target == null || horizontalDistanceSqr(mob.position(), target.position()) < TRIGGER_DISTANCE*TRIGGER_DISTANCE)
            return false;
        destination = findDestination(level, target);
        if (destination == null) { cooldown = 20; return false; }
        moved = false;
        mob.beginTeleport();
        return true;
    }

    private Player target() {
        if (mob.getTarget() instanceof Player player && valid(player)
                && player.distanceToSqr(mob) <= 64*64) return player;
        // Обычный FollowRange — 30. Не теряем убегающего игрока сразу за этой границей.
        Player best = null;
        double distance = 64*64;
        for (Player player : mob.level().players()) {
            double d = player.distanceToSqr(mob);
            if (valid(player) && d < distance) { distance = d; best = player; }
        }
        if (best != null) mob.setTarget(best);
        return best;
    }

    private boolean valid(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.isCreative()
                && player.level() == mob.level() && mob.canAttack(player);
    }

    private Vec3 findDestination(ServerLevel level, Player player) {
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = mob.getRandom().nextDouble()*Math.PI*2;
            double radius = MIN_RADIUS + mob.getRandom().nextDouble()*(MAX_RADIUS-MIN_RADIUS);
            double x = player.getX()+Math.cos(angle)*radius;
            double z = player.getZ()+Math.sin(angle)*radius;
            Vec3 from = new Vec3(x, Math.min(level.getMaxBuildHeight()-1, player.getY()+4), z);
            Vec3 to = new Vec3(x, Math.max(level.getMinBuildHeight(), player.getY()-24), z);
            // Не загружаем новые чанки ради поиска позиции.
            if (!level.hasChunkAt(BlockPos.containing(from))) continue;
            var hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
            if (hit.getType() != HitResult.Type.BLOCK) continue;
            Vec3 candidate = new Vec3(x, hit.getLocation().y+ApollyonEntity.HOVER_HEIGHT, z);
            if (isSafe(level, candidate, player)) return candidate;
        }
        return null;
    }

    private boolean isSafe(ServerLevel level, Vec3 candidate, Player player) {
        AABB bounds = mob.getBoundingBox().move(candidate.subtract(mob.position())).inflate(.2);
        if (bounds.minY < level.getMinBuildHeight() || bounds.maxY >= level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(bounds)
                || !level.hasChunksAt(BlockPos.containing(bounds.minX,bounds.minY,bounds.minZ),
                                     BlockPos.containing(bounds.maxX,bounds.maxY,bounds.maxZ))
                || !level.noCollision(mob,bounds) || level.containsAnyLiquid(bounds)) return false;
        // Проверяем и чанки всего луча видимости, не только места появления.
        if (!level.hasChunksAt(BlockPos.containing(Math.min(candidate.x,player.getX()),bounds.minY,
                                                   Math.min(candidate.z,player.getZ())),
                               BlockPos.containing(Math.max(candidate.x,player.getX()),bounds.maxY,
                                                   Math.max(candidate.z,player.getZ())))) return false;
        // Не переносим за стену/на крышу над игроком: из нового места он должен быть виден.
        Vec3 eye = candidate.add(0,mob.getEyeHeight(),0);
        return level.clip(new ClipContext(eye, player.getEyePosition(), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, mob)).getType() == HitResult.Type.MISS;
    }

    private static double horizontalDistanceSqr(Vec3 a, Vec3 b) {
        double x = a.x-b.x, z = a.z-b.z;
        return x*x+z*z;
    }
}
