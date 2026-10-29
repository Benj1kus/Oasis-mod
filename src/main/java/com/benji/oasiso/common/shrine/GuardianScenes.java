package com.benji.oasiso.common.shrine;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.SoulGuardianEntity;
import com.benji.oasiso.common.gravesoul.GraveSoulScenes;
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
public final class GuardianScenes {
    private record Session(UUID token, ServerPlayer player, SoulGuardianEntity guardian, Vec3 position, long start) {
    }

    private static final Map<UUID, Session> ACTIVE = new HashMap<>();

    public static boolean busy(Player player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    public static boolean eligible(ServerPlayer p) {
        return p.isAlive() && !p.isSpectator() && !p.isPassenger() && !p.isSleeping() && !busy(p) && !GraveSoulScenes.isInScene(p);
    }

    public static void begin(SoulGuardianEntity g, ServerPlayer p) {
        if (!eligible(p) || g.getShrinePhase() != 0 || !g.hasLineOfSight(p)) return;
        UUID token = UUID.randomUUID();
        g.face(p);
        g.setShrinePhase(1);
        ACTIVE.put(p.getUUID(), new Session(token, p, g, p.position(), p.serverLevel().getGameTime()));
        GuardianNetwork.send(p, new GuardianNetwork.Scene(token, g.getUUID(), false));
    }

    public static void finish(ServerPlayer p, UUID token, boolean completed) {
        Session s = ACTIVE.get(p.getUUID());
        if (s == null || !s.token.equals(token)) return;
        ACTIVE.remove(p.getUUID());
        boolean valid = completed && p.isAlive() && p.level() == s.guardian.level() && s.guardian.isAlive() && p.serverLevel().getGameTime() - s.start >= 140;
        s.guardian.endScene(valid);
        if (!p.hasDisconnected()) GuardianNetwork.send(p, new GuardianNetwork.Scene(token, s.guardian.getUUID(), true));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        for (Session s : new ArrayList<>(ACTIVE.values())) {
            ServerPlayer p = s.player;
            if (p.hasDisconnected() || !eligibleDuring(p) || p.level() != s.guardian.level() || s.guardian.isRemoved() || !s.guardian.isAlive() || p.serverLevel().getGameTime() - s.start > 6000) {
                finish(p, s.token, false);
                continue;
            }
            p.setDeltaMovement(Vec3.ZERO);
            p.fallDistance = 0;
            if (p.position().distanceToSqr(s.position) > .0025)
                p.connection.teleport(s.position.x, s.position.y, s.position.z, p.getYRot(), p.getXRot());
        }
    }

    private static boolean eligibleDuring(ServerPlayer p) {
        return p.isAlive() && !p.isSpectator();
    }

    @SubscribeEvent
    public static void damage(LivingAttackEvent e) {
        if (ACTIVE.containsKey(e.getEntity().getUUID()) || ACTIVE.values().stream().anyMatch(s -> s.guardian == e.getEntity()))
            e.setCanceled(true);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        Session s = ACTIVE.get(e.getEntity().getUUID());
        if (s != null) finish(s.player, s.token, false);
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent e) {
        for (Session s : ACTIVE.values()) s.guardian.endScene(false);
        ACTIVE.clear();
    }
}
