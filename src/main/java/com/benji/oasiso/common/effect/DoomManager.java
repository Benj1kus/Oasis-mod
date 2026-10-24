package com.benji.oasiso.common.effect;

import com.benji.oasiso.common.entity.ApollyonEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public final class DoomManager {

    private static final String BOSS_TAG = "OasisoDoomBoss";

    private DoomManager() {
    }

    public static void bindPlayer(ServerPlayer player, ApollyonEntity boss) {
        if (!player.isAlive() || player.isSpectator() || !boss.isAlive() || boss.isDissolvingDeath() || boss.inCutscene())
            return;
        CompoundTag data = player.getPersistentData();

        data.putUUID(BOSS_TAG, boss.getUUID());

        ensureEffect(player);
    }

    public static void maintainPlayer(ServerPlayer player) {
        UUID bossId = getBoundBossId(player);

        if (bossId == null) {
            return;
        }
        ApollyonEntity boss = resolveBoss(player.getServer(), bossId);
        if (!player.isAlive() || boss == null || !boss.isAlive() || boss.isDissolvingDeath() || boss.level() != player.level() || boss.inCutscene()) {
            clearPlayer(player);
            return;
        }
        ensureEffect(player);
    }

    private static void ensureEffect(ServerPlayer player) {
        if (player.hasEffect(com.benji.oasiso.registry.ModEffects.DOOM.get())) {
            return;
        }
        player.addEffect(new MobEffectInstance(com.benji.oasiso.registry.ModEffects.DOOM.get(), MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
    }

    public static void releasePlayers(MinecraftServer server, UUID bossId) {
        if (server == null) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {

            UUID playerBossId = getBoundBossId(player);

            if (playerBossId == null || !playerBossId.equals(bossId)) {

                continue;
            }


            clearPlayer(player);
        }
    }

    private static void clearPlayer(ServerPlayer player) {
        player.removeEffect(com.benji.oasiso.registry.ModEffects.DOOM.get());

        player.getPersistentData().remove(BOSS_TAG);
    }

    public static UUID getBoundBossId(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();

        if (!data.hasUUID(BOSS_TAG)) {

            return null;
        }
        return data.getUUID(BOSS_TAG);
    }


    private static ApollyonEntity resolveBoss(MinecraftServer server, UUID bossId) {
        if (server == null) {
            return null;
        }

        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(bossId);
            if (entity instanceof ApollyonEntity apollyon) {

                return apollyon;
            }
        }


        return null;
    }
}