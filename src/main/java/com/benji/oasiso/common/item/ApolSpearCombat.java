package com.benji.oasiso.common.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoItem;

import java.util.Comparator;

public final class ApolSpearCombat {
    public static final int SWING_TICKS = 21, MAX_TARGETS = 5;
    public static final double CLEAVE_REACH = 5.0, CLEAVE_RADIUS = 2.5;
    public static final double PUSH_SPEED = 1.85, PUSH_UP = 0.40;
    private static final String LAST = "OasisoApolSpearSwing", DIM = "OasisoApolSpearDimension";
    private static final String HIT = "OasisoApolSpearHit", POWER = "OasisoApolSpearPower";

    private ApolSpearCombat() {
    }

    public static boolean begin(Player player, ItemStack stack, ApolSpearItem item, boolean hit) {
        if (!(player.level() instanceof ServerLevel level) || player.getMainHandItem() != stack) return false;
        var data = player.getPersistentData();
        long now = level.getGameTime();
        String dimension = level.dimension().location().toString();
        if (data.contains(LAST) && dimension.equals(data.getString(DIM))) {
            long elapsed = now - data.getLong(LAST);
            if (elapsed >= 0 && elapsed < SWING_TICKS) return false;
        }
        data.putLong(LAST, now);
        data.putString(DIM, dimension);
        data.putLong(HIT, hit ? now : Long.MIN_VALUE);
        data.putFloat(POWER, player.getAttackStrengthScale(.5F));
        item.triggerAnim(player, GeoItem.getOrAssignId(stack, level), "controller", "attack");
        return true;
    }

    public static void hit(Player player, LivingEntity primary, ItemStack stack) {
        knockback(player, primary, stack);
        var data = player.getPersistentData();
        long now = player.level().getGameTime();
        if (!data.contains(HIT) || data.getLong(HIT) != now) return;
        data.putLong(HIT, Long.MIN_VALUE);
        float strength = data.getFloat(POWER);
        float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * (.2F + .8F * strength * strength);
        int fire = EnchantmentHelper.getFireAspect(player);
        var nearby = player.level().getEntitiesOfClass(Mob.class, primary.getBoundingBox().inflate(CLEAVE_RADIUS), target -> target != primary && target.isAlive() && !target.isInvulnerable() && !target.isAlliedTo(player) && !player.isAlliedTo(target) && target.distanceToSqr(primary) <= CLEAVE_RADIUS * CLEAVE_RADIUS && target.distanceToSqr(player) <= CLEAVE_REACH * CLEAVE_REACH && inFront(player.getLookAngle(), target.position().subtract(player.position())) && player.hasLineOfSight(target));
        nearby.sort(Comparator.comparingDouble(target -> target.distanceToSqr(primary)));
        int count = Math.min(MAX_TARGETS - 1, nearby.size());
        for (int i = 0; i < count; i++) {
            Mob target = nearby.get(i);
            float damage = base * (1 + EnchantmentHelper.getSweepingDamageRatio(player)) + EnchantmentHelper.getDamageBonus(stack, target.getMobType()) * strength;
            if (target.hurt(player.damageSources().playerAttack(player), damage)) {
                knockback(player, target, stack);
                if (fire > 0) target.setSecondsOnFire(fire * 4);
                EnchantmentHelper.doPostHurtEffects(target, player);
                EnchantmentHelper.doPostDamageEffects(player, target);
            }
        }
    }

    static boolean inFront(Vec3 look, Vec3 offset) {
        Vec3 flatLook = new Vec3(look.x, 0, look.z), flatOffset = new Vec3(offset.x, 0, offset.z);
        if (flatOffset.lengthSqr() < 1e-8) return true;
        if (flatLook.lengthSqr() < 1e-8) return false;
        return flatLook.normalize().dot(flatOffset.normalize()) >= .2;
    }

    private static void knockback(Player player, LivingEntity target, ItemStack stack) {
        if (!target.isAlive()) return;
        double resistance = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        double factor = Math.max(0, 1 - resistance);
        if (factor == 0) return;
        Vec3 delta = target.position().subtract(player.position());
        Vec3 direction = new Vec3(delta.x, 0, delta.z);
        if (direction.lengthSqr() < 1e-8) {
            Vec3 look = player.getLookAngle();
            direction = new Vec3(look.x, 0, look.z);
        }
        if (direction.lengthSqr() < 1e-8) direction = new Vec3(0, 0, 1);
        double speed = (PUSH_SPEED + .15 * EnchantmentHelper.getKnockbackBonus(player)) * factor;
        Vec3 push = direction.normalize().scale(speed);
        target.setDeltaMovement(push.x, PUSH_UP * factor, push.z);
        target.hasImpulse = true;
        target.hurtMarked = true;
    }
}
