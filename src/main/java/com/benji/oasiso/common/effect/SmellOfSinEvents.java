package com.benji.oasiso.common.effect;

import com.benji.oasiso.Oasiso;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SmellOfSinEvents {

    private SmellOfSinEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {

            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {

            return;
        }
        SmellOfSinManager.maintainPlayer(player);
    }
}