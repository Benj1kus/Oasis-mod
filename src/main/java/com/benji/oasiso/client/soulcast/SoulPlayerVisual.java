package com.benji.oasiso.client.soulcast;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;

import org.joml.Matrix4f;

import software.bernie.geckolib.cache.object.*;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.util.*;

import java.util.concurrent.atomic.AtomicLong;

final class SoulPlayerVisual implements GeoAnimatable {
    private static final AtomicLong IDS = new AtomicLong();
    private static final Renderer RENDERER = new Renderer();
    final long id = IDS.incrementAndGet(), start;
    final float yaw;
    final AbstractClientPlayer player;
    boolean priming = true;
    boolean strikePlayed;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    SoulPlayerVisual(AbstractClientPlayer p, long start, float yaw) {
        this.player = p;
        this.start = start;
        this.yaw = yaw;
        cache.getManagerForId(id).startedAt(0);
        prepare();
        priming = false;
    }

    double age() {
        return priming ? 0 : Math.max(0, player.level().getGameTime() - start + Minecraft.getInstance().getFrameTime());
    }

    public double getTick(Object o) {
        return age();
    }

    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<SoulPlayerVisual>(this, "summon", 0, s -> s.setAndContinue(RawAnimation.begin().thenPlay("summon_souls"))) {
            @Override
            protected double adjustTick(double tick) {
                super.adjustTick(tick);
                return SoulPlayerVisual.this.age();
            }
        }.setCustomInstructionKeyframeHandler(event -> {
            var key = event.getKeyframeData();
            if (!strikePlayed && !priming) {
                for (String instruction : key.getInstructions().split(";")) {
                    if ("soul_summon".equals(instruction.trim())) {
                        strikePlayed = true;
                        SoulStrikeEffects.hit(player, start, key.getStartTick());
                        break;
                    }
                }
            }
        }));
    }

    void prepare() {
        var m = RENDERER.getGeoModel();
        m.getBakedModel(m.getModelResource(this));
        m.handleAnimations(this, id, new AnimationState<>(this, 0, 0, Minecraft.getInstance().getFrameTime(), false));
    }

    Matrix4f head() {
        prepare();
        PoseStack pose = new PoseStack();
        root(pose);
        var head = RENDERER.getGeoModel().getBone("Head").orElseThrow();
        applyParents(pose, head);
        return new Matrix4f(pose.last().pose());
    }

    static void applyParents(PoseStack pose, GeoBone bone) {
        if (bone.getParent() != null) applyParents(pose, bone.getParent());
        RenderUtils.prepMatrixForBone(pose, bone);
    }

    void root(PoseStack pose) {
        pose.mulPose(Axis.YP.rotationDegrees(180 - yaw));
        pose.scale(.9375F, .9375F, .9375F);
    }

    void draw(PoseStack pose, MultiBufferSource buffers, int light, boolean hideHead) {
        prepare();
        var head = RENDERER.getGeoModel().getBone("Head").orElseThrow();
        pose.pushPose();
        root(pose);
        head.setHidden(hideHead);
        try {
            RENDERER.draw(this, pose, buffers, light);
            SoulCastArmor.draw(this, RENDERER, pose, buffers, light, hideHead);
        } finally {
            head.setHidden(false);
            pose.popPose();
        }
    }

    private static ResourceLocation id(String p) {
        return ResourceLocation.fromNamespaceAndPath("oasiso", p);
    }

    private static final class Model extends GeoModel<SoulPlayerVisual> {
        public ResourceLocation getModelResource(SoulPlayerVisual a) {
            return id("geo/soul_player.geo.json");
        }

        public ResourceLocation getTextureResource(SoulPlayerVisual a) {
            return a.player.getSkinTextureLocation();
        }

        public ResourceLocation getAnimationResource(SoulPlayerVisual a) {
            return id("animations/soul_player.animation.json");
        }

        @Override
        public void handleAnimations(SoulPlayerVisual a, long id, AnimationState<SoulPlayerVisual> s) {
            s.setData(DataTickets.TICK, a.age());
            super.handleAnimations(a, id, s);
        }
    }

    private static final class Renderer extends GeoObjectRenderer<SoulPlayerVisual> {
        Renderer() {
            super(new Model());
        }

        @Override
        public long getInstanceId(SoulPlayerVisual a) {
            return a.id;
        }

        void draw(SoulPlayerVisual a, PoseStack pose, MultiBufferSource buffers, int light) {
            this.animatable = a;
            var type = RenderType.entityTranslucent(a.player.getSkinTextureLocation());
            defaultRender(pose, a, buffers, type, buffers.getBuffer(type), 0, Minecraft.getInstance().getFrameTime(), light);
        }

        @Override
        public void preRender(PoseStack p, SoulPlayerVisual a, BakedGeoModel m, MultiBufferSource s, VertexConsumer v, boolean again, float partial, int light, int overlay, float red, float green, float blue, float alpha) {
            this.objectRenderTranslations = new Matrix4f(p.last().pose());
        }
    }
}
