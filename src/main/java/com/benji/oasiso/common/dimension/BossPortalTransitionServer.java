package com.benji.oasiso.common.dimension;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.BossPortalEntity;
import com.benji.oasiso.network.BossPortalTransitionNetwork;
import com.benji.oasiso.network.BossPortalTransitionS2CPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BossPortalTransitionServer {

    private static final int TELEPORT_DELAY = 30;

    private static final int POST_TELEPORT_HOLD = 20;

    private static final Map<UUID, Transition> ACTIVE = new HashMap<>();

    private BossPortalTransitionServer() {
    }


    public static boolean beginEnter(ServerPlayer player, BossPortalEntity portal) {
        if (player.level().dimension().equals(Oasiso.CHAOS_DIMENSION)) {
            return false;
        }

        return begin(player, portal, TravelType.ENTER);
    }


    public static boolean beginReturn(ServerPlayer player, BossPortalEntity portal) {
        if (!player.level().dimension().equals(Oasiso.CHAOS_DIMENSION)) {
            return false;
        }

        if (!BossArenaEncounter.isArenaSession(player)) {
            return false;
        }

        UUID playerSession = BossArenaEncounter.getArenaSessionId(player);

        UUID portalSession = portal.getArenaSessionId();

        if (portalSession != null && !portalSession.equals(playerSession)) {
            return false;
        }

        return begin(player, portal, TravelType.RETURN);
    }


    private static boolean begin(ServerPlayer player, BossPortalEntity portal, TravelType type) {
        if (ACTIVE.containsKey(player.getUUID())) {
            return true;
        }

        UUID sessionId;

        if (type == TravelType.ENTER) {
            sessionId = portal.getOrCreateArenaSessionId();
        } else {
            sessionId = portal.getArenaSessionId();

            if (sessionId == null) {
                sessionId = BossArenaEncounter.getArenaSessionId(player);
            }
        }

        if (sessionId == null) {
            return false;
        }

        ACTIVE.put(player.getUUID(), new Transition(type, portal.getUUID(), sessionId));

        BossPortalTransitionNetwork.send(player, BossPortalTransitionS2CPacket.Action.CLOSE);

        return true;
    }


    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        Transition transition = ACTIVE.get(player.getUUID());

        if (transition == null) {
            return;
        }

        if (!player.isAlive()) {
            cancel(player);
            return;
        }

        freezePlayer(player);

        transition.ticks++;

        if (!transition.teleported && transition.ticks >= TELEPORT_DELAY) {

            boolean success = performTeleport(player, transition);

            if (!success) {
                cancel(player);
                return;
            }

            transition.teleported = true;
            transition.ticks = 0;

            return;
        }

        if (transition.teleported && transition.ticks >= POST_TELEPORT_HOLD) {

            BossPortalTransitionNetwork.send(player, BossPortalTransitionS2CPacket.Action.OPEN);

            ACTIVE.remove(player.getUUID());
        }
    }


    private static boolean performTeleport(ServerPlayer player, Transition transition) {
        Entity entity = player.serverLevel().getEntity(transition.portalId);

        if (!(entity instanceof BossPortalEntity portal)) {
            return false;
        }

        return switch (transition.type) {
            case ENTER -> BossArenaEncounter.enterArenaNow(player, portal, transition.sessionId);

            case RETURN -> BossArenaEncounter.returnToEntranceNow(player, portal);
        };
    }


    private static void freezePlayer(ServerPlayer player) {
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.hurtMarked = true;
    }


    private static void cancel(ServerPlayer player) {
        ACTIVE.remove(player.getUUID());

        BossPortalTransitionNetwork.send(player, BossPortalTransitionS2CPacket.Action.CANCEL);
    }


    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE.remove(event.getEntity().getUUID());
    }


    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {

            ACTIVE.remove(player.getUUID());
        }
    }


    private enum TravelType {
        ENTER, RETURN
    }


    private static final class Transition {

        private final TravelType type;
        private final UUID portalId;
        private final UUID sessionId;

        private int ticks;
        private boolean teleported;

        private Transition(TravelType type, UUID portalId, UUID sessionId) {
            this.type = type;
            this.portalId = portalId;
            this.sessionId = sessionId;
        }
    }
}