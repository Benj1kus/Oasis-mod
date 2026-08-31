package com.benji.oasiso.network;

import com.benji.oasiso.common.waypoint.AzumalitWaypointManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AzumalitWaypointRequestPacket {

    public AzumalitWaypointRequestPacket() {
    }

    public AzumalitWaypointRequestPacket(FriendlyByteBuf buffer) {
    }

    public void toBytes(FriendlyByteBuf buffer) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player != null) {
                AzumalitWaypointManager.requestCast(player);
            }
        });

        context.setPacketHandled(true);
        return true;
    }
}
