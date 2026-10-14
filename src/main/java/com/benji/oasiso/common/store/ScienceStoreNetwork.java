package com.benji.oasiso.common.store;

import com.benji.oasiso.Oasiso;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ScienceStoreNetwork {
    public static final int BEGIN = 0, CANCEL = 1, BUY = 2, CLOSE = 3;
    public static final int OK = 0, NOT_ENOUGH = 1, NO_SPACE = 2, UNAVAILABLE = 3, WAIT = 4, INVALID = 5;
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "science_store"), () -> "1", "1"::equals, "1"::equals);

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CHANNEL.registerMessage(0, Open.class, Open::encode, Open::decode, Open::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
            CHANNEL.registerMessage(1, Action.class, Action::encode, Action::decode, Action::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
            CHANNEL.registerMessage(2, Reply.class, Reply::encode, Reply::decode, Reply::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        });
    }

    public static void open(ServerPlayer p, UUID session, InteractionHand hand, int balance) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new Open(session, hand, balance));
    }

    public static void reply(ServerPlayer p, UUID session, int sequence, int result, int balance) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new Reply(session, sequence, result, balance));
    }

    public static void send(UUID session, int sequence, int action, int offer) {
        CHANNEL.sendToServer(new Action(session, sequence, action, offer));
    }

    public record Open(UUID session, InteractionHand hand, int balance) {
        static void encode(Open p, FriendlyByteBuf b) {
            b.writeUUID(p.session);
            b.writeEnum(p.hand);
            b.writeVarInt(p.balance);
        }

        static Open decode(FriendlyByteBuf b) {
            return new Open(b.readUUID(), b.readEnum(InteractionHand.class), b.readVarInt());
        }

        static void handle(Open p, Supplier<NetworkEvent.Context> supplier) {
            var ctx = supplier.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.benji.oasiso.client.store.ScienceStoreClient.open(p)));
            ctx.setPacketHandled(true);
        }
    }

    public record Action(UUID session, int sequence, int action, int offer) {
        static void encode(Action p, FriendlyByteBuf b) {
            b.writeUUID(p.session);
            b.writeVarInt(p.sequence);
            b.writeByte(p.action);
            b.writeByte(p.offer);
        }

        static Action decode(FriendlyByteBuf b) {
            return new Action(b.readUUID(), b.readVarInt(), b.readUnsignedByte(), b.readUnsignedByte());
        }

        static void handle(Action p, Supplier<NetworkEvent.Context> supplier) {
            var ctx = supplier.get();
            ctx.enqueueWork(() -> {
                ServerPlayer sender = ctx.getSender();
                if (sender != null) ScienceStoreServer.action(sender, p);
            });
            ctx.setPacketHandled(true);
        }
    }

    public record Reply(UUID session, int sequence, int result, int balance) {
        static void encode(Reply p, FriendlyByteBuf b) {
            b.writeUUID(p.session);
            b.writeVarInt(p.sequence);
            b.writeByte(p.result);
            b.writeVarInt(p.balance);
        }

        static Reply decode(FriendlyByteBuf b) {
            return new Reply(b.readUUID(), b.readVarInt(), b.readUnsignedByte(), b.readVarInt());
        }

        static void handle(Reply p, Supplier<NetworkEvent.Context> supplier) {
            var ctx = supplier.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.benji.oasiso.client.store.ScienceStoreClient.reply(p)));
            ctx.setPacketHandled(true);
        }
    }

    private ScienceStoreNetwork() {
    }
}
