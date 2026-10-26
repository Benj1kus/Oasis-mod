package com.benji.oasiso.client.particle;

import com.benji.oasiso.common.effect.SadnessStun;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

public final class SadBopParticle extends TextureSheetParticle {
    public static SpriteSet SPRITES;
    private final SpriteSet sprites;
    private final Mob attached;
    private int tail = 8;
    public SadBopParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites, Mob attached) {
        super(level,x,y,z); this.sprites=sprites; this.attached=attached;
        xd=vx; yd=vy+.01; zd=vz; lifetime=30+random.nextInt(9);
        quadSize=attached == null ? .18F+random.nextFloat()*.12F : .28F;
        friction=.92F; hasPhysics=false; gravity=0; roll=0; oRoll=0;
        setSpriteFromAge(sprites);
    }
    @Override public void tick() {
        if (attached == null) {
            super.tick(); if (removed) return;
            setSpriteFromAge(sprites); alpha=Mth.clamp((1-age/(float)lifetime)/.35F,0,1);
        } else {
            xo=x; yo=y; zo=z; age++;
            if (attached.isRemoved() || !SadnessStun.stunned(attached)) { if (--tail<=0) { remove(); return; } }
            else tail=8;
            setPos(attached.getX(),attached.getY()+attached.getBbHeight()+.48,attached.getZ());
            setSprite(sprites.get((age/2)%12,11)); alpha=Math.min(1,age/5F)*tail/8F;
        }
    }
    @Override protected int getLightColor(float partial) { return 0xF000F0; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level, double x,double y,double z,double vx,double vy,double vz) {
            return new SadBopParticle(level,x,y,z,vx,vy,vz,sprites,null);
        }
    }
}
