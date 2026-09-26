package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ApollyonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;


public final class ApollyonDrillAttack {
    public static final double SPEED = 0.80;
    public static final double DAMAGE_RADIUS = 5;
    public static final int DAMAGE_INTERVAL = 10;
    private final ApollyonEntity mob;
    private Player victim;
    private boolean active, launched, recovering, cruising;
    private int age, recovery;
    private Vec3 direction = Vec3.ZERO, flat = Vec3.ZERO;
    private double targetY;

    public ApollyonDrillAttack(ApollyonEntity mob) {
        this.mob = mob;
    }

    public boolean isActive() {
        return active;
    }

    public boolean start(Player target) {
        if (active || !ApollyonCombatController.valid(mob, target)) return false;
        active = true;
        launched = recovering = cruising = false;
        age = recovery = 0;
        victim = target;
        mob.getNavigation().stop();
        mob.setAttackControlsMovement(true);
        mob.setCombatMode(5);
        mob.setDeltaMovement(Vec3.ZERO);
        Vec3 axis = target.position().add(0, .75, 0).subtract(mob.position()).normalize();
        mob.beginDrill(axis.lengthSqr() < 1E-8 ? new Vec3(0, -.3, 1).normalize() : axis);
        return true;
    }

    public void tick() {
        if (!active) return;
        if (recovering) {
            mob.updateHover(.50, .17, .25);
            if (++recovery > 100 || (recovery >= 12 && mob.atHoverHeight(.4))) cancel();
            return;
        }
        if (age >= ApollyonAttackTimeline.DRILL_END) {
            recover();
            return;
        }
        if (!launched) {
            if (!ApollyonCombatController.valid(mob, victim) || victim.distanceToSqr(mob) > 32 * 32) {
                recover();
                return;
            }
            mob.setTarget(victim);
            if (age >= ApollyonAttackTimeline.DRILL_START) launch();
            else {
                mob.setDeltaMovement(Vec3.ZERO);
                Vec3 axis = victim.position().add(0, .75, 0).subtract(mob.position()).normalize();
                if (axis.lengthSqr() > 1E-8) mob.setDrillAxis(axis);
            }
        }
        if (launched) {
            moveForward();
            if ((age - ApollyonAttackTimeline.DRILL_START) % DAMAGE_INTERVAL == 0) damageNearby();
            if (age % 2 == 0) floorDebris();
        }
        if (age % (launched ? 4 : 8) == 0) lightning();
        age++;
    }

    private void launch() {
        launched = true;
        Vec3 aim = victim.position().add(0, .75, 0);
        targetY = aim.y;
        direction = aim.subtract(mob.position()).normalize();
        flat = ApollyonCombatController.flatDirection(mob.position(), aim);
        if (direction.lengthSqr() < 1E-8) direction = flat;
        mob.setDrillAxis(direction);
        float yaw = (float) (Math.atan2(flat.z, flat.x) * 180 / Math.PI) - 90;
        mob.setYRot(yaw);
        mob.yBodyRot = mob.yHeadRot = yaw;
    }

    private void moveForward() {
        if (!cruising && (Math.abs(mob.getY() - targetY) < .45 || (direction.y < 0 && mob.getY() <= targetY) || (direction.y > 0 && mob.getY() >= targetY) || mob.verticalCollision))
            cruising = true;
        Vec3 motion = direction.scale(SPEED);
        if (cruising) {
            double x = mob.getX() + flat.x * SPEED, z = mob.getZ() + flat.z * SPEED;
            var hit = mob.level().clip(new ClipContext(new Vec3(x, mob.getY() + 1, z), new Vec3(x, mob.getY() - 3, z), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
            double dy = 0;
            if (hit.getType() == HitResult.Type.BLOCK && hit.getDirection() == Direction.UP)
                dy = Mth.clamp(hit.getLocation().y + .45 - mob.getY(), -.35, .35);
            motion = new Vec3(flat.x * SPEED, dy, flat.z * SPEED);
        }
        mob.setDeltaMovement(motion);
    }

    private void damageNearby() {
        if (!(mob.level() instanceof ServerLevel level)) return;
        for (Player player : level.players()) {
            if (!player.isAlive() || player.isCreative() || player.isSpectator()) continue;
            if (player.distanceToSqr(mob) > DAMAGE_RADIUS * DAMAGE_RADIUS || !mob.hasLineOfSight(player)) continue;
            player.hurt(mob.damageSources().mobAttack(mob), (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
    }

    private void lightning() {
        if (!(mob.level() instanceof ServerLevel level)) return;
        var r = mob.getRandom();
        for (int i = 0; i < (launched ? 2 : 1); i++) {
            double angle = r.nextDouble() * Math.PI * 2;
            level.sendParticles(Oasiso.ENTROPY_LIGHTNING.get(), mob.getX() + Math.cos(angle) * 1.1, mob.getY() + .5 + r.nextDouble() * 1.2, mob.getZ() + Math.sin(angle) * 1.1, 1, 0, 0, 0, 0);
        }
    }

    private void floorDebris() {
        if (!(mob.level() instanceof ServerLevel level)) return;
        var r = mob.getRandom();
        for (int i = 0; i < 3; i++) {
            double x = mob.getX() + (r.nextDouble() - .5) * 1.8;
            double z = mob.getZ() + (r.nextDouble() - .5) * 1.8;
            var hit = level.clip(new ClipContext(new Vec3(x, mob.getY() + .2, z), new Vec3(x, mob.getY() - 2.5, z), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
            if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) continue;
            BlockPos pos = hit.getBlockPos();
            var state = level.getBlockState(pos);
            if (state.isAir()) continue;
            Vec3 p = hit.getLocation();
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), p.x, p.y + .12, p.z, 5, .25, .12, .25, .12);
        }
    }

    private void recover() {
        recovering = true;
        recovery = 0;
        mob.setCombatMode(0);
        mob.resetHover();
        mob.setDeltaMovement(Vec3.ZERO);
    }

    public void cancel() {
        if (!active) return;
        active = recovering = false;
        victim = null;
        mob.setCombatMode(0);
        mob.setAttackControlsMovement(false);
        mob.setDeltaMovement(Vec3.ZERO);
    }
}
