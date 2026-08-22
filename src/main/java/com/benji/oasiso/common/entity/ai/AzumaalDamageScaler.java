package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.common.entity.AzumaalEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class AzumaalDamageScaler {

    private static final double COMBAT_SCAN_RANGE = 96.0D;

    private static final double FREE_TOUGHNESS = 8.0D;
    private static final double FREE_ARMOR = 20.0D;

    private static final double TOUGHNESS_BONUS_PER_POINT = 0.125D;

    private static final double ARMOR_BONUS_PER_POINT = 0.025D;
    private static final double ENCHANTMENT_BONUS_PER_POINT = 0.025D;

    private static final double MAX_DAMAGE_MULTIPLIER = 4.0D;
    private static final int MAX_PROTECTION_POINTS = 20;

    private AzumaalDamageScaler() {
    }
    public static float scaleDamage(ServerLevel level, AzumaalEntity boss, DamageSource source, float baseDamage) {
        double multiplier = findStrongestMultiplier(level, boss, source);
        return (float) (baseDamage * multiplier);
    }

    private static double findStrongestMultiplier(ServerLevel level, AzumaalEntity boss, DamageSource source) {
        double strongestMultiplier = 1.0D;
        double maxDistanceSqr = COMBAT_SCAN_RANGE * COMBAT_SCAN_RANGE;

        for (ServerPlayer player : level.players()) {
            if (!isValidCombatant(player)) {
                continue;
            }
            if (boss.distanceToSqr(player) > maxDistanceSqr) {
                continue;
            }
            double multiplier = getPlayerMultiplier(player, source);
            strongestMultiplier = Math.max(strongestMultiplier, multiplier);
        }
        return strongestMultiplier;
    }

    private static double getPlayerMultiplier(ServerPlayer player, DamageSource source) {

        double toughness = Math.max(0.0D, player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        double armor = Math.max(0.0D, player.getAttributeValue(Attributes.ARMOR));

        int protection = EnchantmentHelper.getDamageProtection(player.getArmorSlots(), source);
        protection = Math.max(0, Math.min(MAX_PROTECTION_POINTS, protection));

        double toughnessBonus = Math.max(0.0D, toughness - FREE_TOUGHNESS) * TOUGHNESS_BONUS_PER_POINT;
        double armorBonus = Math.max(0.0D, armor - FREE_ARMOR) * ARMOR_BONUS_PER_POINT;
        double enchantmentBonus = protection * ENCHANTMENT_BONUS_PER_POINT;

        double multiplier = 1.0D + toughnessBonus + armorBonus + enchantmentBonus;
        double finalMultiplier = Math.max(1.0D, Math.min(MAX_DAMAGE_MULTIPLIER, multiplier));

        return finalMultiplier;
    }


    private static boolean isValidCombatant(ServerPlayer player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator();
    }
}