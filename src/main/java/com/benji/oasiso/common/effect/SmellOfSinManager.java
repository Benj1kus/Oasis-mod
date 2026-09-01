package com.benji.oasiso.common.effect;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.PaladinEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public final class SmellOfSinManager {

    private static final String BOSS_TAG = "OasisoSmellOfSinBoss";
    private SmellOfSinManager() {
    }

    public static void bindPlayer(ServerPlayer player, PaladinEntity boss) {
        CompoundTag data = player.getPersistentData();

        data.putUUID(BOSS_TAG, boss.getUUID());

        ensureEffect(player);
    }
    public static void maintainPlayer(ServerPlayer player) {
        UUID bossId = getBoundBossId(player);

        if (bossId == null) {
            return;
        }
        PaladinEntity boss = resolveBoss(player.getServer(), bossId);
        if (boss == null || !boss.isAlive() || boss.isDeathSequenceActive()) {
            clearPlayer(player);
            return;
        }
        ensureEffect(player);
    }

    private static void ensureEffect(ServerPlayer player) {
        if (player.hasEffect(Oasiso.SMELL_OF_SIN_EFFECT.get())) {
            return;
        }
        player.addEffect(new MobEffectInstance(Oasiso.SMELL_OF_SIN_EFFECT.get(),
                MobEffectInstance.INFINITE_DURATION,
                0,
                false,
                false,
                true));
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
        player.removeEffect(Oasiso.SMELL_OF_SIN_EFFECT.get());

        player.getPersistentData().remove(BOSS_TAG);
    }

    private static UUID getBoundBossId(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();

        if (!data.hasUUID(BOSS_TAG)) {

            return null;
        }
        return data.getUUID(BOSS_TAG);
    }


    private static PaladinEntity resolveBoss(MinecraftServer server, UUID bossId) {
        if (server == null) {
            return null;
        }

        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(bossId);
            if (entity instanceof PaladinEntity paladin) {

                return paladin;
            }
        }


        return null;
    }
}