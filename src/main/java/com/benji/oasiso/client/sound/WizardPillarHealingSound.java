package com.benji.oasiso.client.sound;

import com.benji.oasiso.common.entity.WizardPillarEntity;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class WizardPillarHealingSound extends AbstractTickableSoundInstance {

    private static final double EYE_CENTER_Y = 4.5D;

    private final WizardPillarEntity pillar;


    public WizardPillarHealingSound(WizardPillarEntity pillar) {
        super(SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, RandomSource.create());

        this.pillar = pillar;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.38F;
        this.pitch = 1.30F;
        this.attenuation = SoundInstance.Attenuation.LINEAR;

        updatePosition();
    }

    @Override
    public void tick() {

        if (this.pillar.isRemoved() || !this.pillar.isAlive() || this.pillar.isCollapsing() || this.pillar.getVisibleHeight() < 5 || this.pillar.getHealTargetId() < 0) {

            this.stop();
            return;
        }
        updatePosition();
    }


    private void updatePosition() {
        this.x = this.pillar.getX();
        this.y = this.pillar.getY() + EYE_CENTER_Y;
        this.z = this.pillar.getZ();
    }
}