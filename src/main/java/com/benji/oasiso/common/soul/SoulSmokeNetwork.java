package com.benji.oasiso.common.soul;

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

import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SoulSmokeNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "soul_smoke_splash"), () -> "1", "1"::equals, "1"::equals);

    private SoulSmokeNetwork() {
    }

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> CHANNEL.messageBuilder(Splash.class, 0, NetworkDirection.PLAY_TO_CLIENT).encoder(Splash::encode).decoder(Splash::decode).consumerNetworkThread(Splash::handle).add());
    }

    public static void send(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Splash(player.level().dimension().location()));
    }

    public record Splash(ResourceLocation dimension) {
        static void encode(Splash packet, FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(packet.dimension);
        }

        static Splash decode(FriendlyByteBuf buffer) {
            return new Splash(buffer.readResourceLocation());
        }

        static void handle(Splash packet, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.benji.oasiso.client.gui.SoulSmokeSplashOverlay.show(packet.dimension)));
            context.setPacketHandled(true);
        }
    }
}
