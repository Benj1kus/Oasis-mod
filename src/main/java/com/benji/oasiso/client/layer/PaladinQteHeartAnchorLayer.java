package com.benji.oasiso.client.layer;

import com.benji.oasiso.common.entity.PaladinEntity;
import com.benji.oasiso.common.entity.SwordHeartEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.List;
import java.util.UUID;

public class PaladinQteHeartAnchorLayer extends GeoRenderLayer<PaladinEntity> {

    public PaladinQteHeartAnchorLayer(GeoRenderer<PaladinEntity> renderer) {
        super(renderer);
    }


    @Override
    public void render(PoseStack poseStack, PaladinEntity paladin, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (!(paladin.level() instanceof ClientLevel level)) {

            return;
        }

        if (paladin.getAnimState() != PaladinEntity.STATE_SHIELD) {
            return;
        }

        Vec3 qte1 = getBonePosition(bakedModel, "qte_1");
        Vec3 qte2 = getBonePosition(bakedModel, "qte_2");
        Vec3 qte3 = getBonePosition(bakedModel, "qte_3");


        if (qte1 == null || qte2 == null || qte3 == null) {
            return;
        }


        Vec3[] anchors = {qte1, qte2, qte3};
        UUID ownerId = paladin.getUUID();
        AABB search = paladin.getBoundingBox().inflate(12.0D);

        List<SwordHeartEntity> hearts = level.getEntitiesOfClass(SwordHeartEntity.class, search, heart -> ownerId.equals(heart.getOwnerUuid()));

        for (SwordHeartEntity heart : hearts) {
            int slot = heart.getQteSlot();

            if (slot < 0 || slot >= anchors.length) {

                continue;
            }

            Vec3 position = anchors[slot];

            heart.setPos(position.x, position.y, position.z);

            heart.xOld = position.x;
            heart.yOld = position.y;
            heart.zOld = position.z;
        }
    }


    private Vec3 getBonePosition(BakedGeoModel model, String boneName) {
        GeoBone bone = model.getBone(boneName).orElse(null);


        if (bone == null) {
            return null;
        }

        Vector3d position = bone.getWorldPosition();

        return new Vec3(position.x, position.y, position.z);
    }
}