package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.AzumalitArmSmokeLayer;
import com.benji.oasiso.client.layer.AzumalitBladeSlashLayer;
import com.benji.oasiso.client.layer.AzumalitHologramTrailLayer;
import com.benji.oasiso.client.layer.AzumalitShockwaveLayer;
import com.benji.oasiso.client.model.AzumalitArmorModel;
import com.benji.oasiso.common.item.AzumalitArmorItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class AzumalitArmorRenderer extends GeoArmorRenderer<AzumalitArmorItem> {

    private LivingEntity currentEntity;
    private EquipmentSlot currentSlot;


    public AzumalitArmorRenderer() {
        super(new AzumalitArmorModel());

        this.addRenderLayer(new AzumalitShockwaveLayer(this));
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.addRenderLayer(new AzumalitHologramTrailLayer(this));
    }

    public void prepForRender(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
        this.currentEntity = livingEntity;
        this.currentSlot = equipmentSlot;

        super.prepForRender(livingEntity, itemStack, equipmentSlot, original);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, AzumalitArmorItem animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        String boneName = bone.getName();

        boolean smokeTrackingBone = "smokearm_left".equals(boneName) || "arm_p_left".equals(boneName) || "smokearm_right".equals(boneName) || "arm_p_right".equals(boneName);
        boolean bladeTrackingBone = "blade_left_bottom".equals(boneName) || "blade_left_middle".equals(boneName) || "blade_left_top".equals(boneName) || "blade_right_bottom".equals(boneName) || "blade_right_middle".equals(boneName) || "blade_right_top".equals(boneName);

        if (!isReRender && (smokeTrackingBone || bladeTrackingBone)) {

            bone.setTrackingMatrices(true);
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        if (isReRender || this.currentSlot != EquipmentSlot.CHEST || this.currentEntity == null || !AzumalitArmorItem.hasAzumalitChestplate(this.currentEntity)) {
            return;
        }

        if (smokeTrackingBone) {
            AzumalitArmSmokeLayer.captureBone(this.currentEntity, boneName, bone, partialTick);
        }

        if (bladeTrackingBone) {
            AzumalitBladeSlashLayer.captureBone(this.currentEntity, boneName, bone, partialTick);
        }
    }

    public LivingEntity getCurrentEntity() {
        return this.currentEntity;
    }

    public EquipmentSlot getCurrentSlot() {
        return this.currentSlot;
    }
}
