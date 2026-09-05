package com.benji.oasiso.client.event;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.renderer.ScarabFlightRenderUtil;
import com.benji.oasiso.common.entity.ScarabEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.ViewportEvent;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ScarabPassengerRenderHandler {

    private ScarabPassengerRenderHandler() {
    }

    private static final float CAMERA_PITCH_FACTOR = 0.25F;
    private static final float CAMERA_ROLL_FACTOR = 0.45F;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerRenderPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();

        if (!(player.getVehicle() instanceof ScarabEntity scarab)) {
            return;
        }

        event.getPoseStack().pushPose();

        ScarabFlightRenderUtil.applySurfaceOffset(event.getPoseStack(), scarab);
        ScarabFlightRenderUtil.applyTilt(event.getPoseStack(), scarab, event.getPartialTick());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerRenderPost(RenderPlayerEvent.Post event) {
        if (!(event.getEntity().getVehicle() instanceof ScarabEntity)) {
            return;
        }

        event.getPoseStack().popPose();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!(event.getCamera().getEntity() instanceof Player player)) {
            return;
        }

        if (!(player.getVehicle() instanceof ScarabEntity scarab)) {
            return;
        }

        if (!scarab.isFlyingMode()) {
            return;
        }

        float partialTick = (float) event.getPartialTick();
        float flightPitch = scarab.getFlightPitch(partialTick);
        float flightRoll = scarab.getFlightRoll(partialTick);

        event.setPitch(event.getPitch() - flightPitch * CAMERA_PITCH_FACTOR);
        event.setRoll(event.getRoll() + flightRoll * CAMERA_ROLL_FACTOR);
    }
}