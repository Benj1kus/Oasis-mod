package com.benji.oasiso.common.soul;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class VibritSoulNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "vibrit_soul"), () -> "1", "1"::equals, "1"::equals);

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> CHANNEL.messageBuilder(Flight.class, 0, NetworkDirection.PLAY_TO_CLIENT).encoder(Flight::encode).decoder(Flight::decode).consumerNetworkThread(Flight::handle).add());
    }

    public static void send(ServerLevel level, BlockPos target, Vec3 from, Vec3 to, int seed) {
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(target.getX() + .5, target.getY() + .5, target.getZ() + .5, 80, level.dimension())), new Flight(target, from, to, seed, level.dimension().location(), level.getGameTime()));
    }

    public record Flight(BlockPos target, Vec3 from, Vec3 to, int seed, ResourceLocation dimension, long startTick) {
        static void encode(Flight p, FriendlyByteBuf b) {
            b.writeBlockPos(p.target);
            writeVec(b, p.from);
            writeVec(b, p.to);
            b.writeInt(p.seed);
            b.writeResourceLocation(p.dimension);
            b.writeLong(p.startTick);
        }

        static Flight decode(FriendlyByteBuf b) {
            return new Flight(b.readBlockPos(), readVec(b), readVec(b), b.readInt(), b.readResourceLocation(), b.readLong());
        }

        private static void writeVec(FriendlyByteBuf b, Vec3 v) {
            b.writeDouble(v.x);
            b.writeDouble(v.y);
            b.writeDouble(v.z);
        }

        private static Vec3 readVec(FriendlyByteBuf b) {
            return new Vec3(b.readDouble(), b.readDouble(), b.readDouble());
        }

        static void handle(Flight p, Supplier<NetworkEvent.Context> supplier) {
            var ctx = supplier.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.benji.oasiso.client.soul.VibritSoulEffects.receive(p)));
            ctx.setPacketHandled(true);
        }
    }

    private VibritSoulNetwork() {
    }
}
