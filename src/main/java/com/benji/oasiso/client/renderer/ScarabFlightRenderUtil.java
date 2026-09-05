package com.benji.oasiso.client.renderer;

import com.benji.oasiso.common.entity.ScarabEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class ScarabFlightRenderUtil {


    private ScarabFlightRenderUtil() {
    }

    public static void applySurfaceOffset(PoseStack poseStack, ScarabEntity scarab) {
        Vec3 offset = scarab.getSurfaceVisualOffset();
        poseStack.translate(offset.x, offset.y, offset.z);
    }

    public static void applyTilt(PoseStack poseStack, ScarabEntity scarab, float partialTick) {
        float pitch = scarab.getFlightPitch(partialTick);
        float roll = scarab.getFlightRoll(partialTick);
        if (Math.abs(pitch) < 0.001F && Math.abs(roll) < 0.001F) {
            return;
        }

        float bodyYaw = Mth.rotLerp(partialTick, scarab.yBodyRotO, scarab.yBodyRot);
        float renderYaw = 180.0F - bodyYaw;

        poseStack.mulPose(Axis.YP.rotationDegrees(renderYaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(-renderYaw));
    }
}