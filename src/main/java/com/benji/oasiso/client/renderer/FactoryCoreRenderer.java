package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.model.FactoryCoreModel;
import com.benji.oasiso.client.factory.FactoryCoreVisuals;
import com.benji.oasiso.common.block.entity.FactoryCoreBlockEntity;
import com.benji.oasiso.registry.ModBlockEntities;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.*;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.util.RenderUtils;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FactoryCoreRenderer extends GeoBlockRenderer<FactoryCoreBlockEntity> {
    private final Matrix4f inverseBase = new Matrix4f();

    public FactoryCoreRenderer(BlockEntityRendererProvider.Context context) {
        super(new FactoryCoreModel());
        addRenderLayer(new AutoGlowingGeoLayer<FactoryCoreBlockEntity>(this) {
            @Override
            protected RenderType getRenderType(FactoryCoreBlockEntity b) {
                return RenderType.eyes(FactoryCoreModel.glow(b));
            }
        });
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(ModBlockEntities.FACTORY_CORE_BE.get(), FactoryCoreRenderer::new);
    }

    @Override
    public void preRender(PoseStack p, FactoryCoreBlockEntity b, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer v, boolean again, float partial, int light, int overlay, float r, float g, float blue, float a) {
        if (!again) inverseBase.set(p.last().pose()).invert();
        super.preRender(p, b, model, buffers, v, again, partial, light, overlay, r, g, blue, a);
    }

    @Override
    public void renderRecursively(PoseStack p, FactoryCoreBlockEntity b, GeoBone bone, RenderType type, MultiBufferSource buffers, VertexConsumer v, boolean again, float partial, int light, int overlay, float r, float g, float blue, float a) {
        if (!bone.getName().equals("core")) {
            super.renderRecursively(p, b, bone, type, buffers, v, again, partial, light, overlay, r, g, blue, a);
            return;
        }
        float sx = bone.getScaleX(), sy = bone.getScaleY(), sz = bone.getScaleZ(), pulse = FactoryCoreVisuals.pulse(b, partial);
        bone.setScaleX(sx * pulse);
        bone.setScaleY(sy * pulse);
        bone.setScaleZ(sz * pulse);
        try {
            if (!again) {
                p.pushPose();
                RenderUtils.prepMatrixForBone(p, bone);
                Matrix4f local = new Matrix4f(inverseBase).mul(p.last().pose()).translate(bone.getPivotX() / 16F, bone.getPivotY() / 16F, bone.getPivotZ() / 16F);
                p.popPose();
                FactoryCoreVisuals.capture(b, local);
            }
            super.renderRecursively(p, b, bone, type, buffers, v, again, partial, light, overlay, r, g, blue, a);
        } finally {
            bone.setScaleX(sx);
            bone.setScaleY(sy);
            bone.setScaleZ(sz);
        }
    }
}
