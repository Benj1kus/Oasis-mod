package com.benji.oasiso.common.tooltip;

import com.benji.oasiso.Oasiso;
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

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TooltipDataNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "tooltip_data"), () -> "1", "1"::equals, "1"::equals);

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CHANNEL.registerMessage(0, Sync.class, Sync::encode, Sync::decode, Sync::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        });
    }

    public static void send(ServerPlayer player, Sync message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public record Sync(String json) {
        private static void encode(Sync message, FriendlyByteBuf buffer) {
            buffer.writeUtf(message.json(), TooltipData.MAX_JSON_CHARS);
        }

        private static Sync decode(FriendlyByteBuf buffer) {
            return new Sync(buffer.readUtf(TooltipData.MAX_JSON_CHARS));
        }

        private static void handle(Sync message, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.benji.oasiso.client.tooltip.TooltipVFXClient.receive(message.json())));
            context.setPacketHandled(true);
        }
    }

    private TooltipDataNetwork() {
    }
}
