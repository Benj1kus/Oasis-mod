package com.benji.oasiso.common.cowrit;

import com.benji.oasiso.Oasiso;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FatNetwork {
    static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "fat_coatings"), () -> "1", "1"::equals, "1"::equals);

    record Mark(ResourceLocation dimension, BlockPos pos, boolean coated) {
    }

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> CHANNEL.messageBuilder(Mark.class, 0, NetworkDirection.PLAY_TO_CLIENT).encoder((mark, buf) -> {
            buf.writeResourceLocation(mark.dimension());
            buf.writeBlockPos(mark.pos());
            buf.writeBoolean(mark.coated());
        }).decoder(buf -> new Mark(buf.readResourceLocation(), buf.readBlockPos(), buf.readBoolean())).consumerMainThread((mark, ctx) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.accept(mark))).add());
    }

    private static final class Client {
        static void accept(Mark mark) {
            var level = Minecraft.getInstance().level;
            if (level != null) FatCoatings.receive(level, mark);
        }
    }
}
