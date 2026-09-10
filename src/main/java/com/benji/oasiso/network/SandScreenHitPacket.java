package com.benji.oasiso.network;

import com.benji.oasiso.client.gui.SandScreenOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SandScreenHitPacket {

    public SandScreenHitPacket() {
    }

    public SandScreenHitPacket(
            FriendlyByteBuf buffer
    ) {
    }

    public void toBytes(
            FriendlyByteBuf buffer
    ) {
        /*
         * Payload не нужен.
         *
         * Сам факт получения packet =
         * создать новое пятно песка.
         */
    }

    public static void handle(
            SandScreenHitPacket message,
            Supplier<NetworkEvent.Context> context
    ) {
        context.get().enqueueWork(
                () -> DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> SandScreenOverlay::spawn
                )
        );

        context.get().setPacketHandled(
                true
        );
    }
}