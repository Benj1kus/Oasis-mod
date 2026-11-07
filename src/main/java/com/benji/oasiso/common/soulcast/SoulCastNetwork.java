package com.benji.oasiso.common.soulcast;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.*;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = "oasiso", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SoulCastNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath("oasiso", "soul_player_cast"), () -> "1", "1"::equals, "1"::equals);

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent e) {
        e.enqueueWork(() -> {
            CHANNEL.registerMessage(0, Request.class, (m, b) -> {
            }, b -> new Request(), (m, c) -> {
                var ctx = c.get();
                ctx.enqueueWork(() -> {
                    if (ctx.getSender() != null) SoulCast.start(ctx.getSender());
                });
                ctx.setPacketHandled(true);
            }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
            CHANNEL.registerMessage(1, State.class, State::encode, State::decode, State::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        });
    }

    public record Request() {
    }

    public static void request() {
        CHANNEL.sendToServer(new Request());
    }

    public static void broadcast(ServerPlayer p, State s) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), s);
    }

    public static void send(ServerPlayer p, State s) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), s);
    }

    public record State(UUID player, long start, float yaw, boolean active) {
        static void encode(State s, FriendlyByteBuf b) {
            b.writeUUID(s.player);
            b.writeLong(s.start);
            b.writeFloat(s.yaw);
            b.writeBoolean(s.active);
        }

        static State decode(FriendlyByteBuf b) {
            return new State(b.readUUID(), b.readLong(), b.readFloat(), b.readBoolean());
        }

        static void handle(State s, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.benji.oasiso.client.soulcast.SoulCastClient.receive(s)));
            c.get().setPacketHandled(true);
        }
    }
}
