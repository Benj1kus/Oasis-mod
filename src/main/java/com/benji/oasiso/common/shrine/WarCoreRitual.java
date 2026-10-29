package com.benji.oasiso.common.shrine;

import com.benji.oasiso.common.entity.*;
import com.benji.oasiso.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class WarCoreRitual {
    public static final String GROUP = "OasisoShrineGroup";
    public static final int LEGACY_RADIUS = 16;
    public static final float ANGER_PITCH = .65F, HAPPINESS_PITCH = 1.5F, SADNESS_PITCH = 1F;

    public static int color(Mob mob) {
        return mob instanceof AngerEntity ? 0xFF354B : mob instanceof HappinessEntity ? 0xFFD34A : 0x60C8FF;
    }

    public static float pitch(Mob mob) {
        return mob instanceof AngerEntity ? ANGER_PITCH : mob instanceof HappinessEntity ? HAPPINESS_PITCH : SADNESS_PITCH;
    }

    public static boolean canUse(ServerPlayer player, ItemStack stack) {
        return !stack.isEmpty() && !player.isSpectator() && player.isAlive() && !(stack.hasTag() && stack.getTag().hasUUID(GROUP) && WarCoreDecisions.get(player.server).choice(stack.getTag().getUUID(GROUP)) != 0);
    }

    public static void complete(ServerPlayer player, ItemStack stack, boolean release) {
        if (!canUse(player, stack)) return;
        boolean legacy = !stack.hasTag() || !stack.getTag().hasUUID(GROUP);
        UUID group = legacy ? UUID.randomUUID() : stack.getTag().getUUID(GROUP);
        if (!WarCoreDecisions.get(player.server).decide(group, release ? 2 : 1)) return;
        if (legacy) {
            for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(LEGACY_RADIUS), m -> m.isAlive() && m instanceof ShrineSoul s && s.isShrineSoul() && !m.getPersistentData().hasUUID(GROUP) && m.distanceToSqr(player) <= LEGACY_RADIUS * LEGACY_RADIUS)) {
                mob.getPersistentData().putUUID(GROUP, group);
            }
        }
        stack.shrink(1);
        if (release) give(player, ModItems.COREREM.get());
        else {
            give(player, ModItems.AZUMALIT_UPGRADE.get());
            give(player, ModItems.AZUMALIT_SHARD.get());
            give(player, ModItems.AZUMALIT_PIECE.get());
            give(player, ModItems.NEPHRITIS_CORE.get());
            WarCoreNetwork.flash(player);
        }
    }

    private static void give(ServerPlayer player, Item item) {
        ItemStack reward = new ItemStack(item, 1);
        if (!player.getInventory().add(reward)) player.drop(reward, false);
    }

    public static boolean tickSoul(Mob mob) {
        if (!(mob instanceof ShrineSoul soul) || !soul.isShrineSoul() || !mob.isAlive()) return false;
        if (mob.level().isClientSide) return soul.isSoulDissolving();
        if (!soul.isSoulDissolving() && mob.getPersistentData().hasUUID(GROUP)) {
            int choice = WarCoreDecisions.get(((ServerLevel) mob.level()).getServer()).choice(mob.getPersistentData().getUUID(GROUP));
            if (choice == 2) {
                soul.releaseShrineSoul();
                return true;
            }
            if (choice == 1) soul.beginSoulDissolve();
        }
        if (!soul.isSoulDissolving()) return false;
        mob.getNavigation().stop();
        mob.setDeltaMovement(Vec3.ZERO);
        int age = soul.soulDissolveAge() + 1;
        if (age >= ShrineSoul.DISSOLVE_TICKS) mob.discard();
        else soul.setSoulDissolveAge(age);
        return true;
    }

    private WarCoreRitual() {
    }
}
