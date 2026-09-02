package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.shader.ChaosScissorsPortalShader;
import com.benji.oasiso.common.entity.ChaosPortalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChaosScissorsPortalRenderer {

    private static final float PORTAL_WIDTH = 4.00F;
    private static final float PORTAL_HEIGHT = 4.90F;
    private static final double VISUAL_Y_OFFSET = 1.30D;
    private static final double RENDER_DISTANCE = 96.0D;

    private ChaosScissorsPortalRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        List<ChaosPortalEntity> portals = minecraft.level.getEntitiesOfClass(ChaosPortalEntity.class, minecraft.player.getBoundingBox().inflate(RENDER_DISTANCE), ChaosPortalEntity::isAlive);
        if (portals.isEmpty()) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        float time = (minecraft.level.getGameTime() + event.getPartialTick()) / 20.0F;

        for (ChaosPortalEntity portal : portals) {
            renderPortal(event, portal, camera, time);
        }
    }

    private static void renderPortal(RenderLevelStageEvent event, ChaosPortalEntity portal, Vec3 camera, float time) {

        Vec3 center = new Vec3(portal.getX(), portal.getY() + VISUAL_Y_OFFSET, portal.getZ());
        Vec3 relative = center.subtract(camera);

        PoseStack portalStack = new PoseStack();

        portalStack.translate(relative.x, relative.y, relative.z);
        portalStack.mulPose(Axis.YP.rotationDegrees(portal.getPortalYaw()));

        float reveal = portal.getOpenProgress(event.getPartialTick());
        float despawn = portal.getDespawnProgress(event.getPartialTick());

        ChaosScissorsPortalShader.render(portalStack, PORTAL_WIDTH, PORTAL_HEIGHT, time, reveal, despawn, portal.getShaderSeed(), false);
        ChaosScissorsPortalShader.render(portalStack, PORTAL_WIDTH, PORTAL_HEIGHT, time, reveal, despawn, portal.getShaderSeed(), true);
    }
}