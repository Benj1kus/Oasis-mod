package com.benji.oasiso.common.combat;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.CrusaderScienceEntity;
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

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ScienceCombatNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "science_combat"), () -> "1", "1"::equals, "1"::equals);

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CHANNEL.registerMessage(0, Fx.class, Fx::encode, Fx::decode, Fx::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        });
    }

    public static void hit(CrusaderScienceEntity scientist, Vec3 normal) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> scientist), new Fx(0, scientist.getId(), scientist.getUUID(), normal));
    }

    public static void blast(ServerLevel level, Vec3 pos) {
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(pos.x, pos.y, pos.z, 64, level.dimension())), new Fx(1, 0, new UUID(0, 0), pos));
    }

    public record Fx(int kind, int id, UUID uuid, Vec3 vector) {
        static void encode(Fx p, FriendlyByteBuf b) {
            b.writeByte(p.kind);
            b.writeVarInt(p.id);
            b.writeUUID(p.uuid);
            b.writeDouble(p.vector.x);
            b.writeDouble(p.vector.y);
            b.writeDouble(p.vector.z);
        }

        static Fx decode(FriendlyByteBuf b) {
            return new Fx(b.readUnsignedByte(), b.readVarInt(), b.readUUID(), new Vec3(b.readDouble(), b.readDouble(), b.readDouble()));
        }

        static void handle(Fx p, Supplier<NetworkEvent.Context> supplier) {
            var ctx = supplier.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.benji.oasiso.client.renderer.ScienceCombatEffects.receive(p)));
            ctx.setPacketHandled(true);
        }
    }

    private ScienceCombatNetwork() {
    }
}
