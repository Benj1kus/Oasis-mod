package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.shader.BossPortalDistortionShader;
import com.benji.oasiso.common.entity.BossPortalEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector4f;


@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BossPortalDistortionPass {

    private static final double RANGE = 64.0D;

    private static final double DISTORTION_HEIGHT = 4.1D;

    private static final double BASE_WORLD_RADIUS = 2.55D;
    private static final double TOP_WORLD_RADIUS = 1.20D;


    private BossPortalDistortionPass() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderLevel(RenderLevelStageEvent event) {

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) return;

        BossPortalEntity portal = findNearestPortal(minecraft);

        if (portal == null) return;

        Vec3 camera = event.getCamera().getPosition();
        Vec3 baseWorld = portal.position().add(0.0D, 0.16D, 0.0D);
        Vec3 topWorld = portal.position().add(0.0D, DISTORTION_HEIGHT, 0.0D);

        Vec3 horizontal = baseWorld.subtract(camera);
        horizontal = new Vec3(horizontal.x, 0.0D, horizontal.z);


        ProjectedPoint base = project(baseWorld, event);
        ProjectedPoint top = project(topWorld, event);
        if (base == null || top == null) {
            return;
        }


        float aspect = minecraft.getWindow().getWidth() / (float) minecraft.getWindow().getHeight();


        float baseRadius = projectWorldRadius(BASE_WORLD_RADIUS, base, event, aspect);


        float topRadius = projectWorldRadius(TOP_WORLD_RADIUS, top, event, aspect);


        if (!Float.isFinite(baseRadius) || !Float.isFinite(topRadius) || baseRadius <= 0.001F || topRadius <= 0.001F || baseRadius > 0.55F || topRadius > 0.42F) {

            return;
        }

        if (isCompletelyOffscreen(base, top, baseRadius, topRadius, aspect)) {
            return;
        }


        if (baseRadius < 0.002F || topRadius < 0.001F) return;

        double distance = Math.sqrt(portal.distanceToSqr(minecraft.player));

        float distanceFade = Mth.clamp(1.0F - (float) distance / 72.0F, 0.22F, 1.0F);
        float strength = 0.010F * distanceFade;

        float time = (minecraft.level.getGameTime() + event.getPartialTick()) / 20.0F;

        BossPortalDistortionShader.render(base.u, base.v, top.u, top.v, baseRadius, topRadius, base.depth, top.depth, aspect, time, strength);
    }


    private static BossPortalEntity findNearestPortal(Minecraft minecraft) {

        BossPortalEntity closest = null;
        double bestDistance = RANGE * RANGE;


        for (BossPortalEntity portal : minecraft.level.getEntitiesOfClass(BossPortalEntity.class, minecraft.player.getBoundingBox().inflate(RANGE), candidate -> candidate.isAlive() && candidate.isChaosPortal() && (candidate.getAnimState() == BossPortalEntity.STATE_IDLE || candidate.getAnimState() == BossPortalEntity.STATE_DESPAWN))) {

            double distance = portal.distanceToSqr(minecraft.player);


            if (distance >= bestDistance) continue;

            bestDistance = distance;
            closest = portal;
        }

        return closest;
    }

    private static ProjectedPoint project(Vec3 world, RenderLevelStageEvent event) {
        Vec3 camera = event.getCamera().getPosition();


        Vector4f view = new Vector4f((float) (world.x - camera.x), (float) (world.y - camera.y), (float) (world.z - camera.z), 1.0F);

        view.mul(event.getPoseStack().last().pose());

        Vector4f clip = new Vector4f(view);

        clip.mul(event.getProjectionMatrix());

        if (clip.w() <= 0.08F) {
            return null;
        }

        float ndcX = clip.x() / clip.w();
        float ndcY = clip.y() / clip.w();
        float ndcZ = clip.z() / clip.w();

        if (!Float.isFinite(ndcX) || !Float.isFinite(ndcY) || !Float.isFinite(ndcZ) || Math.abs(ndcX) > 4.0F || Math.abs(ndcY) > 4.0F) {
            return null;
        }
        return new ProjectedPoint(ndcX * 0.5F + 0.5F, ndcY * 0.5F + 0.5F, ndcZ * 0.5F + 0.5F, clip.w());
    }

    private static float projectWorldRadius(double worldRadius, ProjectedPoint center, RenderLevelStageEvent event, float aspect) {
        float projectedNdcRadius = (float) (worldRadius * event.getProjectionMatrix().m00() / center.clipW);


        return Math.abs(projectedNdcRadius * 0.5F * aspect);
    }

    private static boolean isCompletelyOffscreen(ProjectedPoint base, ProjectedPoint top, float baseRadius, float topRadius, float aspect) {
        float margin = 0.08F;

        float baseRadiusX = baseRadius / aspect;
        float topRadiusX = topRadius / aspect;


        float minX = Math.min(base.u - baseRadiusX, top.u - topRadiusX);
        float maxX = Math.max(base.u + baseRadiusX, top.u + topRadiusX);

        float minY = Math.min(base.v - baseRadius, top.v - topRadius);
        float maxY = Math.max(base.v + baseRadius, top.v + topRadius);


        return maxX < -margin || minX > 1.0F + margin || maxY < -margin || minY > 1.0F + margin;
    }

    private record ProjectedPoint(float u, float v, float depth, float clipW) {
    }
}