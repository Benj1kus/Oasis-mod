package com.benji.oasiso.network;

import com.benji.oasiso.client.event.EntropyGrappleClientState;
import com.benji.oasiso.common.glove.EntropyGrappleManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public final class EntropyGrappleStateS2CPacket {

    public enum VisualState {
        LAUNCHING, ATTACHED, RETRACTING, CLEAR
    }

    private final UUID playerId;
    private final InteractionHand hand;
    private final VisualState state;
    private final Vec3 anchor;
    private final int duration;

    public EntropyGrappleStateS2CPacket(UUID playerId, InteractionHand hand, VisualState state, Vec3 anchor, int duration) {
        this.playerId = playerId;
        this.hand = hand;
        this.state = state;
        this.anchor = anchor;
        this.duration = duration;
    }

    public EntropyGrappleStateS2CPacket(FriendlyByteBuf buffer) {
        this.playerId = buffer.readUUID();
        this.hand = buffer.readEnum(InteractionHand.class);
        this.state = buffer.readEnum(VisualState.class);

        this.anchor = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());

        this.duration = buffer.readVarInt();
    }

    public static EntropyGrappleStateS2CPacket state(UUID playerId, InteractionHand hand, EntropyGrappleManager.State state, Vec3 anchor, int duration) {
        return new EntropyGrappleStateS2CPacket(playerId, hand, switch (state) {
            case LAUNCHING -> VisualState.LAUNCHING;
            case ATTACHED -> VisualState.ATTACHED;
            case RETRACTING -> VisualState.RETRACTING;
        }, anchor, duration);
    }

    public static EntropyGrappleStateS2CPacket clear(UUID playerId) {
        return new EntropyGrappleStateS2CPacket(playerId, InteractionHand.MAIN_HAND, VisualState.CLEAR, Vec3.ZERO, 0);
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeUUID(this.playerId);
        buffer.writeEnum(this.hand);
        buffer.writeEnum(this.state);

        buffer.writeDouble(this.anchor.x);
        buffer.writeDouble(this.anchor.y);
        buffer.writeDouble(this.anchor.z);

        buffer.writeVarInt(this.duration);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> EntropyGrappleClientState.apply(this.playerId, this.hand, this.state, this.anchor, this.duration)));

        context.setPacketHandled(true);
        return true;
    }
}
