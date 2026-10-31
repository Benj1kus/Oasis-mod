package com.benji.oasiso.common.voice;

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

import java.util.Optional;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = "oasiso", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class InnerVoiceNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath("oasiso", "inner_voice"), () -> "1", "1"::equals, "1"::equals);

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent e) {
        e.enqueueWork(() -> {
            CHANNEL.registerMessage(0, Say.class, Say::encode, Say::decode, Say::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
            CHANNEL.registerMessage(1, Done.class, Done::encode, Done::decode, Done::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        });
    }

    public static void say(ServerPlayer p, String key, long token) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new Say(key, token));
    }

    public static void done(long token) {
        CHANNEL.sendToServer(new Done(token));
    }

    public record Say(String key, long token) {
        static void encode(Say m, FriendlyByteBuf b) {
            b.writeUtf(m.key, 256);
            b.writeLong(m.token);
        }

        static Say decode(FriendlyByteBuf b) {
            return new Say(b.readUtf(256), b.readLong());
        }

        static void handle(Say m, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.benji.oasiso.client.voice.InnerVoiceClient.show(m.key, m.token)));
            c.get().setPacketHandled(true);
        }
    }

    public record Done(long token) {
        static void encode(Done m, FriendlyByteBuf b) {
            b.writeLong(m.token);
        }

        static Done decode(FriendlyByteBuf b) {
            return new Done(b.readLong());
        }

        static void handle(Done m, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> {
                ServerPlayer p = c.get().getSender();
                if (p != null) InnerVoiceRules.done(p, m.token);
            });
            c.get().setPacketHandled(true);
        }
    }
}
