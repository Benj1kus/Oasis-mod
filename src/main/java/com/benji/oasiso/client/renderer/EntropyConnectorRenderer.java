package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.renderutil.EntropyRopeRenderUtil;
import com.benji.oasiso.common.block.entity.EntropyConnectorBlockEntity;
import com.benji.oasiso.common.entity.EntropyPhysicsBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EntropyConnectorRenderer implements BlockEntityRenderer<EntropyConnectorBlockEntity> {

    private static final ResourceLocation ARROW_TEXTURE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/block/connector_arrow.png");
    private static final ResourceLocation HINT_TEXTURE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/block/connector_hint.png");

    private static final float ARROW_SIZE = 0.62F;
    private static final double ARROW_DISTANCE = 0.82D;
    private static final double ARROW_MOVE_DISTANCE = 0.105D;
    private static final double ARROW_Y = 1.015D;
    private static final float ARROW_ALPHA = 0.82F;
    private static final int ROPE_SEGMENTS_PER_BLOCK = 4;
    private static final int MIN_ROPE_SEGMENTS = 6;
    private static final int MAX_ROPE_SEGMENTS = 40;
    private static final double ROPE_SAG_PER_BLOCK = 0.035D;
    private static final double MAX_ROPE_SAG = 0.35D;

    public EntropyConnectorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(EntropyConnectorBlockEntity connector, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (connector.getLevel() == null) {
            return;
        }
        float time = connector.getLevel().getGameTime() + partialTick;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (!connector.shouldShowArrow(side)) {
                continue;
            }
            renderArrow(side, time, poseStack, bufferSource);
        }

        Minecraft minecraft = Minecraft.getInstance();

        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 renderOrigin = new Vec3(connector.getBlockPos().getX(), connector.getBlockPos().getY(), connector.getBlockPos().getZ());

        for (EntropyConnectorBlockEntity.PullState pull : connector.getActivePulls()) {

            Vec3 start = getConnectorMouth(connector, pull.side());
            Vec3 end;

            if (pull.phase() == EntropyConnectorBlockEntity.PHASE_LAUNCHING) {

                Vec3 target = Vec3.atCenterOf(pull.sourcePos());
                float age = (float) (connector.getLevel().getGameTime() - pull.phaseStartGameTime()) + partialTick;
                float progress = pull.durationTicks() <= 0 ? 1.0F : Mth.clamp(age / pull.durationTicks(), 0.0F, 1.0F);

                progress = easeOutCubic(progress);
                end = start.lerp(target, progress);

            } else {
                EntropyPhysicsBlockEntity physics = findPhysicsEntity(connector, pull.physicsEntityId());
                if (physics == null) {
                    continue;
                }
                end = physics.position().add(0.0D, 0.50D, 0.0D);
            }

            List<Vec3> ropePoints = createRopePoints(start, end);

            EntropyRopeRenderUtil.renderCyanRope(poseStack, ropePoints, camera, renderOrigin);

            EntropyRopeRenderUtil.renderCyanHook(poseStack, end, renderOrigin, 0.085F);
        }
        renderPlatformHint(connector, partialTick, time, poseStack, bufferSource);
    }

    private static void renderPlatformHint(EntropyConnectorBlockEntity connector, float partialTick, float time, PoseStack poseStack, MultiBufferSource bufferSource) {
        float alpha = connector.getPlatformHintAlpha(partialTick);

        if (alpha <= 0.001F) {
            return;
        }

        float bob = Mth.sin(time * 0.16F) * 0.055F;

        poseStack.pushPose();
        poseStack.translate(0.5D, 1.72D + bob, 0.5D);
        poseStack.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());

        float halfWidth = 0.46F;
        float halfHeight = halfWidth * 26.0F / 28.0F;

        int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, HINT_TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Matrix4f matrix = poseStack.last().pose();

        builder.vertex(matrix, -halfWidth, -halfHeight, 0.0F).uv(0.0F, 1.0F).color(255, 255, 255, a).endVertex();
        builder.vertex(matrix, halfWidth, -halfHeight, 0.0F).uv(1.0F, 1.0F).color(255, 255, 255, a).endVertex();
        builder.vertex(matrix, halfWidth, halfHeight, 0.0F).uv(1.0F, 0.0F).color(255, 255, 255, a).endVertex();
        builder.vertex(matrix, -halfWidth, halfHeight, 0.0F).uv(0.0F, 0.0F).color(255, 255, 255, a).endVertex();

        BufferUploader.drawWithShader(builder.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    private static void renderArrow(Direction side, float time, PoseStack poseStack, MultiBufferSource bufferSource) {
        float phase = time * 0.16F + side.get2DDataValue() * 1.21F;
        double movement = Mth.sin(phase) * ARROW_MOVE_DISTANCE;
        float scale = 0.96F + 0.04F * Mth.sin(time * 0.21F + side.get2DDataValue());
        float alphaPulse = 0.88F + 0.12F * Mth.sin(time * 0.18F + side.get2DDataValue());
        double distance = ARROW_DISTANCE + movement;

        Vec3 forward = new Vec3(side.getStepX(), 0.0D, side.getStepZ());
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);
        Vec3 center = new Vec3(0.5D + forward.x * distance, ARROW_Y, 0.5D + forward.z * distance);

        double half = ARROW_SIZE * scale * 0.5D;

        Vec3 topLeft = center.add(forward.scale(half)).subtract(right.scale(half));
        Vec3 topRight = center.add(forward.scale(half)).add(right.scale(half));
        Vec3 bottomRight = center.subtract(forward.scale(half)).add(right.scale(half));
        Vec3 bottomLeft = center.subtract(forward.scale(half)).subtract(right.scale(half));
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(ARROW_TEXTURE));

        int alpha = Mth.clamp((int) (255.0F * ARROW_ALPHA * alphaPulse), 0, 255);

        PoseStack.Pose pose = poseStack.last();

        arrowVertex(consumer, pose, topLeft, 0.0F, 0.0F, alpha);
        arrowVertex(consumer, pose, topRight, 1.0F, 0.0F, alpha);
        arrowVertex(consumer, pose, bottomRight, 1.0F, 1.0F, alpha);
        arrowVertex(consumer, pose, bottomLeft, 0.0F, 1.0F, alpha);
    }

    private static void arrowVertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 position, float u, float v, int alpha) {
        consumer.vertex(pose.pose(), (float) position.x, (float) position.y, (float) position.z).color(255, 255, 255, alpha).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();
    }

    private static Vec3 getConnectorMouth(EntropyConnectorBlockEntity connector, Direction side) {
        return Vec3.atCenterOf(connector.getBlockPos()).add(side.getStepX() * 0.505D, 0.0D, side.getStepZ() * 0.505D);
    }

    private static List<Vec3> createRopePoints(Vec3 start, Vec3 end) {
        double distance = start.distanceTo(end);

        int segments = Mth.clamp((int) Math.ceil(distance * ROPE_SEGMENTS_PER_BLOCK), MIN_ROPE_SEGMENTS, MAX_ROPE_SEGMENTS);
        List<Vec3> points = new ArrayList<>(segments + 1);
        double sag = Math.min(MAX_ROPE_SAG, distance * ROPE_SAG_PER_BLOCK);

        for (int i = 0; i <= segments; i++) {
            double t = i / (double) segments;
            Vec3 point = start.lerp(end, t);

            double verticalSag = Math.sin(Math.PI * t) * sag;

            point = point.add(0.0D, -verticalSag, 0.0D);
            points.add(point);
        }

        return points;
    }

    private static EntropyPhysicsBlockEntity findPhysicsEntity(EntropyConnectorBlockEntity connector, UUID uuid) {
        if (uuid == null || connector.getLevel() == null) {

            return null;
        }

        AABB search = new AABB(connector.getBlockPos()).inflate(EntropyConnectorBlockEntity.MAX_DISTANCE + 3.0D);
        List<EntropyPhysicsBlockEntity> entities = connector.getLevel().getEntitiesOfClass(EntropyPhysicsBlockEntity.class, search, entity -> uuid.equals(entity.getUUID()));
        return entities.isEmpty() ? null : entities.get(0);
    }

    private static float easeOutCubic(float value) {
        value = Mth.clamp(value, 0.0F, 1.0F);

        float inverse = 1.0F - value;

        return 1.0F - inverse * inverse * inverse;
    }

    @Override
    public boolean shouldRenderOffScreen(EntropyConnectorBlockEntity blockEntity) {
        return true;
    }
}