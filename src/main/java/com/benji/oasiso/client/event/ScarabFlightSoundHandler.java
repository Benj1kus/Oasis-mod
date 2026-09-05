package com.benji.oasiso.client.event;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.sound.ScarabFlightSoundInstance;
import com.benji.oasiso.common.entity.ScarabEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ScarabFlightSoundHandler {

    private static final Map<Integer, ScarabFlightSoundInstance> ACTIVE_SOUNDS = new HashMap<>();

    private ScarabFlightSoundHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {

            for (ScarabFlightSoundInstance sound : ACTIVE_SOUNDS.values()) {
                sound.stopNow();
            }
            ACTIVE_SOUNDS.clear();
            return;
        }

        for (Entity entity : minecraft.level.entitiesForRendering()) {

            if (!(entity instanceof ScarabEntity scarab)) {
                continue;
            }

            if (!scarab.isAlive() || !scarab.isFlyingMode()) {
                continue;
            }

            ScarabFlightSoundInstance existing = ACTIVE_SOUNDS.get(scarab.getId());

            if (existing == null || existing.isStopped()) {
                ScarabFlightSoundInstance sound = new ScarabFlightSoundInstance(scarab);
                ACTIVE_SOUNDS.put(scarab.getId(), sound);
                minecraft.getSoundManager().play(sound);
            }
        }

        Iterator<Map.Entry<Integer, ScarabFlightSoundInstance>> iterator = ACTIVE_SOUNDS.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<Integer, ScarabFlightSoundInstance> entry = iterator.next();
            ScarabFlightSoundInstance sound = entry.getValue();
            Entity entity = minecraft.level.getEntity(entry.getKey());

            if (sound.isStopped()) {
                iterator.remove();
                continue;
            }

            if (!(entity instanceof ScarabEntity scarab) || !scarab.isAlive() || !scarab.isFlyingMode()) {
                sound.stopNow();
                iterator.remove();
            }
        }
    }
}