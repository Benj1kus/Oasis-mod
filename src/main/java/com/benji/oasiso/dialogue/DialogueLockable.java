package com.benji.oasiso.dialogue;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public interface DialogueLockable {

    void setDialogueLocked(boolean locked, ServerPlayer viewer, ResourceLocation dialogueId);
}