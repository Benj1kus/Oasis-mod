package com.benji.oasiso.network.dialogueengine;

import com.benji.oasiso.client.dialogue.DialogueZonePreviewRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record DialogueZonePreviewS2CPacket(List<Zone> zones) {

    private static final int MAX_ZONES = 96;

    public static void encode(DialogueZonePreviewS2CPacket packet, FriendlyByteBuf buffer) {
        int count = Math.min(packet.zones.size(), MAX_ZONES);

        buffer.writeVarInt(count);

        for (int i = 0; i < count; i++) {
            Zone zone = packet.zones.get(i);

            buffer.writeUtf(zone.key, 512);
            buffer.writeUtf(zone.shape, 32);

            buffer.writeDouble(zone.x);
            buffer.writeDouble(zone.y);
            buffer.writeDouble(zone.z);

            buffer.writeDouble(zone.radius);
            buffer.writeDouble(zone.height);

            buffer.writeDouble(zone.sizeX);
            buffer.writeDouble(zone.sizeY);
            buffer.writeDouble(zone.sizeZ);

            buffer.writeUtf(zone.style, 32);

            buffer.writeUtf(zone.texture != null ? zone.texture : "", 512);

            buffer.writeUtf(zone.color, 64);

            buffer.writeFloat(zone.alpha);

            buffer.writeDouble(zone.yOffset);
            buffer.writeDouble(zone.visualSize);
            buffer.writeDouble(zone.visualHeight);

            buffer.writeBoolean(zone.pulse);

            buffer.writeDouble(zone.previewDistance);
        }
    }


    public static DialogueZonePreviewS2CPacket decode(FriendlyByteBuf buffer) {
        int count = Math.min(buffer.readVarInt(), MAX_ZONES);

        List<Zone> zones = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            zones.add(new Zone(buffer.readUtf(512), buffer.readUtf(32),

                    buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),

                    buffer.readDouble(), buffer.readDouble(),

                    buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),

                    buffer.readUtf(32), buffer.readUtf(512), buffer.readUtf(64),

                    buffer.readFloat(),

                    buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),

                    buffer.readBoolean(),

                    buffer.readDouble()));
        }

        return new DialogueZonePreviewS2CPacket(zones);
    }


    public static void handle(DialogueZonePreviewS2CPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DialogueZonePreviewRenderer.setZones(packet.zones)));

        context.setPacketHandled(true);
    }


    public record Zone(String key, String shape,

                       double x, double y, double z,
                       double radius, double height,
                       double sizeX, double sizeY, double sizeZ,

                       String style, String texture, String color,

                       float alpha,
                       double yOffset, double visualSize, double visualHeight,
                       boolean pulse,

                       double previewDistance) {
    }
}