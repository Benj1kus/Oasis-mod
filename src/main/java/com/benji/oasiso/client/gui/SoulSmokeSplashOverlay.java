package com.benji.oasiso.client.gui;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SoulSmokeSplashOverlay {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/smokesplash_gui.png");
    public static final int HOLD_TICKS = 60;
    public static final int FADE_TICKS = 20;
    public static final float OPACITY = 1F;
    public static final float CORNER_SCALE = 1F;
    private static int age = -1;
    private static ClientLevel world;
    private static LocalPlayer viewer;

    private SoulSmokeSplashOverlay() {
    }

    public static void show(ResourceLocation dimension) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !mc.player.isAlive() || !mc.level.dimension().location().equals(dimension))
            return;
        world = mc.level;
        viewer = mc.player;
        age = 0;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || age < 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != world || mc.player != viewer || viewer == null || !viewer.isAlive()) {
            clear();
        } else if (!mc.isPaused() && ++age >= HOLD_TICKS + FADE_TICKS) {
            clear();
        }
    }

    private static void clear() {
        age = -1;
        world = null;
        viewer = null;
    }

    private static void render(GuiGraphics graphics, float partial, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (age < 0 || mc.level != world || mc.player != viewer || viewer == null || !viewer.isAlive() || mc.options.hideGui || width <= 0 || height <= 0)
            return;
        float elapsed = age + (mc.isPaused() ? 0 : partial);
        float t = Math.max(0, Math.min(1, (elapsed - HOLD_TICKS) / FADE_TICKS));
        float alpha = OPACITY * (1 - t * t * (3 - 2 * t));
        if (alpha <= 0) return;
        float size = Math.min(width, height) * .5F * Math.max(0, Math.min(1, CORNER_SCALE));
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND), depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        int src = GL11.glGetInteger(0x80C9), dst = GL11.glGetInteger(0x80C8);
        int srcA = GL11.glGetInteger(0x80CB), dstA = GL11.glGetInteger(0x80CA);
        float[] color = RenderSystem.getShaderColor().clone();
        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            graphics.setColor(1, 1, 1, alpha);
            corner(graphics, 0, 0, size, 0, 0);
            corner(graphics, width - size, 0, size, 128, 0);
            corner(graphics, 0, height - size, size, 0, 128);
            corner(graphics, width - size, height - size, size, 128, 128);
        } finally {
            graphics.setColor(color[0], color[1], color[2], color[3]);
            if (depth) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            RenderSystem.blendFuncSeparate(src, dst, srcA, dstA);
            if (blend) RenderSystem.enableBlend();
            else RenderSystem.disableBlend();
        }
    }

    private static void corner(GuiGraphics graphics, float x, float y, float size, int u, int v) {
        var pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(x, y, 0);
            pose.scale(size / 128F, size / 128F, 1);
            graphics.blit(TEXTURE, 0, 0, (float) u, (float) v, 128, 128, 256, 256);
        } finally {
            pose.popPose();
        }
    }

    @Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterGuiOverlaysEvent event) {
            event.registerBelow(VanillaGuiOverlay.HOTBAR.id(), "soul_smoke_splash", (gui, graphics, partial, width, height) -> render(graphics, partial, width, height));
        }
    }
}
