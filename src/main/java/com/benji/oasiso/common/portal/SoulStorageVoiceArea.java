package com.benji.oasiso.common.portal;

import net.minecraft.server.level.ServerPlayer;

public final class SoulStorageVoiceArea {
    public static boolean contains(ServerPlayer p) {
        for (var v : SoulStorageData.get(p.serverLevel()).visits.values())
            if (!v.closing && v.bounds().contains(p.position())) return true;
        return false;
    }
}
