package com.benji.oasiso.network;

import com.benji.oasiso.Oasiso;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder.named(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "messages")).networkProtocolVersion(() -> "1.0").clientAcceptedVersions(s -> true).serverAcceptedVersions(s -> true).simpleChannel();

        INSTANCE = net;
        net.messageBuilder(EntropyAmmoSlotClickPacket.class, id(), NetworkDirection.PLAY_TO_SERVER).decoder(EntropyAmmoSlotClickPacket::new).encoder(EntropyAmmoSlotClickPacket::toBytes).consumerMainThread(EntropyAmmoSlotClickPacket::handle).add();
        net.messageBuilder(EntropyAmmoInsertPacket.class, id(), NetworkDirection.PLAY_TO_SERVER).decoder(EntropyAmmoInsertPacket::new).encoder(EntropyAmmoInsertPacket::toBytes).consumerMainThread(EntropyAmmoInsertPacket::handle).add();
        net.messageBuilder(EntropyAmmoExtractPacket.class, id(), NetworkDirection.PLAY_TO_SERVER).decoder(EntropyAmmoExtractPacket::new).encoder(EntropyAmmoExtractPacket::toBytes).consumerMainThread(EntropyAmmoExtractPacket::handle).add();
        net.messageBuilder(EntropyTurretTogglePacket.class, id(), NetworkDirection.PLAY_TO_SERVER).decoder(EntropyTurretTogglePacket::new).encoder(EntropyTurretTogglePacket::toBytes).consumerMainThread(EntropyTurretTogglePacket::handle).add();
        net.messageBuilder(SuperGoldShockwavePacket.class, id(), NetworkDirection.PLAY_TO_SERVER).decoder(SuperGoldShockwavePacket::new).encoder(SuperGoldShockwavePacket::toBytes).consumerMainThread(SuperGoldShockwavePacket::handle).add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
}