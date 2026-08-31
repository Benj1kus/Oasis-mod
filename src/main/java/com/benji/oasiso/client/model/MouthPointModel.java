package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.MouthPointBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class MouthPointModel extends GeoModel<MouthPointBlockEntity> {

    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/mouth_point.geo.json");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/mouth_point.animation.json");
    private static final ResourceLocation FIRST_TEXTURE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/block/mouth_point.png");
    private static final ResourceLocation SECOND_TEXTURE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/block/mouth_point_second.png");

    @Override
    public ResourceLocation getModelResource(MouthPointBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(MouthPointBlockEntity animatable) {
        return animatable.getVariant() == 2 ? SECOND_TEXTURE : FIRST_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(MouthPointBlockEntity animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(MouthPointBlockEntity animatable, long instanceId, AnimationState<MouthPointBlockEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        CoreGeoBone head = this.getAnimationProcessor().getBone("head");

        if (head == null) {
            return;
        }

        head.setRotX(0.0F);
        head.setRotY(0.0F);

        if (animatable.getLevel() == null || animatable.isSpawnAnimationActive()) {
            return;
        }

        double centerX = animatable.getBlockPos().getX() + 0.5D;
        double centerY = animatable.getBlockPos().getY() + 1.75D;
        double centerZ = animatable.getBlockPos().getZ() + 0.5D;

        Player nearest = animatable.getLevel().getNearestPlayer(centerX, centerY, centerZ, 24.0D, false);

        if (nearest == null || nearest.isSpectator()) {
            return;
        }

        double dx = nearest.getX() - centerX;
        double dz = nearest.getZ() - centerZ;
        double dy = nearest.getEyeY() - centerY;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        if (horizontal < 0.0001D) {
            return;
        }

        float yaw = (float) Math.atan2(dx, -dz);
        float pitch = (float) -Math.atan2(dy, horizontal);

        head.setRotY(-yaw);
        head.setRotX(Mth.clamp(pitch, -0.65F, 0.65F));
    }
}
