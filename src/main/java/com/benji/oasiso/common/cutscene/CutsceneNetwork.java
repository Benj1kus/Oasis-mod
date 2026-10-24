package com.benji.oasiso.common.cutscene;

import com.benji.oasiso.Oasiso;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;

import java.util.UUID;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CutsceneNetwork {
    private static final SimpleChannel C = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "cutscenes"), () -> "1", "1"::equals, "1"::equals);

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent e) {
        e.enqueueWork(() -> {
            C.messageBuilder(Start.class, 0, NetworkDirection.PLAY_TO_CLIENT).encoder(Start::encode).decoder(Start::decode).consumerNetworkThread(Start::handle).add();
            C.messageBuilder(Finish.class, 1, NetworkDirection.PLAY_TO_SERVER).encoder(Finish::encode).decoder(Finish::decode).consumerNetworkThread(Finish::handle).add();
            C.messageBuilder(Stop.class, 2, NetworkDirection.PLAY_TO_CLIENT).encoder(Stop::encode).decoder(Stop::decode).consumerNetworkThread(Stop::handle).add();
        });
    }

    public static void start(ServerPlayer p, Start s) {
        C.send(PacketDistributor.PLAYER.with(() -> p), s);
    }

    public static void stop(ServerPlayer p, UUID token) {
        C.send(PacketDistributor.PLAYER.with(() -> p), new Stop(token));
    }

    public static void finish(UUID token) {
        C.sendToServer(new Finish(token));
    }

    public record Start(UUID token, UUID boss, long start, String json) {
        static void encode(Start p, FriendlyByteBuf b) {
            b.writeUUID(p.token);
            b.writeUUID(p.boss);
            b.writeLong(p.start);
            b.writeUtf(p.json, 60000);
        }

        static Start decode(FriendlyByteBuf b) {
            return new Start(b.readUUID(), b.readUUID(), b.readLong(), b.readUtf(60000));
        }

        static void handle(Start p, Supplier<NetworkEvent.Context> s) {
            var c = s.get();
            c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.benji.oasiso.client.cutscene.CutsceneScreen.open(p)));
            c.setPacketHandled(true);
        }
    }

    public record Finish(UUID token) {
        static void encode(Finish p, FriendlyByteBuf b) {
            b.writeUUID(p.token);
        }

        static Finish decode(FriendlyByteBuf b) {
            return new Finish(b.readUUID());
        }

        static void handle(Finish p, Supplier<NetworkEvent.Context> s) {
            var c = s.get();
            c.enqueueWork(() -> {
                if (c.getSender() != null) CutsceneServer.finish(c.getSender(), p.token);
            });
            c.setPacketHandled(true);
        }
    }

    public record Stop(UUID token) {
        static void encode(Stop p, FriendlyByteBuf b) {
            b.writeUUID(p.token);
        }

        static Stop decode(FriendlyByteBuf b) {
            return new Stop(b.readUUID());
        }

        static void handle(Stop p, Supplier<NetworkEvent.Context> s) {
            var c = s.get();
            c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.benji.oasiso.client.cutscene.CutsceneScreen.stop(p.token)));
            c.setPacketHandled(true);
        }
    }
}
