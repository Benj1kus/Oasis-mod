package com.benji.oasiso.common.dimension;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ChaosPortalTeleporter {


    private static final int REQUIRED_PORTAL_TICKS = 60;

    private static final double DESTINATION_Y = 80.0D;

    private static final String PORTAL_TICKS_TAG =
            Oasiso.MODID + ":chaos_portal_ticks";

    private static final String LAST_PORTAL_TICK_TAG =
            Oasiso.MODID + ":chaos_portal_last_tick";

    private ChaosPortalTeleporter() {
    }

    public static void handleInside(
            Level level,
            BlockPos portalPos,
            Direction.Axis portalAxis,
            Entity entity
    ) {
        if (level.isClientSide) {
            return;
        }

        if (!(entity instanceof ServerPlayer player)) {
            return;
        }


        if (player.level().dimension()
                .equals(Oasiso.CHAOS_DIMENSION)) {
            return;
        }

        CompoundTag data =
                player.getPersistentData();

        long gameTime =
                level.getGameTime();

        long lastPortalTick =
                data.getLong(LAST_PORTAL_TICK_TAG);


        if (data.contains(
                LAST_PORTAL_TICK_TAG,
                Tag.TAG_LONG
        ) && lastPortalTick == gameTime) {
            return;
        }


        if (!data.contains(
                LAST_PORTAL_TICK_TAG,
                Tag.TAG_LONG
        ) || lastPortalTick < gameTime - 1L) {
            data.putInt(PORTAL_TICKS_TAG, 0);
        }

        data.putLong(
                LAST_PORTAL_TICK_TAG,
                gameTime
        );

        int portalTicks =
                data.getInt(PORTAL_TICKS_TAG) + 1;

        data.putInt(
                PORTAL_TICKS_TAG,
                portalTicks
        );

        if (portalTicks < REQUIRED_PORTAL_TICKS) {
            return;
        }

        teleportPlayer(
                player,
                portalPos,
                portalAxis
        );
    }

    private static void teleportPlayer(
            ServerPlayer player,
            BlockPos portalPos,
            Direction.Axis portalAxis
    ) {
        MinecraftServer server =
                player.getServer();

        if (server == null) {
            return;
        }

        ServerLevel destinationLevel =
                server.getLevel(
                        Oasiso.CHAOS_DIMENSION
                );

        if (destinationLevel == null) {
            return;
        }

        double destinationX =
                player.getX();

        double destinationZ =
                player.getZ();


        spawnPortalBurst(
                player.serverLevel(),
                player.getX(),
                player.getY()
                        + player.getBbHeight() * 0.5D,
                player.getZ()
        );

        playRandomEntropySound(
                player.serverLevel(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getRandom()
        );

        clearPortalProgress(player);

        player.stopRiding();


        ChaosReturnData.save(
                player,
                portalPos,
                portalAxis
        );

        player.teleportTo(
                destinationLevel,
                destinationX,
                DESTINATION_Y,
                destinationZ,
                player.getYRot(),
                player.getXRot()
        );

        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;


        spawnPortalBurst(
                destinationLevel,
                destinationX,
                DESTINATION_Y + 1.0D,
                destinationZ
        );

        playRandomEntropySound(
                destinationLevel,
                destinationX,
                DESTINATION_Y,
                destinationZ,
                player.getRandom()
        );
    }

    private static void clearPortalProgress(
            ServerPlayer player
    ) {
        CompoundTag data =
                player.getPersistentData();

        data.remove(PORTAL_TICKS_TAG);
        data.remove(LAST_PORTAL_TICK_TAG);
    }

    private static void spawnPortalBurst(
            ServerLevel level,
            double x,
            double y,
            double z
    ) {
        level.sendParticles(
                Oasiso.PURPLE_STARS.get(),
                x,
                y,
                z,
                70,
                0.9D,
                1.1D,
                0.9D,
                0.12D
        );
    }

    private static void playRandomEntropySound(
            ServerLevel level,
            double x,
            double y,
            double z,
            RandomSource random
    ) {
        SoundEvent[] sounds = {
                ModSounds.ENTROPY1.get(),
                ModSounds.ENTROPY2.get(),
                ModSounds.ENTROPY3.get()
        };

        SoundEvent sound =
                sounds[random.nextInt(sounds.length)];

        level.playSound(
                null,
                x,
                y,
                z,
                sound,
                SoundSource.AMBIENT,
                1.25F,
                0.85F + random.nextFloat() * 0.2F
        );
    }
}