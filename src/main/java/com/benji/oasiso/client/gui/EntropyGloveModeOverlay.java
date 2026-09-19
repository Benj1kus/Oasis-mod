package com.benji.oasiso.client.gui;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.item.EntropyChestplateGloveItem;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EntropyGloveModeOverlay {
    private static final String GIF_FOLDER = "textures/gui/entropy_glove/";

    private static Mode currentMode;
    private static ResourceLocation activeAnimation;
    private static long animationStartedAt;
    private static boolean transitionPlaying;

    private EntropyGloveModeOverlay() {
    }

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            reset();
            return;
        }

        ItemStack glove = EntropyChestplateGloveItem.findGloveInHands(minecraft.player);
        if (glove.isEmpty()) {
            reset();
            return;
        }

        Mode observedMode = Mode.from(glove);
        long now = Util.getMillis();

        if (currentMode == null) {
            currentMode = observedMode;
            activeAnimation = selected(observedMode);
            animationStartedAt = now;
            transitionPlaying = false;
        } else if (observedMode != currentMode) {
            activeAnimation = transition(currentMode, observedMode);
            currentMode = observedMode;
            animationStartedAt = now;
            transitionPlaying = true;
        }

        AnimatedGifTexture gif = AnimatedGifTexture.get(activeAnimation);
        long age = Math.max(0L, now - animationStartedAt);

        if (transitionPlaying && (!gif.isAvailable() || age >= gif.durationMillis())) {
            activeAnimation = selected(currentMode);
            animationStartedAt = now;
            transitionPlaying = false;
            gif = AnimatedGifTexture.get(activeAnimation);
            age = 0L;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        gif.render(event.getGuiGraphics(), age, !transitionPlaying);
        RenderSystem.disableBlend();
    }

    private static void reset() {
        currentMode = null;
        activeAnimation = null;
        transitionPlaying = false;
        animationStartedAt = 0L;
    }

    private static ResourceLocation selected(Mode mode) {
        return gif(switch (mode) {
            case GRAVITY -> "gravy_selected.gif";
            case GRAPPLE -> "hook_selected.gif";
            case BUILD -> "build_selected.gif";
        });
    }

    private static ResourceLocation transition(Mode from, Mode to) {
        String file = switch (from) {
            case GRAVITY -> switch (to) {
                case GRAPPLE -> "gravy_to_hook.gif";
                case BUILD -> "gravy_to_build.gif";
                case GRAVITY -> "gravy_selected.gif";
            };
            case GRAPPLE -> switch (to) {
                case GRAVITY -> "hook_to_gravy.gif";
                case BUILD -> "hook_to_build.gif";
                case GRAPPLE -> "hook_selected.gif";
            };
            case BUILD -> switch (to) {
                case GRAVITY -> "build_to_gravy.gif";
                case GRAPPLE -> "build_to_hook.gif";
                case BUILD -> "build_selected.gif";
            };
        };
        return gif(file);
    }

    private static ResourceLocation gif(String file) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, GIF_FOLDER + file);
    }

    private enum Mode {
        GRAVITY, GRAPPLE, BUILD;

        static Mode from(ItemStack glove) {
            if (EntropyChestplateGloveItem.isGrappleMode(glove)) {
                return GRAPPLE;
            }
            if (EntropyChestplateGloveItem.isFillMode(glove)) {
                return BUILD;
            }
            return GRAVITY;
        }
    }
}
