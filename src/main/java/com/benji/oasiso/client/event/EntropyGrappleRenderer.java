package com.benji.oasiso.client.event;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.network.EntropyGrappleStateS2CPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EntropyGrappleRenderer {

    private static final float HALO_WIDTH = 0.100F;
    private static final float CORE_WIDTH = 0.034F;

    private EntropyGrappleRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {

            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;

        if (level == null || EntropyGrappleClientState.sessions().isEmpty()) {

            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        float partialTick = event.getPartialTick();

        boolean prepared = false;

        for (EntropyGrappleClientState.Session session : EntropyGrappleClientState.sessions()) {

            Player player = EntropyGrappleClientState.findPlayer(level, session.playerId());

            if (player == null) {
                continue;
            }

            EntropyGrappleClientState.Rope rope = session.rope();

            if (rope.size() < 2) {
                continue;
            }

            if (!prepared) {
                RenderSystem.enableDepthTest();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableCull();
                RenderSystem.depthMask(false);
                RenderSystem.setShader(GameRenderer::getPositionColorShader);

                prepared = true;
            }

            drawRope(poseStack, rope, camera, partialTick, HALO_WIDTH, 0.00F, 1.00F, 0.64F, 0.20F);
            drawRope(poseStack, rope, camera, partialTick, CORE_WIDTH, 0.10F, 1.00F, 0.88F, 0.94F);

            Vec3 hook = session.currentEnd().subtract(camera);
            drawHook(poseStack, hook, session.state());
        }

        if (prepared) {
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    private static void drawRope(PoseStack poseStack, EntropyGrappleClientState.Rope rope, Vec3 camera, float partialTick, float width, float red, float green, float blue, float alpha) {
        Matrix4f matrix = poseStack.last().pose();

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < rope.size() - 1; i++) {

            Vec3 first = rope.getPoint(i, partialTick);
            Vec3 second = rope.getPoint(i + 1, partialTick);
            Vec3 segment = second.subtract(first);

            if (segment.lengthSqr() < 1.0E-8D) {
                continue;
            }

            Vec3 midpoint = first.add(second).scale(0.5D);
            Vec3 toCamera = camera.subtract(midpoint);
            Vec3 side = segment.cross(toCamera);

            if (side.lengthSqr() < 1.0E-8D) {
                side = segment.cross(new Vec3(0.0D, 1.0D, 0.0D));
            }

            if (side.lengthSqr() < 1.0E-8D) {
                side = new Vec3(1.0D, 0.0D, 0.0D);
            } else {
                side = side.normalize();
            }

            double pulse = 0.88D + 0.12D * Math.sin(i * 0.83D);

            Vec3 half = side.scale(width * pulse);

            Vec3 a = first.add(half).subtract(camera);
            Vec3 b = first.subtract(half).subtract(camera);
            Vec3 c = second.subtract(half).subtract(camera);
            Vec3 d = second.add(half).subtract(camera);

            vertex(buffer, matrix, a, red, green, blue, alpha);
            vertex(buffer, matrix, b, red, green, blue, alpha);
            vertex(buffer, matrix, c, red, green, blue, alpha);
            vertex(buffer, matrix, d, red, green, blue, alpha);
        }

        BufferUploader.drawWithShader(buffer.end());
    }

    private static void drawHook(PoseStack poseStack, Vec3 hook, EntropyGrappleStateS2CPacket.VisualState state) {
        poseStack.pushPose();
        poseStack.translate(hook.x, hook.y, hook.z);

        float size = state == EntropyGrappleStateS2CPacket.VisualState.ATTACHED ? 0.115F : 0.085F;

        Matrix4f matrix = poseStack.last().pose();

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float h = size * 0.5F;

        vertex(buffer, matrix, new Vec3(-h, -h, 0), 0.15F, 1.0F, 0.90F, 0.95F);
        vertex(buffer, matrix, new Vec3(h, -h, 0), 0.15F, 1.0F, 0.90F, 0.95F);
        vertex(buffer, matrix, new Vec3(h, h, 0), 0.15F, 1.0F, 0.90F, 0.95F);
        vertex(buffer, matrix, new Vec3(-h, h, 0), 0.15F, 1.0F, 0.90F, 0.95F);

        vertex(buffer, matrix, new Vec3(0, -h, -h), 0.15F, 1.0F, 0.90F, 0.95F);
        vertex(buffer, matrix, new Vec3(0, -h, h), 0.15F, 1.0F, 0.90F, 0.95F);
        vertex(buffer, matrix, new Vec3(0, h, h), 0.15F, 1.0F, 0.90F, 0.95F);
        vertex(buffer, matrix, new Vec3(0, h, -h), 0.15F, 1.0F, 0.90F, 0.95F);

        BufferUploader.drawWithShader(buffer.end());

        poseStack.popPose();
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 position, float red, float green, float blue, float alpha) {
        buffer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z).color(red, green, blue, alpha).endVertex();
    }
}
