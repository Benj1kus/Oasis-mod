package com.benji.oasiso.dialogue.editor;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.dialogue.data.DialogueDefinition;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.Locale;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DialogueZoneWorldEditRenderer {

    private static final int X_COLOR = 0xFF4D55;
    private static final int Y_COLOR = 0x55E878;
    private static final int Z_COLOR = 0x4AA3FF;
    private static final int SELECTED_COLOR = 0xFFF08A;
    private static final int ANCHOR_COLOR = 0xFFFFFF;
    private static final int GRID_COLOR = 0x6B7C8D;
    private static final int CURSOR_COLOR = 0xFFD45A;
    private static final int MARKER_CURSOR_COLOR = 0xFF75D8;
    private static final int XY_COLOR = 0xFFD45A;
    private static final int XZ_COLOR = 0xB76CFF;
    private static final int YZ_COLOR = 0x42F2E1;

    private DialogueZoneWorldEditRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof DialogueZoneWorldEditScreen editor) || minecraft.level == null || minecraft.player == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        renderCustomFloorSprite(editor, poseStack, minecraft);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableDepthTest();
        drawEditorFills(editor, poseStack.last().pose());

        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(1.0F);
        drawEditorLines(editor, poseStack.last().pose(), 0.24F, true);

        RenderSystem.enableDepthTest();
        RenderSystem.lineWidth(2.0F);
        drawEditorLines(editor, poseStack.last().pose(), 1.0F, false);

        RenderSystem.lineWidth(1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();

        poseStack.popPose();
    }

    private static void drawEditorFills(DialogueZoneWorldEditScreen editor, Matrix4f matrix) {
        boolean hasPlanes = editor.mode() == DialogueZoneWorldEditScreen.EditMode.MOVE;
        boolean hasBlockAnchor = editor.resolvedBlockAnchor() != null;
        boolean hasEntityAnchor = editor.resolvedEntityAnchor() != null;

        if (!hasPlanes && !hasBlockAnchor && !hasEntityAnchor) {
            return;
        }

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        if (hasPlanes) {
            drawPlaneFill(buffer, matrix, editor, DialogueZoneWorldEditScreen.GizmoPlane.XY, XY_COLOR);
            drawPlaneFill(buffer, matrix, editor, DialogueZoneWorldEditScreen.GizmoPlane.XZ, XZ_COLOR);
            drawPlaneFill(buffer, matrix, editor, DialogueZoneWorldEditScreen.GizmoPlane.YZ, YZ_COLOR);
        }

        BlockPos block = editor.resolvedBlockAnchor();
        if (block != null) {
            fillBox(buffer, matrix, new AABB(block.getX(), block.getY(), block.getZ(), block.getX() + 1.0D, block.getY() + 1.0D, block.getZ() + 1.0D), ANCHOR_COLOR, 30);
        }

        if (editor.resolvedEntityAnchor() != null) {
            fillBox(buffer, matrix, editor.resolvedEntityAnchor().getBoundingBox().inflate(0.025D), ANCHOR_COLOR, 22);
        }

        BufferUploader.drawWithShader(buffer.end());
    }

    private static void drawPlaneFill(BufferBuilder buffer, Matrix4f matrix, DialogueZoneWorldEditScreen editor, DialogueZoneWorldEditScreen.GizmoPlane plane, int color) {
        Vec3[] corners = editor.planeCorners(plane);
        if (corners.length != 4) return;

        boolean selected = editor.dragPlane() == plane || editor.hoveredPlane() == plane;
        int actualColor = selected ? SELECTED_COLOR : color;
        int alpha = selected ? 88 : 34;
        quad(buffer, matrix, corners[0], corners[1], corners[2], corners[3], actualColor, alpha);
    }

    private static void drawEditorLines(DialogueZoneWorldEditScreen editor, Matrix4f matrix, float alphaScale, boolean xray) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        DialogueDefinition.Trigger trigger = editor.trigger();
        Vec3 center = editor.center();
        Vec3 anchorBase = editor.anchorBase();

        int zoneColor = trigger.visual != null ? DialogueEditorPreview.parseColor(trigger.visual.color) : 0x42F2E1;

        int zoneAlpha = Math.round((xray ? 92.0F : 235.0F) * alphaScale);
        int gridAlpha = Math.round((xray ? 34.0F : 82.0F) * alphaScale);
        int helperAlpha = Math.round((xray ? 90.0F : 220.0F) * alphaScale);

        drawGroundGrid(buffer, matrix, center, trigger, gridAlpha);
        drawGameplayShape(buffer, matrix, center, trigger, zoneColor, zoneAlpha);

        if (anchorBase != null) {
            drawWireCube(buffer, matrix, anchorBase, editor.handleSize() * 0.55D, ANCHOR_COLOR, Math.round(helperAlpha * 0.75F));

            if (anchorBase.distanceToSqr(center) > 0.0001D) {
                line(buffer, matrix, anchorBase, center, ANCHOR_COLOR, Math.round(helperAlpha * 0.55F));
            }
        }

        drawResolvedAnchorBounds(buffer, matrix, editor, helperAlpha);

        if (editor.mode() == DialogueZoneWorldEditScreen.EditMode.MOVE) {
            drawPlaneOutlines(buffer, matrix, editor, helperAlpha);
        }

        drawCenterMarker(buffer, matrix, center, zoneColor, helperAlpha, editor.handleSize());
        drawGizmo(buffer, matrix, editor, helperAlpha);

        Vec3 cursor = editor.cursorHit();
        if (cursor != null) {
            int cursorColor = editor.markerPlacementMode() ? MARKER_CURSOR_COLOR : CURSOR_COLOR;
            double cursorScale = editor.markerPlacementMode() ? 1.35D : 0.9D;
            drawCursorCross(buffer, matrix, cursor, editor.handleSize() * cursorScale, cursorColor, Math.round(helperAlpha * 0.9F));
        }

        BufferUploader.drawWithShader(buffer.end());
    }

    private static void drawGroundGrid(BufferBuilder buffer, Matrix4f matrix, Vec3 center, DialogueDefinition.Trigger trigger, int alpha) {
        double extent = Math.min(16.0D, Math.max(3.0D, horizontalExtent(trigger) + 2.0D));
        int whole = Mth.clamp((int) Math.ceil(extent), 3, 16);
        double y = center.y + 0.012D;

        for (int i = -whole; i <= whole; i++) {
            int lineAlpha = i == 0 ? Math.min(255, alpha * 2) : alpha;

            line(buffer, matrix, new Vec3(center.x - whole, y, center.z + i), new Vec3(center.x + whole, y, center.z + i), GRID_COLOR, lineAlpha);
            line(buffer, matrix, new Vec3(center.x + i, y, center.z - whole), new Vec3(center.x + i, y, center.z + whole), GRID_COLOR, lineAlpha);
        }
    }

    private static void drawGameplayShape(BufferBuilder buffer, Matrix4f matrix, Vec3 center, DialogueDefinition.Trigger trigger, int color, int alpha) {
        switch (normalizeShape(trigger.shape)) {
            case "sphere" -> drawSphere(buffer, matrix, center, Math.max(0.1D, trigger.radius), color, alpha);
            case "box" ->
                    drawBox(buffer, matrix, center, Math.max(0.1D, trigger.size_x), Math.max(0.1D, trigger.size_y), Math.max(0.1D, trigger.size_z), color, alpha);
            default ->
                    drawCylinder(buffer, matrix, center, Math.max(0.1D, trigger.radius), Math.max(0.1D, trigger.height), color, alpha);
        }
    }

    private static void drawCylinder(BufferBuilder buffer, Matrix4f matrix, Vec3 center, double radius, double height, int color, int alpha) {
        final int segments = 64;
        double bottom = center.y;
        double top = center.y + height;

        circleXZ(buffer, matrix, center.x, bottom, center.z, radius, segments, color, alpha);
        circleXZ(buffer, matrix, center.x, top, center.z, radius, segments, color, alpha);
        circleXZ(buffer, matrix, center.x, center.y + height * 0.5D, center.z, radius, segments, color, Math.max(30, alpha / 3));

        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2.0D * i / 8.0D;
            double x = center.x + Math.cos(a) * radius;
            double z = center.z + Math.sin(a) * radius;
            line(buffer, matrix, new Vec3(x, bottom, z), new Vec3(x, top, z), color, Math.max(50, alpha / 2));
        }
    }

    private static void drawSphere(BufferBuilder buffer, Matrix4f matrix, Vec3 center, double radius, int color, int alpha) {
        circleXZ(buffer, matrix, center.x, center.y, center.z, radius, 64, color, alpha);
        circleXY(buffer, matrix, center.x, center.y, center.z, radius, 64, color, alpha);
        circleYZ(buffer, matrix, center.x, center.y, center.z, radius, 64, color, alpha);

        double diagonal = radius * 0.70710678118D;
        circleXZ(buffer, matrix, center.x, center.y + diagonal, center.z, diagonal, 48, color, Math.max(35, alpha / 3));
        circleXZ(buffer, matrix, center.x, center.y - diagonal, center.z, diagonal, 48, color, Math.max(35, alpha / 3));
    }

    private static void drawBox(BufferBuilder buffer, Matrix4f matrix, Vec3 center, double sx, double sy, double sz, int color, int alpha) {
        double hx = sx * 0.5D;
        double hz = sz * 0.5D;
        double y0 = center.y;
        double y1 = center.y + sy;

        Vec3 a = new Vec3(center.x - hx, y0, center.z - hz);
        Vec3 b = new Vec3(center.x + hx, y0, center.z - hz);
        Vec3 c = new Vec3(center.x + hx, y0, center.z + hz);
        Vec3 d = new Vec3(center.x - hx, y0, center.z + hz);

        Vec3 e = new Vec3(center.x - hx, y1, center.z - hz);
        Vec3 f = new Vec3(center.x + hx, y1, center.z - hz);
        Vec3 g = new Vec3(center.x + hx, y1, center.z + hz);
        Vec3 h = new Vec3(center.x - hx, y1, center.z + hz);

        line(buffer, matrix, a, b, color, alpha);
        line(buffer, matrix, b, c, color, alpha);
        line(buffer, matrix, c, d, color, alpha);
        line(buffer, matrix, d, a, color, alpha);

        line(buffer, matrix, e, f, color, alpha);
        line(buffer, matrix, f, g, color, alpha);
        line(buffer, matrix, g, h, color, alpha);
        line(buffer, matrix, h, e, color, alpha);

        line(buffer, matrix, a, e, color, alpha);
        line(buffer, matrix, b, f, color, alpha);
        line(buffer, matrix, c, g, color, alpha);
        line(buffer, matrix, d, h, color, alpha);

        double midY = center.y + sy * 0.5D;
        line(buffer, matrix, new Vec3(center.x - hx, midY, center.z - hz), new Vec3(center.x + hx, midY, center.z - hz), color, Math.max(35, alpha / 3));
        line(buffer, matrix, new Vec3(center.x + hx, midY, center.z - hz), new Vec3(center.x + hx, midY, center.z + hz), color, Math.max(35, alpha / 3));
        line(buffer, matrix, new Vec3(center.x + hx, midY, center.z + hz), new Vec3(center.x - hx, midY, center.z + hz), color, Math.max(35, alpha / 3));
        line(buffer, matrix, new Vec3(center.x - hx, midY, center.z + hz), new Vec3(center.x - hx, midY, center.z - hz), color, Math.max(35, alpha / 3));
    }

    private static void drawPlaneOutlines(BufferBuilder buffer, Matrix4f matrix, DialogueZoneWorldEditScreen editor, int alpha) {
        drawPlaneOutline(buffer, matrix, editor, DialogueZoneWorldEditScreen.GizmoPlane.XY, XY_COLOR, alpha);
        drawPlaneOutline(buffer, matrix, editor, DialogueZoneWorldEditScreen.GizmoPlane.XZ, XZ_COLOR, alpha);
        drawPlaneOutline(buffer, matrix, editor, DialogueZoneWorldEditScreen.GizmoPlane.YZ, YZ_COLOR, alpha);
    }

    private static void drawPlaneOutline(BufferBuilder buffer, Matrix4f matrix, DialogueZoneWorldEditScreen editor, DialogueZoneWorldEditScreen.GizmoPlane plane, int baseColor, int alpha) {
        Vec3[] c = editor.planeCorners(plane);
        if (c.length != 4) return;
        boolean selected = editor.dragPlane() == plane || editor.hoveredPlane() == plane;
        int color = selected ? SELECTED_COLOR : baseColor;
        int a = selected ? 255 : Math.max(90, alpha / 2);
        line(buffer, matrix, c[0], c[1], color, a);
        line(buffer, matrix, c[1], c[2], color, a);
        line(buffer, matrix, c[2], c[3], color, a);
        line(buffer, matrix, c[3], c[0], color, a);
    }

    private static void drawResolvedAnchorBounds(BufferBuilder buffer, Matrix4f matrix, DialogueZoneWorldEditScreen editor, int alpha) {
        BlockPos block = editor.resolvedBlockAnchor();
        if (block != null) {
            drawAabb(buffer, matrix, new AABB(block.getX(), block.getY(), block.getZ(), block.getX() + 1.0D, block.getY() + 1.0D, block.getZ() + 1.0D), ANCHOR_COLOR, Math.max(120, alpha));
        }

        if (editor.resolvedEntityAnchor() != null) {
            drawAabb(buffer, matrix, editor.resolvedEntityAnchor().getBoundingBox().inflate(0.025D), ANCHOR_COLOR, Math.max(110, alpha));
        }
    }

    private static void drawAabb(BufferBuilder buffer, Matrix4f matrix, AABB box, int color, int alpha) {
        Vec3 a = new Vec3(box.minX, box.minY, box.minZ);
        Vec3 b = new Vec3(box.maxX, box.minY, box.minZ);
        Vec3 c = new Vec3(box.maxX, box.minY, box.maxZ);
        Vec3 d = new Vec3(box.minX, box.minY, box.maxZ);
        Vec3 e = new Vec3(box.minX, box.maxY, box.minZ);
        Vec3 f = new Vec3(box.maxX, box.maxY, box.minZ);
        Vec3 g = new Vec3(box.maxX, box.maxY, box.maxZ);
        Vec3 h = new Vec3(box.minX, box.maxY, box.maxZ);

        line(buffer, matrix, a, b, color, alpha);
        line(buffer, matrix, b, c, color, alpha);
        line(buffer, matrix, c, d, color, alpha);
        line(buffer, matrix, d, a, color, alpha);
        line(buffer, matrix, e, f, color, alpha);
        line(buffer, matrix, f, g, color, alpha);
        line(buffer, matrix, g, h, color, alpha);
        line(buffer, matrix, h, e, color, alpha);
        line(buffer, matrix, a, e, color, alpha);
        line(buffer, matrix, b, f, color, alpha);
        line(buffer, matrix, c, g, color, alpha);
        line(buffer, matrix, d, h, color, alpha);
    }

    private static void drawGizmo(BufferBuilder buffer, Matrix4f matrix, DialogueZoneWorldEditScreen editor, int alpha) {
        Vec3 center = editor.center();

        if (editor.mode() == DialogueZoneWorldEditScreen.EditMode.MOVE) {
            drawMoveAxis(buffer, matrix, editor, center, DialogueZoneWorldEditScreen.GizmoAxis.X, X_COLOR, alpha);
            drawMoveAxis(buffer, matrix, editor, center, DialogueZoneWorldEditScreen.GizmoAxis.Y, Y_COLOR, alpha);
            drawMoveAxis(buffer, matrix, editor, center, DialogueZoneWorldEditScreen.GizmoAxis.Z, Z_COLOR, alpha);
            return;
        }

        drawSizeAxis(buffer, matrix, editor, center, DialogueZoneWorldEditScreen.GizmoAxis.X, X_COLOR, alpha);
        drawSizeAxis(buffer, matrix, editor, center, DialogueZoneWorldEditScreen.GizmoAxis.Y, Y_COLOR, alpha);
        drawSizeAxis(buffer, matrix, editor, center, DialogueZoneWorldEditScreen.GizmoAxis.Z, Z_COLOR, alpha);
    }

    private static void drawMoveAxis(BufferBuilder buffer, Matrix4f matrix, DialogueZoneWorldEditScreen editor, Vec3 start, DialogueZoneWorldEditScreen.GizmoAxis axis, int baseColor, int alpha) {
        Vec3 end = editor.gizmoEnd(axis, 1);
        boolean selected = (editor.dragAxis() == axis && editor.dragAxisSign() > 0) || (editor.hoveredAxis() == axis && editor.hoveredAxisSign() > 0);
        int color = selected ? SELECTED_COLOR : baseColor;
        int a = selected ? 255 : alpha;

        line(buffer, matrix, start, end, color, a);

        double handle = editor.handleSize();
        drawWireCube(buffer, matrix, end, handle, color, a);
        drawArrowHead(buffer, matrix, start, end, axis, color, a, handle * 2.1D);
    }

    private static void drawSizeAxis(BufferBuilder buffer, Matrix4f matrix, DialogueZoneWorldEditScreen editor, Vec3 start, DialogueZoneWorldEditScreen.GizmoAxis axis, int baseColor, int alpha) {
        drawSizeHandle(buffer, matrix, editor, start, axis, 1, baseColor, alpha);
        if (editor.supportsNegativeSizeHandle(axis)) {
            drawSizeHandle(buffer, matrix, editor, start, axis, -1, baseColor, alpha);
        }
    }

    private static void drawSizeHandle(BufferBuilder buffer, Matrix4f matrix, DialogueZoneWorldEditScreen editor, Vec3 start, DialogueZoneWorldEditScreen.GizmoAxis axis, int sign, int baseColor, int alpha) {
        Vec3 end = editor.gizmoEnd(axis, sign);
        boolean selected = (editor.dragAxis() == axis && editor.dragAxisSign() == sign) || (editor.hoveredAxis() == axis && editor.hoveredAxisSign() == sign);
        int color = selected ? SELECTED_COLOR : baseColor;
        int a = selected ? 255 : alpha;

        line(buffer, matrix, start, end, color, Math.max(90, a));
        double handle = editor.handleSize() * (selected ? 1.25D : 1.05D);
        drawWireCube(buffer, matrix, end, handle, color, a);
        Vec3 axisVec = switch (axis) {
            case X -> new Vec3(0, handle * 1.6D, 0);
            case Y -> new Vec3(handle * 1.6D, 0, 0);
            case Z -> new Vec3(0, handle * 1.6D, 0);
            default -> Vec3.ZERO;
        };
        line(buffer, matrix, end.subtract(axisVec), end.add(axisVec), color, a);
    }

    private static void drawArrowHead(BufferBuilder buffer, Matrix4f matrix, Vec3 start, Vec3 end, DialogueZoneWorldEditScreen.GizmoAxis axis, int color, int alpha, double size) {
        Vec3 direction = end.subtract(start).normalize();
        Vec3 back = end.subtract(direction.scale(size));

        switch (axis) {
            case X -> {
                line(buffer, matrix, end, back.add(0, size * 0.45D, 0), color, alpha);
                line(buffer, matrix, end, back.add(0, -size * 0.45D, 0), color, alpha);
                line(buffer, matrix, end, back.add(0, 0, size * 0.45D), color, alpha);
                line(buffer, matrix, end, back.add(0, 0, -size * 0.45D), color, alpha);
            }
            case Y -> {
                line(buffer, matrix, end, back.add(size * 0.45D, 0, 0), color, alpha);
                line(buffer, matrix, end, back.add(-size * 0.45D, 0, 0), color, alpha);
                line(buffer, matrix, end, back.add(0, 0, size * 0.45D), color, alpha);
                line(buffer, matrix, end, back.add(0, 0, -size * 0.45D), color, alpha);
            }
            case Z -> {
                line(buffer, matrix, end, back.add(size * 0.45D, 0, 0), color, alpha);
                line(buffer, matrix, end, back.add(-size * 0.45D, 0, 0), color, alpha);
                line(buffer, matrix, end, back.add(0, size * 0.45D, 0), color, alpha);
                line(buffer, matrix, end, back.add(0, -size * 0.45D, 0), color, alpha);
            }
        }
    }

    private static void drawCenterMarker(BufferBuilder buffer, Matrix4f matrix, Vec3 center, int color, int alpha, double handleSize) {
        double s = handleSize * 0.75D;
        drawWireCube(buffer, matrix, center, s, color, alpha);

        line(buffer, matrix, center.add(-s * 1.8D, 0, 0), center.add(s * 1.8D, 0, 0), color, alpha);
        line(buffer, matrix, center.add(0, -s * 1.8D, 0), center.add(0, s * 1.8D, 0), color, alpha);
        line(buffer, matrix, center.add(0, 0, -s * 1.8D), center.add(0, 0, s * 1.8D), color, alpha);
    }

    private static void drawCursorCross(BufferBuilder buffer, Matrix4f matrix, Vec3 center, double size, int color, int alpha) {
        line(buffer, matrix, center.add(-size, 0, 0), center.add(size, 0, 0), color, alpha);
        line(buffer, matrix, center.add(0, -size, 0), center.add(0, size, 0), color, alpha);
        line(buffer, matrix, center.add(0, 0, -size), center.add(0, 0, size), color, alpha);
        circleXZ(buffer, matrix, center.x, center.y + 0.01D, center.z, size * 1.4D, 20, color, alpha);
    }

    private static void drawWireCube(BufferBuilder buffer, Matrix4f matrix, Vec3 center, double half, int color, int alpha) {
        double x0 = center.x - half;
        double x1 = center.x + half;
        double y0 = center.y - half;
        double y1 = center.y + half;
        double z0 = center.z - half;
        double z1 = center.z + half;

        Vec3 a = new Vec3(x0, y0, z0);
        Vec3 b = new Vec3(x1, y0, z0);
        Vec3 c = new Vec3(x1, y0, z1);
        Vec3 d = new Vec3(x0, y0, z1);
        Vec3 e = new Vec3(x0, y1, z0);
        Vec3 f = new Vec3(x1, y1, z0);
        Vec3 g = new Vec3(x1, y1, z1);
        Vec3 h = new Vec3(x0, y1, z1);

        line(buffer, matrix, a, b, color, alpha);
        line(buffer, matrix, b, c, color, alpha);
        line(buffer, matrix, c, d, color, alpha);
        line(buffer, matrix, d, a, color, alpha);
        line(buffer, matrix, e, f, color, alpha);
        line(buffer, matrix, f, g, color, alpha);
        line(buffer, matrix, g, h, color, alpha);
        line(buffer, matrix, h, e, color, alpha);
        line(buffer, matrix, a, e, color, alpha);
        line(buffer, matrix, b, f, color, alpha);
        line(buffer, matrix, c, g, color, alpha);
        line(buffer, matrix, d, h, color, alpha);
    }

    private static void fillBox(BufferBuilder buffer, Matrix4f matrix, AABB box, int color, int alpha) {
        Vec3 a = new Vec3(box.minX, box.minY, box.minZ);
        Vec3 b = new Vec3(box.maxX, box.minY, box.minZ);
        Vec3 c = new Vec3(box.maxX, box.minY, box.maxZ);
        Vec3 d = new Vec3(box.minX, box.minY, box.maxZ);
        Vec3 e = new Vec3(box.minX, box.maxY, box.minZ);
        Vec3 f = new Vec3(box.maxX, box.maxY, box.minZ);
        Vec3 g = new Vec3(box.maxX, box.maxY, box.maxZ);
        Vec3 h = new Vec3(box.minX, box.maxY, box.maxZ);

        quad(buffer, matrix, a, b, c, d, color, alpha);
        quad(buffer, matrix, e, h, g, f, color, alpha);
        quad(buffer, matrix, a, e, f, b, color, alpha);
        quad(buffer, matrix, b, f, g, c, color, alpha);
        quad(buffer, matrix, c, g, h, d, color, alpha);
        quad(buffer, matrix, d, h, e, a, color, alpha);
    }

    private static void quad(BufferBuilder buffer, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int rgb, int alpha) {
        int r = rgb >> 16 & 255;
        int g = rgb >> 8 & 255;
        int bl = rgb & 255;
        int clampedAlpha = Mth.clamp(alpha, 0, 255);

        buffer.vertex(matrix, (float) a.x, (float) a.y, (float) a.z).color(r, g, bl, clampedAlpha).endVertex();
        buffer.vertex(matrix, (float) b.x, (float) b.y, (float) b.z).color(r, g, bl, clampedAlpha).endVertex();
        buffer.vertex(matrix, (float) c.x, (float) c.y, (float) c.z).color(r, g, bl, clampedAlpha).endVertex();
        buffer.vertex(matrix, (float) d.x, (float) d.y, (float) d.z).color(r, g, bl, clampedAlpha).endVertex();
    }

    private static void circleXZ(BufferBuilder buffer, Matrix4f matrix, double cx, double cy, double cz, double radius, int segments, int color, int alpha) {
        for (int i = 0; i < segments; i++) {
            double a = Math.PI * 2.0D * i / segments;
            double b = Math.PI * 2.0D * (i + 1) / segments;
            line(buffer, matrix, new Vec3(cx + Math.cos(a) * radius, cy, cz + Math.sin(a) * radius), new Vec3(cx + Math.cos(b) * radius, cy, cz + Math.sin(b) * radius), color, alpha);
        }
    }

    private static void circleXY(BufferBuilder buffer, Matrix4f matrix, double cx, double cy, double cz, double radius, int segments, int color, int alpha) {
        for (int i = 0; i < segments; i++) {
            double a = Math.PI * 2.0D * i / segments;
            double b = Math.PI * 2.0D * (i + 1) / segments;
            line(buffer, matrix, new Vec3(cx + Math.cos(a) * radius, cy + Math.sin(a) * radius, cz), new Vec3(cx + Math.cos(b) * radius, cy + Math.sin(b) * radius, cz), color, alpha);
        }
    }

    private static void circleYZ(BufferBuilder buffer, Matrix4f matrix, double cx, double cy, double cz, double radius, int segments, int color, int alpha) {
        for (int i = 0; i < segments; i++) {
            double a = Math.PI * 2.0D * i / segments;
            double b = Math.PI * 2.0D * (i + 1) / segments;
            line(buffer, matrix, new Vec3(cx, cy + Math.sin(a) * radius, cz + Math.cos(a) * radius), new Vec3(cx, cy + Math.sin(b) * radius, cz + Math.cos(b) * radius), color, alpha);
        }
    }

    private static void line(BufferBuilder buffer, Matrix4f matrix, Vec3 a, Vec3 b, int rgb, int alpha) {
        int r = rgb >> 16 & 255;
        int g = rgb >> 8 & 255;
        int bl = rgb & 255;
        int clampedAlpha = Mth.clamp(alpha, 0, 255);

        buffer.vertex(matrix, (float) a.x, (float) a.y, (float) a.z).color(r, g, bl, clampedAlpha).endVertex();
        buffer.vertex(matrix, (float) b.x, (float) b.y, (float) b.z).color(r, g, bl, clampedAlpha).endVertex();
    }

    private static void renderCustomFloorSprite(DialogueZoneWorldEditScreen editor, PoseStack poseStack, Minecraft minecraft) {
        DialogueDefinition.Trigger trigger = editor.trigger();
        DialogueDefinition.ZoneVisual visual = trigger.visual;
        if (visual == null || visual.texture == null || visual.texture.isBlank()) {
            return;
        }

        String style = visual.style != null ? visual.style.toLowerCase(Locale.ROOT) : "auto";
        if (!"auto".equals(style) && !"sprite".equals(style)) {
            return;
        }

        ResourceLocation texture = DialogueEditorTextureCache.resolve(editor.project(), visual.texture, null);
        if (texture == null) {
            return;
        }

        Vec3 center = editor.center();
        double size = visual.size > 0.0D ? visual.size : horizontalExtent(trigger) * 2.0D;
        size = Math.max(0.1D, size);
        double half = size * 0.5D;
        double y = center.y + visual.y_offset + 0.004D;

        int rgb = DialogueEditorPreview.parseColor(visual.color);
        int r = rgb >> 16 & 255;
        int g = rgb >> 8 & 255;
        int b = rgb & 255;
        int a = Mth.clamp(Math.round(Mth.clamp(visual.alpha, 0.0F, 1.0F) * 190.0F), 0, 255);

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType type = RenderType.entityTranslucent(texture);
        VertexConsumer consumer = buffers.getBuffer(type);
        PoseStack.Pose pose = poseStack.last();

        consumer.vertex(pose.pose(), (float) (center.x - half), (float) y, (float) (center.z - half)).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(pose.pose(), (float) (center.x - half), (float) y, (float) (center.z + half)).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(pose.pose(), (float) (center.x + half), (float) y, (float) (center.z + half)).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(pose.pose(), (float) (center.x + half), (float) y, (float) (center.z - half)).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();

        buffers.endBatch(type);
    }

    private static double horizontalExtent(DialogueDefinition.Trigger trigger) {
        return switch (normalizeShape(trigger.shape)) {
            case "box" -> Math.max(trigger.size_x, trigger.size_z) * 0.5D;
            default -> Math.max(0.1D, trigger.radius);
        };
    }

    private static String normalizeShape(String shape) {
        if (shape == null) {
            return "cylinder";
        }

        String normalized = shape.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "sphere", "box", "cylinder" -> normalized;
            default -> "cylinder";
        };
    }
}
