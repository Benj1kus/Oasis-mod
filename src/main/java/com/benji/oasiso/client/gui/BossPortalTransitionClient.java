package com.benji.oasiso.client.gui;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.shader.BossPortalTransitionShader;
import com.benji.oasiso.network.BossPortalTransitionS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BossPortalTransitionClient {

    private static final int CLOSE_TICKS = 24;
    private static final int OPEN_TICKS = 28;

    private static State state = State.IDLE;

    private static int transitionTick;

    private static float previousCover;
    private static float cover;

    private static float phaseStartCover;

    private BossPortalTransitionClient() {
    }


    public static void handle(BossPortalTransitionS2CPacket.Action action) {
        switch (action) {
            case CLOSE -> beginClosing();
            case OPEN -> beginOpening();
            case CANCEL -> beginOpening();
        }
    }


    private static void beginClosing() {
        phaseStartCover = cover;

        transitionTick = 0;
        state = State.CLOSING;
    }


    private static void beginOpening() {
        phaseStartCover = cover;

        transitionTick = 0;
        state = State.OPENING;
    }


    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            reset();
            return;
        }

        if (minecraft.isPaused() || state == State.IDLE) {
            return;
        }

        previousCover = cover;
        transitionTick++;


        switch (state) {

            case CLOSING -> {
                float progress = smooth(transitionTick / (float) CLOSE_TICKS);

                cover = Mth.lerp(progress, phaseStartCover, 1.0F);

                if (transitionTick >= CLOSE_TICKS) {
                    cover = 1.0F;
                    previousCover = 1.0F;
                    state = State.COVERED;
                }
            }

            case COVERED -> {
                cover = 1.0F;
                previousCover = 1.0F;
            }

            case OPENING -> {
                float progress = smooth(transitionTick / (float) OPEN_TICKS);

                cover = phaseStartCover * (1.0F - progress);

                if (transitionTick >= OPEN_TICKS) {
                    reset();
                }
            }
            case IDLE -> {
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            return;
        }
        render();
    }

    @SubscribeEvent
    public static void onRenderScreen(ScreenEvent.Render.Post event) {
        render();
    }


    private static void render() {
        if (state == State.IDLE && cover <= 0.001F) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        float partialTick = minecraft.getFrameTime();
        float interpolatedCover = Mth.lerp(partialTick, previousCover, cover);

        float time;

        if (minecraft.level != null) {
            time = (minecraft.level.getGameTime() + partialTick) / 20.0F;
        } else {
            time = System.nanoTime() / 1_000_000_000.0F;
        }

        BossPortalTransitionShader.render(interpolatedCover, time);
    }


    private static float smooth(float value) {
        value = Mth.clamp(value, 0.0F, 1.0F);
        return value * value * (3.0F - 2.0F * value);
    }


    private static void reset() {
        state = State.IDLE;

        transitionTick = 0;

        previousCover = 0.0F;
        cover = 0.0F;

        phaseStartCover = 0.0F;
    }


    private enum State {
        IDLE, CLOSING, COVERED, OPENING
    }
}