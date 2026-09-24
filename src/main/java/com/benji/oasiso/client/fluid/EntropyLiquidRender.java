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
public final class EntropyLiquidRender {
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
        ShaderInstance shader =EntropyLiquidShaders.shore;
        if (mc.level == null || mc.level !=EntropyLiquidVfx.world || view == null || shader == null ||EntropyLiquidVfx.SURFACES.isEmpty())
            return;
        Vec3 eye = event.getCamera().getPosition();
        if (EntropyLiquidVfx.contains(mc.level, eye.x, eye.y, eye.z)) return;
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
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        try {
            RenderSystem.setShader(() -> shader);
           EntropyLiquidShaders.uniform(shader, "Time", (EntropyLiquidVfx.ticks + partial) / 20.0F);
           EntropyLiquidShaders.uniform(shader, "Range",EntropyLiquidVfx.RANGE);
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (EntropyLiquidVfx.Surface s :EntropyLiquidVfx.SURFACES) {
                if (!EntropyLiquidVfx.isJade(mc.level.getFluidState(s.pos)) || !mc.level.getBlockState(s.pos.above()).isAir())
                    continue;
                double x = s.pos.getX() - eye.x, y = s.pos.getY() - eye.y + .0025, z = s.pos.getZ() - eye.z;
                float u = Math.floorMod(s.pos.getX(), 256);
                float v = Math.floorMod(s.pos.getZ(), 256);
                vertex(b, view, x, y + s.nw, z, u, v, s.mask, s.depthNw, s.bankNw);
                vertex(b, view, x, y + s.sw, z + 1, u, v + 1, s.mask, s.depthSw, s.bankSw);
                vertex(b, view, x + 1, y + s.se, z + 1, u + 1, v + 1, s.mask, s.depthSe, s.bankSe);
                vertex(b, view, x + 1, y + s.ne, z, u + 1, v, s.mask, s.depthNe, s.bankNe);
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

    private static void vertex(BufferBuilder b, Matrix4f m, double x, double y, double z, float u, float v, int mask, float shoreDistance, float bankDistance) {
        int distance = Math.round(Mth.clamp(shoreDistance / 8.0F, 0.0F, 1.0F) * 255.0F);
        int bank = Math.round(Mth.clamp(bankDistance / 8.0F, 0.0F, 1.0F) * 255.0F);
        b.vertex(m, (float) x, (float) y, (float) z).uv(u, v).color(mask, distance, bank, 255).endVertex();
    }

    @SubscribeEvent
    public static void overlay(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        ShaderInstance shader =EntropyLiquidShaders.submerged;
        if (mc.level == null || mc.level !=EntropyLiquidVfx.world || shader == null) return;
        float fade = Mth.lerp(event.getPartialTick(),EntropyLiquidVfx.previousImmersion,EntropyLiquidVfx.immersion);
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
           EntropyLiquidShaders.uniform(shader, "Time", (EntropyLiquidVfx.ticks + event.getPartialTick()) / 20.0F);
           EntropyLiquidShaders.uniform(shader, "Strength", fade);
           EntropyLiquidShaders.uniform(shader, "Aspect", mc.getWindow().getWidth() / (float) mc.getWindow().getHeight());
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

    private EntropyLiquidRender() {
    }
}
