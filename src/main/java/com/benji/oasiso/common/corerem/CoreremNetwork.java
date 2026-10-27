package com.benji.oasiso.common.corerem;

import com.benji.oasiso.Oasiso;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CoreremNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "corerem"), () -> "1", "1"::equals, "1"::equals);

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CHANNEL.registerMessage(0, State.class, State::encode, State::decode, State::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
            CHANNEL.registerMessage(1, Result.class, Result::encode, Result::decode, Result::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        });
    }

    public static void state(ServerPlayer player, State packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void result(ServerPlayer player, int emotion) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Result(emotion));
    }

    public record State(int use, int happy, int anger, int sad, int mask, int steps, int age) {
        static void encode(State p, FriendlyByteBuf b) {
            b.writeVarInt(p.use);
            b.writeVarInt(p.happy);
            b.writeVarInt(p.anger);
            b.writeVarInt(p.sad);
            b.writeVarInt(p.mask);
            b.writeVarInt(p.steps);
            b.writeVarInt(p.age);
        }

        static State decode(FriendlyByteBuf b) {
            return new State(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt());
        }

        static void handle(State p, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.benji.oasiso.client.gui.CoreremHud.state(p)));
            context.setPacketHandled(true);
        }
    }

    public record Result(int emotion) {
        static void encode(Result p, FriendlyByteBuf b) {
            b.writeVarInt(p.emotion);
        }

        static Result decode(FriendlyByteBuf b) {
            return new Result(b.readVarInt());
        }

        static void handle(Result p, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.benji.oasiso.client.gui.CoreremHud.result(p.emotion)));
            context.setPacketHandled(true);
        }
    }

    private CoreremNetwork() {
    }
}
