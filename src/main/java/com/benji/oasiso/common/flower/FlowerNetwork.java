package com.benji.oasiso.common.flower;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FlowerNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "talking_flowers"), () -> "1", "1"::equals, "1"::equals);

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CHANNEL.messageBuilder(Config.class, 0, NetworkDirection.PLAY_TO_CLIENT).encoder(Config::encode).decoder(Config::decode).consumerNetworkThread(Config::handle).add();
            CHANNEL.messageBuilder(Speech.class, 1, NetworkDirection.PLAY_TO_CLIENT).encoder(Speech::encode).decoder(Speech::decode).consumerNetworkThread(Speech::handle).add();
        });
    }

    public static void sync(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Config(FlowerData.syncJson()));
    }

    public static void speak(ServerLevel level, BlockPos pos, FlowerDefinition def, String line, long start) {
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 64, level.dimension())), new Speech(level.dimension().location(), pos, def.id(), line, start));
    }

    public record Config(String json) {
        static void encode(Config p, FriendlyByteBuf b) {
            b.writeUtf(p.json, FlowerData.MAX_JSON);
        }

        static Config decode(FriendlyByteBuf b) {
            return new Config(b.readUtf(FlowerData.MAX_JSON));
        }

        static void handle(Config p, Supplier<NetworkEvent.Context> supplier) {
            var ctx = supplier.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.benji.oasiso.client.flower.FlowerClient.configure(p.json)));
            ctx.setPacketHandled(true);
        }
    }

    public record Speech(ResourceLocation dimension, BlockPos pos, ResourceLocation flower, String line, long start) {
        static void encode(Speech p, FriendlyByteBuf b) {
            b.writeResourceLocation(p.dimension);
            b.writeBlockPos(p.pos);
            b.writeResourceLocation(p.flower);
            b.writeUtf(p.line, 128);
            b.writeLong(p.start);
        }

        static Speech decode(FriendlyByteBuf b) {
            return new Speech(b.readResourceLocation(), b.readBlockPos(), b.readResourceLocation(), b.readUtf(128), b.readLong());
        }

        static void handle(Speech p, Supplier<NetworkEvent.Context> supplier) {
            var ctx = supplier.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.benji.oasiso.client.flower.FlowerClient.speak(p)));
            ctx.setPacketHandled(true);
        }
    }

    private FlowerNetwork() {
    }
}
