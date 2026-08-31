package com.benji.oasiso.client.event;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.item.AzumalitArmorItem;
import com.benji.oasiso.network.AzumalitWaypointRequestPacket;
import com.benji.oasiso.network.ModMessages;
import com.benji.oasiso.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class AzumalitWaypointInput {

    private static int lastSentTick = Integer.MIN_VALUE;

    private AzumalitWaypointInput() {
    }

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        trySend(event.getEntity(), event.getHand());
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (trySend(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (trySend(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (trySend(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();

        if (!player.level().isClientSide) {
            return;
        }
        if (player.level().getBlockState(event.getPos()).is(ModBlocks.MOUTH_POINT.get())) {
            return;
        }

        if (trySend(player, event.getHand())) {
            event.setCanceled(true);
        }
    }

    private static boolean trySend(Player player, InteractionHand hand) {
        if (player == null || !player.level().isClientSide || hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || !AzumalitArmorItem.isWearingFullSet(player)) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player != player || minecraft.screen != null) {
            return false;
        }

        if (lastSentTick == player.tickCount) {
            return true;
        }

        lastSentTick = player.tickCount;
        ModMessages.sendToServer(new AzumalitWaypointRequestPacket());
        return true;
    }
}
