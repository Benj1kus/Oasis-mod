package com.benji.oasiso.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class MouthSmokeParticleProvider implements ParticleProvider<SimpleParticleType> {

    private static final float SIZE_SCALE = 0.42F;

    private final SpriteSet sprites;

    public MouthSmokeParticleProvider(SpriteSet sprites) {
        this.sprites = sprites;
    }

    @Nullable
    @Override
    public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        return new ArmSmokeParticle(level, x, y, z, velocityX, velocityY, velocityZ, this.sprites, SIZE_SCALE);
    }
}
