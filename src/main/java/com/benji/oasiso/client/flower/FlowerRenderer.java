package com.benji.oasiso.client.flower;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class FlowerRenderer {
    private static Matrix4f view, projection;
    private static final MultiBufferSource.BufferSource TEXT = MultiBufferSource.immediate(new BufferBuilder(4096));

    private record Row(int start, int end, String text) {
    }

    static void clear() {
        view = null;
        projection = null;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            view = new Matrix4f(event.getPoseStack().last().pose());
            projection = new Matrix4f(event.getProjectionMatrix());
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Matrix4f savedView = view, savedProjection = projection;
        clear();
        Minecraft mc = Minecraft.getInstance();
        if (savedView == null || mc.level == null || mc.player == null || mc.options.hideGui) return;
        if (FlowerClient.FLAMES.isEmpty() && FlowerClient.SPEECHES.isEmpty()) return;
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND), depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE), mask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int src = GL11.glGetInteger(0x80C9), dst = GL11.glGetInteger(0x80C8);
        int srcA = GL11.glGetInteger(0x80CB), dstA = GL11.glGetInteger(0x80CA);
        float[] color = RenderSystem.getShaderColor().clone();
        Matrix4f oldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        var oldShader = RenderSystem.getShader();
        int oldTexture = RenderSystem.getShaderTexture(0);
        PoseStack pose = new PoseStack();
        pose.mulPoseMatrix(savedView);
        float partial = mc.isPaused() ? 0 : event.getPartialTick();
        double time = mc.level.getGameTime() + partial;
        Vec3 camera = event.getCamera().getPosition();
        try {
            RenderSystem.setProjectionMatrix(savedProjection, sorting);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            List<BlockPos> flames = new ArrayList<>(FlowerClient.FLAMES.keySet());
            flames.sort(Comparator.comparingDouble((BlockPos p) -> Vec3.atCenterOf(p).distanceToSqr(camera)).reversed());
            for (BlockPos pos : flames) {
                var flame = FlowerClient.FLAMES.get(pos);
                if (!FlowerClient.matches(pos, flame.def)) continue;
                float alpha = Mth.lerp(partial, flame.previous, flame.alpha);
                if (alpha <= .005F) continue;
                int frame = Math.floorMod((long) Math.floor(time / flame.def.frameTicks()) + pos.asLong(), flame.def.frames().size());
                double bob = Math.sin(time * .09 + (pos.asLong() & 255)) * .035;
                billboard(pose, pos, camera, flame.def.flameHeight() + bob);
                float size = flame.def.flameSize();
                pose.scale(-size, -size, size);
                texture(pose.last().pose(), flame.def.frames().get(frame), alpha);
                pose.popPose();
            }
            List<BlockPos> speakers = new ArrayList<>(FlowerClient.SPEECHES.keySet());
            speakers.sort(Comparator.comparingDouble((BlockPos p) -> Vec3.atCenterOf(p).distanceToSqr(camera)).reversed());
            for (BlockPos pos : speakers) {
                var speech = FlowerClient.SPEECHES.get(pos);
                double elapsed = time - speech.start;
                double distance = Vec3.atCenterOf(pos).distanceTo(camera);
                if (elapsed < 0 || elapsed >= speech.line.duration() || distance > speech.def.viewRadius() || !FlowerClient.matches(pos, speech.def))
                    continue;
                float alpha = (float) (smooth(elapsed / 4) * (1 - smooth((elapsed - speech.line.typingTicks() - speech.line.holdTicks()) / speech.line.fadeTicks())) * (1 - smooth((distance - speech.def.viewRadius() + 3) / 3)));
                if (alpha <= .02F) continue;
                billboard(pose, pos, camera, speech.def.textHeight());
                float scale = speech.def.textScale();
                pose.scale(-scale, -scale, scale);
                drawSpeech(pose, speech, elapsed, alpha);
                pose.popPose();
            }
        } finally {
            TEXT.endBatch();
            RenderSystem.setProjectionMatrix(oldProjection, sorting);
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            if (oldShader != null) RenderSystem.setShader(() -> oldShader);
            RenderSystem.setShaderTexture(0, oldTexture);
            RenderSystem.depthMask(mask);
            if (depth) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull();
            else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(src, dst, srcA, dstA);
            if (blend) RenderSystem.enableBlend();
            else RenderSystem.disableBlend();
        }
    }

    private static void billboard(PoseStack pose, BlockPos pos, Vec3 camera, double height) {
        pose.pushPose();
        pose.translate(pos.getX() + .5 - camera.x, pos.getY() + height - camera.y, pos.getZ() + .5 - camera.z);
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
    }

    private static void texture(Matrix4f pose, ResourceLocation texture, float alpha) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture);
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        b.vertex(pose, -.5F, -.5F, 0).uv(0, 0).color(1F, 1F, 1F, alpha).endVertex();
        b.vertex(pose, -.5F, .5F, 0).uv(0, 1).color(1F, 1F, 1F, alpha).endVertex();
        b.vertex(pose, .5F, .5F, 0).uv(1, 1).color(1F, 1F, 1F, alpha).endVertex();
        b.vertex(pose, .5F, -.5F, 0).uv(1, 0).color(1F, 1F, 1F, alpha).endVertex();
        BufferUploader.drawWithShader(b.end());
    }

    private static void drawSpeech(PoseStack pose, FlowerClient.Speech speech, double elapsed, float alpha) {
        Font font = Minecraft.getInstance().font;
        List<Row> rows = wrap(font, speech.chars, 160);
        int width = rows.stream().mapToInt(r -> font.width(r.text())).max().orElse(1);
        int height = rows.size() * 10;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        background(pose.last().pose(), -width / 2F - 4, -height - 3, width / 2F + 4, 2, alpha * .65F);
        int shown = speech.visible(elapsed), argb = ((int) (alpha * 255) << 24) | speech.def.color();
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int count = Math.max(0, Math.min(row.end() - row.start(), shown - row.start()));
            if (count == 0) continue;
            String prefix = new String(speech.chars, row.start(), count);
            font.drawInBatch(prefix, -font.width(row.text()) / 2F, -height + i * 10, argb, false, pose.last().pose(), TEXT, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        }
        TEXT.endBatch();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
    }

    private static List<Row> wrap(Font font, int[] chars, int maxWidth) {
        List<Row> rows = new ArrayList<>();
        for (int start = 0; start < chars.length; ) {
            int end = start, space = -1;
            while (end < chars.length && chars[end] != '\n') {
                if (font.width(new String(chars, start, end - start + 1)) > maxWidth && end > start) break;
                if (Character.isWhitespace(chars[end])) space = end;
                end++;
            }
            if (end < chars.length && chars[end] != '\n' && space >= start) end = space + 1;
            rows.add(new Row(start, end, new String(chars, start, end - start)));
            start = end < chars.length && chars[end] == '\n' ? end + 1 : end;
        }
        if (rows.isEmpty()) rows.add(new Row(0, 0, ""));
        return rows;
    }

    private static void background(Matrix4f pose, float left, float top, float right, float bottom, float alpha) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        b.vertex(pose, left, top, -.1F).color(.025F, .015F, .045F, alpha).endVertex();
        b.vertex(pose, left, bottom, -.1F).color(.025F, .015F, .045F, alpha).endVertex();
        b.vertex(pose, right, bottom, -.1F).color(.025F, .015F, .045F, alpha).endVertex();
        b.vertex(pose, right, top, -.1F).color(.025F, .015F, .045F, alpha).endVertex();
        BufferUploader.drawWithShader(b.end());
    }

    private static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    private FlowerRenderer() {
    }
}
