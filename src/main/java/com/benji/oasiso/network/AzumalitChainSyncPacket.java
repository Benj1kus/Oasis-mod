package com.benji.oasiso.network;

import com.benji.oasiso.client.AzumalitChainClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class AzumalitChainSyncPacket {

    public static final byte PHASE_CAST_STARTED = 0;
    public static final byte PHASE_CHAIN_STARTED = 1;
    public static final byte PHASE_STOPPED = 2;

    private final byte phase;
    private final int ownerEntityId;
    private final long chainStartGameTime;
    private final List<Integer> targetEntityIds;

    private AzumalitChainSyncPacket(byte phase, int ownerEntityId, long chainStartGameTime, List<Integer> targetEntityIds) {
        this.phase = phase;
        this.ownerEntityId = ownerEntityId;
        this.chainStartGameTime = chainStartGameTime;
        this.targetEntityIds = List.copyOf(targetEntityIds);
    }

    public static AzumalitChainSyncPacket castStarted(int ownerEntityId) {
        return new AzumalitChainSyncPacket(PHASE_CAST_STARTED, ownerEntityId, 0L, List.of());
    }

    public static AzumalitChainSyncPacket chainStarted(int ownerEntityId, long chainStartGameTime, List<Integer> targetEntityIds) {
        return new AzumalitChainSyncPacket(PHASE_CHAIN_STARTED, ownerEntityId, chainStartGameTime, targetEntityIds);
    }

    public static AzumalitChainSyncPacket stopped(int ownerEntityId) {
        return new AzumalitChainSyncPacket(PHASE_STOPPED, ownerEntityId, 0L, List.of());
    }

    public AzumalitChainSyncPacket(FriendlyByteBuf buffer) {
        this.phase = buffer.readByte();
        this.ownerEntityId = buffer.readVarInt();
        this.chainStartGameTime = buffer.readLong();

        int size = buffer.readVarInt();
        List<Integer> targets = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            targets.add(buffer.readVarInt());
        }

        this.targetEntityIds = List.copyOf(targets);
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeByte(this.phase);
        buffer.writeVarInt(this.ownerEntityId);
        buffer.writeLong(this.chainStartGameTime);
        buffer.writeVarInt(this.targetEntityIds.size());

        for (int entityId : this.targetEntityIds) {
            buffer.writeVarInt(entityId);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AzumalitChainClient.handleSync(this.phase, this.ownerEntityId, this.chainStartGameTime, this.targetEntityIds)));

        context.setPacketHandled(true);
        return true;
    }
}
