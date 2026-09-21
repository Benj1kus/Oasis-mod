package com.benji.oasiso.client.fluid;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class KarakLiquidRender {
    private static Matrix4f worldView;
    private static float partial;

    @SubscribeEvent
    public static void world(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            worldView = new Matrix4f(event.getPoseStack().last().pose());
            partial = event.getPartialTick();
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Matrix4f view = worldView;
        worldView = null;
        Minecraft mc = Minecraft.getInstance();
        ShaderInstance shader = KarakLiquidShaders.shore;
        if (mc.level == null || mc.level != KarakLiquidVfx.world || view == null || shader == null || KarakLiquidVfx.SURFACES.isEmpty())
            return;
        Vec3 eye = event.getCamera().getPosition();
        if (KarakLiquidVfx.contains(mc.level, eye.x, eye.y, eye.z)) return;
        ShaderInstance previous = RenderSystem.getShader();
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();
        mc.getMainRenderTarget().bindWrite(false);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            RenderSystem.setShader(() -> shader);
            KarakLiquidShaders.uniform(shader, "Time", (KarakLiquidVfx.ticks + partial) / 20.0F);
            KarakLiquidShaders.uniform(shader, "Range", KarakLiquidVfx.RANGE);
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (KarakLiquidVfx.Surface s : KarakLiquidVfx.SURFACES) {
                if (s.mask == 0 || !KarakLiquidVfx.isJade(mc.level.getFluidState(s.pos)) || !mc.level.getBlockState(s.pos.above()).isAir())
                    continue;
                double x = s.pos.getX() - eye.x, y = s.pos.getY() - eye.y + .0025, z = s.pos.getZ() - eye.z;
                vertex(b, view, x, y + s.nw, z, 0, 0, s.mask);
                vertex(b, view, x, y + s.sw, z + 1, 0, 1, s.mask);
                vertex(b, view, x + 1, y + s.se, z + 1, 1, 1, s.mask);
                vertex(b, view, x + 1, y + s.ne, z, 1, 0, s.mask);
            }
            BufferUploader.drawWithShader(b.end());
        } finally {
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            if (previous != null) RenderSystem.setShader(() -> previous);
        }
    }

    private static void vertex(BufferBuilder b, Matrix4f m, double x, double y, double z, float u, float v, int mask) {
        b.vertex(m, (float) x, (float) y, (float) z).uv(u, v).color((mask & 1) != 0 ? 255 : 0, (mask & 2) != 0 ? 255 : 0, (mask & 4) != 0 ? 255 : 0, (mask & 8) != 0 ? 255 : 0).endVertex();
    }

    @SubscribeEvent
    public static void overlay(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        ShaderInstance shader = KarakLiquidShaders.submerged;
        if (mc.level == null || mc.level != KarakLiquidVfx.world || shader == null) return;
        float fade = Mth.lerp(event.getPartialTick(), KarakLiquidVfx.previousImmersion, KarakLiquidVfx.immersion);
        if (fade <= .001F) return;
        event.getGuiGraphics().flush();
        ShaderInstance previous = RenderSystem.getShader();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            RenderSystem.setShader(() -> shader);
            KarakLiquidShaders.uniform(shader, "Time", (KarakLiquidVfx.ticks + event.getPartialTick()) / 20.0F);
            KarakLiquidShaders.uniform(shader, "Strength", fade);
            KarakLiquidShaders.uniform(shader, "Aspect", mc.getWindow().getWidth() / (float) mc.getWindow().getHeight());
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            b.vertex(-1, 1, 0).uv(0, 0).endVertex();
            b.vertex(-1, -1, 0).uv(0, 1).endVertex();
            b.vertex(1, -1, 0).uv(1, 1).endVertex();
            b.vertex(1, 1, 0).uv(1, 0).endVertex();
            BufferUploader.drawWithShader(b.end());
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            if (previous != null) RenderSystem.setShader(() -> previous);
        }
    }

    private KarakLiquidRender() {
    }
}
