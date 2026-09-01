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
public final class PaladinBossMusic {

    private static SimpleSoundInstance currentSound;


    private PaladinBossMusic() {
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

        boolean active = player.hasEffect(Oasiso.SMELL_OF_SIN_EFFECT.get());
        if (!active) {
            if (currentSound != null) {
                stopMusic(minecraft);
            }
            return;
        }
        minecraft.getMusicManager().stopPlaying();
        if (currentSound == null) {
            startTheme(minecraft);
        }
    }

    private static void startTheme(Minecraft minecraft) {
        stopCurrentSound(minecraft);
        currentSound = createGlobalLoopingSound(ModSounds.PALADIN_THEME.get());
        minecraft.getSoundManager().play(currentSound);
    }

    private static SimpleSoundInstance createGlobalLoopingSound(SoundEvent sound) {
        return new SimpleSoundInstance(sound.getLocation(),
                SoundSource.HOSTILE,
                0.6F,
                1.0F,
                RandomSource.create(),
                true,
                0,
                SoundInstance.Attenuation.NONE,
                0.0D, 0.0D, 0.0D,
                true);
    }
    private static void stopMusic(Minecraft minecraft) {
        stopCurrentSound(minecraft);
    }
    private static void stopCurrentSound(Minecraft minecraft) {
        if (currentSound == null) {
            return;
        }
        minecraft.getSoundManager().stop(currentSound);
        currentSound = null;
    }
}