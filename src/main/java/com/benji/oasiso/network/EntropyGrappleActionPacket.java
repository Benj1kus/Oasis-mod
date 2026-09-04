package com.benji.oasiso.network;

import com.benji.oasiso.common.glove.EntropyGrappleManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class EntropyGrappleActionPacket {

    public enum Action {
        TOGGLE_MODE, USE
    }

    private final Action action;
    private final InteractionHand hand;

    public EntropyGrappleActionPacket(Action action, InteractionHand hand) {
        this.action = action;
        this.hand = hand;
    }

    public EntropyGrappleActionPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(Action.class);
        this.hand = buffer.readEnum(InteractionHand.class);
    }

    public static EntropyGrappleActionPacket toggle(InteractionHand hand) {
        return new EntropyGrappleActionPacket(Action.TOGGLE_MODE, hand);
    }

    public static EntropyGrappleActionPacket use(InteractionHand hand) {
        return new EntropyGrappleActionPacket(Action.USE, hand);
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeEnum(this.hand);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player == null) {
                return;
            }

            switch (this.action) {
                case TOGGLE_MODE -> EntropyGrappleManager.toggleMode(player, this.hand);

                case USE -> EntropyGrappleManager.use(player, this.hand);
            }
        });

        context.setPacketHandled(true);
        return true;
    }
}
