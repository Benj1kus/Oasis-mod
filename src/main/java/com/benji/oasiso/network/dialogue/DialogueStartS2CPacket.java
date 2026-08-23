package com.benji.oasiso.network.dialogue;

import com.benji.oasiso.client.gui.BossDialogueClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record DialogueStartS2CPacket(UUID bossId, String dialogueId) {

    public static void encode(DialogueStartS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.bossId);
        buffer.writeUtf(packet.dialogueId, 32);
    }

    public static DialogueStartS2CPacket decode(FriendlyByteBuf buffer) {
        return new DialogueStartS2CPacket(buffer.readUUID(), buffer.readUtf(32));
    }

    public static void handle(DialogueStartS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BossDialogueClient.start(packet.bossId, packet.dialogueId)));

        context.setPacketHandled(true);
    }
}