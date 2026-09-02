package com.benji.oasiso.client.dimension;

public final class PocketModeClient {

    private static boolean active;

    private PocketModeClient() {
    }

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean value) {
        active = value;
    }
}