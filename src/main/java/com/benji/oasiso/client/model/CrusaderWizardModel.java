package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.CrusaderWizardEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CrusaderWizardModel extends GeoModel<CrusaderWizardEntity> {
    @Override
    public ResourceLocation getModelResource(CrusaderWizardEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/crusader_wizard.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CrusaderWizardEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/crusader_wizard.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CrusaderWizardEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/crusader_wizard.animation.json");
    }
}