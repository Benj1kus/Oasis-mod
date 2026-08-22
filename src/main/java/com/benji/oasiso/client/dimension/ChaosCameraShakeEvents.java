package com.benji.oasiso.client.dimension;

import com.benji.oasiso.Oasiso;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ChaosCameraShakeEvents {

    private ChaosCameraShakeEvents() {
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null || !minecraft.level.dimension().equals(Oasiso.CHAOS_DIMENSION)) {
            return;
        }

        int stage = ChaosDimensionClientEvents.getMoonStage();

        float intensity = switch (stage) {
            case 3 -> 0.30F;
            case 4 -> 0.60F;
            case 5 -> 1.20F;
            case 6 -> 1.60F;
            default -> 0.0F;
        };

        if (intensity <= 0.0F) {
            return;
        }

        float time = (float) (ChaosDimensionClientEvents.getDimensionTicks() + event.getPartialTick());

        float yawOffset = (Mth.sin(time * 0.91F) * 0.7F + Mth.sin(time * 1.73F) * 0.3F) * intensity;
        float pitchOffset = (Mth.sin(time * 1.19F) * 0.65F + Mth.sin(time * 0.53F) * 0.35F) * intensity;
        float rollOffset = Mth.sin(time * 0.77F) * intensity * 0.75F;

        event.setYaw(event.getYaw() + yawOffset);

        event.setPitch(event.getPitch() + pitchOffset);

        event.setRoll(event.getRoll() + rollOffset);
    }
}