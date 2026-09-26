package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ApollyonEntity;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ApollyonShockwaveAttack {
    public static final double LOW_HEIGHT = 1;
    public static final int WAIT_TICKS = 40;
    public static final double JUMP_RISE = 10;
    public static final double WAVE_RADIUS = 8;
    public static final int WAVE_TICKS = 44;
    public static final int LIGHTNING_COUNT = 24;
    private final ApollyonEntity mob;
    private Player victim;
    private boolean active;
    private int phase, age, total;
    private double floorY, launchY;

    public ApollyonShockwaveAttack(ApollyonEntity mob) {
        this.mob = mob;
    }

    public boolean isActive() {
        return active;
    }

    public boolean start(Player player) {
        if (active || !ApollyonCombatController.valid(mob, player)) return false;
        double floor = floorBelow();
        if (!Double.isFinite(floor)) return false;
        floorY = floor;
        victim = player;
        active = true;
        phase = age = total = 0;
        mob.getNavigation().stop();
        mob.setAttackControlsMovement(true);
        mob.setCombatMode(0);
        mob.setDeltaMovement(Vec3.ZERO);
        return true;
    }

    public void tick() {
        if (!active) return;
        if (phase < 2 && (!ApollyonCombatController.valid(mob, victim) || victim.distanceToSqr(mob) > 40 * 40)) {
            recover();
            return;
        }
        if (victim != null && ApollyonCombatController.valid(mob, victim)) mob.setTarget(victim);
        if (phase < 2) {
            if (++total > 200) {
                recover();
                return;
            }
            if (total % 4 == 0) {
                double floor = floorBelow();
                if (!Double.isFinite(floor)) {
                    recover();
                    return;
                }
                floorY = floor;
            }
            double dy = floorY + LOW_HEIGHT - mob.getY();
            if (Math.abs(dy) > .08) {
                phase = 0;
                age = 0;
                mob.setDeltaMovement(0, Mth.clamp(dy * .28, -.48, .48), 0);
                return;
            }
            mob.setDeltaMovement(0, dy, 0);
            if (phase == 0) {
                phase = 1;
                age = 0;
                return;
            }
            if (++age >= WAIT_TICKS) launch();
            return;
        }
        if (phase == 2) {
            int duration = Math.max(1, ApollyonAttackTimeline.JUMP_LENGTH);
            double t = Math.min(1, (age + 1.0) / duration);
            double ease = 1 - Math.pow(1 - t, 3);
            double desired = launchY + JUMP_RISE * ease;
            mob.setDeltaMovement(0, Mth.clamp(desired - mob.getY(), 0, 1.5), 0);
            if ((age > 1 && mob.verticalCollision) || age >= duration) {
                recover();
                return;
            }
            age++;
            return;
        }
        mob.updateHover(.38, .12, .22);
        if (++age > 120 || (age >= 12 && mob.atHoverHeight(.35))) cancel();
    }

    private void launch() {
        phase = 2;
        age = 0;
        launchY = mob.getY();
        mob.setCombatMode(2);
        Vec3 origin = new Vec3(mob.getX(), floorY + .06, mob.getZ());
        mob.beginShockwave(origin);
        spawnLightning(origin);
        launchPlayers(origin);
        double first = 1 - Math.pow(1 - 1.0 / Math.max(1, ApollyonAttackTimeline.JUMP_LENGTH), 3);
        mob.setDeltaMovement(0, Math.min(1.5, JUMP_RISE * first), 0);
        age = 1;
    }

    private double floorBelow() {
        var hit = mob.level().clip(new ClipContext(mob.position().add(0, .5, 0), new Vec3(mob.getX(), mob.level().getMinBuildHeight(), mob.getZ()), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
        return hit.getType() == HitResult.Type.BLOCK && hit.getDirection() == Direction.UP ? hit.getLocation().y : Double.NaN;
    }

    private void spawnLightning(Vec3 origin) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        var random = mob.getRandom();
        for (int i = 0; i < LIGHTNING_COUNT; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = Math.sqrt(random.nextDouble()) * WAVE_RADIUS;
            double x = origin.x + Math.cos(angle) * radius;
            double z = origin.z + Math.sin(angle) * radius;
            var hit = level.clip(new ClipContext(new Vec3(x, origin.y + 2, z), new Vec3(x, origin.y - 4, z), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
            if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) continue;
            Vec3 p = hit.getLocation();
            level.sendParticles(Oasiso.ENTROPY_LIGHTNING.get(), p.x, p.y + .45, p.z, 1, 0, 0, 0, 0);
        }
    }

    private void launchPlayers(Vec3 origin) {
        if (!(mob.level() instanceof ServerLevel level)) return;

        for (Player player : level.players()) {
            if (!player.isAlive() || player.isCreative() || player.isSpectator() || player.isPassenger()) continue;

            double dx = player.getX() - origin.x;
            double dz = player.getZ() - origin.z;

            if (dx * dx + dz * dz > WAVE_RADIUS * WAVE_RADIUS) continue;

            if (Math.abs(player.getY() - origin.y) > 2.5) continue;

            if (!mob.hasLineOfSight(player)) continue;

            Vec3 motion = player.getDeltaMovement();

            player.setDeltaMovement(motion.x, 1.85, motion.z);
            player.fallDistance = 0;
            player.hurtMarked = true;
        }
    }

    private void recover() {
        phase = 3;
        age = 0;
        mob.setCombatMode(0);
        mob.resetHover();
        mob.setDeltaMovement(Vec3.ZERO);
    }

    public void cancel() {
        if (!active) return;
        active = false;
        victim = null;
        mob.setCombatMode(0);
        mob.setAttackControlsMovement(false);
        mob.setDeltaMovement(Vec3.ZERO);
    }
}
