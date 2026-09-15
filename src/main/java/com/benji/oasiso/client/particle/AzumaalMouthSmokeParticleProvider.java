package com.benji.oasiso.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import com.benji.oasiso.common.entity.AzumaalEntity;
import com.benji.oasiso.common.entity.ai.AzumaalDeathManager;
import net.minecraft.world.phys.AABB;

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
        return new MouthSmokeParticle(level, x, y, z, velocityX, velocityY, velocityZ, this.sprites, sizeScale);
    }

    private static final class MouthSmokeParticle extends ArmSmokeParticle {

        private MouthSmokeParticle(ClientLevel level, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteSet sprites, float sizeScale) {
            super(level, x, y, z, velocityX, velocityY, velocityZ, sprites, sizeScale);
        }

        @Override
        public void tick() {
            super.tick();

            if (this.removed) {
                return;
            }

            boolean puddleDeathNearby = !this.level.getEntitiesOfClass(AzumaalEntity.class,
                    new AABB(this.x - 8.0D, this.y - 8.0D, this.z - 8.0D,
                            this.x + 8.0D, this.y + 8.0D, this.z + 8.0D),
                    boss -> boss.isStageTwo() && boss.isDeathSequenceActive() && boss.getDeathVisualTicks() >= AzumaalDeathManager.STAGE_TWO_PUDDLE_TICK).isEmpty();

            if (puddleDeathNearby) {
                this.remove();
            }
        }
    }
}