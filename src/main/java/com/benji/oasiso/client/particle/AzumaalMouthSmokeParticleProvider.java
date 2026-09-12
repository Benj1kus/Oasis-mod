package com.benji.oasiso.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public class AzumaalMouthSmokeParticleProvider implements ParticleProvider<SimpleParticleType> {

    private final SpriteSet sprites;

    public AzumaalMouthSmokeParticleProvider(SpriteSet sprites) {
        this.sprites = sprites;
    }

    @Nullable
    @Override
    public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        double speed = Math.sqrt(velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ);
        float speedScale = Mth.clamp((float) (speed * 5.2D), 0.0F, 0.95F);
        float sizeScale = 0.82F + level.random.nextFloat() * 0.52F + speedScale;
        return new ArmSmokeParticle(level, x, y, z, velocityX, velocityY, velocityZ, this.sprites, sizeScale);
    }
}