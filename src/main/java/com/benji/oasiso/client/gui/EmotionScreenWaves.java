package com.benji.oasiso.client.gui;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.AngerEntity;
import com.benji.oasiso.common.entity.HappinessEntity;
import com.benji.oasiso.common.entity.SadnessEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.io.IOException;
import java.util.Arrays;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class EmotionScreenWaves {
    public static final double RANGE = 24, FULL_DISTANCE = 3;
    public static final float MAX_WIDTH = .085F, OPACITY = .72F, RESPONSE = .14F;
    public static final int[] COLORS = {0xFF354B, 0xFFD34A, 0x60C8FF};
    private static final float[] current = new float[3], previous = new float[3];
    private static ClientLevel world;
    private static LocalPlayer player;
    private static long age;
    private static ShaderInstance shader;

    private EmotionScreenWaves() {
    }

    private static void reset(Minecraft mc) {
        world = mc.level;
        player = mc.player;
        age = 0;
        Arrays.fill(current, 0);
        Arrays.fill(previous, 0);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (world != mc.level || player != mc.player) reset(mc);
        if (player == null || world == null || !player.isAlive()) {
            reset(mc);
            return;
        }
        if (mc.isPaused()) return;
        age++;
        float[] target = new float[3];
        for (Mob mob : world.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(RANGE), e -> e.isAlive() && !e.isRemoved() && !e.isSpectator() && (e instanceof AngerEntity || e instanceof HappinessEntity || e instanceof SadnessEntity))) {
            int index = mob instanceof AngerEntity ? 0 : mob instanceof HappinessEntity ? 1 : 2;
            double distance = Math.sqrt(mob.distanceToSqr(player));
            float t = (float) Mth.clamp((RANGE - distance) / (RANGE - FULL_DISTANCE), 0, 1);
            float weight = t * t * (3 - 2 * t);
            target[index] = Math.max(target[index], weight);
        }
        for (int i = 0; i < 3; i++) {
            previous[i] = current[i];
            current[i] += (target[i] - current[i]) * RESPONSE;
            if (target[i] == 0 && current[i] < .001F) current[i] = 0;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hud(RenderGuiEvent.Post event) {
        if (Minecraft.getInstance().screen == null) draw(event.getGuiGraphics(), event.getPartialTick());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void screen(ScreenEvent.Render.Post event) {
        draw(event.getGuiGraphics(), event.getPartialTick());
    }

    private static void draw(GuiGraphics gui, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != world || mc.player != player || world == null || player == null || !player.isAlive() || mc.options.hideGui || shader == null)
            return;
        float p = mc.isPaused() ? 1 : partial;
        float sum = 0, strength = 0, r = 0, g = 0, b = 0;
        for (int i = 0; i < 3; i++) {
            float w = Mth.lerp(p, previous[i], current[i]);
            sum += w;
            strength = Math.max(strength, w);
            r += ((COLORS[i] >> 16) & 255) / 255F * w;
            g += ((COLORS[i] >> 8) & 255) / 255F * w;
            b += (COLORS[i] & 255) / 255F * w;
        }
        if (strength < .001F || sum <= 0) return;
        gui.flush();
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        if (viewport[2] <= 0 || viewport[3] <= 0) return;
        var oldShader = RenderSystem.getShader();
        float[] oldColor = RenderSystem.getShaderColor().clone();
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND), depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), cull = GL11.glIsEnabled(GL11.GL_CULL_FACE), write = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK), scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        int[] box = new int[4];
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, box);
        int sr = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dr = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB), sa = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), da = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        try {
            RenderSystem.disableScissor();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            shader.safeGetUniform("Viewport").set((float) viewport[0], (float) viewport[1], (float) viewport[2], (float) viewport[3]);
            shader.safeGetUniform("Time").set((age + (mc.isPaused() ? 0 : partial)) / 20F);
            shader.safeGetUniform("Strength").set(strength);
            shader.safeGetUniform("MaxWidth").set(MAX_WIDTH);
            shader.safeGetUniform("Opacity").set(OPACITY);
            shader.safeGetUniform("EmotionColor").set(r / sum, g / sum, b / sum);
            RenderSystem.setShader(() -> shader);
            BufferBuilder out = Tesselator.getInstance().getBuilder();
            out.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            out.vertex(-1, -1, 0).uv(0, 0).endVertex();
            out.vertex(1, -1, 0).uv(1, 0).endVertex();
            out.vertex(1, 1, 0).uv(1, 1).endVertex();
            out.vertex(-1, 1, 0).uv(0, 1).endVertex();
            BufferUploader.drawWithShader(out.end());
        } finally {
            RenderSystem.setShader(() -> oldShader);
            RenderSystem.setShaderColor(oldColor[0], oldColor[1], oldColor[2], oldColor[3]);
            RenderSystem.depthMask(write);
            if (depth) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull();
            else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(sr, dr, sa, da);
            if (blend) RenderSystem.enableBlend();
            else RenderSystem.disableBlend();
            if (scissor) RenderSystem.enableScissor(box[0], box[1], box[2], box[3]);
            else RenderSystem.disableScissor();
        }
    }

    @Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void shaders(RegisterShadersEvent event) throws IOException {
            shader = null;
            event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "emotion_screen_waves"), DefaultVertexFormat.POSITION_TEX), value -> shader = value);
        }
    }
}
