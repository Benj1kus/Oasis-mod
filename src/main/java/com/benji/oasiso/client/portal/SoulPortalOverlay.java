package com.benji.oasiso.client.portal;

import com.benji.oasiso.registry.ModBlocks;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "oasiso", value = Dist.CLIENT)
public final class SoulPortalOverlay {
    private static final float BUILDUP_TICKS = 80;
    private static final float FADE_TICKS = 15;
    private static final float MAX_ALPHA = 0.72F;
    private static float previous, strength;
    private static ClientLevel world;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != world || mc.player == null || !mc.player.isAlive()) {
            world = mc.level;
            previous = strength = 0;
            return;
        }
        if (mc.isPaused()) return;
        previous = strength;
        boolean inside = false;
        if (!mc.player.isSpectator()) {
            AABB b = mc.player.getBoundingBox().deflate(0.001);
            for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(b.minX, b.minY, b.minZ), BlockPos.containing(b.maxX, b.maxY, b.maxZ))) {
                if (mc.level.getBlockState(p).is(ModBlocks.SOUL_PORTAL.get())) {
                    inside = true;
                    break;
                }
            }
        }
        strength = Mth.clamp(strength + (inside ? 1 / BUILDUP_TICKS : -1 / FADE_TICKS), 0, 1);
    }

    private static float power(float partial) {
        return Mth.lerp(partial, previous, strength);
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        float p = power((float) e.getPartialTick());
        double t = (mc.level.getGameTime() % 24000 + e.getPartialTick()) * 0.12;
        float a = p * p;
        e.setYaw(e.getYaw() + (float) Math.sin(t * 0.71) * a * 1.1F);
        e.setPitch(e.getPitch() + (float) Math.sin(t * 0.93) * a * 0.8F);
        e.setRoll(e.getRoll() + (float) Math.sin(t) * a * 2.0F);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hud(RenderGuiEvent.Post e) {
        if (Minecraft.getInstance().screen == null) render(e.getGuiGraphics());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void screen(ScreenEvent.Render.Post e) {
        render(e.getGuiGraphics());
    }

    private static void render(GuiGraphics gui) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || world != mc.level) return;
        float p = power(mc.getFrameTime());
        if (p <= 0.001F) return;
        TextureAtlasSprite sprite = mc.getBlockRenderer().getBlockModelShaper().getParticleIcon(ModBlocks.SOUL_PORTAL.get().defaultBlockState());
        gui.flush();
        try (SoulRenderState ignored = new SoulRenderState()) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.colorMask(true, true, true, false);
            gui.setColor(1, 1, 1, MAX_ALPHA * p * p);
            gui.blit(0, 0, 0, gui.guiWidth(), gui.guiHeight(), sprite);
            gui.flush();
        }
    }
}
