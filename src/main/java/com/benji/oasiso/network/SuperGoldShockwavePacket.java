package com.benji.oasiso.network;

import com.benji.oasiso.common.event.SuperGoldAbilityHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SuperGoldShockwavePacket {

    public SuperGoldShockwavePacket() {
    }

    public SuperGoldShockwavePacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                SuperGoldAbilityHandler.executeServerShockwave(player);
            }
        });
        return true;
    }
}