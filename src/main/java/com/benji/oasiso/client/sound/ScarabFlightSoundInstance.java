package com.benji.oasiso.client.sound;

import com.benji.oasiso.common.entity.ScarabEntity;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class ScarabFlightSoundInstance extends AbstractTickableSoundInstance {

    private final ScarabEntity scarab;

    public ScarabFlightSoundInstance(ScarabEntity scarab) {
        super(SoundEvents.BEE_LOOP, SoundSource.NEUTRAL, RandomSource.create());

        this.scarab = scarab;
        this.looping = true;
        this.delay = 0;

        this.volume = 0.42F;
        this.pitch = 0.70F;

        this.relative = false;
        this.attenuation = SoundInstance.Attenuation.LINEAR;

        updatePosition();
    }

    @Override
    public void tick() {

        if (!this.scarab.isAlive() || this.scarab.isRemoved() || !this.scarab.isFlyingMode()) {
            this.stop();
            return;
        }

        updatePosition();

        double speed = this.scarab.getDeltaMovement().length();
        float targetPitch = 0.68F + (float) Mth.clamp(speed * 0.075D, 0.0D, 0.15D);
        this.pitch = Mth.lerp(0.08F, this.pitch, targetPitch);
    }

    private void updatePosition() {

        this.x = this.scarab.getX();
        this.y = this.scarab.getY() + this.scarab.getBbHeight() * 0.55D;
        this.z = this.scarab.getZ();
    }

    public void stopNow() {
        this.stop();
    }
}