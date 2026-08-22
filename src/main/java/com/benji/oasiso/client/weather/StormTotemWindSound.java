package com.benji.oasiso.client.weather;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.common.block.entity.StormTotemBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class StormTotemWindSound extends AbstractTickableSoundInstance {

    private static final float MAX_VOLUME = 0.85F;

    public StormTotemWindSound() {
        super(ModSounds.SANDSTORM.get(), SoundSource.WEATHER, RandomSource.create());

        this.looping = true;
        this.delay = 0;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.pitch = 1.0F;
        this.volume = 0.0F;

    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) {
            stop();
            return;
        }


        float strength = StormTotemBlockEntity.getClientStormStrength(minecraft.level.dimension(),

                minecraft.player.getEyePosition());

        if (strength <= 0.001F) {
            stop();
            return;
        }

        strength = Mth.clamp(strength, 0.0F, 1.0F);
        strength = strength * strength * (3.0F - 2.0F * strength);


        this.volume = MAX_VOLUME * strength;
    }

    public void requestStop() {
        stop();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}