package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.common.entity.ApollyonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;


public final class ApollyonSpearAttack {
    public static final double START_RANGE = 10;
    public static final double BACKSTEP = 2;
    public static final double DASH_SPEED = 1.10;
    public static final double PUSH_SPEED = .32;
    public static final int UNARMED_INTERVAL = 20, ARMED_INTERVAL = 40;
    public static final TagKey<Item> RESIST_WEAPONS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("oasiso", "apollyon_resist_weapons"));
    private final ApollyonEntity mob;
    private Player victim;
    private int cooldown = 40, age, recovery, lastDamage = -1000;
    private boolean active, jumping, prepared, launched, caught, missed;
    private Vec3 retreat, dashAim, direction = Vec3.ZERO;

    public ApollyonSpearAttack(ApollyonEntity mob) {
        this.mob = mob;
    }

    public boolean tick() {
        if (!(mob.level() instanceof ServerLevel)) return false;
        if (!active) {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            if (mob.isTeleporting() || mob.isAttackControllingMovement() || mob.isPassenger() || mob.isVehicle())
                return false;
            if (!(mob.getTarget() instanceof Player player) || !valid(player) || !mob.hasLineOfSight(player) || horizontal(mob.position(), player.position()) > START_RANGE * START_RANGE || Math.abs(mob.getY() - player.getY()) > 12)
                return false;
            active = true;
            jumping = prepared = launched = caught = missed = false;
            victim = player;
            age = 0;
            lastDamage = -1000;
            mob.setAttackControlsMovement(true);
            mob.setCombatMode(1);
            mob.getNavigation().stop();
            mob.setDeltaMovement(Vec3.ZERO);
        }
        if (jumping) {
            mob.updateHover(.60, .24, .32);
            recovery++;
            if (recovery >= ApollyonAttackTimeline.JUMP_LENGTH && (mob.atHoverHeight(.45) || recovery > 80)) finish();
            return true;
        }
        if (!valid(victim) || victim.distanceToSqr(mob) > 24 * 24) {
            jump();
            return true;
        }
        mob.setTarget(victim);
        if (age >= ApollyonAttackTimeline.END) {
            jump();
            return true;
        }
        if (!prepared && age >= ApollyonAttackTimeline.PREPARE) {
            prepared = true;
            direction = flatDirection(mob.position(), victim.position());
            retreat = mob.position().subtract(direction.scale(BACKSTEP));
        }
        if (!launched && age >= ApollyonAttackTimeline.START) {
            launched = true;
            direction = flatDirection(mob.position(), victim.position());
            dashAim = victim.position().subtract(direction.scale(2.05)).add(0, .75, 0);
        }
        if (!prepared) mob.setDeltaMovement(Vec3.ZERO);
        else if (!launched) {
            Vec3 back = retreat.subtract(mob.position());
            mob.setDeltaMovement(limit(back, .34));
        } else if (caught) push();
        else if (!missed) dash();
        else mob.setDeltaMovement(Vec3.ZERO);
        age++;
        return true;
    }

    private void dash() {
        if (age - ApollyonAttackTimeline.START > 20 || mob.horizontalCollision) {
            missed = true;
            mob.setDeltaMovement(Vec3.ZERO);
            return;
        }

        Vec3 aim = dashAim;
        Vec3 motion = limit(aim.subtract(mob.position()), DASH_SPEED);
        mob.setDeltaMovement(motion);
        Vec3 tip = mob.position().add(direction.scale(2.3)).add(0, .20, 0);
        Vec3 next = tip.add(motion);
        var targetBox = victim.getBoundingBox().inflate(.60, .35, .60);
        var intersection = targetBox.clip(tip, next);
        boolean contact = targetBox.contains(tip) || intersection.isPresent();

        if (contact && clear(mob.position().add(0, .20, 0), tip) && clear(tip, victim.getEyePosition()) && clear(tip, intersection.orElse(tip))) {
            caught = true;
            mob.setPushedPlayerId(victim.getId());
            lastDamage = age;
            mob.setDeltaMovement(limit(aim.subtract(mob.position()), .45));
        } else if (motion.lengthSqr() < .001) {
            missed = true;
        }
    }

    private void push() {
        if (victim.distanceToSqr(mob) > 5 * 5 || !mob.hasLineOfSight(victim)) {
            caught = false;
            missed = true;
            mob.setPushedPlayerId(-1);
            mob.setDeltaMovement(Vec3.ZERO);
            return;
        }
        Vec3 old = victim.getDeltaMovement();
        victim.setDeltaMovement(direction.x * PUSH_SPEED, old.y, direction.z * PUSH_SPEED);
        victim.hurtMarked = true;
        Vec3 desired = victim.position().subtract(direction.scale(2.05)).add(0, .75, 0);
        Vec3 correction = desired.subtract(mob.position()).scale(.40);
        mob.setDeltaMovement(limit(correction.add(direction.scale(PUSH_SPEED)), .50));
        int interval = hasWeapon(victim) ? ARMED_INTERVAL : UNARMED_INTERVAL;
        if (age - lastDamage >= interval) damage();

    }

    private void damage() {
        victim.hurt(mob.damageSources().mobAttack(mob), (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
        lastDamage = age;
    }

    private boolean clear(Vec3 from, Vec3 to) {
        return mob.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob)).getType() == HitResult.Type.MISS;
    }

    private boolean valid(Player player) {
        return player != null && player.isAlive() && !player.isSpectator() && !player.isCreative() && player.level() == mob.level() && (!(player instanceof ServerPlayer p) || !p.hasDisconnected());
    }

    private void jump() {
        jumping = true;
        recovery = 0;
        caught = false;
        mob.setPushedPlayerId(-1);
        mob.setCombatMode(2);
        mob.resetHover();
        mob.setDeltaMovement(0, .45, 0);
    }

    private void finish() {
        active = false;
        jumping = false;
        victim = null;
        mob.setCombatMode(0);
        mob.setPushedPlayerId(-1);
        mob.setAttackControlsMovement(false);
        cooldown = 60 + mob.getRandom().nextInt(41);
    }

    public void cancel() {
        if (active) finish();
    }

    public static boolean hasWeapon(Player player) {
        return weapon(player.getMainHandItem()) || weapon(player.getOffhandItem());
    }

    private static boolean weapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return stack.is(RESIST_WEAPONS) || item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem || item instanceof ProjectileWeaponItem || stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).stream().anyMatch(modifier -> modifier.getAmount() > 0);
    }

    private static Vec3 limit(Vec3 v, double speed) {
        return v.lengthSqr() > speed * speed ? v.normalize().scale(speed) : v;
    }

    private static Vec3 flatDirection(Vec3 from, Vec3 to) {
        Vec3 v = new Vec3(to.x - from.x, 0, to.z - from.z);
        return v.lengthSqr() < 1E-8 ? new Vec3(0, 0, 1) : v.normalize();
    }

    private static double horizontal(Vec3 a, Vec3 b) {
        double x = a.x - b.x, z = a.z - b.z;
        return x * x + z * z;
    }
}
