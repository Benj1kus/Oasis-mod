package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.combat.ScienceCombatNetwork;
import com.benji.oasiso.common.entity.CrusaderScienceEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ScienceCombatEffects {
    public record Hit(Vec3 normal, long start) {
    }

    private static final Map<UUID, Deque<Hit>> HITS = new HashMap<>();
    private static ClientLevel world;

    public static void receive(ScienceCombatNetwork.Fx packet) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        if (world != level) {
            HITS.clear();
            world = level;
        }
        if (packet.kind() == 0) {
            var entity = level.getEntity(packet.id());
            if (!(entity instanceof CrusaderScienceEntity) || !entity.getUUID().equals(packet.uuid())) return;
            Deque<Hit> hits = HITS.computeIfAbsent(packet.uuid(), id -> new ArrayDeque<>());
            if (hits.size() >= 4) hits.removeFirst();
            hits.addLast(new Hit(packet.vector().normalize(), level.getGameTime()));
        } else if (packet.kind() == 1) burst(level, packet.vector());
    }

    public static Collection<Hit> hits(UUID uuid) {
        var hits = HITS.get(uuid);
        return hits == null ? List.of() : hits;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var level = Minecraft.getInstance().level;
        if (world != level) {
            HITS.clear();
            world = level;
        }
        if (level == null) return;
        HITS.values().forEach(h -> h.removeIf(hit -> level.getGameTime() - hit.start() > 26));
        HITS.values().removeIf(Deque::isEmpty);
    }

    private static void burst(ClientLevel level, Vec3 p) {
        var random = level.random;
        for (int i = 0; i < 6; i++)
            level.addParticle(Oasiso.CHAOS_BOMB_CENTER_SMOKE.get(), p.x + (random.nextDouble() - .5) * .15, p.y, p.z + (random.nextDouble() - .5) * .15, (random.nextDouble() - .5) * .055, .025 + random.nextDouble() * .045, (random.nextDouble() - .5) * .055);
        for (int i = 0; i < 10; i++) {
            Vec3 dir = new Vec3(random.nextDouble() - .5, random.nextDouble() - .25, random.nextDouble() - .5).normalize();
            double speed = .045 + random.nextDouble() * .08;
            level.addParticle(Oasiso.CHAOS_BOMB_FIRE_SMOKE.get(), p.x, p.y, p.z, dir.x * speed, dir.y * speed + .025, dir.z * speed);
        }
        for (int i = 0; i < 18; i++) {
            double angle = random.nextDouble() * Math.PI * 2, speed = .08 + random.nextDouble() * .17;
            level.addParticle(Oasiso.CHAOS_BOMB_SPARKS.get(), p.x, p.y, p.z, Math.cos(angle) * speed, .1 + random.nextDouble() * .16, Math.sin(angle) * speed);
        }
    }

    private ScienceCombatEffects() {
    }
}
