package com.benji.oasiso.client.event;

import com.benji.oasiso.common.entity.AzumaalEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class AzumaalMegaBeamCameraShake {

    private static final double MAX_DISTANCE = 70.0D;
    private static int shakeFrames = 0;
    private static float shakeIntensity = 0.0F;

    private AzumaalMegaBeamCameraShake() {
    }

    public static void activate(AzumaalEntity boss) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        double distance = minecraft.player.distanceTo(boss);

        if (distance > MAX_DISTANCE) {
            return;
        }

        float proximity = 1.0F - Mth.clamp((float) (distance / MAX_DISTANCE), 0.0F, 1.0F);
        float intensity = 2.2F + proximity * 3.3F;

        shakeFrames = 4;
        shakeIntensity = Math.max(shakeIntensity, intensity);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        if (shakeFrames <= 0 || shakeIntensity <= 0.001F) {

            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.isPaused()) {
            return;
        }

        float time = minecraft.level != null ? minecraft.level.getGameTime() + (float) event.getPartialTick() : 0.0F;

        float randomPitch = (minecraft.player.getRandom().nextFloat() - 0.5F) * shakeIntensity;
        float randomYaw = (minecraft.player.getRandom().nextFloat() - 0.5F) * shakeIntensity * 0.80F;
        float randomRoll = (minecraft.player.getRandom().nextFloat() - 0.5F) * shakeIntensity * 0.55F;

        float heavyPitch = Mth.sin(time * 2.8F) * shakeIntensity * 0.22F;
        float heavyYaw = Mth.sin(time * 3.4F + 1.3F) * shakeIntensity * 0.16F;
        float heavyRoll = Mth.sin(time * 2.2F + 0.7F) * shakeIntensity * 0.10F;

        event.setPitch(event.getPitch() + randomPitch + heavyPitch);
        event.setYaw(event.getYaw() + randomYaw + heavyYaw);
        event.setRoll(event.getRoll() + randomRoll + heavyRoll);

        shakeFrames--;

        if (shakeFrames <= 2) {
            shakeIntensity *= 0.72F;
        }

        if (shakeFrames <= 0) {
            shakeIntensity = 0.0F;
        }
    }
}