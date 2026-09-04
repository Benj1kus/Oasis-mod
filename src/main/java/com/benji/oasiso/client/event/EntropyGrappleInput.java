package com.benji.oasiso.client.event;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.key.ModKeyMappings;
import com.benji.oasiso.common.item.EntropyChestplateGloveItem;
import com.benji.oasiso.network.EntropyGrappleActionPacket;
import com.benji.oasiso.network.ModMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EntropyGrappleInput {

    private EntropyGrappleInput() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.screen != null) {
            return;
        }

        while (ModKeyMappings.GLOVE_GRAPPLE_MODE.consumeClick()) {
            InteractionHand hand = EntropyChestplateGloveItem.findGloveHand(player);

            if (hand == null) {
                continue;
            }

            ModMessages.sendToServer(EntropyGrappleActionPacket.toggle(hand));
        }
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.screen != null) {
            return;
        }

        InteractionHand hand = EntropyChestplateGloveItem.findGloveHand(player);

        if (hand == null) {
            return;
        }

        ItemStack glove = player.getItemInHand(hand);

        if (!EntropyChestplateGloveItem.isGrappleMode(glove)) {
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(false);

        ModMessages.sendToServer(EntropyGrappleActionPacket.use(hand));
    }
}
