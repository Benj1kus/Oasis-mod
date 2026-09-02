package com.benji.oasiso.network;

import com.benji.oasiso.common.glove.EntropyGloveFillManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EntropyGloveFillActionPacket {

    public enum Action {
        TOGGLE_MODE, START_SELECTION, FINISH_SELECTION, FILL
    }

    private final Action action;
    private final InteractionHand hand;
    private final BlockPos pos;
    private final Direction.Axis axis;


    public EntropyGloveFillActionPacket(Action action, InteractionHand hand, BlockPos pos, Direction.Axis axis) {
        this.action = action;
        this.hand = hand;
        this.pos = pos;
        this.axis = axis;
    }


    public EntropyGloveFillActionPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(Action.class);
        this.hand = buffer.readEnum(InteractionHand.class);
        this.pos = buffer.readBlockPos();
        this.axis = buffer.readEnum(Direction.Axis.class);
    }


    public static EntropyGloveFillActionPacket toggle(InteractionHand hand) {
        return new EntropyGloveFillActionPacket(Action.TOGGLE_MODE,
                hand,
                BlockPos.ZERO,
                Direction.Axis.Y);
    }


    public static EntropyGloveFillActionPacket start(InteractionHand hand, BlockPos pos, Direction.Axis axis) {
        return new EntropyGloveFillActionPacket(Action.START_SELECTION,
                hand, pos, axis);
    }


    public static EntropyGloveFillActionPacket finish(InteractionHand hand, BlockPos pos) {
        return new EntropyGloveFillActionPacket(Action.FINISH_SELECTION,
                hand,
                pos,
                Direction.Axis.Y);
    }


    public static EntropyGloveFillActionPacket fill(InteractionHand hand, BlockPos pos) {
        return new EntropyGloveFillActionPacket(Action.FILL,
                hand,
                pos,
                Direction.Axis.Y);
    }


    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeEnum(this.hand);
        buffer.writeBlockPos(this.pos);
        buffer.writeEnum(this.axis);
    }


    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            switch (this.action) {
                case TOGGLE_MODE -> EntropyGloveFillManager.toggleMode(player, this.hand);

                case START_SELECTION -> EntropyGloveFillManager.startSelection(player,
                        this.hand,
                        this.pos,
                        this.axis);

                case FINISH_SELECTION -> EntropyGloveFillManager.finishSelection(player,
                        this.hand,
                        this.pos);

                case FILL -> EntropyGloveFillManager.startFill(player,
                        this.hand,
                        this.pos);
            }
        });

        context.setPacketHandled(true);
        return true;
    }
}