package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.KarakWormEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class KarakWormModel extends GeoModel<KarakWormEntity> {
    @Override
    public ResourceLocation getModelResource(KarakWormEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/karak_worm.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(KarakWormEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/karak_worm.png");
    }

    @Override
    public ResourceLocation getAnimationResource(KarakWormEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/karak_worm.animation.json");
    }

    private static final String[] JOINTS = {"worm_head", "worm_neck", "worm_part1", "worm_part2", "worm_part3", "worm_tail"};
    private static final double[] PIVOT_Z = {-55.5, -35.54167, -20.58333, -5.4, 9.5, 24.33333, 52.5};

    @Override
    public void setCustomAnimations(KarakWormEntity entity, long id, AnimationState<KarakWormEntity> state) {
        super.setCustomAnimations(entity, id, state);
        var root = getAnimationProcessor().getBone("worm");
        if (root == null) return;
        root.setPosZ(55.5F);
        root.setPosY(-2F);

        float partial = state.getPartialTick();
        float bodyYaw = Mth.rotLerp(partial, entity.yBodyRotO, entity.yBodyRot);
        Quaternionf parent = new Quaternionf();
        Vector3f angles = new Vector3f();
        Vec3 front = entity.trailPoint(0, partial);
        for (int i = 0; i < JOINTS.length; i++) {
            var bone = getAnimationProcessor().getBone(JOINTS[i]);
            if (bone == null) break;
            Vec3 back = entity.trailPoint((PIVOT_Z[i + 1] - PIVOT_Z[0]) / 16, partial);
            Vec3 tangent = front.subtract(back);
            float yaw = (float) Math.atan2(-tangent.x, tangent.z);
            float pitch = (float) Math.atan2(tangent.y, tangent.horizontalDistance());
            Quaternionf desired = new Quaternionf().rotationY(bodyYaw * Mth.DEG_TO_RAD - yaw).rotateX(pitch);
            new Quaternionf(parent).conjugate().mul(desired).normalize().getEulerAnglesZYX(angles);
            bone.setRotX(angles.x);
            bone.setRotY(angles.y);
            bone.setRotZ(angles.z);
            parent.set(desired);
            front = back;
        }
    }
}
