package com.benji.oasiso.common.dimension;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.network.ModMessages;
import com.benji.oasiso.network.PocketModeS2CPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PocketModeSyncEvents {

    private PocketModeSyncEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        sync(player);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        sync(player);
    }

    private static void sync(ServerPlayer player) {
        boolean active = player.level().dimension().equals(Oasiso.CHAOS_DIMENSION) && PocketTravelData.isActive(player);
        ModMessages.sendToPlayer(player, new PocketModeS2CPacket(active));
    }
}