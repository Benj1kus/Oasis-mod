package com.benji.oasiso.client.light;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BoneLightEntityRenderer<T extends Entity & GeoAnimatable> extends GeoEntityRenderer<T> {
    public BoneLightEntityRenderer(EntityRendererProvider.Context context, GeoModel<T> model) {
        super(context, model);
    }

    @Override
    public void renderRecursively(PoseStack pose, T entity, GeoBone bone, RenderType type, MultiBufferSource buffers, VertexConsumer vertices, boolean reRender, float partial, int light, int overlay, float red, float green, float blue, float alpha) {
        if (!reRender) OasisoLocalLights.captureBone(entity, bone, pose, partial);
        super.renderRecursively(pose, entity, bone, type, buffers, vertices, reRender, partial, light, overlay, red, green, blue, alpha);
    }
}
