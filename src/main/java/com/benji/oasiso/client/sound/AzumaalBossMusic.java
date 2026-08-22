package com.benji.oasiso.client.sound;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AzumaalBossMusic {

    // 61 = 61 sec  = 1:01 minutes
    private static final int INTRO_TICKS = 61 * 20;
    private static MusicStage stage = MusicStage.NONE;
    private static int stageTicks;
    private static SimpleSoundInstance currentSound;


    private AzumaalBossMusic() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {

            return;
        }


        Minecraft minecraft = Minecraft.getInstance();


        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.level == null) {
            stopMusic(minecraft);
            return;
        }

        boolean chamberActive = player.hasEffect(Oasiso.CHAOS_CHAMBER_EFFECT.get());

        if (!chamberActive) {
            if (stage != MusicStage.NONE) {
                stopMusic(minecraft);
            }
            return;
        }

        if (stage == MusicStage.NONE) {
            startIntro(minecraft);
            return;
        }

        if (minecraft.isPaused()) {
            return;
        }

        if (stage == MusicStage.INTRO) {
            stageTicks++;
            if (stageTicks >= INTRO_TICKS) {
                startLoop(minecraft);
            }
        }
    }

    private static void startIntro(Minecraft minecraft) {
        stopCurrentSound(minecraft);
        minecraft.getMusicManager().stopPlaying();

        currentSound = createGlobalSound(ModSounds.AZUMAAL_SONG.get(), false);

        minecraft.getSoundManager().play(currentSound);
        stage = MusicStage.INTRO;
        stageTicks = 0;
    }

    private static void startLoop(Minecraft minecraft) {
        stopCurrentSound(minecraft);
        currentSound = createGlobalSound(ModSounds.AZUMAAL_LOOPED.get(), true);

        minecraft.getSoundManager().play(currentSound);
        stage = MusicStage.LOOP;
        stageTicks = 0;
    }

    private static SimpleSoundInstance createGlobalSound(SoundEvent sound, boolean looping) {
        return new SimpleSoundInstance(sound.getLocation(), SoundSource.HOSTILE, 0.7F, 1.0F, RandomSource.create(), looping, 0, SoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true);
    }

    private static void stopMusic(Minecraft minecraft) {
        stopCurrentSound(minecraft);
        stage = MusicStage.NONE;
        stageTicks = 0;
    }


    private static void stopCurrentSound(Minecraft minecraft) {
        if (currentSound == null) {
            return;
        }

        minecraft.getSoundManager().stop(currentSound);
        currentSound = null;
    }


    private enum MusicStage {
        NONE,
        INTRO,
        LOOP
    }
}