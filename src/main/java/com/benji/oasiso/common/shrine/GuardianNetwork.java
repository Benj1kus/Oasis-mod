package com.benji.oasiso.common.shrine;

import com.benji.oasiso.Oasiso;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.*;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GuardianNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "soul_guardian_scene"), () -> "1", "1"::equals, "1"::equals);

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent e) {
        e.enqueueWork(() -> {
            CHANNEL.registerMessage(0, Scene.class, Scene::encode, Scene::decode, Scene::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
            CHANNEL.registerMessage(1, Done.class, Done::encode, Done::decode, Done::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        });
    }

    public static void send(ServerPlayer p, Scene s) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), s);
    }

    public static void finish(UUID token, boolean completed) {
        CHANNEL.sendToServer(new Done(token, completed));
    }

    public record Scene(UUID token, UUID soul, boolean stop) {
        static void encode(Scene p, FriendlyByteBuf b) {
            b.writeUUID(p.token);
            b.writeUUID(p.soul);
            b.writeBoolean(p.stop);
        }

        static Scene decode(FriendlyByteBuf b) {
            return new Scene(b.readUUID(), b.readUUID(), b.readBoolean());
        }

        static void handle(Scene p, Supplier<NetworkEvent.Context> s) {
            var c = s.get();
            c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.benji.oasiso.client.shrine.GuardianScene.receive(p)));
            c.setPacketHandled(true);
        }
    }

    public record Done(UUID token, boolean completed) {
        static void encode(Done p, FriendlyByteBuf b) {
            b.writeUUID(p.token);
            b.writeBoolean(p.completed);
        }

        static Done decode(FriendlyByteBuf b) {
            return new Done(b.readUUID(), b.readBoolean());
        }

        static void handle(Done p, Supplier<NetworkEvent.Context> s) {
            var c = s.get();
            c.enqueueWork(() -> {
                if (c.getSender() != null) GuardianScenes.finish(c.getSender(), p.token, p.completed);
            });
            c.setPacketHandled(true);
        }
    }
}
