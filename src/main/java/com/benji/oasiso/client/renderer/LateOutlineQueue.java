package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/** World-only outline jobs; GUI and first-person rendering still run immediately. */
@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LateOutlineQueue {
    private static final List<Runnable> PASSES = new ArrayList<>();
    private static ClientLevel frameWorld;
    private static boolean collecting;

    private LateOutlineQueue() {}

    public static boolean isCollecting() {
        return collecting && frameWorld != null && Minecraft.getInstance().level == frameWorld;
    }

    public static void submit(Runnable pass) {
        if (isCollecting()) PASSES.add(pass);
        else pass.run();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void begin(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        PASSES.clear(); // Also discard jobs left over from an interrupted frame.
        frameWorld = Minecraft.getInstance().level;
        collecting = frameWorld != null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void finish(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        collecting = false;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (frameWorld == null || mc.level != frameWorld) return;
            for (Runnable pass : PASSES) {
                // Fabulous transparency has already been composited at this stage.
                mc.getMainRenderTarget().bindWrite(true);
                SavedState saved = new SavedState();
                try {
                    RenderSystem.disableScissor();
                    pass.run();
                } finally {
                    mc.getMainRenderTarget().bindWrite(true);
                    saved.restore();
                }
            }
        } finally {
            PASSES.clear();
            frameWorld = null;
        }
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() == frameWorld) {
            PASSES.clear();
            frameWorld = null;
            collecting = false;
        }
    }

    /** Restore RenderSystem's state through its API, so its GL cache stays in sync. */
    private static final class SavedState {
        final ShaderInstance shader = RenderSystem.getShader();
        final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        final boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        final boolean write = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        final int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        final int src = GL11.glGetInteger(0x80C9), dst = GL11.glGetInteger(0x80C8);
        final int srcA = GL11.glGetInteger(0x80CB), dstA = GL11.glGetInteger(0x80CA);
        final int tex0 = RenderSystem.getShaderTexture(0), tex1 = RenderSystem.getShaderTexture(1);
        final float[] tint = RenderSystem.getShaderColor().clone();
        final int[] box = new int[4];

        SavedState() { GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, box); }

        void restore() {
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            if (scissor) RenderSystem.enableScissor(box[0], box[1], box[2], box[3]);
            else RenderSystem.disableScissor();
            RenderSystem.depthMask(write);
            RenderSystem.depthFunc(depthFunc);
            RenderSystem.blendFuncSeparate(src, dst, srcA, dstA);
            RenderSystem.setShaderColor(tint[0], tint[1], tint[2], tint[3]);
            RenderSystem.setShaderTexture(0, tex0);
            RenderSystem.setShaderTexture(1, tex1);
            if (shader != null) RenderSystem.setShader(() -> shader);
        }
    }
}
