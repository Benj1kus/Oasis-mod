package com.benji.oasiso.client.tooltip;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.shader.OasisoTooltipShaders;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;
import software.bernie.geckolib.animatable.GeoItem;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OasisoTooltipRenderer {
    private static final ResourceLocation FRAME = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/tooltip/oasiso_tooltip_frame.png");

    private static final int TEX_W = 64;
    private static final int TEX_H = 16;

    private static final int TOP_H = 8;
    private static final int BOTTOM_H = 8;

    private static final int SLICE_W = 16;
    private static final int CENTER_W = 32;

    private static final int PAD_X = 7;
    private static final int PAD_TOP = 7;
    private static final int PAD_BOTTOM = 7;

    private static final int PREVIEW_COLUMN_W = 34;
    private static final int PREVIEW_INNER_GAP = 5;

    private static ItemStack lastHoveredStack = ItemStack.EMPTY;
    private static long hoverStartedAtMs;
    private static long lastTooltipFrameMs;

    private static final long HOVER_RESET_GAP_MS = 120L;
    private static final float INTRO_DURATION_MS = 260.0F;

    private OasisoTooltipRenderer() {
    }

    @SubscribeEvent
    public static void onTooltipColor(RenderTooltipEvent.Color event) {
        ItemStack stack = event.getItemStack();

        if (!shouldUseCustomTooltip(stack)) {
            return;
        }

        long now = System.currentTimeMillis();
        updateHoverState(stack, now);

        float intro = getIntroProgress(now);
        float heartbeat = getHeartbeatPulse(now);

        GuiGraphics graphics = event.getGraphics();

        int contentX = event.getX();
        int contentY = event.getY();

        int contentW = 0;
        int contentH = event.getComponents().size() == 1 ? -2 : 0;

        for (var component : event.getComponents()) {
            contentW = Math.max(contentW, component.getWidth(event.getFont()));
            contentH += component.getHeight();
        }

        int frameX = contentX - PAD_X - PREVIEW_COLUMN_W;
        int frameY = contentY - PAD_TOP;
        int frameW = contentW + PAD_X * 2 + PREVIEW_COLUMN_W;
        int frameH = contentH + PAD_TOP + PAD_BOTTOM;

        int previewX1 = frameX + PREVIEW_INNER_GAP;
        int previewX2 = contentX - PAD_X + 1;
        int previewY1 = frameY + PREVIEW_INNER_GAP;
        int previewY2 = frameY + frameH - PREVIEW_INNER_GAP;

        graphics.drawManaged(() -> {
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 399.0F);

            drawTooltipBackgroundShader(graphics, frameX + 4, frameY + 4, frameX + frameW - 4, frameY + frameH - 4, now, intro, heartbeat);

            drawPreviewGlowShader(graphics, previewX1, previewY1, previewX2, previewY2, now, intro);

            drawHorizontalFrameParts(graphics, frameX, frameY, frameW, frameH);
            drawVerticalSides(graphics, frameX, frameY, frameW, frameH);
            drawInnerBevel(graphics, frameX, frameY, frameW, frameH);

            drawPreviewSeparator(graphics, contentX - PAD_X, frameY + TOP_H, frameY + frameH - BOTTOM_H);

            int dividerY = contentY + 10;
            int dividerX1 = contentX + 2;
            int dividerX2 = frameX + frameW - 10;

            if (dividerX2 > dividerX1 + 8) {
                drawGoldDivider(graphics, dividerX1, dividerY, dividerX2);
            }

            graphics.pose().popPose();
        });
        drawPreviewItem(graphics, stack, previewX1, previewY1, previewX2, previewY2, now, intro);

        event.setBackgroundStart(0x00000000);
        event.setBackgroundEnd(0x00000000);
        event.setBorderStart(0x00000000);
        event.setBorderEnd(0x00000000);
    }

    private static void updateHoverState(ItemStack stack, long now) {
        boolean renderGap = now - lastTooltipFrameMs > HOVER_RESET_GAP_MS;

        boolean changedStack = lastHoveredStack.isEmpty() || !ItemStack.isSameItemSameTags(lastHoveredStack, stack);

        if (renderGap || changedStack) {
            lastHoveredStack = stack.copy();
            hoverStartedAtMs = now;
        }

        lastTooltipFrameMs = now;
    }

    private static float getIntroProgress(long now) {
        float raw = Mth.clamp((now - hoverStartedAtMs) / INTRO_DURATION_MS, 0.0F, 1.0F);
        return raw * raw * raw * (raw * (raw * 6.0F - 15.0F) + 10.0F);
    }

    private static boolean shouldUseCustomTooltip(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && Oasiso.MODID.equals(id.getNamespace());
    }

    private static void drawTooltipBackgroundShader(GuiGraphics graphics, int x1, int y1, int x2, int y2, long now, float intro, float heartbeat) {
        ShaderInstance shader = OasisoTooltipShaders.getBackgroundShader();

        if (shader == null) {
            drawFallbackInnerGradient(graphics, x1, y1, x2, y2);
            return;
        }

        setUniform(shader, "Time", shaderTime(now));
        setUniform(shader, "Intro", intro);
        setUniform(shader, "Heartbeat", heartbeat);
        setUniform(shader, "Aspect", aspect(x1, y1, x2, y2));

        drawShaderQuad(graphics, shader, x1, y1, x2, y2);
    }

    private static void drawPreviewGlowShader(GuiGraphics graphics, int x1, int y1, int x2, int y2, long now, float intro) {
        ShaderInstance shader = OasisoTooltipShaders.getGlowShader();

        if (shader == null || x2 <= x1 || y2 <= y1) {
            return;
        }

        setUniform(shader, "Time", shaderTime(now));
        setUniform(shader, "Intro", intro);
        setUniform(shader, "Aspect", aspect(x1, y1, x2, y2));

        drawShaderQuad(graphics, shader, x1, y1, x2, y2);
    }

    private static void drawShaderQuad(GuiGraphics graphics, ShaderInstance shader, int x1, int y1, int x2, int y2) {
        if (x2 <= x1 || y2 <= y1) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        RenderSystem.setShader(() -> shader);

        Matrix4f pose = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        builder.vertex(pose, x1, y2, 0.0F).uv(0.0F, 1.0F).endVertex();
        builder.vertex(pose, x2, y2, 0.0F).uv(1.0F, 1.0F).endVertex();
        builder.vertex(pose, x2, y1, 0.0F).uv(1.0F, 0.0F).endVertex();
        builder.vertex(pose, x1, y1, 0.0F).uv(0.0F, 0.0F).endVertex();

        BufferUploader.drawWithShader(builder.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
    }

    private static float shaderTime(long now) {
        return (now % 600_000L) / 1000.0F;
    }

    private static float aspect(int x1, int y1, int x2, int y2) {
        return (x2 - x1) / (float) Math.max(1, y2 - y1);
    }

    private static void setUniform(ShaderInstance shader, String name, float value) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static void drawPreviewItem(GuiGraphics graphics, ItemStack stack, int x1, int y1, int x2, int y2, long now, float intro) {
        if (stack.isEmpty() || x2 <= x1 || y2 <= y1 || intro <= 0.001F) {
            return;
        }

        float zoneW = x2 - x1;
        float zoneH = y2 - y1;

        float centerX = (x1 + x2) * 0.5F;
        float centerY = (y1 + y2) * 0.5F;

        float available = Math.min(zoneW, zoneH);
        float baseScale = Mth.clamp(available / 18.0F, 0.90F, 1.72F);
        float scale = baseScale * hoverPop(intro);

        float bob = Mth.sin((float) (now * 0.0020D)) * 0.65F;
        boolean keepNativeGuiPose = stack.getItem() instanceof BlockItem || stack.getItem() instanceof GeoItem;

        graphics.drawManaged(() -> {
            PoseStack pose = graphics.pose();

            pose.pushPose();
            pose.translate(centerX, centerY + bob, 500.0F);
            pose.scale(scale, scale, scale);

            if (!keepNativeGuiPose) {

                float yaw = Mth.sin((float) (now * 0.00115D)) * 11.0F;
                float pitch = -24.0F + Mth.sin((float) (now * 0.00080D + 0.6D)) * 2.5F;
                float roll = -38.0F + Mth.sin((float) (now * 0.00070D + 1.4D)) * 2.2F;

                pose.mulPose(Axis.XP.rotationDegrees(pitch));
                pose.mulPose(Axis.YP.rotationDegrees(yaw));
                pose.mulPose(Axis.ZP.rotationDegrees(roll));
            }

            pose.pushPose();
            pose.translate(-8.0F, -8.0F, -150.0F);
            graphics.renderItem(stack, 0, 0);
            pose.popPose();

            pose.popPose();
        });
    }

    private static float hoverPop(float intro) {
        if (intro < 0.62F) {
            return Mth.lerp(intro / 0.62F, 0.84F, 1.04F);
        }

        return Mth.lerp((intro - 0.62F) / 0.38F, 1.04F, 1.0F);
    }

    private static void drawHorizontalFrameParts(GuiGraphics gg, int x, int y, int w, int h) {
        int topY = y;
        int bottomY = y + h - BOTTOM_H;

        int centerX = x + (w - CENTER_W) / 2;

        int leftGapStart = x + SLICE_W;
        int leftGapEnd = centerX;

        int rightGapStart = centerX + CENTER_W;
        int rightGapEnd = x + w - SLICE_W;

        gg.blit(FRAME, x, topY, 0, 0, 16, TOP_H, TEX_W, TEX_H);
        gg.blit(FRAME, centerX, topY, 16, 0, 16, TOP_H, TEX_W, TEX_H);
        gg.blit(FRAME, centerX + 16, topY, 32, 0, 16, TOP_H, TEX_W, TEX_H);
        gg.blit(FRAME, x + w - 16, topY, 48, 0, 16, TOP_H, TEX_W, TEX_H);

        repeatFrameColumn(gg, leftGapStart, topY, Math.max(0, leftGapEnd - leftGapStart), TOP_H, 15, 0);

        repeatFrameColumn(gg, rightGapStart, topY, Math.max(0, rightGapEnd - rightGapStart), TOP_H, 48, 0);

        gg.blit(FRAME, x, bottomY, 0, 8, 16, BOTTOM_H, TEX_W, TEX_H);
        gg.blit(FRAME, centerX, bottomY, 16, 8, 16, BOTTOM_H, TEX_W, TEX_H);
        gg.blit(FRAME, centerX + 16, bottomY, 32, 8, 16, BOTTOM_H, TEX_W, TEX_H);
        gg.blit(FRAME, x + w - 16, bottomY, 48, 8, 16, BOTTOM_H, TEX_W, TEX_H);

        repeatFrameColumn(gg, leftGapStart, bottomY, Math.max(0, leftGapEnd - leftGapStart), BOTTOM_H, 15, 8);

        repeatFrameColumn(gg, rightGapStart, bottomY, Math.max(0, rightGapEnd - rightGapStart), BOTTOM_H, 48, 8);
    }

    private static void repeatFrameColumn(GuiGraphics gg, int x, int y, int width, int height, int u, int v) {
        if (width <= 0 || height <= 0) {
            return;
        }

        for (int i = 0; i < width; i++) {
            gg.blit(FRAME, x + i, y, u, v, 1, height, TEX_W, TEX_H);
        }
    }

    private static void drawVerticalSides(GuiGraphics gg, int x, int y, int w, int h) {
        int top = y + TOP_H;
        int bottom = y + h - BOTTOM_H;

        if (bottom <= top) {
            return;
        }

        gg.fill(x, top, x + 1, bottom, 0xFF0C2230);
        gg.fill(x + 1, top, x + 2, bottom, 0xFF1B7936);
        gg.fill(x + 2, top, x + 3, bottom, 0xFF2A9147);
        gg.fill(x + 3, top, x + 4, bottom, 0xFF163A3E);

        gg.fill(x + w - 1, top, x + w, bottom, 0xFF0C2230);
        gg.fill(x + w - 2, top, x + w - 1, bottom, 0xFF1B7936);
        gg.fill(x + w - 3, top, x + w - 2, bottom, 0xFF2A9147);
        gg.fill(x + w - 4, top, x + w - 3, bottom, 0xFF163A3E);
    }

    private static void drawInnerBevel(GuiGraphics gg, int x, int y, int w, int h) {
        int left = x + 4;
        int right = x + w - 4;
        int top = y + TOP_H;
        int bottom = y + h - BOTTOM_H;

        if (right <= left || bottom <= top) {
            return;
        }

        gg.fill(left, top, right, top + 1, 0x2238F4F0);
        gg.fill(left, bottom - 1, right, bottom, 0x33000000);
        gg.fill(left, top, left + 1, bottom, 0x1438F4F0);
        gg.fill(right - 1, top, right, bottom, 0x22000000);
    }

    private static void drawPreviewSeparator(GuiGraphics gg, int x, int y1, int y2) {
        if (y2 <= y1) {
            return;
        }

        gg.fill(x, y1 + 1, x + 1, y2 - 1, 0x173CFFF2);
        gg.fill(x + 1, y1 + 4, x + 2, y2 - 4, 0x0A8E45FF);
    }

    private static void drawGoldDivider(GuiGraphics gg, int x1, int y, int x2) {
        if (x2 <= x1) {
            return;
        }

        int width = x2 - x1;
        int fadeStart = x1 + (int) (width * 0.60F);

        fillQuadGradient(gg, x1, y, fadeStart, y + 1, 0xFFCB9006, 0xFFCB9006, 0xFF9B5C04, 0xFF9B5C04);

        fillQuadGradient(gg, fadeStart, y, x2, y + 1, 0xFFCB9006, 0x00CB9006, 0x009B5C04, 0xFF9B5C04);
    }

    private static void drawFallbackInnerGradient(GuiGraphics gg, int x1, int y1, int x2, int y2) {
        if (x2 <= x1 || y2 <= y1) {
            return;
        }

        fillQuadGradient(gg, x1, y1, x2, y2, 0xFF07151F, 0xFF061018, 0xFF07131C, 0xFF0A1A24);
    }

    private static float getHeartbeatPulse(long time) {
        float t = (time % 4200L) / 4200.0F;

        float beat1 = smoothPulse(t, 0.16F, 0.10F);
        float beat2 = smoothPulse(t, 0.29F, 0.11F);

        float pulse = (beat1 + beat2 * 0.72F) * 0.85F;

        return Mth.clamp(pulse, 0.0F, 1.0F);
    }

    private static float smoothPulse(float t, float center, float halfWidth) {
        float d = Math.abs(t - center) / halfWidth;

        if (d >= 1.0F) {
            return 0.0F;
        }

        float x = 1.0F - d;

        return x * x * x * (x * (x * 6.0F - 15.0F) + 10.0F);
    }

    private static void fillQuadGradient(GuiGraphics gg, int x1, int y1, int x2, int y2, int topLeft, int topRight, int bottomRight, int bottomLeft) {
        if (x2 <= x1 || y2 <= y1) {
            return;
        }

        Matrix4f pose = gg.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        vertex(builder, pose, x1, y2, bottomLeft);
        vertex(builder, pose, x2, y2, bottomRight);
        vertex(builder, pose, x2, y1, topRight);
        vertex(builder, pose, x1, y1, topLeft);

        BufferUploader.drawWithShader(builder.end());

        RenderSystem.disableBlend();
    }

    private static void vertex(BufferBuilder builder, Matrix4f pose, float x, float y, int argb) {
        float a = ((argb >>> 24) & 0xFF) / 255.0F;
        float r = ((argb >>> 16) & 0xFF) / 255.0F;
        float g = ((argb >>> 8) & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;

        builder.vertex(pose, x, y, 0.0F).color(r, g, b, a).endVertex();
    }
}
