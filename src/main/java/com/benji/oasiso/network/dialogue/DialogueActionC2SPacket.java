package com.benji.oasiso.network.dialogue;

import com.benji.oasiso.common.entity.AzumaalEntity;
import com.benji.oasiso.common.entity.PaladinEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record DialogueActionC2SPacket(UUID bossId, String dialogueId, Action action) {

    public enum Action {
        PANEL_FINISHED, DIALOGUE_FINISHED
    }

    public static void encode(DialogueActionC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.bossId);
        buffer.writeUtf(packet.dialogueId, 32);
        buffer.writeEnum(packet.action);
    }

    public static DialogueActionC2SPacket decode(FriendlyByteBuf buffer) {
        return new DialogueActionC2SPacket(buffer.readUUID(), buffer.readUtf(32), buffer.readEnum(Action.class));
    }

    public static void handle(DialogueActionC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        ServerPlayer player = context.getSender();

        if (player != null) {
            context.enqueueWork(() -> handleServer(player, packet));
        }

        context.setPacketHandled(true);
    }

    private static void handleServer(ServerPlayer player, DialogueActionC2SPacket packet) {
        Entity entity = player.serverLevel().getEntity(packet.bossId);

        if (entity instanceof AzumaalEntity azumaal && packet.dialogueId.equals("azumaal")) {

            if (packet.action == Action.PANEL_FINISHED) {
                azumaal.onIntroPanelFinished(player);
            } else {
                azumaal.finishIntroDialogue(player);
            }
            return;
        }

        if (entity instanceof PaladinEntity paladin && packet.dialogueId.equals("paladin")) {

            if (packet.action == Action.PANEL_FINISHED) {
                paladin.onIntroPanelFinished(player);
            } else {
                paladin.finishIntroDialogue(player);
            }
        }
    }
}