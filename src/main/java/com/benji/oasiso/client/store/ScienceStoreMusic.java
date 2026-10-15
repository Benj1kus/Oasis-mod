package com.benji.oasiso.client.store;

import com.benji.oasiso.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class ScienceStoreMusic extends AbstractTickableSoundInstance {
    private boolean fadingOut = false;
    private float fadeProgress = 0.0f;
    private final float maxVolume;
    private boolean stoppedFlag = false;

    public ScienceStoreMusic(float maxVolume) {
        super(ModSounds.STORE_THEME.get(), SoundSource.MASTER, RandomSource.create());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0f;
        this.maxVolume = maxVolume;
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.relative = true;
    }


    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (stoppedFlag) {
            this.stop();
            return;
        }

        if (fadingOut) {
            fadeProgress -= 0.04f;
            if (fadeProgress <= 0.0f) {
                fadeProgress = 0.0f;
                stoppedFlag = true;
                this.stop();
            }
        } else {
            fadeProgress += 0.025f;
            if (fadeProgress >= 1.0f) {
                fadeProgress = 1.0f;
            }
        }

        this.volume = fadeProgress * maxVolume;
    }

    public void fadeOut() {
        this.fadingOut = true;
    }
}