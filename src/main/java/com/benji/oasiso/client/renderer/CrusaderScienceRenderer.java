package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.CrusaderScienceModel;
import com.benji.oasiso.common.entity.CrusaderScienceEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.benji.oasiso.Oasiso.MODID, value = net.minecraftforge.api.distmarker.Dist.CLIENT, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public class CrusaderScienceRenderer extends GeoEntityRenderer<CrusaderScienceEntity> {
    public CrusaderScienceRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CrusaderScienceModel());
        this.shadowRadius = 0.5f;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void registerShieldShader(net.minecraftforge.client.event.RegisterShadersEvent event) throws java.io.IOException {
        ScienceShieldRenderer.registerShader(event);
    }

    @Override
    public void render(CrusaderScienceEntity entity, float entityYaw, float partialTick, com.mojang.blaze3d.vertex.PoseStack poses, net.minecraft.client.renderer.MultiBufferSource buffers, int packedLight) {
        super.render(entity, entityYaw, partialTick, poses, buffers, packedLight);
        ScienceShieldRenderer.render(entity, partialTick, poses, buffers);
    }

    @Override
    public boolean shouldRender(CrusaderScienceEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}