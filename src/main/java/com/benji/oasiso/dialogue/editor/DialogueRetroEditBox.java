package com.benji.oasiso.dialogue.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public final class DialogueRetroEditBox extends EditBox {

    public DialogueRetroEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
        setTextColor(DialogueRetroTheme.TEXT_LIGHT);
        setTextColorUneditable(DialogueRetroTheme.TEXT_MUTED);
    }
}
