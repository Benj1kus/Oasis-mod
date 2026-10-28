package com.benji.oasiso.client.particle;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.gravesoul.GraveSoulParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Oasiso.MODID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class GraveSoulPixelParticle extends TextureSheetParticle {
    private GraveSoulPixelParticle(ClientLevel level,double x,double y,double z,double dx,double dy,double dz,SpriteSet sprites) {
        super(level,x,y,z);
        pickSprite(sprites);xd=dx;yd=dy;zd=dz;
        lifetime=26+random.nextInt(20);quadSize=.018F+random.nextFloat()*.022F;
        gravity=0;hasPhysics=false;friction=.98F;
        if(random.nextBoolean())setColor(1,1,1);else setColor(95F/255F,1,204F/255F);
    }
    @Override public void tick() {
        super.tick();yd+=.0015;
        alpha=Math.min(1F,Math.max(0F,(lifetime-age)/12F));
    }
    @Override public int getLightColor(float partial){return 0xF000F0;}
    @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
    @SubscribeEvent public static void register(RegisterParticleProvidersEvent e){e.registerSpriteSet(GraveSoulParticles.PIXEL,Provider::new);}
    private record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override public Particle createParticle(SimpleParticleType type,ClientLevel level,double x,double y,double z,double dx,double dy,double dz){
            return new GraveSoulPixelParticle(level,x,y,z,dx,dy,dz,sprites);
        }
    }
}
