package com.benji.oasiso.client;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import com.benji.oasiso.client.sound.EntropyVoicesSound;
import com.benji.oasiso.ModSounds;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class EntropyOverlayEvents {

    private static final ResourceLocation ENTROPY_OVERLAY = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/entropy_overlay.png");

    private static final ResourceLocation WHITE_OVERLAY = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/white_overlay.png");

    private static final int TEXTURE_WIDTH = 1920;
    private static final int TEXTURE_HEIGHT = 1080;
    private static EntropyVoicesSound voicesSound;

    private static final RandomSource RANDOM = RandomSource.create();

    private static float flashAlpha = 0.0F;
    private static int nextFlashTimer = 0;
    private static boolean wasActive = false;

    private EntropyOverlayEvents() {
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

        boolean active = hasEntropyEffect();

        if (!active) {
            flashAlpha = 0.0F;
            nextFlashTimer = 0;
            wasActive = false;

            stopVoicesSound();

            return;
        }

        if (!wasActive) {
            wasActive = true;

            nextFlashTimer = 40 + RANDOM.nextInt(81);

            startVoicesSound();
        }

        if (voicesSound == null || voicesSound.isStopped()) {
            startVoicesSound();
        }

        if (nextFlashTimer > 0) {
            nextFlashTimer--;
        } else if (flashAlpha <= 0.0F) {
            flashAlpha = 1.0F;

            playWhiteFlashSound();

            nextFlashTimer = 60 + RANDOM.nextInt(121);
        }

        if (flashAlpha > 0.0F) {
            flashAlpha = Math.max(0.0F, flashAlpha - 0.05F);
        }
    }

    private static void startVoicesSound() {
        Minecraft minecraft = Minecraft.getInstance();

        if (voicesSound != null && !voicesSound.isStopped()) {
            return;
        }

        voicesSound = new EntropyVoicesSound();

        minecraft.getSoundManager().play(voicesSound);
    }

    private static void stopVoicesSound() {
        if (voicesSound == null) {
            return;
        }

        Minecraft.getInstance().getSoundManager().stop(voicesSound);

        voicesSound = null;
    }


    private static void playWhiteFlashSound() {
        Minecraft minecraft = Minecraft.getInstance();

        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.WHITE_FLASH.get(), 1.0F, 1.0F));
    }

    @SubscribeEvent
    public static void onRenderHud(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen != null) {
            return;
        }

        if (hasEntropyEffect()) {
            renderOverlays(event.getGuiGraphics());
        }
    }

    @SubscribeEvent
    public static void onRenderScreen(ScreenEvent.Render.Post event) {
        if (hasEntropyEffect()) {
            renderOverlays(event.getGuiGraphics());
        }
    }

    private static boolean hasEntropyEffect() {
        Minecraft minecraft = Minecraft.getInstance();

        return minecraft.player != null && minecraft.player.hasEffect(Oasiso.ENTROPY_EFFECT.get());
    }

    private static void renderOverlays(GuiGraphics guiGraphics) {
        int guiWidth = guiGraphics.guiWidth();
        int guiHeight = guiGraphics.guiHeight();

        float scaleX = guiWidth / (float) TEXTURE_WIDTH;

        float scaleY = guiHeight / (float) TEXTURE_HEIGHT;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.scale(scaleX, scaleY, 1.0F);

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // gui
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(ENTROPY_OVERLAY, 0, 0, 0.0F, 0.0F, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        // white splash
        if (flashAlpha > 0.0F) {
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, flashAlpha);
            guiGraphics.blit(WHITE_OVERLAY, 0, 0, 0.0F, 0.0F, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }

        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();

        poseStack.popPose();
    }
}