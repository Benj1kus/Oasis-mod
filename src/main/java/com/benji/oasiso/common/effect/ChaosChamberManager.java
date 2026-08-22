package com.benji.oasiso.common.effect;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.AzumaalEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public final class ChaosChamberManager {


    private static final double CAPTURE_RANGE = 96.0D;

    private static final String BOSS_TAG = "OasisoChaosChamberBoss";

    private ChaosChamberManager() {
    }

    public static void captureNearbyPlayers(ServerLevel level, AzumaalEntity boss) {
        if (boss.isClone()) {
            return;
        }

        double rangeSqr = CAPTURE_RANGE * CAPTURE_RANGE;

        for (ServerPlayer player : level.players()) {
            if (!isValidPlayer(player)) {
                continue;
            }

            if (boss.distanceToSqr(player) > rangeSqr) {
                continue;
            }

            bindPlayer(player, boss);
        }
    }

    private static void bindPlayer(ServerPlayer player, AzumaalEntity boss) {
        CompoundTag data = player.getPersistentData();

        data.putUUID(BOSS_TAG, boss.getUUID());
        ensureEffect(player);
    }

    public static void maintainPlayer(ServerPlayer player) {
        UUID bossId = getBoundBossId(player);
        if (bossId == null) {
            return;
        }

        AzumaalEntity boss = resolveBoss(player.getServer(), bossId);

        if (boss == null || boss.isClone() || !boss.isAlive()) {
            clearPlayer(player);
            return;
        }

        ensureEffect(player);
    }

    private static void ensureEffect(ServerPlayer player) {
        if (player.hasEffect(Oasiso.CHAOS_CHAMBER_EFFECT.get())) {
            return;
        }


        player.addEffect(new MobEffectInstance(Oasiso.CHAOS_CHAMBER_EFFECT.get(), MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
    }

    public static void releasePlayers(MinecraftServer server, UUID bossId) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {

            UUID playerBossId = getBoundBossId(player);

            if (playerBossId == null || !playerBossId.equals(bossId)) {
                continue;
            }

            clearPlayer(player);
        }
    }

    private static void clearPlayer(ServerPlayer player) {
        player.removeEffect(Oasiso.CHAOS_CHAMBER_EFFECT.get());
        player.getPersistentData().remove(BOSS_TAG);
    }

    public static boolean isRestricted(ServerPlayer player) {
        return player.hasEffect(Oasiso.CHAOS_CHAMBER_EFFECT.get());
    }

    private static UUID getBoundBossId(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.hasUUID(BOSS_TAG)) {
            return null;
        }
        return data.getUUID(BOSS_TAG);
    }

    private static AzumaalEntity resolveBoss(MinecraftServer server, UUID bossId) {
        if (server == null) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(bossId);
            if (entity instanceof AzumaalEntity boss) {
                return boss;
            }
        }
        return null;
    }

    private static boolean isValidPlayer(ServerPlayer player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator();
    }
}