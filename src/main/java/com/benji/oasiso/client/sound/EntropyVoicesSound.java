package com.benji.oasiso.client.sound;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class EntropyVoicesSound extends AbstractTickableSoundInstance {

    public EntropyVoicesSound() {
        super(
                ModSounds.VOICES.get(),
                SoundSource.AMBIENT,
                RandomSource.create()
        );

        this.looping = true;
        this.delay = 0;

        this.volume = 0.45F;
        this.pitch = 0.95F;


        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();


        if (minecraft.player == null
                || !minecraft.player.hasEffect(
                Oasiso.ENTROPY_EFFECT.get()
        )) {
            this.stop();
        }
    }
}