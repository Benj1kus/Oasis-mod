package com.benji.oasiso.common.gravesoul;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GraveSoulParticles {
    public static final SimpleParticleType PIXEL = new SimpleParticleType(false);

    @SubscribeEvent
    public static void register(RegisterEvent e) {
        e.register(ForgeRegistries.Keys.PARTICLE_TYPES, helper -> helper.register(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "grave_soul_pixel"), PIXEL));
    }

    private GraveSoulParticles() {
    }
}
