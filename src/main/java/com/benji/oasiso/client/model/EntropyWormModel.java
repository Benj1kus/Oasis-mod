package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.EntropyWormEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class EntropyWormModel extends GeoModel<EntropyWormEntity> {
    private static final String[] CHAIN = {"worm_head", "worm_neck", "worm_part1", "worm_part2", "worm_part3", "worm_part4", "worm_tail"};

    @Override
    public ResourceLocation getModelResource(EntropyWormEntity entity) {
        return id("geo/entropy_worm.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(EntropyWormEntity entity) {
        return id("textures/entity/entropy_worm.png");
    }

    @Override
    public ResourceLocation getAnimationResource(EntropyWormEntity entity) {
        return id("animations/entropy_worm.animation.json");
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, path);
    }

    @Override
    public void setCustomAnimations(EntropyWormEntity entity, long instanceId, AnimationState<EntropyWormEntity> state) {
        super.setCustomAnimations(entity, instanceId, state);
        float partial = state.getPartialTick();
        float bodyYaw = Mth.rotLerp(partial, entity.yBodyRotO, entity.yBodyRot);
        float phase = entity.wavePhase(partial);
        var root = getAnimationProcessor().getBone("worm");
        if (root != null) {
            root.setRotX(0);
            root.setRotY(0);
            root.setRotZ(0);
            root.setPosZ(31.5F);
        }
        Quaternionf parent = new Quaternionf();
        for (int i = 0; i < CHAIN.length; i++) {
            var bone = getAnimationProcessor().getBone(CHAIN[i]);
            if (bone == null) continue;

            int segment = Math.max(0, i - 1);
            float delay = segment * entity.segmentDelay();
            float yaw = -Mth.wrapDegrees(entity.historyYaw(delay, partial) - bodyYaw) * Mth.DEG_TO_RAD;
            float pitch = entity.historyPitch(delay, partial);
            float strength = segment / 5F;
            float amplitude = entity.isDashing() ? .25F : .16F;
            yaw += Mth.sin(phase - segment * .85F) * amplitude * strength;
            pitch += Mth.sin(phase - segment * .85F + 1F) * amplitude * .65F * strength;

            Quaternionf desired = new Quaternionf().rotationY(yaw).rotateX(pitch);
            Quaternionf local = new Quaternionf(parent).conjugate().mul(desired);

            local.normalize();
            float x = local.x, y = local.y, z = local.z, w = local.w;
            bone.setRotX((float) Math.atan2(2 * (w * x + y * z), 1 - 2 * (x * x + y * y)));
            bone.setRotY((float) Math.asin(Mth.clamp(2 * (w * y - z * x), -1F, 1F)));
            bone.setRotZ((float) Math.atan2(2 * (w * z + x * y), 1 - 2 * (y * y + z * z)));
            parent.set(desired);
        }
    }
}
