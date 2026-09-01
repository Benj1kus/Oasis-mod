package com.benji.oasiso.network;

import com.benji.oasiso.Oasiso;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class BossPortalTransitionNetwork {

    private static final String PROTOCOL = "1";

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "boss_portal_transition"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private BossPortalTransitionNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, BossPortalTransitionS2CPacket.class, BossPortalTransitionS2CPacket::encode, BossPortalTransitionS2CPacket::decode, BossPortalTransitionS2CPacket::handle);
        CHANNEL.registerMessage(1, MouthUseSoundS2CPacket.class, MouthUseSoundS2CPacket::encode, MouthUseSoundS2CPacket::decode, MouthUseSoundS2CPacket::handle);
    }

    public static void sendMouthUseSound(
            ServerPlayer player
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(
                        () -> player
                ),
                new MouthUseSoundS2CPacket()
        );
    }

    public static void send(ServerPlayer player, BossPortalTransitionS2CPacket.Action action) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new BossPortalTransitionS2CPacket(action));
    }
}