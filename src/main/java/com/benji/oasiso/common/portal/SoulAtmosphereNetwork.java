package com.benji.oasiso.common.portal;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = "oasiso")
public final class SoulAtmosphereNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath("oasiso", "soul_atmosphere"), () -> "1", "1"::equals, "1"::equals);

    @Mod.EventBusSubscriber(modid = "oasiso", bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        @SubscribeEvent
        public static void setup(FMLCommonSetupEvent e) {
            e.enqueueWork(() -> {
                CHANNEL.registerMessage(0, Area.class, Area::encode, Area::decode, Area::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
            });
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || p.tickCount % 10 != 0) return;
        AABB area = null;
        for (var visit : SoulStorageData.get(p.serverLevel()).visits.values()) {
            if (!visit.closing && visit.bounds().inflate(1).contains(p.position())) {
                area = visit.bounds();
                break;
            }
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new Area(area));
    }

    public record Area(AABB box) {
        static void encode(Area a, FriendlyByteBuf b) {
            b.writeBoolean(a.box != null);
            if (a.box != null) {
                b.writeDouble(a.box.minX);
                b.writeDouble(a.box.minY);
                b.writeDouble(a.box.minZ);
                b.writeDouble(a.box.maxX);
                b.writeDouble(a.box.maxY);
                b.writeDouble(a.box.maxZ);
            }
        }

        static Area decode(FriendlyByteBuf b) {
            return new Area(b.readBoolean() ? new AABB(b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble()) : null);
        }

        static void handle(Area a, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.benji.oasiso.client.soulstorage.SoulAtmosphere.area(a.box)));
            ctx.get().setPacketHandled(true);
        }
    }
}
