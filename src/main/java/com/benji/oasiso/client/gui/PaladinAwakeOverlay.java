package com.benji.oasiso.client.gui;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.shader.AzumaalPanelShader;
import com.benji.oasiso.common.entity.PaladinEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.benji.oasiso.network.dialogue.BossDialogueNetwork;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class PaladinAwakeOverlay {

    private static final ResourceLocation FRAME_TEXTURE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/azumaal/frame.png");

    private static final int FRAME_TEXTURE_WIDTH = 400;
    private static final int FRAME_TEXTURE_HEIGHT = 120;

    private static final float PANEL_SCALE = 0.5F;
    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_HEIGHT = 60;

    private static final int INNER_BORDER = 3;

    private static final int BOTTOM_MARGIN = 30;

    private static final float TITLE_SCALE = 1.55F;
    private static final float SUBTITLE_SCALE = 0.72F;

    private static final int TITLE_RGB = 0xFFD45A;

    private static final int SUBTITLE_RGB = 0xD9B75B;
    private static final int OUTLINE_RGB = 0x3A260A;

    private static final int FRAME_IN_END = 5;

    private static final int BG_IN_START = 2;
    private static final int BG_IN_END = 8;

    private static final int TITLE_IN_START = 5;
    private static final int TITLE_IN_END = 11;

    private static final int BG_DISSOLVE_START = 92;
    private static final int BG_DISSOLVE_END = 114;

    private static final int SUBTITLE_IN_START = 8;
    private static final int SUBTITLE_IN_END = 14;

    private static final int TEXT_OUT_START = 88;
    private static final int TEXT_OUT_END = 104;

    private static final int FRAME_OUT_START = 108;
    private static final int TOTAL_TICKS = 124;

    private static final Set<UUID> SHOWN_FOR_PALADINS = new HashSet<>();

    private static boolean active;
    private static int overlayTick;
    private static UUID activePaladinId;

    private PaladinAwakeOverlay() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            active = false;
            overlayTick = 0;
            activePaladinId = null;
            SHOWN_FOR_PALADINS.clear();
            return;
        }

        if (minecraft.player == null || minecraft.isPaused()) {
            return;
        }

        PaladinEntity paladin = findAwakingPaladin(minecraft);

        if (paladin != null && !SHOWN_FOR_PALADINS.contains(paladin.getUUID())) {

            SHOWN_FOR_PALADINS.add(paladin.getUUID());

            activePaladinId = paladin.getUUID();

            active = true;
            overlayTick = 0;
        }

        if (!active) {
            return;
        }

        overlayTick++;

        if (overlayTick >= TOTAL_TICKS) {
            active = false;

            if (activePaladinId != null) {
                BossDialogueNetwork.panelFinished(activePaladinId, "paladin");

                activePaladinId = null;
            }
        }
    }

    private static PaladinEntity findAwakingPaladin(Minecraft minecraft) {
        PaladinEntity closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (PaladinEntity paladin : minecraft.level.getEntitiesOfClass(PaladinEntity.class, minecraft.player.getBoundingBox().inflate(96.0D))) {

            if (paladin.getAnimState() != PaladinEntity.STATE_AWAKE) {
                continue;
            }
            double distance = paladin.distanceToSqr(minecraft.player);

            if (distance >= closestDistance) {
                continue;
            }
            closestDistance = distance;
            closest = paladin;
        }
        return closest;
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!active) {
            return;
        }
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        renderOverlay(event.getGuiGraphics(), minecraft);
    }
    // render
    private static void renderOverlay(GuiGraphics graphics, Minecraft minecraft) {
        int screenWidth = graphics.guiWidth();

        int screenHeight = graphics.guiHeight();
        int panelX = (screenWidth - PANEL_WIDTH) / 2;
        int visibleY = screenHeight - PANEL_HEIGHT - BOTTOM_MARGIN;
        int hiddenY = screenHeight + 8;

        float frameProgress;


        if (overlayTick < FRAME_IN_END) {

            frameProgress = easeOutBack(overlayTick / (float) FRAME_IN_END);

        } else if (overlayTick >= FRAME_OUT_START) {

            float out = Mth.clamp((overlayTick - FRAME_OUT_START) / (float) (TOTAL_TICKS - FRAME_OUT_START), 0.0F, 1.0F);
            frameProgress = 1.0F - easeInBack(out);

        } else {
            frameProgress = 1.0F;
        }

        int panelY = Mth.floor(Mth.lerp(frameProgress, hiddenY, visibleY));

        float bgReveal = smoothProgress(overlayTick, BG_IN_START, BG_IN_END);

        float bgAlpha = 1.0F;

        float bgDissolve = smoothProgress(overlayTick, BG_DISSOLVE_START, BG_DISSOLVE_END);


        float time = (overlayTick + minecraft.getFrameTime()) / 20.0F;

        if (bgReveal > 0.0F && bgAlpha > 0.0F) {

            float innerX = panelX + INNER_BORDER;
            float innerY = panelY + INNER_BORDER;

            float innerWidth = PANEL_WIDTH - INNER_BORDER * 2;
            float innerHeight = PANEL_HEIGHT - INNER_BORDER * 2;


            boolean rendered =
                    AzumaalPanelShader.render(
                            graphics.pose(),
                            innerX,
                            innerY,
                            innerWidth,
                            innerHeight,
                            time,
                            bgReveal,
                            bgAlpha,
                            bgDissolve
                    );

            if (!rendered) {
                int alpha = Mth.floor((1.0F - bgDissolve) * 200.0F);

                graphics.fill((int) innerX, (int) innerY, (int) (innerX + innerWidth), (int) (innerY + innerHeight), (alpha << 24) | 0x083C3A);
            }
        }

        drawFrame(graphics, panelX, panelY);

        String title = I18n.get("gui.oasiso.paladin.title");
        String subtitle = I18n.get("gui.oasiso.paladin.subtitle");


        float hide = smoothProgress(overlayTick, TEXT_OUT_START, TEXT_OUT_END);

        float titleAlpha = smoothProgress(overlayTick, TITLE_IN_START, TITLE_IN_END) * (1.0F - hide);

        float subtitleAlpha = smoothProgress(overlayTick, SUBTITLE_IN_START, SUBTITLE_IN_END) * (1.0F - hide);

        drawGoldText(graphics, minecraft.font, title, panelX + PANEL_WIDTH / 2, panelY + 18, TITLE_SCALE, TITLE_RGB, titleAlpha);

        drawGoldText(graphics, minecraft.font, subtitle, panelX + PANEL_WIDTH / 2, panelY + 42, SUBTITLE_SCALE, SUBTITLE_RGB, subtitleAlpha);
    }

    private static void drawGoldText(GuiGraphics graphics, Font font, String text, int centerX, int y, float scale, int rgb, float alpha) {
        if (alpha <= 0.001F) {

            return;
        }

        PoseStack poseStack = graphics.pose();

        poseStack.pushPose();
        poseStack.translate(centerX, y, 200.0F);
        poseStack.scale(scale, scale, 1.0F);

        int x = -font.width(text) / 2;
        int alphaByte = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        int color = (alphaByte << 24) | rgb;
        int outline = (alphaByte << 24) | OUTLINE_RGB;

        drawOutlinedString(graphics, font, text, x, 0, color, outline);
        poseStack.popPose();
    }


    private static void drawOutlinedString(GuiGraphics graphics, Font font, String text, int x, int y, int color, int outlineColor) {
        graphics.drawString(font, text, x - 1, y, outlineColor, false);
        graphics.drawString(font, text, x + 1, y, outlineColor, false);
        graphics.drawString(font, text, x, y - 1, outlineColor, false);
        graphics.drawString(font, text, x, y + 1, outlineColor, false);
        graphics.drawString(font, text, x - 1, y - 1, outlineColor, false);
        graphics.drawString(font, text, x + 1, y - 1, outlineColor, false);
        graphics.drawString(font, text, x - 1, y + 1, outlineColor, false);
        graphics.drawString(font, text, x + 1, y + 1, outlineColor, false);
        graphics.drawString(font, text, x, y, color, false);
    }


    private static void drawFrame(GuiGraphics graphics, int x, int y) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, 0.0F);

        poseStack.scale(PANEL_SCALE, PANEL_SCALE, 1.0F);

        graphics.blit(FRAME_TEXTURE, 0, 0, 0, 0, FRAME_TEXTURE_WIDTH, FRAME_TEXTURE_HEIGHT, FRAME_TEXTURE_WIDTH, FRAME_TEXTURE_HEIGHT);

        poseStack.popPose();
    }

    private static float smoothProgress(int tick, int start, int end) {
        if (tick <= start) {
            return 0.0F;
        }

        if (tick >= end) {
            return 1.0F;
        }

        float value = (tick - start) / (float) (end - start);
        return value * value * (3.0F - 2.0F * value);
    }


    private static float easeOutBack(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);

        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;

        return 1.0F + c3 * (t - 1.0F) * (t - 1.0F) * (t - 1.0F) + c1 * (t - 1.0F) * (t - 1.0F);
    }


    private static float easeInBack(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);

        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;

        return c3 * t * t * t - c1 * t * t;
    }
}