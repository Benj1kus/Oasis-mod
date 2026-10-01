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

import java.util.UUID;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ApolWingsTransitNetwork {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "wings_transit"), () -> "1", "1"::equals, "1"::equals);

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent e) {
        e.enqueueWork(() -> CHANNEL.messageBuilder(Fx.class, 0, NetworkDirection.PLAY_TO_CLIENT).encoder(Fx::encode).decoder(Fx::decode).consumerMainThread(Fx::handle).add());
    }

    public record Fx(int kind, UUID player, int ticks, Vec3 pos, Vec3 normal, Vec3 motion) {
        static void vec(FriendlyByteBuf b, Vec3 v) {
            b.writeDouble(v.x);
            b.writeDouble(v.y);
            b.writeDouble(v.z);
        }

        static Vec3 vec(FriendlyByteBuf b) {
            return new Vec3(b.readDouble(), b.readDouble(), b.readDouble());
        }

        static void encode(Fx p, FriendlyByteBuf b) {
            b.writeByte(p.kind);
            b.writeUUID(p.player);
            b.writeVarInt(p.ticks);
            vec(b, p.pos);
            vec(b, p.normal);
            vec(b, p.motion);
        }

        static Fx decode(FriendlyByteBuf b) {
            return new Fx(b.readUnsignedByte(), b.readUUID(), b.readVarInt(), vec(b), vec(b), vec(b));
        }

        static void handle(Fx p, Supplier<NetworkEvent.Context> s) {
            var c = s.get();
            c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.benji.oasiso.client.wings.ApolWingsTransitClient.receive(p)));
            c.setPacketHandled(true);
        }
    }

    private ApolWingsTransitNetwork() {
    }
}