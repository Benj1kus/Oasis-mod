package com.benji.oasiso.client.event;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.EntropyPhysicsBlockEntity;
import com.benji.oasiso.common.item.EntropyChestplateGloveItem;
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
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EntropyGravityBeamRenderer {

    private static final int SEGMENTS = 26;
    private static final double SEARCH_RANGE = 96.0D;

    private static final float HALO_WIDTH = 0.095F;
    private static final float CORE_WIDTH = 0.032F;

    private EntropyGravityBeamRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;

        if (minecraft.player == null || level == null) {
            return;
        }

        float partialTick = event.getPartialTick();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        boolean preparedRenderState = false;

        for (Player player : level.players()) {
            ItemStack glove = EntropyChestplateGloveItem.findActiveGlove(player);
            UUID heldId = EntropyChestplateGloveItem.getHeldBlockId(glove);

            if (glove.isEmpty() || heldId == null) {
                continue;
            }

            EntropyPhysicsBlockEntity held = findHeldEntity(level, player, heldId);
            if (held == null || (held.getMode() != EntropyPhysicsBlockEntity.MODE_PULLING && held.getMode() != EntropyPhysicsBlockEntity.MODE_HELD)) {

                continue;
            }

            Vec3 start = getGloveOrigin(player, glove, partialTick);
            Vec3 end = held.getPosition(partialTick).add(0.0D, 0.52D, 0.0D);

            if (start.distanceToSqr(end) < 0.09D) {
                continue;
            }

            if (!preparedRenderState) {
                RenderSystem.enableDepthTest();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableCull();
                RenderSystem.depthMask(false);
                RenderSystem.setShader(GameRenderer::getPositionColorShader);
                preparedRenderState = true;
            }

            Vec3[] curve = buildJellyCurve(level, start, end, partialTick);

            drawRibbon(poseStack, curve, camera, HALO_WIDTH, 0.00F, 1.00F, 0.64F, 0.20F);
            drawRibbon(poseStack, curve, camera, CORE_WIDTH, 0.10F, 1.00F, 0.88F, 0.92F);
        }

        if (preparedRenderState) {
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    private static EntropyPhysicsBlockEntity findHeldEntity(ClientLevel level, Player player, UUID heldId) {
        AABB search = player.getBoundingBox().inflate(SEARCH_RANGE);

        for (EntropyPhysicsBlockEntity entity : level.getEntitiesOfClass(EntropyPhysicsBlockEntity.class, search)) {
            if (heldId.equals(entity.getUUID())) {
                return entity;
            }
        }

        return null;
    }

    private static Vec3 getGloveOrigin(Player player, ItemStack glove, float partialTick) {
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick).normalize();

        Vec3 horizontalForward = new Vec3(look.x, 0.0D, look.z);
        if (horizontalForward.lengthSqr() < 1.0E-5D) {
            double yaw = Math.toRadians(player.getYRot());
            horizontalForward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        } else {
            horizontalForward = horizontalForward.normalize();
        }

        Vec3 right = new Vec3(-horizontalForward.z, 0.0D, horizontalForward.x);

        boolean mainHand = EntropyChestplateGloveItem.isBoundTo(player.getMainHandItem(), EntropyChestplateGloveItem.getHeldBlockId(glove));
        HumanoidArm arm = mainHand ? player.getMainArm() : (player.getMainArm() == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT);

        double side = arm == HumanoidArm.RIGHT ? 1.0D : -1.0D;

        return eye.add(look.scale(0.28D)).add(right.scale(0.30D * side)).add(0.0D, -0.34D, 0.0D);
    }

    private static Vec3[] buildJellyCurve(ClientLevel level, Vec3 start, Vec3 end, float partialTick) {
        Vec3 direction = end.subtract(start);
        double distance = direction.length();

        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        Vec3 lateral;

        if (horizontal.lengthSqr() < 1.0E-5D) {
            lateral = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            horizontal = horizontal.normalize();
            lateral = new Vec3(-horizontal.z, 0.0D, horizontal.x);
        }

        double arcHeight = Math.min(0.78D, 0.14D + distance * 0.11D);
        Vec3 controlA = start.add(direction.scale(0.32D)).add(0.0D, arcHeight, 0.0D);
        Vec3 controlB = start.add(direction.scale(0.70D)).add(0.0D, arcHeight * 0.64D, 0.0D);

        double time = (level.getGameTime() + partialTick) * 0.31D;
        double wobbleStrength = Math.min(0.16D, 0.035D + distance * 0.012D);

        Vec3[] points = new Vec3[SEGMENTS + 1];

        for (int i = 0; i <= SEGMENTS; i++) {
            double t = i / (double) SEGMENTS;
            double oneMinus = 1.0D - t;

            Vec3 base = start.scale(oneMinus * oneMinus * oneMinus).add(controlA.scale(3.0D * oneMinus * oneMinus * t)).add(controlB.scale(3.0D * oneMinus * t * t)).add(end.scale(t * t * t));

            double envelope = Math.sin(Math.PI * t);
            double waveA = Math.sin(time * 3.6D + t * 14.0D);
            double waveB = Math.cos(time * 2.8D - t * 10.0D);

            Vec3 wobble = lateral.scale(waveA * wobbleStrength * envelope).add(0.0D, waveB * wobbleStrength * 0.58D * envelope, 0.0D);

            points[i] = base.add(wobble);
        }

        return points;
    }

    private static void drawRibbon(PoseStack poseStack, Vec3[] points, Vec3 camera, float width, float red, float green, float blue, float alpha) {
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < points.length - 1; i++) {
            Vec3 first = points[i];
            Vec3 second = points[i + 1];
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

            double pulse = 0.84D + 0.16D * Math.sin(i * 0.9D);
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

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 position, float red, float green, float blue, float alpha) {
        buffer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z).color(red, green, blue, alpha).endVertex();
    }
}
