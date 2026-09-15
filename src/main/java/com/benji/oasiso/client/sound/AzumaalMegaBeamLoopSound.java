package com.benji.oasiso.client.sound;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.common.entity.AzumaalEntity;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class AzumaalMegaBeamLoopSound extends AbstractTickableSoundInstance {

    private static final double ANCHOR_SIDE = 0.75D / 16.0D;
    private static final double ANCHOR_HEIGHT = 43.75D / 16.0D;
    private static final double ANCHOR_FORWARD = 8.75D / 16.0D;

    private final AzumaalEntity boss;

    public AzumaalMegaBeamLoopSound(AzumaalEntity boss) {
        super(ModSounds.BEAM_LOOP.get(), SoundSource.HOSTILE, SoundInstance.createUnseededRandom());

        this.boss = boss;
        this.looping = true;
        this.delay = 0;
        this.volume = 3.25F;
        this.pitch = 1.0F;

        this.attenuation = SoundInstance.Attenuation.LINEAR;

        updatePosition();
    }

    @Override
    public void tick() {
        if (this.boss.isRemoved() || !this.boss.isAlive() || !this.boss.isStageTwo() || this.boss.getAnimState() != AzumaalEntity.STATE_STAGE_TWO_BEAM_ACTIVE) {
            this.stop();
            return;
        }

        updatePosition();
    }

    public void forceStop() {
        this.stop();
    }

    private void updatePosition() {
        float yaw = this.boss.getYRot() * Mth.DEG_TO_RAD;

        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));

        Vec3 right = new Vec3(Mth.cos(yaw), 0.0D, Mth.sin(yaw));

        Vec3 position = new Vec3(this.boss.getX(), this.boss.getY() + ANCHOR_HEIGHT, this.boss.getZ()).add(forward.scale(ANCHOR_FORWARD)).add(right.scale(ANCHOR_SIDE));

        this.x = position.x;
        this.y = position.y;
        this.z = position.z;
    }
}