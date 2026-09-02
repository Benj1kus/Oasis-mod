package com.benji.oasiso.network;

import com.benji.oasiso.client.event.EntropyGloveFillClientState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EntropyGloveFillSelectionSyncPacket {

    private final boolean mode;
    private final InteractionHand hand;
    private final boolean hasSelection;
    private final boolean complete;
    private final BlockPos first;
    private final BlockPos second;
    private final Direction.Axis axis;


    public EntropyGloveFillSelectionSyncPacket(boolean mode, InteractionHand hand, boolean hasSelection, boolean complete, BlockPos first, BlockPos second, Direction.Axis axis) {
        this.mode = mode;
        this.hand = hand;

        this.hasSelection = hasSelection;
        this.complete = complete;

        this.first = first;
        this.second = second;

        this.axis = axis;
    }


    public EntropyGloveFillSelectionSyncPacket(FriendlyByteBuf buffer) {
        this.mode = buffer.readBoolean();
        this.hand = buffer.readEnum(InteractionHand.class);

        this.hasSelection = buffer.readBoolean();
        this.complete = buffer.readBoolean();

        this.first = buffer.readBlockPos();
        this.second = buffer.readBlockPos();

        this.axis = buffer.readEnum(Direction.Axis.class);
    }


    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.mode);
        buffer.writeEnum(this.hand);
        buffer.writeBoolean(this.hasSelection);
        buffer.writeBoolean(this.complete);
        buffer.writeBlockPos(this.first);
        buffer.writeBlockPos(this.second);
        buffer.writeEnum(this.axis);
    }


    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();


        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> EntropyGloveFillClientState.apply(this.mode, this.hand, this.hasSelection, this.complete, this.first, this.second, this.axis)));


        context.setPacketHandled(true);
        return true;
    }
}