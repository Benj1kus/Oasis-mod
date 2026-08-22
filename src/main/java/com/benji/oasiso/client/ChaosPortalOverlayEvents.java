package com.benji.oasiso.client;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ChaosPortalOverlayEvents {

    private static final ResourceLocation PORTAL_TEXTURE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/block/chaos_portal.png");

    private static final int FRAME_SIZE = 16;
    private static final int FRAME_COUNT = 14;
    private static final int TEXTURE_WIDTH = 16;
    private static final int TEXTURE_HEIGHT = 224;

    private static float portalAlpha;

    private ChaosPortalOverlayEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.isPaused()) {
            return;
        }

        boolean insidePortal = isInsideChaosPortal();

        if (insidePortal) {

            portalAlpha = Math.min(1.0F, portalAlpha + 1.0F / 60.0F);
        } else {
            portalAlpha = Math.max(0.0F, portalAlpha - 0.08F);
        }
    }

    @SubscribeEvent
    public static void onRenderHud(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen != null) {
            return;
        }

        renderOverlay(event.getGuiGraphics());
    }

    @SubscribeEvent
    public static void onRenderScreen(ScreenEvent.Render.Post event) {
        renderOverlay(event.getGuiGraphics());
    }

    private static boolean isInsideChaosPortal() {
        Minecraft minecraft = Minecraft.getInstance();

        Player player = minecraft.player;

        if (player == null || minecraft.level == null) {
            return false;
        }

        AABB bounds = player.getBoundingBox().inflate(0.05D);

        BlockPos min = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ);
        BlockPos max = BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {

            if (minecraft.level.getBlockState(pos).is(Oasiso.CHAOS_PORTAL.get())) {
                return true;
            }
        }

        return false;
    }

    private static void renderOverlay(GuiGraphics guiGraphics) {
        if (portalAlpha <= 0.01F) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        int guiWidth = guiGraphics.guiWidth();
        int guiHeight = guiGraphics.guiHeight();


        int frame = (int) (minecraft.level.getGameTime() / 2L % FRAME_COUNT);

        float scaleX = guiWidth / (float) FRAME_SIZE;
        float scaleY = guiHeight / (float) FRAME_SIZE;

        PoseStack poseStack = guiGraphics.pose();

        poseStack.pushPose();
        poseStack.scale(scaleX, scaleY, 1.0F);

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        guiGraphics.setColor(1.0F, 1.0F, 1.0F, portalAlpha);

        guiGraphics.blit(PORTAL_TEXTURE, 0, 0, 0.0F, frame * FRAME_SIZE, FRAME_SIZE, FRAME_SIZE, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();

        poseStack.popPose();
    }
}