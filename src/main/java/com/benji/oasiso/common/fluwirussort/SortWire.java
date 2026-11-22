package com.benji.oasiso.common.fluwirussort;

import com.benji.oasiso.client.fluwirussort.SortDisplay;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = "oasiso", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SortWire {
    static final int MAX = 1024;
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath("oasiso", "fluwirus_sort"), () -> "1", "1"::equals, "1"::equals);

    public enum Kind {OPEN, MOVE, LAND, DONE, CANCEL}

    public record Cue(Kind kind, int session, int menu, int duration, int[] slots) {
    }

    private record Order(int session, int[] indices) {
    }

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CHANNEL.registerMessage(0, Cue.class, (p, b) -> {
                b.writeEnum(p.kind);
                b.writeInt(p.session);
                b.writeVarInt(p.menu);
                b.writeVarInt(p.duration);
                b.writeVarIntArray(p.slots);
            }, b -> new Cue(b.readEnum(Kind.class), b.readInt(), b.readVarInt(), b.readVarInt(), b.readVarIntArray(MAX)), SortWire::client, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
            CHANNEL.registerMessage(1, Order.class, (p, b) -> {
                b.writeInt(p.session);
                b.writeVarIntArray(p.indices);
            }, b -> new Order(b.readInt(), b.readVarIntArray(MAX)), SortWire::server, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        });
    }

    private static void client(Cue packet, Supplier<NetworkEvent.Context> supplier) {
        var ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SortDisplay.receive(packet)));
        ctx.setPacketHandled(true);
    }

    private static void server(Order packet, Supplier<NetworkEvent.Context> supplier) {
        var ctx = supplier.get();
        ServerPlayer player = ctx.getSender();
        ctx.enqueueWork(() -> {
            if (player != null) FluwirusSort.order(player, packet.session, packet.indices);
        });
        ctx.setPacketHandled(true);
    }

    public static void reply(int session, int[] order) {
        CHANNEL.sendToServer(new Order(session, order));
    }

    static void cue(ServerPlayer player, Kind kind, int id, int menu, int duration, int... slots) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Cue(kind, id, menu, duration, slots));
    }
}
