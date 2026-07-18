package com.benji.oasiso.client.layer;

import com.benji.oasiso.common.entity.CaserEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.List;

public class CaserLootLayer extends GeoRenderLayer<CaserEntity> {
    private final ItemRenderer itemRenderer;

    private static final List<Item> FAKE_ITEMS = List.of(
            Items.DIAMOND, Items.EMERALD, Items.GOLD_INGOT, Items.IRON_INGOT,
            Items.ROTTEN_FLESH, Items.NETHERITE_INGOT, Items.ENDER_PEARL, Items.STICK
    );

    public CaserLootLayer(GeoEntityRenderer<CaserEntity> entityRendererIn) {
        super(entityRendererIn);
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(PoseStack poseStack, CaserEntity entity, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        int spinTicks = entity.getEntityData().get(CaserEntity.SPIN_TICKS);
        if (spinTicks <= 0) return;

        GeoBone bodyBone = bakedModel.getBone("case_body").orElse(null);
        if (bodyBone == null) return;

        poseStack.pushPose();

        RenderUtils.translateMatrixToBone(poseStack, bodyBone);

        float yaw = entity.getEntityData().get(CaserEntity.FIXED_YAW);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));

        poseStack.translate(0.0D, 1.0D, 0.65D);

        ItemStack reward = entity.getEntityData().get(CaserEntity.REWARD_ITEM);
        ItemStack displayItem = reward;

        if (spinTicks > 10) {
            int speed = spinTicks > 60 ? 2 : (spinTicks > 30 ? 5 : 10);
            int itemIndex = (entity.tickCount / speed) % FAKE_ITEMS.size();
            displayItem = new ItemStack(FAKE_ITEMS.get(itemIndex));
        }

        int rarity = CaserEntity.getRarityLevel(displayItem);
        float pulse = (float) (Math.sin((entity.tickCount + partialTick) * 0.3) * 0.5 + 0.5);

        int r, g, b;
        switch (rarity) {
            case 3:
                r = 255; g = 255; b = (int) (100 + 155 * pulse); break;
            case 2:
                r = (int) (128 + 127 * pulse); g = (int) (105 * pulse); b = (int) (128 + 52 * pulse); break;
            case 0:
                r = (int) (128 + 127 * pulse); g = r; b = r; break;
            default:
                r = (int) (173 * pulse); g = (int) (216 * pulse); b = 255; break;
        }

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.gui());
        Matrix4f matrix4f = poseStack.last().pose();
        float size = 0.45F;

        vertexConsumer.vertex(matrix4f, 0, size, 0).color(r, g, b, 200).endVertex();
        vertexConsumer.vertex(matrix4f, -size, 0, 0).color(r, g, b, 200).endVertex();
        vertexConsumer.vertex(matrix4f, 0, -size, 0).color(r, g, b, 200).endVertex();
        vertexConsumer.vertex(matrix4f, size, 0, 0).color(r, g, b, 200).endVertex();

        poseStack.pushPose();

        poseStack.translate(0.0D, 0.0D, 0.15D);

        poseStack.scale(0.8F, 0.8F, 0.8F);
        poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * 15.0F));

        this.itemRenderer.renderStatic(displayItem, ItemDisplayContext.GROUND, packedLight, packedOverlay, poseStack, bufferSource, entity.level(), 0);
        poseStack.popPose();

        poseStack.popPose();
    }
}