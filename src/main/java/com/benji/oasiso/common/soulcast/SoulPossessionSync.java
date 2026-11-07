package com.benji.oasiso.common.soulcast;

import com.benji.oasiso.client.cloud.SoulCloudEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "oasiso", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SoulPossessionSync {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath("oasiso", "soul_possession"), () -> "1", "1"::equals, "1"::equals);

    public record State(ResourceLocation dimension, UUID target, int id, Vec3 origin, long launch, long arrival,
                        long end) {
        void write(FriendlyByteBuf b) {
            b.writeResourceLocation(dimension);
            b.writeUUID(target);
            b.writeVarInt(id);
            b.writeDouble(origin.x);
            b.writeDouble(origin.y);
            b.writeDouble(origin.z);
            b.writeLong(launch);
            b.writeLong(arrival);
            b.writeLong(end);
        }

        static State read(FriendlyByteBuf b) {
            return new State(b.readResourceLocation(), b.readUUID(), b.readVarInt(), new Vec3(b.readDouble(), b.readDouble(), b.readDouble()), b.readLong(), b.readLong(), b.readLong());
        }
    }

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CHANNEL.registerMessage(0, State.class, State::write, State::read, (state, supplier) -> {
                var ctx = supplier.get();
                ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SoulCloudEffects.receivePossession(state)));
                ctx.setPacketHandled(true);
            }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        });
    }

    static void trackers(Mob mob, State state) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> mob), state);
    }

    static void send(ServerPlayer player, State state) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), state);
    }
}
