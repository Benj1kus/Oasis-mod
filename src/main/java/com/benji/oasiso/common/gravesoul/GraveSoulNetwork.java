package com.benji.oasiso.common.gravesoul;

import com.benji.oasiso.Oasiso;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
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
public final class GraveSoulNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "grave_soul_scene"), () -> "2", "2"::equals, "2"::equals);

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

    public static void finish(UUID token) {
        CHANNEL.sendToServer(new Done(token, false));
    }

    public static void stopPose(UUID token) {
        CHANNEL.sendToServer(new Done(token, true));
    }

    public record Scene(UUID token, UUID soul, int phrase, boolean stop) {
        static void encode(Scene p, FriendlyByteBuf b) {
            b.writeUUID(p.token);
            b.writeUUID(p.soul);
            b.writeVarInt(p.phrase);
            b.writeBoolean(p.stop);
        }

        static Scene decode(FriendlyByteBuf b) {
            return new Scene(b.readUUID(), b.readUUID(), b.readVarInt(), b.readBoolean());
        }

        static void handle(Scene p, Supplier<NetworkEvent.Context> s) {
            var c = s.get();
            c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.benji.oasiso.client.gravesoul.GraveSoulScene.receive(p)));
            c.setPacketHandled(true);
        }
    }

    public record Done(UUID token, boolean poseOnly) {
        static void encode(Done p, FriendlyByteBuf b) {
            b.writeUUID(p.token);
            b.writeBoolean(p.poseOnly);
        }

        static Done decode(FriendlyByteBuf b) {
            return new Done(b.readUUID(), b.readBoolean());
        }

        static void handle(Done p, Supplier<NetworkEvent.Context> s) {
            var c = s.get();
            c.enqueueWork(() -> {
                if (c.getSender() != null) {
                    if (p.poseOnly) GraveSoulScenes.stopPose(c.getSender(), p.token);
                    else GraveSoulScenes.finish(c.getSender(), p.token);
                }
            });
            c.setPacketHandled(true);
        }
    }

    private GraveSoulNetwork() {
    }
}
