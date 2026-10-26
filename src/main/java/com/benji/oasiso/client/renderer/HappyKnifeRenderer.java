package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.model.HappyKnifeModel;
import com.benji.oasiso.common.entity.HappyKnifeEntity;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.*;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class HappyKnifeRenderer extends GeoEntityRenderer<HappyKnifeEntity> {
    public HappyKnifeRenderer(EntityRendererProvider.Context context) {
        super(context, new HappyKnifeModel());
        shadowRadius = 0;
    }

    @Override
    public RenderType getRenderType(HappyKnifeEntity e, ResourceLocation texture, MultiBufferSource source, float partial) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    protected void applyRotations(HappyKnifeEntity e, PoseStack pose, float age, float yaw, float partial) {
        float pitch = Mth.lerp(partial, e.xRotO, e.getXRot());
        boolean stuck = e.phase() == HappyKnifeEntity.STUCK || e.phase() == HappyKnifeEntity.FADE;
        pose.mulPose(Axis.XP.rotationDegrees(stuck ? -pitch : pitch));
        pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partial, e.xRotO, e.getXRot())));
        pose.translate(.4 / 16, -11.0 / 16, 0);
    }

    @Override
    public void actuallyRender(PoseStack pose, HappyKnifeEntity e, BakedGeoModel model, RenderType type, MultiBufferSource source, VertexConsumer vertices, boolean rerender, float partial, int light, int overlay, float red, float green, float blue, float alpha) {
        super.actuallyRender(pose, e, model, type, source, vertices, rerender, partial, 0xF000F0, overlay, red, green, blue, alpha * e.opacity(partial));
    }

    @Override
    public void renderRecursively(PoseStack pose, HappyKnifeEntity e, GeoBone bone, RenderType type, MultiBufferSource source, VertexConsumer vertices, boolean rerender, float partial, int light, int overlay, float red, float green, float blue, float alpha) {
        if (bone.getName().equals("spin_bone") && (e.phase() == HappyKnifeEntity.STUCK || e.phase() == HappyKnifeEntity.FADE)) {

            float oldX = bone.getRotX();
            float oldY = bone.getRotY();
            float oldZ = bone.getRotZ();

            bone.setRotX((float) Math.PI / 2);
            bone.setRotY(0);
            bone.setRotZ(0);

            try {
                super.renderRecursively(pose, e, bone, type, source, vertices, rerender, partial, light, overlay, red, green, blue, alpha);
            } finally {
                bone.setRotX(oldX);
                bone.setRotY(oldY);
                bone.setRotZ(oldZ);
            }
            return;
        }
        boolean blade = bone.getName().equals("blade");
        if (blade) bone.setTrackingMatrices(true);
        super.renderRecursively(pose, e, bone, type, source, vertices, rerender, partial, light, overlay, red, green, blue, alpha);
        if (blade && !rerender && e.phase() == HappyKnifeEntity.FLY) {
            float half = (float) (15.0 / 32 / Math.sqrt(2));
            Vector4f a = bone.getLocalSpaceMatrix().transform(new Vector4f(-half, -half, 0, 1));
            Vector4f b = bone.getLocalSpaceMatrix().transform(new Vector4f(half, half, 0, 1));
            Vec3 origin = new Vec3(Mth.lerp(partial, e.xo, e.getX()), Mth.lerp(partial, e.yo, e.getY()), Mth.lerp(partial, e.zo, e.getZ()));
            HappyKnifeTrail.sample(e, origin.add(a.x, a.y, a.z), origin.add(b.x, b.y, b.z), partial);
        }
    }
}
