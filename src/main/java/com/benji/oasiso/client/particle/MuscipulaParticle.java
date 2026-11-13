package com.benji.oasiso.client.particle;

import com.benji.oasiso.Oasiso;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class MuscipulaParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    private MuscipulaParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites, boolean blood) {
        super(level, x, y, z);
        this.sprites = sprites;
        xd = vx;
        yd = vy;
        zd = vz;
        lifetime = blood ? 14 : 12;
        quadSize = blood ? .09F : .16F;
        gravity = blood ? .35F : 0F;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (isAlive()) setSprite(sprites.get(age, lifetime - 1));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @SubscribeEvent
    public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(Oasiso.FERAL_BITE.get(), sprites -> (type, level, x, y, z, vx, vy, vz) -> new MuscipulaParticle(level, x, y, z, vx, vy, vz, sprites, false));
        event.registerSpriteSet(Oasiso.FERAL_BLOOD.get(), sprites -> (type, level, x, y, z, vx, vy, vz) -> new MuscipulaParticle(level, x, y, z, vx, vy, vz, sprites, true));
    }
}
