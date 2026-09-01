package com.benji.oasiso.network;

import com.benji.oasiso.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class MouthUseSoundS2CPacket {

    public MouthUseSoundS2CPacket() {
    }

    public static void encode(MouthUseSoundS2CPacket packet, FriendlyByteBuf buffer) {
    }

    public static MouthUseSoundS2CPacket decode(FriendlyByteBuf buffer) {
        return new MouthUseSoundS2CPacket();
    }

    public static void handle(MouthUseSoundS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> MouthUseSoundS2CPacket::playClient));

        context.setPacketHandled(true);
    }

    private static void playClient() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.MOUTH_USE.get(), 1.0F, 1.0F));
    }
}