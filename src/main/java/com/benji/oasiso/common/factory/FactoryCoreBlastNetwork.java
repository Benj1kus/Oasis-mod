package com.benji.oasiso.common.factory;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
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

import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FactoryCoreBlastNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "factory_core_blast"), () -> "1", "1"::equals, "1"::equals);

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CHANNEL.messageBuilder(Blast.class, 0, NetworkDirection.PLAY_TO_CLIENT).encoder(Blast::encode).decoder(Blast::decode).consumerMainThread(Blast::handle).add();
        });
    }

    public static void send(ServerPlayer player, BlockPos pos, long tick) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Blast(player.level().dimension().location(), pos, tick));
    }

    public record Blast(ResourceLocation dimension, BlockPos pos, long tick) {
        static void encode(Blast p, FriendlyByteBuf b) {
            b.writeResourceLocation(p.dimension);
            b.writeBlockPos(p.pos);
            b.writeLong(p.tick);
        }

        static Blast decode(FriendlyByteBuf b) {
            return new Blast(b.readResourceLocation(), b.readBlockPos(), b.readLong());
        }

        static void handle(Blast p, Supplier<NetworkEvent.Context> supplier) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.benji.oasiso.client.factory.FactoryCoreBlastClient.receive(p.dimension, p.pos, p.tick));
            supplier.get().setPacketHandled(true);
        }
    }

    private FactoryCoreBlastNetwork() {
    }
}
