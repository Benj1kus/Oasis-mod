package com.benji.oasiso.network;

import com.benji.oasiso.client.gui.BossPortalTransitionClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class BossPortalTransitionS2CPacket {

    public enum Action {
        CLOSE, OPEN, CANCEL
    }

    private final Action action;

    public BossPortalTransitionS2CPacket(Action action) {
        this.action = action;
    }

    public static void encode(BossPortalTransitionS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.action.ordinal());
    }

    public static BossPortalTransitionS2CPacket decode(FriendlyByteBuf buffer) {
        int index = buffer.readUnsignedByte();

        Action[] values = Action.values();

        if (index < 0 || index >= values.length) {
            index = Action.CANCEL.ordinal();
        }

        return new BossPortalTransitionS2CPacket(values[index]);
    }

    public static void handle(BossPortalTransitionS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BossPortalTransitionClient.handle(packet.action)));

        context.setPacketHandled(true);
    }
}