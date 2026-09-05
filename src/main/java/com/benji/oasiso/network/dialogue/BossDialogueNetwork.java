package com.benji.oasiso.network.dialogue;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.AzumaalEntity;
import com.benji.oasiso.common.entity.PaladinEntity;
import com.benji.dialoguestudio.dialogue.DialogueApi;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public final class BossDialogueNetwork {

    private static final String PROTOCOL = "1";

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "boss_dialogue"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private BossDialogueNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, DialogueStartS2CPacket.class, DialogueStartS2CPacket::encode, DialogueStartS2CPacket::decode, DialogueStartS2CPacket::handle);

        CHANNEL.registerMessage(1, DialogueActionC2SPacket.class, DialogueActionC2SPacket::encode, DialogueActionC2SPacket::decode, DialogueActionC2SPacket::handle);
    }

    public static void startDialogue(ServerPlayer player, UUID bossId, String dialogueId) {
        Entity source = player.serverLevel().getEntity(bossId);

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, dialogueId);

        boolean started = DialogueApi.start(player, source, id, () -> {
            if (source instanceof AzumaalEntity azumaal) {
                azumaal.finishIntroDialogue(player);
            }

            if (source instanceof PaladinEntity paladin) {
                paladin.finishIntroDialogue(player);
            }
        });

        if (!started) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DialogueStartS2CPacket(bossId, dialogueId));
        }
    }

    public static void panelFinished(UUID bossId, String dialogueId) {
        CHANNEL.sendToServer(new DialogueActionC2SPacket(bossId, dialogueId, DialogueActionC2SPacket.Action.PANEL_FINISHED));
    }

    public static void dialogueFinished(UUID bossId, String dialogueId) {
        CHANNEL.sendToServer(new DialogueActionC2SPacket(bossId, dialogueId, DialogueActionC2SPacket.Action.DIALOGUE_FINISHED));
    }
}