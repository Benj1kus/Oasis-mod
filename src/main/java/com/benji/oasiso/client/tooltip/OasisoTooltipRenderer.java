package com.benji.oasiso.client.tooltip;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;

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

    private OasisoTooltipRenderer() {
    }

    @SubscribeEvent
    public static void onTooltipColor(RenderTooltipEvent.Color event) {
        ItemStack stack = event.getItemStack();

        if (!shouldUseCustomTooltip(stack)) {
            return;
        }

        GuiGraphics graphics = event.getGraphics();

        int contentX = event.getX();
        int contentY = event.getY();
        int contentW = 0;
        int contentH = event.getComponents().size() == 1 ? -2 : 0;

        for (var component : event.getComponents()) {
            contentW = Math.max(contentW, component.getWidth(event.getFont()));
            contentH += component.getHeight();
        }

        int frameX = contentX - PAD_X;
        int frameY = contentY - PAD_TOP;
        int frameW = contentW + PAD_X * 2;
        int frameH = contentH + PAD_TOP + PAD_BOTTOM;

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 399.0F);
        drawTooltipBody(graphics, frameX, frameY, frameW, frameH);

        int dividerY = contentY + 10;
        int dividerX1 = frameX + 10;
        int dividerX2 = frameX + frameW - 10;
        if (dividerX2 > dividerX1 + 8) {
            drawGoldDivider(graphics, dividerX1, dividerY, dividerX2);
        }
        graphics.pose().popPose();

        event.setBackgroundStart(0x00000000);
        event.setBackgroundEnd(0x00000000);
        event.setBorderStart(0x00000000);
        event.setBorderEnd(0x00000000);
    }

    private static boolean shouldUseCustomTooltip(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && Oasiso.MODID.equals(id.getNamespace());
    }

    private static void drawTooltipBody(GuiGraphics gg, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return;

        RenderSystem.enableBlend();

        drawAnimatedInnerGradient(gg, x + 4, y + 4, x + w - 4, y + h - 4);
        drawHorizontalFrameParts(gg, x, y, w, h);

        drawVerticalSides(gg, x, y, w, h);
        drawInnerBevel(gg, x, y, w, h);

        RenderSystem.disableBlend();
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
        if (width <= 0 || height <= 0) return;

        for (int i = 0; i < width; i++) {
            gg.blit(FRAME, x + i, y, u, v, 1, height, TEX_W, TEX_H);
        }
    }

    private static void drawVerticalSides(GuiGraphics gg, int x, int y, int w, int h) {
        int top = y + TOP_H;
        int bottom = y + h - BOTTOM_H;

        if (bottom <= top) return;


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

        if (right <= left || bottom <= top) return;

        gg.fill(left, top, right, top + 1, 0x2238F4F0);
        gg.fill(left, bottom - 1, right, bottom, 0x33000000);
        gg.fill(left, top, left + 1, bottom, 0x1438F4F0);
        gg.fill(right - 1, top, right, bottom, 0x22000000);
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

    private static void drawAnimatedInnerGradient(GuiGraphics gg, int x1, int y1, int x2, int y2) {
        if (x2 <= x1 || y2 <= y1) return;

        long time = System.currentTimeMillis();

        int w = x2 - x1;
        int h = y2 - y1;

        float beat = getHeartbeatPulse(time);

        fillQuadGradient(gg, x1, y1, x2, y2, 0xFF091823, 0xFF06111A, 0xFF08171F, 0xFF0A1D28);
        fillQuadGradient(gg, x1, y1, x2, y2, 0x221A7A63, 0x0E0A342C, 0x26135948, 0x08000000);

        int ambientA = Mth.lerpInt(beat * 0.45F, 10, 28);

        fillQuadGradient(gg, x1, y1, x2, y2, (ambientA << 24) | 0x1F8C74, ((ambientA - 4) << 24) | 0x124B40, ((ambientA + 4) << 24) | 0x1A705F, ((ambientA - 6) << 24) | 0x0A2D25);

        int strongGreen = lerpColor(0x0838C362, 0x5638C362, beat);
        int softGreen = lerpColor(0x022A9147, 0x182A9147, beat);
        int strongCyan = lerpColor(0x0427E0C8, 0x3027E0C8, beat * 0.85F);
        int softCyan = lerpColor(0x011AA89A, 0x101AA89A, beat * 0.85F);

        fillQuadGradient(gg, x1, y1, x2, y2, strongGreen, softGreen, 0x00000000, softCyan);
        fillQuadGradient(gg, x1, y1, x2, y2, softGreen, strongGreen, softCyan, 0x00000000);
        fillQuadGradient(gg, x1, y1, x2, y2, softCyan, 0x00000000, softGreen, strongCyan);
        fillQuadGradient(gg, x1, y1, x2, y2, 0x00000000, softCyan, strongCyan, softGreen);


        int bridgeAlpha = Mth.lerpInt(beat * 0.35F, 6, 18);
        fillQuadGradient(gg, x1, y1, x2, y2, (bridgeAlpha << 24) | 0x1C8A6F, ((bridgeAlpha / 2) << 24) | 0x0D3E34, (bridgeAlpha << 24) | 0x145C4E, ((bridgeAlpha / 2) << 24) | 0x081E18);
        int edgeGlow = lerpColor(0x041B7936, 0x1838C362, beat);
        gg.fill(x1, y1, x2, y1 + 1, edgeGlow);
        gg.fill(x1, y2 - 1, x2, y2, edgeGlow);
        gg.fill(x1, y1, x1 + 1, y2, edgeGlow);
        gg.fill(x2 - 1, y1, x2, y2, edgeGlow);
        gg.fill(x1, y1, x2, y2, 0x14000000);
    }

    private static float getHeartbeatPulse(long time) {
        float t = (time % 4200L) / 4200.0F;
        float beat1 = smoothPulse(t, 0.16F, 0.10F);
        float beat2 = smoothPulse(t, 0.29F, 0.11F);
        float pulse = beat1 + beat2 * 0.72F;
        pulse = pulse * 0.85F;

        return Mth.clamp(pulse, 0.0F, 1.0F);
    }

    private static float smoothPulse(float t, float center, float halfWidth) {
        float d = Math.abs(t - center) / halfWidth;
        if (d >= 1.0F) return 0.0F;
        float x = 1.0F - d;

        return x * x * x * (x * (x * 6.0F - 15.0F) + 10.0F);
    }

    private static int lerpColor(int a, int b, float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);

        int aa = (a >>> 24) & 0xFF;
        int ar = (a >>> 16) & 0xFF;
        int ag = (a >>> 8) & 0xFF;
        int ab = a & 0xFF;

        int ba = (b >>> 24) & 0xFF;
        int br = (b >>> 16) & 0xFF;
        int bg = (b >>> 8) & 0xFF;
        int bb = b & 0xFF;

        int ra = Mth.lerpInt(t, aa, ba);
        int rr = Mth.lerpInt(t, ar, br);
        int rg = Mth.lerpInt(t, ag, bg);
        int rb = Mth.lerpInt(t, ab, bb);

        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }

    private static void fillQuadGradient(GuiGraphics gg, int x1, int y1, int x2, int y2, int topLeft, int topRight, int bottomRight, int bottomLeft) {
        if (x2 <= x1 || y2 <= y1) return;

        Matrix4f pose = gg.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();

        RenderSystem.enableBlend();
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
        float a = ((argb >> 24) & 0xFF) / 255.0F;
        float r = ((argb >> 16) & 0xFF) / 255.0F;
        float g = ((argb >> 8) & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;

        builder.vertex(pose, x, y, 0.0F).color(r, g, b, a).endVertex();
    }
}