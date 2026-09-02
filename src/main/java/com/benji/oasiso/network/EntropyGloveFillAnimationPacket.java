package com.benji.oasiso.network;

import com.benji.oasiso.client.event.EntropyGloveFillRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EntropyGloveFillAnimationPacket {

    private final BlockPos pos;
    private final int stateId;
    private final int duration;


    public EntropyGloveFillAnimationPacket(BlockPos pos, int stateId, int duration) {
        this.pos = pos;
        this.stateId = stateId;
        this.duration = duration;
    }


    public EntropyGloveFillAnimationPacket(FriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
        this.stateId = buffer.readVarInt();
        this.duration = buffer.readVarInt();
    }


    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
        buffer.writeVarInt(this.stateId);
        buffer.writeVarInt(this.duration);
    }


    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            BlockState state = Block.stateById(this.stateId);
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> EntropyGloveFillRenderer.addAnimation(this.pos, state, this.duration));
        });

        context.setPacketHandled(true);
        return true;
    }
}