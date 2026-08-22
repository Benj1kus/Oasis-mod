package com.benji.oasiso.common.effect;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChaosChamberEvents {

    private ChaosChamberEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        ChaosChamberManager.maintainPlayer(player);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        if (!ChaosChamberManager.isRestricted(player)) {

            return;
        }
        event.setCanceled(true);
        spawnDeniedParticles(player.serverLevel(), event.getPos());
    }


    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!ChaosChamberManager.isRestricted(player)) {
            return;
        }

        event.setCanceled(true);
        spawnDeniedParticles(player.serverLevel(), event.getPos());
    }

    private static void spawnDeniedParticles(ServerLevel level, BlockPos pos) {
        level.sendParticles(Oasiso.PURPLE_STARS.get(), pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 14, 0.38D, 0.38D, 0.38D, 0.055D);
    }
}