package com.benji.oasiso.common.item;

import com.benji.oasiso.Oasiso;
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
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ApolSpearVolleyNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "apol_spear_volley"), () -> "1", "1"::equals, "1"::equals);

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CHANNEL.messageBuilder(Impact.class, 0, NetworkDirection.PLAY_TO_CLIENT).encoder(Impact::encode).decoder(Impact::decode).consumerNetworkThread(Impact::handle).add();
        });
    }

    public static void impact(ServerLevel level, Vec3 pos, Vec3 normal, int seed) {
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(pos.x, pos.y, pos.z, 80, level.dimension())), new Impact(pos, normal, seed));
    }

    public record Impact(Vec3 pos, Vec3 normal, int seed) {
        private static void vec(FriendlyByteBuf b, Vec3 v) {
            b.writeDouble(v.x);
            b.writeDouble(v.y);
            b.writeDouble(v.z);
        }

        private static Vec3 vec(FriendlyByteBuf b) {
            return new Vec3(b.readDouble(), b.readDouble(), b.readDouble());
        }

        static void encode(Impact p, FriendlyByteBuf b) {
            vec(b, p.pos);
            vec(b, p.normal);
            b.writeInt(p.seed);
        }

        static Impact decode(FriendlyByteBuf b) {
            return new Impact(vec(b), vec(b), b.readInt());
        }

        static void handle(Impact p, Supplier<NetworkEvent.Context> supplier) {
            var c = supplier.get();
            c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.benji.oasiso.client.renderer.ApolSpearImpactFx.add(p.pos, p.normal, p.seed)));
            c.setPacketHandled(true);
        }
    }

    private ApolSpearVolleyNetwork() {
    }
}
