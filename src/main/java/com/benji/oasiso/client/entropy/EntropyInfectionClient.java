package com.benji.oasiso.client.entropy;

import com.benji.oasiso.Oasiso;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class EntropyInfectionClient {
    private static ClientLevel world;
    private static final Map<UUID, Long> SHAKING = new HashMap<>();

    private static void checkWorld() {
        var current = Minecraft.getInstance().level;
        if (current != world) {
            world = current;
            SHAKING.clear();
        }
    }

    public static void receive(UUID mob, int ticks) {
        checkWorld();
        if (world == null) return;
        if (ticks <= 0) SHAKING.remove(mob);
        else SHAKING.put(mob, world.getGameTime() + Math.min(100, ticks));
    }

    public static boolean isShaking(LivingEntity mob) {
        checkWorld();
        return world != null && mob.level() == world && SHAKING.getOrDefault(mob.getUUID(), Long.MIN_VALUE) > world.getGameTime();
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        checkWorld();
        if (world != null) SHAKING.values().removeIf(end -> end <= world.getGameTime());
    }

    private EntropyInfectionClient() {
    }
}
