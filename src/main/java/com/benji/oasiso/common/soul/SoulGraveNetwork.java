package com.benji.oasiso.common.soul;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.SoulGiverEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.UUID;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SoulGraveNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "soul_grave_fx"), () -> "1", "1"::equals, "1"::equals);

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> CHANNEL.messageBuilder(Strike.class, 0, NetworkDirection.PLAY_TO_CLIENT).encoder(Strike::encode).decoder(Strike::decode).consumerNetworkThread(Strike::handle).add());
    }

    public static void send(SoulGiverEntity entity, BlockPos grave, int seed) {
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(entity.getX(), entity.getY(), entity.getZ(), 64, entity.level().dimension())), new Strike(entity.getId(), entity.getUUID(), grave, entity.position().add(0, 49.5 / 16.0, 0), seed, entity.level().dimension().location()));
    }

    public record Strike(int id, UUID uuid, BlockPos grave, Vec3 origin, int seed, ResourceLocation dimension) {
        static void encode(Strike p, FriendlyByteBuf b) {
            b.writeVarInt(p.id);
            b.writeUUID(p.uuid);
            b.writeBlockPos(p.grave);
            b.writeDouble(p.origin.x);
            b.writeDouble(p.origin.y);
            b.writeDouble(p.origin.z);
            b.writeInt(p.seed);
            b.writeResourceLocation(p.dimension);
        }

        static Strike decode(FriendlyByteBuf b) {
            return new Strike(b.readVarInt(), b.readUUID(), b.readBlockPos(), new Vec3(b.readDouble(), b.readDouble(), b.readDouble()), b.readInt(), b.readResourceLocation());
        }

        static void handle(Strike p, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.benji.oasiso.client.soul.SoulGraveEffects.receive(p)));
            context.setPacketHandled(true);
        }
    }

    private SoulGraveNetwork() {
    }
}
