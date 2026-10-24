package com.benji.oasiso.common.cutscene;

import com.benji.oasiso.Oasiso;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID)
public final class CutsceneServer {
    private record Viewer(ServerPlayer player, Vec3 position) {
    }

    private static final class Session {
        final UUID token = UUID.randomUUID();
        final Mob boss;
        final boolean noAi, invulnerable;
        final long deadline;
        final Map<UUID, Viewer> viewers = new HashMap<>();

        Session(Mob b) {
            boss = b;
            noAi = b.isNoAi();
            invulnerable = b.isInvulnerable();
            deadline = b.level().getGameTime() + 2400;
        }
    }

    private static final Map<UUID, Session> sessions = new HashMap<>();
    private static final Map<UUID, Set<ResourceLocation>> seen = new HashMap<>();

    @SubscribeEvent
    public static void recover(net.minecraftforge.event.entity.EntityJoinLevelEvent e) {
        if (!e.getLevel().isClientSide && e.getEntity() instanceof Mob mob && mob.getPersistentData().getBoolean("OasisoSceneLock")) {
            mob.setNoAi(mob.getPersistentData().getBoolean("OasisoSceneOldAI"));
            mob.setInvulnerable(mob.getPersistentData().getBoolean("OasisoSceneOldInv"));
            mob.getPersistentData().remove("OasisoSceneLock");
            if (mob instanceof CutsceneActor actor) actor.endCutscene();
        }
    }

    @SubscribeEvent
    public static void reload(AddReloadListenerEvent e) {
        e.addListener(new CutsceneData());
    }

    public static void begin(Mob boss, ResourceLocation id) {
        if (!(boss.level() instanceof ServerLevel level) || !(boss instanceof CutsceneActor actor)) return;
        String json = CutsceneData.DATA.get(id);
        if (json == null) return;
        var data = JsonParser.parseString(json).getAsJsonObject();
        double radius = data.has("radius") ? Math.max(1, Math.min(32, data.get("radius").getAsDouble())) : 10;
        Session session = new Session(boss);
        for (ServerPlayer p : level.players()) {
            if (!p.isAlive() || p.isSpectator() || p.distanceToSqr(boss) > radius * radius || busy(p.getUUID()))
                continue;
            if (!seen.computeIfAbsent(p.getUUID(), k -> new HashSet<>()).add(id)) continue;
            session.viewers.put(p.getUUID(), new Viewer(p, p.position()));
        }
        if (session.viewers.isEmpty()) return;
        long start = level.getGameTime() + CutsceneTiming.preparationTicks(data);
        boss.getPersistentData().putBoolean("OasisoSceneLock", true);
        boss.getPersistentData().putBoolean("OasisoSceneOldAI", session.noAi);
        boss.getPersistentData().putBoolean("OasisoSceneOldInv", session.invulnerable);
        boss.getNavigation().stop();
        boss.setNoAi(true);
        boss.setInvulnerable(true);
        boss.setDeltaMovement(Vec3.ZERO);
        actor.setCutscene(data.get("animation").getAsString(), start);
        sessions.put(session.token, session);
        for (Viewer v : session.viewers.values())
            CutsceneNetwork.start(v.player, new CutsceneNetwork.Start(session.token, boss.getUUID(), start, json));
    }

    private static boolean busy(UUID player) {
        return sessions.values().stream().anyMatch(s -> s.viewers.containsKey(player));
    }

    public static void finish(ServerPlayer p, UUID token) {
        Session s = sessions.get(token);
        if (s != null) s.viewers.remove(p.getUUID());
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var it = sessions.values().iterator();
        while (it.hasNext()) {
            Session s = it.next();
            s.viewers.values().removeIf(v -> {
                var p = v.player;
                boolean gone = p.hasDisconnected() || !p.isAlive() || p.level() != s.boss.level();
                if (gone) CutsceneNetwork.stop(p, s.token);
                return gone;
            });
            if (!s.boss.isAlive() || s.boss.isRemoved() || s.boss.level().getGameTime() >= s.deadline || s.viewers.isEmpty()) {
                for (Viewer v : s.viewers.values()) CutsceneNetwork.stop(v.player, s.token);
                s.boss.getPersistentData().remove("OasisoSceneLock");
                s.boss.setNoAi(s.noAi);
                s.boss.setInvulnerable(s.invulnerable);
                ((CutsceneActor) s.boss).endCutscene();
                it.remove();
                continue;
            }
            s.boss.setDeltaMovement(Vec3.ZERO);
            for (Viewer v : s.viewers.values()) {
                var p = v.player;
                p.setDeltaMovement(Vec3.ZERO);
                p.fallDistance = 0;
                if (p.position().distanceToSqr(v.position) > .0025)
                    p.connection.teleport(v.position.x, v.position.y, v.position.z, p.getYRot(), p.getXRot());
            }
        }
    }

    @SubscribeEvent
    public static void damage(LivingAttackEvent e) {
        if (busy(e.getEntity().getUUID())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        seen.remove(e.getEntity().getUUID());
        for (Session s : sessions.values()) s.viewers.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent e) {
        sessions.clear();
        seen.clear();
    }
}
