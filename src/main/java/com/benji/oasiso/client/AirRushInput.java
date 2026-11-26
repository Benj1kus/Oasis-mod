package com.benji.oasiso.client;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.network.AirRushNetwork;
import com.benji.oasiso.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class AirRushInput {
    private static boolean held;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getInstance();
        boolean down = mc.options.keyJump.isDown();
        boolean pressed = down && !held;
        held = down;
        var player = mc.player;
        if (pressed && mc.screen == null && player != null && !player.onGround() && player.hasEffect(ModEffects.AIR_RUSH.get())) {
            AirRushNetwork.requestJump();
        }
    }
}
