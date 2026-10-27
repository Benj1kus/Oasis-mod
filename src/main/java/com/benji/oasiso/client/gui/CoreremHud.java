package com.benji.oasiso.client.gui;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.corerem.CoreremNetwork;
import com.benji.oasiso.common.corerem.CoreremRoll;
import com.benji.oasiso.common.item.CoreremItem;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.util.Arrays;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import org.lwjgl.opengl.GL20;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class CoreremHud {

    public static final int ORB_SCALE = 2, HUD_BOTTOM = 112, FACE_SCALE = 6;
    public static final int FACE_TICKS = 50, FLASH_TICKS = 10;
    public static final float FLASH_ALPHA = .68F;
    private static final ResourceLocation[] ORBS = {texture("orb_happy"), texture("orb_anger"), texture("orb_sad")};
    private static final ResourceLocation[] SELECTED = {texture("selected_orb_happy"), texture("selected_orb_anger"), texture("selected_orb_sad")};
    private static final ResourceLocation[] FACES = {texture("happy_face"), texture("angry_face"), texture("sad_face")};
    private static final ResourceLocation RELOAD = texture("orb_reload");
    private static final int[] COLORS = {0xFFD34A, 0xFF354B, 0x60C8FF};
    private static final int[] cooldown = new int[3];
    private static int useCooldown, mask, steps, age, lastSoundStep = -1;
    private static int face = -1, faceAge;

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/corerem/" + name + ".png");
    }

    public static void state(CoreremNetwork.State packet) {
        boolean newRoll = mask == 0 && packet.mask() != 0;
        useCooldown = packet.use();
        cooldown[0] = packet.happy();
        cooldown[1] = packet.anger();
        cooldown[2] = packet.sad();
        age = !newRoll && mask != 0 && packet.mask() != 0 ? Math.max(age, packet.age()) : packet.age();
        mask = packet.mask();
        steps = packet.steps();
        if (mask == 0 || newRoll) lastSoundStep = -1;
        if (mask != 0) tickSound();
    }

    public static void result(int emotion) {
        if (emotion < 0 || emotion >= FACES.length) return;
        mask = 0;
        lastSoundStep = -1;
        face = emotion;
        faceAge = 0;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.SCISSORS.get(), 1F));
    }

    private static void tickSound() {
        int step = CoreremRoll.stepAt(age, steps);
        if (step > lastSoundStep) {
            lastSoundStep = step;
            float pitch = Mth.lerp(step / (float) Math.max(1, steps), 1.25F, .85F);
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.TIMER.get(), pitch));
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            reset();
            return;
        }
        if (mc.isPaused()) return;
        if (useCooldown > 0) useCooldown--;
        for (int i = 0; i < 3; i++) if (cooldown[i] > 0) cooldown[i]--;
        if (!mc.player.isAlive()) {
            mask = 0;
            face = -1;
            return;
        }
        if (mask != 0) {
            age = Math.min(age + 1, CoreremRoll.duration(steps));
            tickSound();
        }
        if (face >= 0 && ++faceAge >= FACE_TICKS) face = -1;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }

    private static void reset() {
        Arrays.fill(cooldown, 0);
        useCooldown = mask = steps = age = faceAge = 0;
        lastSoundStep = face = -1;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hud(RenderGuiEvent.Post event) {
        if (Minecraft.getInstance().screen == null) draw(event.getGuiGraphics(), event.getPartialTick(), true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void screen(ScreenEvent.Render.Post event) {
        draw(event.getGuiGraphics(), event.getPartialTick(), false);
    }

    private static void draw(GuiGraphics gui, float partial, boolean hud) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !mc.player.isAlive() || mc.options.hideGui) return;
        boolean held = mc.player.getMainHandItem().getItem() instanceof CoreremItem || mc.player.getOffhandItem().getItem() instanceof CoreremItem;
        boolean wheel = hud && (held || mask != 0);
        if (!wheel && face < 0) return;

        float[] oldColor = RenderSystem.getShaderColor().clone();
        var oldShader = RenderSystem.getShader();
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND), depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        int sr = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dr = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int sa = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), da = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        gui.flush();
        gui.pose().pushPose();
        try {
            gui.pose().translate(0, 0, 400);
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            gui.setColor(1, 1, 1, 1);
            if (wheel) wheel(gui, mc);
            if (face >= 0) reveal(gui, mc.isPaused() ? faceAge : faceAge + partial);
        } finally {
            gui.flush();
            gui.pose().popPose();
            RenderSystem.setShaderColor(oldColor[0], oldColor[1], oldColor[2], oldColor[3]);
            RenderSystem.setShader(() -> oldShader);
            RenderSystem.blendFuncSeparate(sr, dr, sa, da);
            if (blend) RenderSystem.enableBlend();
            else RenderSystem.disableBlend();
            if (depth) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
        }
    }

    private static void wheel(GuiGraphics gui, Minecraft mc) {
        int center = gui.guiWidth() / 2, top = gui.guiHeight() - HUD_BOTTOM;
        int size = 10 * ORB_SCALE;
        int selected = mask == 0 ? -1 : CoreremRoll.emotionAt(mask, CoreremRoll.stepAt(age, steps));
        int[] x = {center - size / 2, center + 24 - size / 2, center - 24 - size / 2};
        int[] y = {top, top + 25, top + 25};
        for (int i = 0; i < 3; i++) {
            ResourceLocation sprite = cooldown[i] > 0 ? RELOAD : selected == i ? SELECTED[i] : ORBS[i];
            gui.flush();
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            gui.blit(sprite, x[i], y[i], size, size, 0F, 0F, 10, 10, 10, 10);
            if (cooldown[i] > 0) {
                String time = Integer.toString((cooldown[i] + 19) / 20);
                int tx = i == CoreremRoll.SAD ? x[i] - mc.font.width(time) - 3 : x[i] + size + 3;
                gui.drawString(mc.font, time, tx, y[i] + (size - 8) / 2, COLORS[i], true);
            }
        }
        if (useCooldown > 0 && mask == 0) {
            Component text = Component.translatable("gui.oasiso.corerem.cooldown", (useCooldown + 19) / 20);
            gui.drawCenteredString(mc.font, text, center, top + 50, 0xE4D8BC);
        }
    }

    private static void reveal(GuiGraphics gui, float ticks) {
        gui.flush();

        int[] oldMask = new int[4];
        GL11.glGetIntegerv(GL11.GL_COLOR_WRITEMASK, oldMask);

        boolean oldDepthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int oldEquationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
        int oldEquationAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);

        try {
            RenderSystem.colorMask(true, true, true, false);
            RenderSystem.depthMask(false);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.blendEquation(GL14.GL_FUNC_ADD);

            float flash = FLASH_ALPHA * (float) Math.pow(Mth.clamp(1F - ticks / FLASH_TICKS, 0F, 1F), 2);

            int flashAlpha = Math.round(255 * flash);
            if (flashAlpha > 0) {
                gui.fill(0, 0, gui.guiWidth(), gui.guiHeight(), (flashAlpha << 24) | 0xFFFFFF);
            }
            gui.flush();

            float appear = Mth.clamp(ticks / 3F, 0F, 1F);
            appear = appear * appear * (3F - 2F * appear);

            float fade = (float) Math.pow(Mth.clamp(1F - ticks / FACE_TICKS, 0F, 1F), 1.35);

            float opacity = appear * fade;
            if (opacity <= 0F) return;

            RenderSystem.colorMask(true, true, true, false);
            RenderSystem.depthMask(false);
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.blendEquation(GL14.GL_FUNC_ADD);

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, FACES[face]);
            RenderSystem.setShaderColor(1F, 1F, 1F, opacity);

            float w = 12F * FACE_SCALE;
            float h = 15F * FACE_SCALE;
            float x = (gui.guiWidth() - w) / 2F;
            float y = (gui.guiHeight() - h) / 2F;

            var matrix = gui.pose().last().pose();
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();

            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            buffer.vertex(matrix, x, y, 0F).uv(0F, 0F).endVertex();
            buffer.vertex(matrix, x, y + h, 0F).uv(0F, 1F).endVertex();
            buffer.vertex(matrix, x + w, y + h, 0F).uv(1F, 1F).endVertex();
            buffer.vertex(matrix, x + w, y, 0F).uv(1F, 0F).endVertex();
            BufferUploader.drawWithShader(buffer.end());

        } finally {
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.colorMask(oldMask[0] != 0, oldMask[1] != 0, oldMask[2] != 0, oldMask[3] != 0);
            RenderSystem.depthMask(oldDepthWrite);
            GL20.glBlendEquationSeparate(oldEquationRgb, oldEquationAlpha);
        }
    }

    private CoreremHud() {
    }
}
