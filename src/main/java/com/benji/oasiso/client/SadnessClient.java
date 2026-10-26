package com.benji.oasiso.client;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.particle.SadBopParticle;
import com.benji.oasiso.common.effect.SadnessStun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SadnessClient {
    @SubscribeEvent
    public static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(Oasiso.SAD_BOP.get(), sprites -> {
            SadBopParticle.SPRITES = sprites;
            return new SadBopParticle.Provider(sprites);
        });
    }

    @Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
    public static final class Icons {
        private static ClientLevel world;
        private static final Map<Mob, Particle> ICONS = new WeakHashMap<>();

        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (world != mc.level) {
                ICONS.clear();
                world = mc.level;
            }
            if (world == null || mc.isPaused() || SadBopParticle.SPRITES == null) return;
            ICONS.entrySet().removeIf(e -> e.getKey().isRemoved() || !e.getValue().isAlive());
            for (var entity : world.entitiesForRendering())
                if (entity instanceof Mob mob && SadnessStun.stunned(mob) && !ICONS.containsKey(mob) && mc.player != null && mob.distanceToSqr(mc.player) < 64 * 64) {
                    Particle p = new SadBopParticle(world, mob.getX(), mob.getY() + mob.getBbHeight() + .48, mob.getZ(), 0, 0, 0, SadBopParticle.SPRITES, mob);
                    ICONS.put(mob, p);
                    mc.particleEngine.add(p);
                }
        }
    }
}
