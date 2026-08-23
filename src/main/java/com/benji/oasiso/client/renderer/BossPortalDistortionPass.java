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

        Vec3 right;

        if (horizontal.lengthSqr() < 0.0001D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);

        } else {
            right = new Vec3(-horizontal.z, 0.0D, horizontal.x).normalize();
        }

        ProjectedPoint base = project(baseWorld, event);
        ProjectedPoint top = project(topWorld, event);
        ProjectedPoint baseEdge = project(baseWorld.add(right.scale(BASE_WORLD_RADIUS)), event);
        ProjectedPoint topEdge = project(topWorld.add(right.scale(TOP_WORLD_RADIUS)), event);

        if (base == null || top == null || baseEdge == null || topEdge == null) {
            return;
        }

        float aspect = minecraft.getWindow().getWidth() / (float) minecraft.getWindow().getHeight();

        float baseRadius = screenDistance(base, baseEdge, aspect);
        float topRadius = screenDistance(top, topEdge, aspect);

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


        for (BossPortalEntity portal : minecraft.level.getEntitiesOfClass(BossPortalEntity.class, minecraft.player.getBoundingBox().inflate(RANGE),
                candidate -> candidate.isAlive() && candidate.isChaosPortal() && (candidate.getAnimState() == BossPortalEntity.STATE_IDLE || candidate.getAnimState() == BossPortalEntity.STATE_DESPAWN))) {

            double distance = portal.distanceToSqr(minecraft.player);


            if (distance >= bestDistance) continue;

            bestDistance = distance;
            closest = portal;
        }

        return closest;
    }

    private static ProjectedPoint project(Vec3 world, RenderLevelStageEvent event) {

        Vec3 camera = event.getCamera().getPosition();
        Vector4f clip = new Vector4f((float) (world.x - camera.x), (float) (world.y - camera.y), (float) (world.z - camera.z), 1.0F);

        clip.mul(event.getPoseStack().last().pose());
        clip.mul(event.getProjectionMatrix());

        if (clip.w() <= 0.001F) return null;

        float ndcX = clip.x() / clip.w();
        float ndcY = clip.y() / clip.w();
        float ndcZ = clip.z() / clip.w();

        return new ProjectedPoint(ndcX * 0.5F + 0.5F, ndcY * 0.5F + 0.5F, ndcZ * 0.5F + 0.5F);
    }

    private static float screenDistance(ProjectedPoint first, ProjectedPoint second, float aspect) {

        float x = (first.u - second.u) * aspect;
        float y = first.v - second.v;

        return (float) Math.sqrt(x * x + y * y);
    }

    private record ProjectedPoint(float u, float v, float depth) {
    }
}