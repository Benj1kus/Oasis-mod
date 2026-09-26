package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.common.entity.ApollyonEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ApollyonCommonAttack {
    public static final double BACKSTEP = 2.0;
    public static final double DASH_SPEED = 1.45;
    private final ApollyonEntity mob;
    private Player victim;
    private boolean active, recovering, prepared, launched, hit, missed;
    private int age, recovery;
    private Vec3 retreat, aim, direction = Vec3.ZERO;

    public ApollyonCommonAttack(ApollyonEntity mob) {
        this.mob = mob;
    }

    public boolean isActive() {
        return active;
    }

    public boolean start(Player player) {
        if (active || !ApollyonCombatController.valid(mob, player)) return false;
        victim = player;
        active = true;
        recovering = prepared = launched = hit = missed = false;
        age = recovery = 0;
        mob.getNavigation().stop();
        mob.setAttackControlsMovement(true);
        mob.setCombatMode(4);
        mob.setDeltaMovement(Vec3.ZERO);
        return true;
    }

    public void tick() {
        if (!active) return;
        if (recovering) {
            mob.updateHover(.60, .24, .32);
            if (++recovery > 80 || (recovery >= 8 && mob.atHoverHeight(.45))) cancel();
            return;
        }
        if (!ApollyonCombatController.valid(mob, victim) || victim.distanceToSqr(mob) > 24 * 24 || age >= ApollyonAttackTimeline.COMMON_LENGTH) {
            recover();
            return;
        }
        mob.setTarget(victim);
        if (!prepared && age >= ApollyonAttackTimeline.COMMON_PREPARE) {
            prepared = true;
            direction = ApollyonCombatController.flatDirection(mob.position(), victim.position());
            retreat = mob.position().subtract(direction.scale(BACKSTEP));
        }
        if (!launched && age >= ApollyonAttackTimeline.COMMON_HIT) {
            launched = true;
            direction = ApollyonCombatController.flatDirection(mob.position(), victim.position());
            aim = victim.position().subtract(direction.scale(2.05)).add(0, .75, 0);
        }
        if (!prepared || hit || missed) mob.setDeltaMovement(Vec3.ZERO);
        else if (!launched) mob.setDeltaMovement(ApollyonCombatController.limit(retreat.subtract(mob.position()), .34));
        else dash();
        age++;
    }

    private void dash() {
        if (mob.horizontalCollision) {
            missed = true;
            mob.setDeltaMovement(Vec3.ZERO);
            return;
        }
        Vec3 motion = ApollyonCombatController.limit(aim.subtract(mob.position()), DASH_SPEED);
        Vec3 tip = mob.position().add(direction.scale(2.3)).add(0, .20, 0);
        var box = victim.getBoundingBox().inflate(.60, .35, .60);
        var intersection = box.clip(tip, tip.add(motion));
        boolean contact = box.contains(tip) || intersection.isPresent();
        if (contact && clear(mob.position().add(0, .20, 0), tip) && clear(tip, victim.getEyePosition()) && clear(tip, intersection.orElse(tip))) {
            hit = true;
            victim.hurt(mob.damageSources().mobAttack(mob), (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
            mob.setDeltaMovement(Vec3.ZERO);
        } else {
            mob.setDeltaMovement(motion);
            if (motion.lengthSqr() < .001) missed = true;
        }
    }

    private boolean clear(Vec3 from, Vec3 to) {
        return mob.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob)).getType() == HitResult.Type.MISS;
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
