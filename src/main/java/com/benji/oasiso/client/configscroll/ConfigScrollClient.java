package com.benji.oasiso.client.configscroll;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "oasiso", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ConfigScrollClient {
    private static final ResourceLocation SCROLL = ResourceLocation.fromNamespaceAndPath("oasiso", "config_scroll");
    private ConfigScrollClient() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUse(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (!event.isUseItem() || mc.player == null || mc.screen != null) return;
        if (!SCROLL.equals(BuiltInRegistries.ITEM.getKey(mc.player.getItemInHand(event.getHand()).getItem()))) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        try { mc.setScreen(new ConfigScrollScreen()); }
        catch (RuntimeException failure) {
            com.mojang.logging.LogUtils.getLogger().error("Could not open Config Scroll", failure);
            mc.setScreen(null);
            mc.player.displayClientMessage(Component.literal("Config Scroll: could not open config. See latest.log."), false);
        }
    }
}
