package com.benji.oasiso.client.renderer;

import com.benji.oasiso.common.entity.ChaosPortalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

public class ChaosPortalEntityRenderer extends EntityRenderer<ChaosPortalEntity> {

    public ChaosPortalEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(ChaosPortalEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
    }
    @Override
    public ResourceLocation getTextureLocation(ChaosPortalEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}