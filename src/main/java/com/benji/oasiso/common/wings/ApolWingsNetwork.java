package com.benji.oasiso.common.wings;

import com.benji.oasiso.Oasiso;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ApolWingsNetwork {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "apol_wings"), () -> "1", "1"::equals, "1"::equals);

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CHANNEL.registerMessage(0, Input.class, Input::encode, Input::decode, Input::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
            CHANNEL.registerMessage(1, Fx.class, Fx::encode, Fx::decode, Fx::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        });
    }
    public record Input(int action) {
        static void encode(Input p, FriendlyByteBuf b) {
            b.writeByte(p.action);
        }

        static Input decode(FriendlyByteBuf b) {
            return new Input(b.readUnsignedByte());
        }

        static void handle(Input p, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> {
                var player = context.getSender();
                if (player != null) ApolWingsMechanics.input(player, p.action);
            });
            context.setPacketHandled(true);
        }
    }

    public record Fx(int kind, int id, UUID uuid, long time, Vec3 pos, double velocity) {
        static void encode(Fx p, FriendlyByteBuf b) {
            b.writeByte(p.kind);
            b.writeVarInt(p.id);
            b.writeUUID(p.uuid);
            b.writeLong(p.time);
            b.writeDouble(p.pos.x);
            b.writeDouble(p.pos.y);
            b.writeDouble(p.pos.z);
            b.writeDouble(p.velocity);
        }

        static Fx decode(FriendlyByteBuf b) {
            return new Fx(b.readUnsignedByte(), b.readVarInt(), b.readUUID(), b.readLong(), new Vec3(b.readDouble(), b.readDouble(), b.readDouble()), b.readDouble());
        }

        static void handle(Fx p, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.benji.oasiso.client.wings.ApolWingsClient.receive(p)));
            context.setPacketHandled(true);
        }
    }

    private ApolWingsNetwork() {
    }
}
