package com.benji.oasiso.client.dialogue;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.network.dialogueengine.DialogueZonePreviewS2CPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Locale;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DialogueZonePreviewRenderer {

    private static List<DialogueZonePreviewS2CPacket.Zone> zones = List.of();

    private DialogueZonePreviewRenderer() {
    }


    public static void setZones(List<DialogueZonePreviewS2CPacket.Zone> newZones) {
        zones = newZones != null ? List.copyOf(newZones) : List.of();
    }


    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (Minecraft.getInstance().level == null) {
            zones = List.of();
        }
    }


    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null || zones.isEmpty()) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();

        PoseStack poseStack = event.getPoseStack();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        float time = minecraft.level.getGameTime() + minecraft.getFrameTime();

        Vec3 playerPos = minecraft.player.position();

        for (DialogueZonePreviewS2CPacket.Zone zone : zones) {
            renderZone(poseStack, buffers, zone, playerPos, time);
        }

        buffers.endBatch();

        poseStack.popPose();
    }


    private static void renderZone(PoseStack poseStack, MultiBufferSource.BufferSource buffers, DialogueZonePreviewS2CPacket.Zone zone, Vec3 playerPos, float time) {
        Vec3 center = new Vec3(zone.x(), zone.y(), zone.z());

        float distanceFade = distanceFade(playerPos.distanceTo(center), zone.previewDistance());

        if (distanceFade <= 0.001F) {
            return;
        }

        float pulse = zone.pulse() ? 0.5F + 0.5F * Mth.sin(time * 0.12F) : 0.5F;

        float alpha = Mth.clamp(zone.alpha() * distanceFade * (0.82F + pulse * 0.18F), 0.0F, 1.0F);

        float scale = zone.pulse() ? 0.97F + pulse * 0.06F : 1.0F;

        int color = parseColor(zone.color());

        String style = resolveStyle(zone);

        switch (style) {
            case "sprite" -> renderSprite(poseStack, buffers, zone, color, alpha, scale);

            case "pillar" -> renderPillar(poseStack, buffers, zone, color, alpha, scale);

            case "outline" -> renderOutline(poseStack, buffers, zone, color, alpha, scale);

            default -> renderRing(poseStack, buffers, zone, color, alpha, scale);
        }
    }


    private static String resolveStyle(DialogueZonePreviewS2CPacket.Zone zone) {
        String style = zone.style() != null ? zone.style().toLowerCase(Locale.ROOT) : "auto";

        if (!"auto".equals(style)) {
            return style;
        }

        if (zone.texture() != null && !zone.texture().isBlank()) {
            return "sprite";
        }

        String shape = zone.shape() != null ? zone.shape().toLowerCase(Locale.ROOT) : "cylinder";

        return "cylinder".equals(shape) ? "ring" : "outline";
    }


    private static void renderRing(PoseStack poseStack, MultiBufferSource.BufferSource buffers, DialogueZonePreviewS2CPacket.Zone zone, int color, float alpha, float pulseScale) {
        double radius = zone.visualSize() > 0.0D ? zone.visualSize() * 0.5D : defaultRadius(zone);

        radius *= pulseScale;

        double y = zone.y() + zone.yOffset();

        VertexConsumer consumer = buffers.getBuffer(RenderType.lines());

        drawCircleXZ(consumer, poseStack, zone.x(), y, zone.z(), radius, color, alpha);
        drawCircleXZ(consumer, poseStack, zone.x(), y + 0.004D, zone.z(), radius * 0.94D, color, alpha * 0.45F);
    }


    private static void renderOutline(PoseStack poseStack, MultiBufferSource.BufferSource buffers, DialogueZonePreviewS2CPacket.Zone zone, int color, float alpha, float pulseScale) {
        String shape = zone.shape() != null ? zone.shape().toLowerCase(Locale.ROOT) : "cylinder";

        VertexConsumer consumer = buffers.getBuffer(RenderType.lines());

        switch (shape) {
            case "sphere" -> drawSphereOutline(consumer, poseStack, zone, color, alpha, pulseScale);

            case "box" -> drawBoxOutline(consumer, poseStack, zone, color, alpha, pulseScale);

            default -> drawCylinderOutline(consumer, poseStack, zone, color, alpha, pulseScale);
        }
    }


    private static void renderPillar(PoseStack poseStack, MultiBufferSource.BufferSource buffers, DialogueZonePreviewS2CPacket.Zone zone, int color, float alpha, float pulseScale) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.lines());

        double radius = zone.visualSize() > 0.0D ? zone.visualSize() * 0.5D : defaultRadius(zone);

        radius *= pulseScale;

        double baseY = zone.y() + zone.yOffset();

        double height = zone.visualHeight() > 0.0D ? zone.visualHeight() : Math.max(0.25D, zone.height());

        double topY = baseY + height;

        drawCircleXZ(consumer, poseStack, zone.x(), baseY, zone.z(), radius, color, alpha);

        drawCircleXZ(consumer, poseStack, zone.x(), topY, zone.z(), radius, color, alpha * 0.65F);

        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0D * i / 8.0D;

            double x = zone.x() + Math.cos(angle) * radius;

            double z = zone.z() + Math.sin(angle) * radius;

            line(consumer, poseStack, x, baseY, z, x, topY, z, color, alpha * 0.40F);
        }
    }


    private static void renderSprite(PoseStack poseStack, MultiBufferSource.BufferSource buffers, DialogueZonePreviewS2CPacket.Zone zone, int color, float alpha, float pulseScale) {
        if (zone.texture() == null || zone.texture().isBlank()) {

            renderRing(poseStack, buffers, zone, color, alpha, pulseScale);

            return;
        }

        ResourceLocation texture = ResourceLocation.tryParse(zone.texture());

        if (texture == null) {
            renderRing(poseStack, buffers, zone, color, alpha, pulseScale);

            return;
        }

        double size = zone.visualSize() > 0.0D ? zone.visualSize() : defaultRadius(zone) * 2.0D;

        size *= pulseScale;

        double half = size * 0.5D;

        double y = zone.y() + zone.yOffset();

        int red = color >> 16 & 255;

        int green = color >> 8 & 255;

        int blue = color & 255;

        int alphaByte = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);

        RenderType renderType = RenderType.entityTranslucent(texture);

        VertexConsumer consumer = buffers.getBuffer(renderType);

        PoseStack.Pose pose = poseStack.last();
        consumer.vertex(pose.pose(), (float) (zone.x() - half), (float) y, (float) (zone.z() - half)).color(red, green, blue, alphaByte).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();

        consumer.vertex(pose.pose(), (float) (zone.x() - half), (float) y, (float) (zone.z() + half)).color(red, green, blue, alphaByte).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();

        consumer.vertex(pose.pose(), (float) (zone.x() + half), (float) y, (float) (zone.z() + half)).color(red, green, blue, alphaByte).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();

        consumer.vertex(pose.pose(), (float) (zone.x() + half), (float) y, (float) (zone.z() - half)).color(red, green, blue, alphaByte).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();
    }


    private static void drawCylinderOutline(VertexConsumer consumer, PoseStack poseStack, DialogueZonePreviewS2CPacket.Zone zone, int color, float alpha, float pulseScale) {
        double radius = Math.max(0.1D, zone.radius()) * pulseScale;

        double baseY = zone.y() + zone.yOffset();

        double topY = baseY + Math.max(0.1D, zone.height());

        drawCircleXZ(consumer, poseStack, zone.x(), baseY, zone.z(), radius, color, alpha);

        drawCircleXZ(consumer, poseStack, zone.x(), topY, zone.z(), radius, color, alpha * 0.65F);

        for (int i = 0; i < 4; i++) {
            double angle = Math.PI * 2.0D * i / 4.0D;

            double x = zone.x() + Math.cos(angle) * radius;

            double z = zone.z() + Math.sin(angle) * radius;

            line(consumer, poseStack, x, baseY, z, x, topY, z, color, alpha * 0.45F);
        }
    }


    private static void drawSphereOutline(VertexConsumer consumer, PoseStack poseStack, DialogueZonePreviewS2CPacket.Zone zone, int color, float alpha, float pulseScale) {
        double radius = Math.max(0.1D, zone.radius()) * pulseScale;

        double y = zone.y() + zone.yOffset();

        drawCircleXZ(consumer, poseStack, zone.x(), y, zone.z(), radius, color, alpha);

        drawCircleXY(consumer, poseStack, zone.x(), y, zone.z(), radius, color, alpha * 0.65F);

        drawCircleYZ(consumer, poseStack, zone.x(), y, zone.z(), radius, color, alpha * 0.65F);
    }


    private static void drawBoxOutline(VertexConsumer consumer, PoseStack poseStack, DialogueZonePreviewS2CPacket.Zone zone, int color, float alpha, float pulseScale) {
        double halfX = Math.max(0.1D, zone.sizeX()) * 0.5D * pulseScale;

        double halfZ = Math.max(0.1D, zone.sizeZ()) * 0.5D * pulseScale;

        double baseY = zone.y() + zone.yOffset();

        double topY = baseY + Math.max(0.1D, zone.sizeY());

        double minX = zone.x() - halfX;

        double maxX = zone.x() + halfX;

        double minZ = zone.z() - halfZ;

        double maxZ = zone.z() + halfZ;

        // Bottom
        line(consumer, poseStack, minX, baseY, minZ, maxX, baseY, minZ, color, alpha);
        line(consumer, poseStack, maxX, baseY, minZ, maxX, baseY, maxZ, color, alpha);
        line(consumer, poseStack, maxX, baseY, maxZ, minX, baseY, maxZ, color, alpha);
        line(consumer, poseStack, minX, baseY, maxZ, minX, baseY, minZ, color, alpha);

        // Top
        line(consumer, poseStack, minX, topY, minZ, maxX, topY, minZ, color, alpha * 0.65F);
        line(consumer, poseStack, maxX, topY, minZ, maxX, topY, maxZ, color, alpha * 0.65F);
        line(consumer, poseStack, maxX, topY, maxZ, minX, topY, maxZ, color, alpha * 0.65F);
        line(consumer, poseStack, minX, topY, maxZ, minX, topY, minZ, color, alpha * 0.65F);

        // Vertical
        line(consumer, poseStack, minX, baseY, minZ, minX, topY, minZ, color, alpha * 0.45F);
        line(consumer, poseStack, maxX, baseY, minZ, maxX, topY, minZ, color, alpha * 0.45F);
        line(consumer, poseStack, maxX, baseY, maxZ, maxX, topY, maxZ, color, alpha * 0.45F);
        line(consumer, poseStack, minX, baseY, maxZ, minX, topY, maxZ, color, alpha * 0.45F);
    }


    private static void drawCircleXZ(VertexConsumer consumer, PoseStack poseStack, double centerX, double centerY, double centerZ, double radius, int color, float alpha) {
        final int segments = 64;

        for (int i = 0; i < segments; i++) {
            double a = Math.PI * 2.0D * i / segments;

            double b = Math.PI * 2.0D * (i + 1) / segments;

            line(consumer, poseStack,

                    centerX + Math.cos(a) * radius, centerY, centerZ + Math.sin(a) * radius,

                    centerX + Math.cos(b) * radius, centerY, centerZ + Math.sin(b) * radius,

                    color, alpha);
        }
    }


    private static void drawCircleXY(VertexConsumer consumer, PoseStack poseStack, double centerX, double centerY, double centerZ, double radius, int color, float alpha) {
        final int segments = 48;

        for (int i = 0; i < segments; i++) {
            double a = Math.PI * 2.0D * i / segments;

            double b = Math.PI * 2.0D * (i + 1) / segments;

            line(consumer, poseStack,

                    centerX + Math.cos(a) * radius, centerY + Math.sin(a) * radius, centerZ,

                    centerX + Math.cos(b) * radius, centerY + Math.sin(b) * radius, centerZ,

                    color, alpha);
        }
    }


    private static void drawCircleYZ(VertexConsumer consumer, PoseStack poseStack, double centerX, double centerY, double centerZ, double radius, int color, float alpha) {
        final int segments = 48;

        for (int i = 0; i < segments; i++) {
            double a = Math.PI * 2.0D * i / segments;

            double b = Math.PI * 2.0D * (i + 1) / segments;

            line(consumer, poseStack,

                    centerX, centerY + Math.sin(a) * radius, centerZ + Math.cos(a) * radius,

                    centerX, centerY + Math.sin(b) * radius, centerZ + Math.cos(b) * radius,

                    color, alpha);
        }
    }


    private static void line(VertexConsumer consumer, PoseStack poseStack,

                             double x1, double y1, double z1,

                             double x2, double y2, double z2,

                             int color, float alpha) {
        int red = color >> 16 & 255;

        int green = color >> 8 & 255;

        int blue = color & 255;

        int alphaByte = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);

        PoseStack.Pose pose = poseStack.last();

        consumer.vertex(pose.pose(), (float) x1, (float) y1, (float) z1).color(red, green, blue, alphaByte).normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();

        consumer.vertex(pose.pose(), (float) x2, (float) y2, (float) z2).color(red, green, blue, alphaByte).normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();
    }


    private static double defaultRadius(DialogueZonePreviewS2CPacket.Zone zone) {
        if ("box".equalsIgnoreCase(zone.shape())) {
            return Math.max(zone.sizeX(), zone.sizeZ()) * 0.5D;
        }

        return Math.max(0.1D, zone.radius());
    }


    private static float distanceFade(double distance, double previewDistance) {
        if (previewDistance <= 0.0D) {
            return 1.0F;
        }

        if (distance >= previewDistance) {
            return 0.0F;
        }

        double fadeStart = previewDistance * 0.78D;

        if (distance <= fadeStart) {
            return 1.0F;
        }

        return (float) Mth.clamp(1.0D - (distance - fadeStart) / Math.max(0.001D, previewDistance - fadeStart), 0.0D, 1.0D);
    }


    private static int parseColor(String value) {
        if (value == null) {
            return 0x42F2E1;
        }

        value = value.trim().toLowerCase(Locale.ROOT);

        return switch (value) {
            case "blue" -> 0x4AA3FF;
            case "red" -> 0xFF4D55;
            case "gold", "golden" -> 0xFFD45A;
            case "green" -> 0x55E878;
            case "white" -> 0xFFFFFF;
            case "black" -> 0x000000;
            case "purple" -> 0xB76CFF;
            case "cyan" -> 0x42F2E1;
            default -> parseHex(value);
        };
    }


    private static int parseHex(String value) {
        try {
            if (value.startsWith("#")) {
                value = value.substring(1);
            }

            if (value.startsWith("0x")) {
                value = value.substring(2);
            }

            if (value.length() == 3) {
                value = "" + value.charAt(0) + value.charAt(0) + value.charAt(1) + value.charAt(1) + value.charAt(2) + value.charAt(2);
            }

            return Integer.parseInt(value, 16) & 0xFFFFFF;

        } catch (Exception ignored) {
            return 0x42F2E1;
        }
    }
}