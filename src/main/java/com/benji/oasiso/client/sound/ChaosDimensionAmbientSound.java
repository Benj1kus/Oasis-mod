package com.benji.oasiso.client.sound;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class ChaosDimensionAmbientSound extends AbstractTickableSoundInstance {

    public ChaosDimensionAmbientSound() {
        super(ModSounds.DIMENSION_AMBIENT.get(), SoundSource.AMBIENT, RandomSource.create());

        this.looping = true;
        this.delay = 0;

        this.volume = 0.65F;
        this.pitch = 1.0F;

        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.level == null || !minecraft.level.dimension().equals(Oasiso.CHAOS_DIMENSION)) {
            this.stop();
        }
    }
}