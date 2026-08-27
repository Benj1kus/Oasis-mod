package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.model.EntropyChestplateGloveModel;
import com.benji.oasiso.common.item.EntropyChestplateGloveItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class EntropyChestplateGloveRenderer extends GeoItemRenderer<EntropyChestplateGloveItem> {

    public static final ModelResourceLocation ICON_MODEL = new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "entropy_chestplate_glove_2d"), "inventory");

    public EntropyChestplateGloveRenderer() {
        super(new EntropyChestplateGloveModel());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (displayContext == ItemDisplayContext.GUI) {
            Minecraft minecraft = Minecraft.getInstance();

            BakedModel iconModel = minecraft.getModelManager().getModel(ICON_MODEL);

            poseStack.pushPose();
            poseStack.translate(0.5F, 0.5F, 0.5F);
            Lighting.setupForFlatItems();

            minecraft.getItemRenderer().render(stack, ItemDisplayContext.GUI, false, poseStack, bufferSource, LightTexture.FULL_BRIGHT, packedOverlay, iconModel);
            if (bufferSource instanceof BufferSource immediate) {
                immediate.endBatch();
            }

            Lighting.setupFor3DItems();

            poseStack.popPose();

            return;
        }
        super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
    }
}