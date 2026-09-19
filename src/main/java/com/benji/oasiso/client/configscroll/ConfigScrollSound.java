package com.benji.oasiso.client.configscroll;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

final class ConfigScrollSound {
    private static long hoverAt, sliderAt;
    private ConfigScrollSound() {}
    static void hover() {
        long now = System.nanoTime();
        if (now - hoverAt < 65_000_000L) return;
        hoverAt = now;
        play(SoundEvents.WOODEN_BUTTON_CLICK_ON, 1.55F, .13F);
    }
    static void click() { play(SoundEvents.STONE_BUTTON_CLICK_ON, 1.3F, .22F); }
    static void turn(int direction) { play(SoundEvents.WOODEN_BUTTON_CLICK_OFF, direction > 0 ? 1.32F : 1.12F, .21F); }
    static void slider(double position) {
        long now = System.nanoTime();
        if (now - sliderAt < 65_000_000L) return;
        sliderAt = now;
        play(SoundEvents.WOODEN_BUTTON_CLICK_ON, (float) (1.05 + position * .55), .10F);
    }
    private static void play(SoundEvent event, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, pitch, volume));
    }
}
