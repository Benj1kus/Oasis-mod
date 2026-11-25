package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.QuiModel;
import com.benji.oasiso.common.entity.QuiEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class QuiRenderer extends GeoEntityRenderer<QuiEntity> {
    private static final ResourceLocation BUBBLES = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "karak_bubbles");
    private final Map<QuiEntity, Integer> lastBubbleTick = new WeakHashMap<>();

    public QuiRenderer(EntityRendererProvider.Context context) {
        super(context, new QuiModel());
        this.shadowRadius = 0.4f;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @Override
    public void renderRecursively(PoseStack poseStack, QuiEntity entity, GeoBone bone, RenderType renderType, MultiBufferSource buffers, VertexConsumer vertices, boolean isReRender, float partialTick, int light, int overlay, float red, float green, float blue, float alpha) {
        boolean emitter = !isReRender && "bubbles".equals(bone.getName());
        if (emitter) bone.setTrackingMatrices(true);
        super.renderRecursively(poseStack, entity, bone, renderType, buffers, vertices, isReRender, partialTick, light, overlay, red, green, blue, alpha);

        if (!emitter || net.minecraft.client.Minecraft.getInstance().isPaused()) return;
        int now = entity.tickCount;
        Integer previous = lastBubbleTick.get(entity);
        if (previous != null && now - previous < 3) return;
        lastBubbleTick.put(entity, now);
        if (ForgeRegistries.PARTICLE_TYPES.getValue(BUBBLES) instanceof SimpleParticleType bubbles) {
            var matrix = bone.getWorldSpaceMatrix();
            var random = entity.getRandom();

            for (int i = 0; i < 2; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                float x = (float) Math.cos(angle);
                float z = (float) Math.sin(angle);
                float radius = 0.16F * (float) Math.sqrt(random.nextDouble());
                Vector3f pos = matrix.transformPosition(new Vector3f(x * radius, 0.0F, z * radius));
                float spread = 0.018F + random.nextFloat() * 0.025F;
                Vector3f velocity = matrix.transformDirection(new Vector3f(x * spread, 0.025F + random.nextFloat() * 0.025F, z * spread));

                entity.level().addParticle(bubbles, pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
            }
        }
    }
}
