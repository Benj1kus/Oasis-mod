package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.EntropyLanternBlockEntity;
import com.benji.oasiso.registry.ModBlockEntities;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EntropyLanternRenderer implements BlockEntityRenderer<EntropyLanternBlockEntity> {

    private static final float WIDTH = 1.45F;
    private static final float HEIGHT = 2.35F;
    private static final float BASE_Y = 1.025F;
    private static final int VIEW_DISTANCE = 64;
    private static ShaderInstance flameShader;

    public EntropyLanternRenderer(BlockEntityRendererProvider.Context context) {
    }

    @SubscribeEvent
    public static void registerRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.ENTROPY_LANTERN_BE.get(), EntropyLanternRenderer::new);
    }

    @SubscribeEvent
    public static void registerShader(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "entropy_lantern_flame"), DefaultVertexFormat.POSITION_COLOR_TEX), shader -> flameShader = shader);
    }

    @Override
    public void render(EntropyLanternBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        if (flameShader == null || blockEntity.getLevel() == null) return;
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        double distance = camera.getPosition().distanceTo(Vec3.atCenterOf(blockEntity.getBlockPos()));
        float visibility = 1.0F - Mth.clamp((float) (distance - 48.0D) / 16.0F, 0.0F, 1.0F);
        if (visibility <= 0.0F) return;

        float seconds = ((blockEntity.getLevel().getGameTime() % 1_000_000L) + partialTick) / 20.0F;
        flameShader.safeGetUniform("Time").set(seconds);
        long hash = blockEntity.getBlockPos().asLong();
        hash = (hash ^ (hash >>> 33)) * 0xff51afd7ed558ccdL;
        hash ^= hash >>> 33;
        int seed = (int) (hash & 255L);
        int alpha = Math.round(visibility * 255.0F);

        poseStack.pushPose();
        try {
            poseStack.translate(0.5D, BASE_Y, 0.5D);
            poseStack.mulPose(camera.rotation());
            Matrix4f matrix = poseStack.last().pose();
            VertexConsumer vertex = buffers.getBuffer(FlameRenderType.FLAME);
            float half = WIDTH * 0.5F;

            vertex.vertex(matrix, -half, 0.0F, 0.0F).color(seed, 255, 255, alpha).uv(0, 0).endVertex();
            vertex.vertex(matrix, half, 0.0F, 0.0F).color(seed, 255, 255, alpha).uv(1, 0).endVertex();
            vertex.vertex(matrix, half, HEIGHT, 0.0F).color(seed, 255, 255, alpha).uv(1, 1).endVertex();
            vertex.vertex(matrix, -half, HEIGHT, 0.0F).color(seed, 255, 255, alpha).uv(0, 1).endVertex();
        } finally {
            poseStack.popPose();
        }
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }

    private static final class FlameRenderType extends RenderType {
        private static final RenderType FLAME = create("oasiso_entropy_lantern_flame", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 256, false, false, CompositeState.builder().setShaderState(new ShaderStateShard(() -> flameShader)).setTransparencyState(NO_TRANSPARENCY).setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(false));

        private FlameRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort, Runnable setup, Runnable clear) {
            super(name, format, mode, size, crumbling, sort, setup, clear);
        }
    }
}
