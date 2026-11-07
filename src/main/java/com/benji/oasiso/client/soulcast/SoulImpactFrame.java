package com.benji.oasiso.client.soulcast;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = "oasiso", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SoulImpactFrame {
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("oasiso", "shaders/post/soul_strike_frame.json");
    private static PostChain chain;
    private static int width = -1, height = -1;
    private static boolean failed;

    @SubscribeEvent
    public static void reload(RegisterClientReloadListenersEvent e) {
        e.registerReloadListener(
                (ResourceManagerReloadListener)
                        resources -> {
                            if (RenderSystem.isOnRenderThread()) clear();
                            else RenderSystem.recordRenderCall(SoulImpactFrame::clear);
                        });
    }

    private static void clear() {
        if (chain != null) chain.close();
        chain = null;
        failed = false;
        width = -1;
        height = -1;
    }

    static void render(Minecraft mc, float partial) {
        if (failed) return;
        var main = mc.getMainRenderTarget();
        try {
            if (chain == null)
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), main, ID);
            if (width != main.width || height != main.height) {
                width = main.width;
                height = main.height;
                chain.resize(width, height);
            }
            chain.process(partial);
        } catch (IOException | RuntimeException e) {
            clear();
            failed = true;
            LogUtils.getLogger().error("Could not render Soul Armor impact frame", e);
        } finally {
            main.bindWrite(true);
        }
    }
}
