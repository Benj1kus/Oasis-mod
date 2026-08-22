package com.benji.oasiso.client.dimension;

import com.benji.oasiso.Oasiso;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ChaosFlashClientHandler {


    private static final int MIN_FLASH_DELAY = 120;
    private static final int MAX_FLASH_DELAY = 320;


    private static final int MIN_FLASH_DURATION = 22;
    private static final int MAX_FLASH_DURATION = 34;

    private static final int TINT_RED = 70;
    private static final int TINT_GREEN = 255;
    private static final int TINT_BLUE = 205;
    private static final float MAX_TINT_ALPHA = 0.17F;

    private static final RandomSource RANDOM = RandomSource.create();

    private static int nextFlashTicks = -1;

    private static int flashAge = -1;

    private static int flashDuration = 1;

    private static float previousIntensity;
    private static float currentIntensity;

    private static float flashYaw;
    private static float flashTilt;
    private static float flashRoll;
    private static float flashSize = 28.0F;

    private ChaosFlashClientHandler() {
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

        if (!isInsideChaosDimension(minecraft)) {
            reset();
            return;
        }

        previousIntensity = currentIntensity;

        if (nextFlashTicks < 0 && flashAge < 0) {

            nextFlashTicks = randomFlashDelay();
        }

        if (flashAge >= 0) {
            tickFlash();
            return;
        }

        nextFlashTicks--;

        if (nextFlashTicks <= 0) {
            startFlash();
        }
    }

    private static void startFlash() {
        flashAge = 0;

        flashDuration = randomBetween(MIN_FLASH_DURATION, MAX_FLASH_DURATION);
        flashYaw = RANDOM.nextFloat() * 360.0F;
        flashTilt = 15.0F + RANDOM.nextFloat() * 55.0F;
        flashRoll = RANDOM.nextFloat() * 360.0F;
        flashSize = 24.0F + RANDOM.nextFloat() * 12.0F;
        currentIntensity = 0.0F;
    }

    private static void tickFlash() {
        flashAge++;

        if (flashAge >= flashDuration) {
            flashAge = -1;

            currentIntensity = 0.0F;

            nextFlashTicks = randomFlashDelay();

            return;
        }
        currentIntensity = calculateIntensity(flashAge, flashDuration);
    }

    private static float calculateIntensity(int age, int duration) {
        float progress = Mth.clamp(age / (float) duration, 0.0F, 1.0F);

        final float peakPoint = 0.18F;

        if (progress < peakPoint) {

            float rise = progress / peakPoint;

            return smoothStep(rise);
        }

        float fade = (progress - peakPoint) / (1.0F - peakPoint);

        return 1.0F - smoothStep(fade);
    }

    private static float smoothStep(float value) {
        value = Mth.clamp(value, 0.0F, 1.0F);

        return value * value * (3.0F - 2.0F * value);
    }

    public static float getFlashIntensity(float partialTick) {
        return Mth.lerp(partialTick, previousIntensity, currentIntensity);
    }

    public static float getFlashYaw() {
        return flashYaw;
    }

    public static float getFlashTilt() {
        return flashTilt;
    }

    public static float getFlashRoll() {
        return flashRoll;
    }

    public static float getFlashSize() {
        return flashSize;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (!isInsideChaosDimension(minecraft)) {
            return;
        }

        float intensity = getFlashIntensity(event.getPartialTick());

        if (intensity <= 0.001F) {
            return;
        }

        int alpha = Math.round(255.0F * MAX_TINT_ALPHA * intensity);

        alpha = Math.max(0, Math.min(255, alpha));

        int color = (alpha << 24) | (TINT_RED << 16) | (TINT_GREEN << 8) | TINT_BLUE;

        GuiGraphics graphics = event.getGuiGraphics();

        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), color);
    }

    private static boolean isInsideChaosDimension(Minecraft minecraft) {
        return minecraft.level != null && minecraft.player != null && minecraft.level.dimension().equals(Oasiso.CHAOS_DIMENSION);
    }

    private static int randomFlashDelay() {
        return randomBetween(MIN_FLASH_DELAY, MAX_FLASH_DELAY);
    }

    private static int randomBetween(int minimum, int maximum) {
        return minimum + RANDOM.nextInt(maximum - minimum + 1);
    }

    private static void reset() {
        nextFlashTicks = -1;
        flashAge = -1;
        previousIntensity = 0.0F;
        currentIntensity = 0.0F;
    }
}