package com.benji.oasiso.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public final class EntropyLightningParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private EntropyLightningParticle(ClientLevel world,double x,double y,double z,SpriteSet sprites) {
        super(world,x,y,z);this.sprites=sprites;
        lifetime=20;
        quadSize=.48F+random.nextFloat()*.22F;
        hasPhysics=false;gravity=0;xd=yd=zd=0;
        roll=oRoll=(random.nextFloat()-.5F)*.65F;
        setSprite(sprites.get(0,9));
    }
    @Override public void tick() {
        xo=x;yo=y;zo=z;oRoll=roll;
        if(++age>=lifetime) { remove();return; }
        setSprite(sprites.get(Math.min(9,age/2),9));
        alpha=Math.min(1,(lifetime-age)/3F);
    }
    @Override protected int getLightColor(float partial) { return 0xF000F0; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites=sprites; }
        @Override public Particle createParticle(SimpleParticleType type,ClientLevel level,double x,double y,double z,double vx,double vy,double vz) {
            return new EntropyLightningParticle(level,x,y,z,sprites);
        }
    }
}
