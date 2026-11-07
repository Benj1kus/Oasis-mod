package com.benji.oasiso.client.soulcast;

import com.benji.oasiso.client.model.SoulArmorModel;
import com.benji.oasiso.common.item.SoulArmorItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;

import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;

final class SoulCastArmor {
    private static final SoulArmorModel MODEL = new SoulArmorModel();

    private record Part(EquipmentSlot slot, String armor, String player) {
    }

    private static final Part[] PARTS = {new Part(EquipmentSlot.HEAD, "armorHead", "Head"), new Part(EquipmentSlot.CHEST, "armorBody", "Body"), new Part(EquipmentSlot.CHEST, "armorRightArm", "Right Arm"), new Part(EquipmentSlot.CHEST, "armorLeftArm", "Left Arm"), new Part(EquipmentSlot.LEGS, "armorRightLeg", "Right Leg"), new Part(EquipmentSlot.LEGS, "armorLeftLeg", "Left Leg"), new Part(EquipmentSlot.FEET, "armorRightBoot", "Right Leg"), new Part(EquipmentSlot.FEET, "armorLeftBoot", "Left Leg")};

    static void draw(SoulPlayerVisual visual, GeoRenderer<SoulPlayerVisual> renderer, PoseStack pose, MultiBufferSource buffers, int light, boolean hideHead) {
        for (Part part : PARTS) {
            if (hideHead && part.slot == EquipmentSlot.HEAD) continue;
            var stack = visual.player.getItemBySlot(part.slot);
            if (!(stack.getItem() instanceof SoulArmorItem armor)) continue;

            MODEL.getBakedModel(MODEL.getModelResource(armor));
            GeoBone armorBone = MODEL.getBone(part.armor).orElse(null);
            GeoBone playerBone = renderer.getGeoModel().getBone(part.player).orElse(null);
            if (armorBone == null || playerBone == null) continue;

            var type = RenderType.armorCutoutNoCull(MODEL.getTextureResource(armor));
            VertexConsumer buffer = ItemRenderer.getArmorFoilBuffer(buffers, type, false, stack.hasFoil());
            pose.pushPose();
            try {
                SoulPlayerVisual.applyParents(pose, playerBone);
                pose.translate((playerBone.getPivotX() - armorBone.getPivotX()) / 16F, (playerBone.getPivotY() - armorBone.getPivotY()) / 16F, (playerBone.getPivotZ() - armorBone.getPivotZ()) / 16F);
                restParents(pose, armorBone.getParent());
                drawRestBone(pose, armorBone, renderer, buffer, light);
            } finally {
                pose.popPose();
            }
        }
    }

    private static void restParents(PoseStack pose, GeoBone bone) {
        if (bone == null) return;
        restParents(pose, bone.getParent());
        restTransform(pose, bone);
    }

    private static void drawRestBone(PoseStack pose, GeoBone bone, GeoRenderer<SoulPlayerVisual> renderer, VertexConsumer buffer, int light) {
        pose.pushPose();
        try {
            restTransform(pose, bone);
            if (!Boolean.TRUE.equals(bone.shouldNeverRender())) {
                for (var cube : bone.getCubes()) {
                    pose.pushPose();
                    try {
                        renderer.renderCube(pose, cube, buffer, light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
                    } finally {
                        pose.popPose();
                    }
                }
            }
            for (GeoBone child : bone.getChildBones())
                drawRestBone(pose, child, renderer, buffer, light);
        } finally {
            pose.popPose();
        }
    }

    private static void restTransform(PoseStack pose, GeoBone bone) {
        var rest = bone.getInitialSnapshot();
        pose.translate(-rest.getOffsetX() / 16F, rest.getOffsetY() / 16F, rest.getOffsetZ() / 16F);
        pose.translate(bone.getPivotX() / 16F, bone.getPivotY() / 16F, bone.getPivotZ() / 16F);
        pose.mulPose(Axis.ZP.rotation(rest.getRotZ()));
        pose.mulPose(Axis.YP.rotation(rest.getRotY()));
        pose.mulPose(Axis.XP.rotation(rest.getRotX()));
        pose.scale(rest.getScaleX(), rest.getScaleY(), rest.getScaleZ());
        pose.translate(-bone.getPivotX() / 16F, -bone.getPivotY() / 16F, -bone.getPivotZ() / 16F);
    }
}
