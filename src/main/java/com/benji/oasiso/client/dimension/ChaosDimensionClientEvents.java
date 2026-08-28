package com.benji.oasiso.client.dimension;

import com.benji.oasiso.Oasiso;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import com.benji.oasiso.common.entity.GasterEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.benji.oasiso.common.dimension.BossArenaEncounter;
import com.benji.oasiso.common.entity.AzumaalEntity;
import com.benji.oasiso.common.entity.BossPortalEntity;
import net.minecraftforge.fml.common.Mod;
import com.benji.oasiso.ModSounds;
import com.benji.oasiso.client.sound.ChaosDimensionAmbientSound;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ChaosDimensionClientEvents {

    private static ChaosDimensionAmbientSound ambientSound;
    private static final double GASTER_PAUSE_RADIUS = 40.0D;

    private static int nextEchoTimer;
    private static int lastMoonStage = 1;

    private static final double AZUMAAL_PAUSE_RADIUS = 96.0D;
    private static final double RETURN_PORTAL_RADIUS = 96.0D;

    private static boolean bossArenaPaused;
    private static boolean bossArenaDefeated;

    private static final RandomSource RANDOM = RandomSource.create();

    private static int dimensionTicks;

    private ChaosDimensionClientEvents() {
    }

    public static int getDimensionTicks() {
        return dimensionTicks;
    }

    public static int getMoonStage() {
        if (dimensionTicks < 200) {
            return 1;
        }
        return Math.min(6, 2 + (dimensionTicks - 200) / 100);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.isPaused()) {
            return;
        }

        if (!isInsideChaosDimension(minecraft)) {

            dimensionTicks = 0;
            nextEchoTimer = 0;
            lastMoonStage = 1;

            bossArenaPaused = false;
            bossArenaDefeated = false;

            stopAmbientSound();
            return;
        }

        updateBossArenaState(minecraft.level, minecraft.player);

        if (!hasNearbyGaster(minecraft.level, minecraft.player) && !bossArenaPaused) {
            dimensionTicks++;
        }

        ensureAmbientSound();
        if (dimensionTicks == 1) {
            nextEchoTimer = 120 + RANDOM.nextInt(241);
            lastMoonStage = getMoonStage();
        } else {
            handleMoonStageSound();
            handleRandomEcho();
        }
        spawnAtmosphere(minecraft.level, minecraft.player);
    }
    private static void updateBossArenaState(ClientLevel level, Player player) {
        boolean insideArena =
                BossArenaEncounter.isInsideAnyArenaArea(
                        player.getX(),
                        player.getZ()
                );

        boolean azumaalAlive = !level.getEntitiesOfClass(AzumaalEntity.class, player.getBoundingBox().inflate(AZUMAAL_PAUSE_RADIUS), azumaal -> azumaal.isAlive() && !azumaal.isClone()).isEmpty();
        boolean returnPortalExists = !level.getEntitiesOfClass(BossPortalEntity.class, player.getBoundingBox().inflate(RETURN_PORTAL_RADIUS), portal -> portal.isAlive() && portal.isChaosReturnPortal() && portal.getAnimState() != BossPortalEntity.STATE_DESPAWN).isEmpty();

        bossArenaPaused = insideArena || azumaalAlive;
        bossArenaDefeated = insideArena && returnPortalExists;
    }


    public static boolean isBossArenaDefeated() {
        return bossArenaDefeated;
    }

    private static void handleMoonStageSound() {
        int currentStage = getMoonStage();

        if (currentStage == lastMoonStage) {
            return;
        }

        if (currentStage >= 3) {
            playLocalSound(ModSounds.GOD_SCREAM.get(), 1.0F, 1.0F);
        }

        lastMoonStage = currentStage;
    }

    private static boolean hasNearbyGaster(ClientLevel level, Player player) {
        AABB area = player.getBoundingBox().inflate(GASTER_PAUSE_RADIUS);

        return !level.getEntitiesOfClass(GasterEntity.class, area, GasterEntity::isAlive).isEmpty();
    }

    private static void handleRandomEcho() {
        if (nextEchoTimer > 0) {
            nextEchoTimer--;
            return;
        }

        SoundEvent[] echoes = {ModSounds.ECHO1.get(), ModSounds.ECHO2.get(), ModSounds.ECHO3.get()};

        SoundEvent selectedEcho = echoes[RANDOM.nextInt(echoes.length)];

        playLocalSound(selectedEcho, 0.92F + RANDOM.nextFloat() * 0.16F, 0.75F);

        nextEchoTimer = 120 + RANDOM.nextInt(241);
    }

    private static void ensureAmbientSound() {
        Minecraft minecraft = Minecraft.getInstance();

        if (ambientSound != null && !ambientSound.isStopped()) {
            return;
        }

        ambientSound = new ChaosDimensionAmbientSound();

        minecraft.getSoundManager().play(ambientSound);
    }

    private static void stopAmbientSound() {
        if (ambientSound == null) {
            return;
        }

        Minecraft.getInstance().getSoundManager().stop(ambientSound);

        ambientSound = null;
    }

    private static void playLocalSound(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    private static boolean isInsideChaosDimension(Minecraft minecraft) {
        return minecraft.level != null && minecraft.player != null && minecraft.level.dimension().equals(Oasiso.CHAOS_DIMENSION);
    }

    private static void spawnAtmosphere(ClientLevel level, Player player) {
        for (int i = 0; i < 2; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0D;

            double distance = 3.0D + RANDOM.nextDouble() * 12.0D;

            double x = player.getX() + Math.cos(angle) * distance;
            double y = player.getY() - 4.0D + RANDOM.nextDouble() * 10.0D;
            double z = player.getZ() + Math.sin(angle) * distance;

            level.addParticle(ParticleTypes.ASH, x, y, z, (RANDOM.nextDouble() - 0.5D) * 0.006D, 0.002D + RANDOM.nextDouble() * 0.004D, (RANDOM.nextDouble() - 0.5D) * 0.006D);
        }

        if (RANDOM.nextInt(8) != 0) {
            return;
        }

        double angle = RANDOM.nextDouble() * Math.PI * 2.0D;

        double distance = 2.0D + RANDOM.nextDouble() * 9.0D;

        double x = player.getX() + Math.cos(angle) * distance;
        double y = player.getY() - 3.0D + RANDOM.nextDouble() * 8.0D;
        double z = player.getZ() + Math.sin(angle) * distance;

        level.addParticle(Oasiso.PURPLE_STARS.get(), x, y, z, (RANDOM.nextDouble() - 0.5D) * 0.015D, (RANDOM.nextDouble() - 0.5D) * 0.01D, (RANDOM.nextDouble() - 0.5D) * 0.015D);
    }
}