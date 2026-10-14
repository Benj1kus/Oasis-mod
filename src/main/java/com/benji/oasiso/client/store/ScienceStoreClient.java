package com.benji.oasiso.client.store;

import com.benji.oasiso.common.store.ScienceStoreNetwork;
import net.minecraft.client.Minecraft;

public final class ScienceStoreClient {
    public static void open(ScienceStoreNetwork.Open packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null && mc.player.isAlive())
            mc.setScreen(new ScienceStoreScreen(packet.session(), packet.hand(), packet.balance()));
    }

    public static void reply(ScienceStoreNetwork.Reply packet) {
        if (Minecraft.getInstance().screen instanceof ScienceStoreScreen screen) screen.reply(packet);
    }

    private ScienceStoreClient() {
    }
}
