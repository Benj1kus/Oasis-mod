package com.benji.oasiso.common.entropy;

import com.benji.oasiso.Oasiso;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class EntropyInfectionNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "entropy_infection"), () -> "1", "1"::equals, "1"::equals);

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CHANNEL.registerMessage(0, Shake.class, Shake::encode, Shake::decode, Shake::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        });
    }

    public static void tracking(Entity mob, int ticks) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> mob), new Shake(mob.getUUID(), ticks));
    }

    public static void player(ServerPlayer player, UUID mob, int ticks) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Shake(mob, ticks));
    }

    public record Shake(UUID mob, int ticks) {
        static void encode(Shake packet, FriendlyByteBuf buffer) {
            buffer.writeUUID(packet.mob);
            buffer.writeVarInt(packet.ticks);
        }

        static Shake decode(FriendlyByteBuf buffer) {
            return new Shake(buffer.readUUID(), buffer.readVarInt());
        }

        static void handle(Shake packet, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.benji.oasiso.client.entropy.EntropyInfectionClient.receive(packet.mob, packet.ticks)));
            context.setPacketHandled(true);
        }
    }

    private EntropyInfectionNetwork() {
    }
}
