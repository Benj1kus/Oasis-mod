package com.benji.oasiso.common.waypoint;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.MouthPointBlockEntity;
import com.benji.oasiso.network.BossPortalTransitionNetwork;
import com.benji.oasiso.network.BossPortalTransitionS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
public final class MouthPointTeleportServer {

    private static final int TELEPORT_DELAY = 30;
    private static final int POST_TELEPORT_HOLD = 20;

    private static final Map<UUID, Transition> ACTIVE = new HashMap<>();

    private MouthPointTeleportServer() {
    }

    public static boolean begin(ServerPlayer player, MouthPointBlockEntity source) {
        if (player == null || source == null || !source.isOwnedBy(player.getUUID()) || !source.hasPartner()) {
            return false;
        }

        if (ACTIVE.containsKey(player.getUUID())) {
            return true;
        }

        MinecraftServer server = player.getServer();

        if (server == null) {
            return false;
        }

        AzumalitWaypointManager.WaypointRef sourceRef = AzumalitWaypointManager.refOf(source);
        AzumalitWaypointManager.WaypointRef targetRef = source.getPartnerRef();
        MouthPointBlockEntity target = AzumalitWaypointManager.resolve(server, targetRef, player.getUUID());

        if (target == null || !sourceRef.equals(target.getPartnerRef())) {
            return false;
        }

        ACTIVE.put(player.getUUID(), new Transition(sourceRef, targetRef));
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
            if (!performTeleport(player, transition)) {
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
        MinecraftServer server = player.getServer();

        if (server == null) {
            return false;
        }

        UUID ownerId = player.getUUID();
        MouthPointBlockEntity source = AzumalitWaypointManager.resolve(server, transition.source, ownerId);
        MouthPointBlockEntity target = AzumalitWaypointManager.resolve(server, transition.target, ownerId);

        if (source == null || target == null || !(target.getLevel() instanceof ServerLevel targetLevel)) {
            return false;
        }

        double targetX = target.getBlockPos().getX() + 0.5D;
        double targetY = target.getBlockPos().getY();
        double targetZ = target.getBlockPos().getZ() + 0.5D;

        AzumalitWaypointManager.removePairForTeleport(source, target);

        player.stopRiding();
        player.teleportTo(targetLevel, targetX, targetY, targetZ, player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.hurtMarked = true;

        return true;
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

    private static final class Transition {
        private final AzumalitWaypointManager.WaypointRef source;
        private final AzumalitWaypointManager.WaypointRef target;

        private int ticks;
        private boolean teleported;

        private Transition(AzumalitWaypointManager.WaypointRef source, AzumalitWaypointManager.WaypointRef target) {
            this.source = source;
            this.target = target;
        }
    }
}
