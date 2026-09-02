package com.benji.oasiso.network;

import com.benji.oasiso.client.dimension.PocketModeClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class PocketModeS2CPacket {

    private final boolean active;

    public PocketModeS2CPacket(boolean active) {
        this.active = active;
    }

    public PocketModeS2CPacket(FriendlyByteBuf buffer) {
        this.active = buffer.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.active);
    }

    public static void handle(PocketModeS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PocketModeClient.setActive(packet.active)));

        context.setPacketHandled(true);
    }
}