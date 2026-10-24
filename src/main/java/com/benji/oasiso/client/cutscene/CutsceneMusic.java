package com.benji.oasiso.client.cutscene;

import com.benji.oasiso.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class CutsceneMusic extends AbstractTickableSoundInstance {
    public static final float MAX_VOLUME = 0.65F;
    private static final int FADE_IN_TICKS = 40;
    private static final int FADE_OUT_TICKS = 25;
    private float gain;
    private float exitGain;
    private int exitTicks;
    private boolean fadingOut;

    public CutsceneMusic() {
        super(ModSounds.CUTSCENE_THEME.get(), SoundSource.MASTER, RandomSource.create());
        looping = true;
        delay = 0;
        volume = 0;
        relative = true;
        attenuation = Attenuation.NONE;
        x = y = z = 0;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    public void fadeOut() {
        if (fadingOut) return;
        fadingOut = true;
        exitGain = gain;
    }

    @Override
    public void tick() {
        if (fadingOut) {
            gain = exitGain * Math.max(0, 1F - ++exitTicks / (float) FADE_OUT_TICKS);
            if (exitTicks >= FADE_OUT_TICKS) {
                volume = 0;
                stop();
                return;
            }
        } else gain = Math.min(1, gain + 1F / FADE_IN_TICKS);
        volume = MAX_VOLUME * gain;
    }
}
