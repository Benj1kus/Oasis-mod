package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.ChestBoatModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;
import org.joml.Quaternionf;

public class DoumPalmBoatRenderer extends EntityRenderer<Boat> {
    private final ResourceLocation texture;
    private final ListModel<Boat> model;

    public DoumPalmBoatRenderer(EntityRendererProvider.Context context, boolean hasChest) {
        super(context);
        this.shadowRadius = 0.8F;
        this.texture = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID,
                hasChest ? "textures/entity/chest_boat/doum_palm.png" : "textures/entity/boat/doum_palm.png");

        this.model = hasChest
                ? new ChestBoatModel(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.createChestBoatModelName(Boat.Type.OAK)))
                : new BoatModel(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.createBoatModelName(Boat.Type.OAK)));
    }

    @Override
    public ResourceLocation getTextureLocation(Boat entity) {
        return this.texture;
    }

    @Override
    public void render(Boat boat, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.375F, 0.0F);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - entityYaw));

        float h = (float)boat.getHurtTime() - partialTicks;
        float j = boat.getDamage() - partialTicks;
        if (j < 0.0F) j = 0.0F;
        if (h > 0.0F) {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(Mth.sin(h) * h * j / 10.0F * (float)boat.getHurtDir()));
        }

        float k = boat.getBubbleAngle(partialTicks);
        if (!Mth.equal(k, 0.0F)) {
            poseStack.mulPose(new Quaternionf().setAngleAxis(boat.getBubbleAngle(partialTicks) * ((float)Math.PI / 180F), 1.0F, 0.0F, 1.0F));
        }

        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));

        this.model.setupAnim(boat, partialTicks, 0.0F, -0.1F, 0.0F, 0.0F);
        VertexConsumer vertexconsumer = buffer.getBuffer(this.model.renderType(this.getTextureLocation(boat)));
        this.model.renderToBuffer(poseStack, vertexconsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        if (!boat.isUnderWater()) {
            VertexConsumer waterConsumer = buffer.getBuffer(RenderType.waterMask());
            if (this.model instanceof BoatModel bm) {
                bm.waterPatch().render(poseStack, waterConsumer, packedLight, OverlayTexture.NO_OVERLAY);
            }
        }

        poseStack.popPose();
        super.render(boat, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}