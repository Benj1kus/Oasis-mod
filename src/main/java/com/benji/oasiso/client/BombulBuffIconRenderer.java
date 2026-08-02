package com.benji.oasiso.client;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.support.BombulBuffHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(
        modid = Oasiso.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class BombulBuffIconRenderer {

    private static final ResourceLocation BUFFED_ICON =
            ResourceLocation.fromNamespaceAndPath(
                    Oasiso.MODID,
                    "textures/gui/buffed_mob.png"
            );

    private static final float ICON_SCALE = 0.025F;
    private static final float HALF_SIZE = 8.0F;
    private static final double MAX_RENDER_DISTANCE_SQR = 64.0D * 64.0D;

    private BombulBuffIconRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        float partialTick = event.getPartialTick();

        PoseStack poseStack = event.getPoseStack();

        MultiBufferSource.BufferSource bufferSource =
                minecraft.renderBuffers().bufferSource();

        RenderType renderType =
                RenderType.entityCutoutNoCull(BUFFED_ICON);

        VertexConsumer consumer =
                bufferSource.getBuffer(renderType);

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity livingEntity)) {
                continue;
            }

            if (!livingEntity.isAlive()) {
                continue;
            }

            if (!isBuffed(livingEntity)) {
                continue;
            }

            if (livingEntity.distanceToSqr(cameraPos) > MAX_RENDER_DISTANCE_SQR) {
                continue;
            }

            renderIcon(
                    livingEntity,
                    poseStack,
                    consumer,
                    cameraPos,
                    partialTick
            );
        }


        bufferSource.endBatch(renderType);
    }

    private static boolean isBuffed(LivingEntity entity) {

        return entity.hasEffect(Oasiso.BOMBUL_BUFF_EFFECT.get())
                || BombulBuffHandler.isBuffed(entity);
    }

    private static void renderIcon(
            LivingEntity entity,
            PoseStack poseStack,
            VertexConsumer consumer,
            Vec3 cameraPos,
            float partialTick
    ) {
        double entityX = Mth.lerp(
                partialTick,
                entity.xo,
                entity.getX()
        );

        double entityY = Mth.lerp(
                partialTick,
                entity.yo,
                entity.getY()
        );

        double entityZ = Mth.lerp(
                partialTick,
                entity.zo,
                entity.getZ()
        );

        poseStack.pushPose();


        poseStack.translate(
                entityX - cameraPos.x,
                entityY - cameraPos.y
                        + entity.getBbHeight()
                        + 0.65D,
                entityZ - cameraPos.z
        );

        poseStack.mulPose(
                Minecraft.getInstance()
                        .getEntityRenderDispatcher()
                        .cameraOrientation()
        );

        poseStack.scale(
                -ICON_SCALE,
                -ICON_SCALE,
                ICON_SCALE
        );

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        addVertex(
                consumer,
                pose,
                normal,
                -HALF_SIZE,
                HALF_SIZE,
                0.0F,
                1.0F
        );

        addVertex(
                consumer,
                pose,
                normal,
                HALF_SIZE,
                HALF_SIZE,
                1.0F,
                1.0F
        );

        addVertex(
                consumer,
                pose,
                normal,
                HALF_SIZE,
                -HALF_SIZE,
                1.0F,
                0.0F
        );

        addVertex(
                consumer,
                pose,
                normal,
                -HALF_SIZE,
                -HALF_SIZE,
                0.0F,
                0.0F
        );

        poseStack.popPose();
    }

    private static void addVertex(
            VertexConsumer consumer,
            Matrix4f pose,
            Matrix3f normal,
            float x,
            float y,
            float u,
            float v
    ) {
        consumer.vertex(
                        pose,
                        x,
                        y,
                        0.0F
                )
                .color(
                        255,
                        255,
                        255,
                        255
                )
                .uv(u, v)
                .overlayCoords(
                        OverlayTexture.NO_OVERLAY
                )
                .uv2(
                        LightTexture.FULL_BRIGHT
                )
                .normal(
                        normal,
                        0.0F,
                        0.0F,
                        1.0F
                )
                .endVertex();
    }
}