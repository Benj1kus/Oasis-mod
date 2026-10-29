package com.benji.oasiso.common.fault;

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

import java.util.Optional;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SoulBottleNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "soul_bottle_fx"), () -> "1", "1"::equals, "1"::equals);

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CHANNEL.registerMessage(0, Drink.class, Drink::encode, Drink::decode, Drink::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        });
    }

    public static void send(ServerPlayer player, int remaining) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Drink(remaining));
    }

    public record Drink(int remaining) {
        static void encode(Drink message, FriendlyByteBuf buffer) {
            buffer.writeInt(message.remaining);
        }

        static Drink decode(FriendlyByteBuf buffer) {
            return new Drink(Math.max(-1, Math.min(8, buffer.readInt())));
        }

        static void handle(Drink message, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.benji.oasiso.client.shrine.SoulBottleEffects.show(message.remaining)));
            context.setPacketHandled(true);
        }
    }
}
