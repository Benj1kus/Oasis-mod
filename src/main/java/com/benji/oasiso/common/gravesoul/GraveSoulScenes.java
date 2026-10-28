package com.benji.oasiso.common.gravesoul;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.GraveSoulEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID)
public final class GraveSoulScenes {
    public static final int GRACE_TICKS = 400, TIMEOUT_TICKS = 600;
    private static final String READY = "OasisoGraveSoulReady";

    private record Session(UUID token, ServerPlayer player, GraveSoulEntity soul, Vec3 position, long deadline) {
    }

    private static final Map<UUID, Session> ACTIVE = new HashMap<>();

    private static long clock(ServerPlayer p) {
        return p.serverLevel().getServer().overworld().getGameTime();
    }

    public static boolean eligible(Player p) {
        return p instanceof ServerPlayer s && p.isAlive() && !p.isCreative() && !p.isSpectator() && !p.isPassenger() && !p.isSleeping() && !ACTIVE.containsKey(p.getUUID()) && clock(s) >= p.getPersistentData().getLong(READY);
    }

    public static void begin(GraveSoulEntity soul, ServerPlayer player) {
        if (!eligible(player) || soul.isGrabbing() || !soul.hasLineOfSight(player)) return;
        UUID token = UUID.randomUUID();
        soul.getNavigation().stop();
        soul.setDeltaMovement(Vec3.ZERO);
        float yaw = (float) Math.toDegrees(Math.atan2(player.getZ() - soul.getZ(), player.getX() - soul.getX())) - 90;
        soul.setYRot(yaw);
        soul.yBodyRot = yaw;
        soul.yHeadRot = yaw;
        soul.setGrabbing(true);
        ACTIVE.put(player.getUUID(), new Session(token, player, soul, player.position(), clock(player) + TIMEOUT_TICKS));
        GraveSoulNetwork.send(player, new GraveSoulNetwork.Scene(token, soul.getUUID(), soul.getRandom().nextInt(10), false));
    }

    public static void stopPose(ServerPlayer player, UUID token) {
        Session session = ACTIVE.get(player.getUUID());
        if (session != null && session.token.equals(token)) session.soul.stopAttackPose();
    }

    public static void finish(ServerPlayer player, UUID token) {
        Session s = ACTIVE.get(player.getUUID());
        if (s == null || !s.token.equals(token)) return;
        ACTIVE.remove(player.getUUID());
        s.soul.setGrabbing(false);
        s.soul.setTarget(null);
        player.getPersistentData().putLong(READY, clock(player) + GRACE_TICKS);
        if (!player.hasDisconnected())
            GraveSoulNetwork.send(player, new GraveSoulNetwork.Scene(token, s.soul.getUUID(), 0, true));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        for (Session s : new ArrayList<>(ACTIVE.values())) {
            ServerPlayer p = s.player;
            if (p.hasDisconnected() || !p.isAlive() || p.isSpectator() || p.level() != s.soul.level() || !s.soul.isAlive() || s.soul.isRemoved() || clock(p) >= s.deadline) {
                finish(p, s.token);
                continue;
            }
            s.soul.getNavigation().stop();
            s.soul.setDeltaMovement(Vec3.ZERO);
            p.setDeltaMovement(Vec3.ZERO);
            p.fallDistance = 0;
            if (p.position().distanceToSqr(s.position) > .0025)
                p.connection.teleport(s.position.x, s.position.y, s.position.z, p.getYRot(), p.getXRot());
        }
    }

    @SubscribeEvent
    public static void damage(LivingAttackEvent e) {
        if (ACTIVE.containsKey(e.getEntity().getUUID()) || ACTIVE.values().stream().anyMatch(s -> s.soul == e.getEntity()))
            e.setCanceled(true);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        Session s = ACTIVE.get(e.getEntity().getUUID());
        if (s != null) finish(s.player, s.token);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone e) {
        e.getEntity().getPersistentData().putLong(READY, e.getOriginal().getPersistentData().getLong(READY));
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent e) {
        for (Session s : ACTIVE.values()) s.soul.setGrabbing(false);
        ACTIVE.clear();
    }

    private GraveSoulScenes() {
    }
}
