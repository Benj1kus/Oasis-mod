package com.benji.oasiso.common.soulcast;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = "oasiso")
public final class SoulPossession {
    public static final int RELEASE_TICK = 25, RANGE = 20, MAX_SOULS = 10, CONTROL_TICKS = 200;
    private static final String END = "oasisoSoulControlUntil";
    private static final String SAFE_SHOT = "oasisoSoulFriendlyShot";
    private static final Map<Mob, SoulPossessionSync.State> flights = new WeakHashMap<>();
    private static final Map<Mob, Mob> orders = new HashMap<>();

    private static final Map<Mob, Long> lastAttack = new WeakHashMap<>();

    private static boolean monster(LivingEntity e) {
        return e instanceof Mob && (e instanceof Enemy || e.getType().getCategory() == MobCategory.MONSTER);
    }

    private static boolean controlled(Mob m) {
        return m.getPersistentData().getLong(END) > m.level().getGameTime();
    }

    private static boolean canPossess(Mob m) {
        return monster(m) && m.isAlive() && m.getHealth() < 200F && !controlled(m);
    }

    public static void release(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 origin = player.position().add(0, player.getBbHeight() * .65, 0);
        var mobs = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(RANGE), m -> canPossess(m) && !flights.containsKey(m) && player.distanceToSqr(m) <= RANGE * RANGE);
        mobs.sort(Comparator.comparingDouble((Mob m) -> player.distanceToSqr(m)).thenComparingInt(Mob::getId));
        long now = level.getGameTime();
        for (int i = 0; i < Math.min(MAX_SOULS, mobs.size()); i++) {
            Mob m = mobs.get(i);
            long travel = 14 + Math.round(origin.distanceTo(m.getBoundingBox().getCenter()) * 1.25);
            var state = new SoulPossessionSync.State(level.dimension().location(), m.getUUID(), m.getId(), origin, now, now + travel, 0);
            flights.put(m, state);
            SoulPossessionSync.trackers(m, state);
        }
    }

    private static SoulPossessionSync.State controlState(Mob mob, long end) {
        return new SoulPossessionSync.State(mob.level().dimension().location(), mob.getUUID(), mob.getId(), mob.position(), end - CONTROL_TICKS, end - CONTROL_TICKS, end);
    }

    private static void target(Mob mob, Mob enemy) {
        if (mob.getTarget() != enemy) mob.setTarget(enemy);
        var brain = mob.getBrain();
        if (brain.checkMemory(MemoryModuleType.ATTACK_TARGET, net.minecraft.world.entity.ai.memory.MemoryStatus.REGISTERED)) {
            if (enemy == null) brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
            else if (brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) != enemy)
                brain.setMemory(MemoryModuleType.ATTACK_TARGET, (LivingEntity) enemy);
        }
    }

    private static boolean valid(Mob mob, Mob enemy) {
        return enemy != null && enemy != mob && enemy.isAlive() && enemy.level() == mob.level() && monster(enemy) && mob.distanceToSqr(enemy) <= RANGE * RANGE;
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        var flight = flights.get(mob);
        if (flight != null && now >= flight.arrival()) {
            flights.remove(mob);
            if (canPossess(mob)) {
                long end = now + CONTROL_TICKS;
                lastAttack.put(mob, now);
                mob.getPersistentData().putLong(END, end);
                mob.setLastHurtByMob(null);
                mob.getNavigation().stop();
                SoulPossessionSync.trackers(mob, controlState(mob, end));
            }
        }
        if (!mob.getPersistentData().contains(END)) return;
        if (!controlled(mob)) {
            mob.getPersistentData().remove(END);
            orders.remove(mob);
            lastAttack.remove(mob);
            target(mob, null);
            mob.getNavigation().stop();
            return;
        }

        Mob enemy = orders.get(mob);
        if (!orders.containsKey(mob) || (enemy != null && !valid(mob, enemy)) || mob.tickCount % 8 == 0) {
            enemy = level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(RANGE), candidate -> valid(mob, candidate)).stream().min(Comparator.comparingDouble((Mob candidate) -> mob.distanceToSqr(candidate)).thenComparingInt(Mob::getId)).orElse(null);
            orders.put(mob, enemy);
        }
        target(mob, enemy);
        if (mob.getLastHurtByMob() instanceof Player) mob.setLastHurtByMob(null);
        if (enemy != null && mob.tickCount % 20 == 0 && !mob.isNoAi()) {
            if (mob.getNavigation().isDone() && !mob.getBoundingBox().inflate(.7).intersects(enemy.getBoundingBox()))
                mob.getNavigation().moveTo(enemy, 1.0);
            else if (enemy.invulnerableTime == 0 && mob.getBoundingBox().inflate(.7).intersects(enemy.getBoundingBox()) && mob.hasLineOfSight(enemy) && now - lastAttack.getOrDefault(mob, 0L) >= 24) {
                if (mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) != null)
                    mob.doHurtTarget(enemy);
                else enemy.hurt(mob.damageSources().mobAttack(mob), 2F);
                lastAttack.put(mob, now);
            }
        }
    }

    @SubscribeEvent
    public static void attackLanded(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Mob mob && !mob.level().isClientSide && controlled(mob))
            lastAttack.put(mob, mob.level().getGameTime());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void redirect(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide && controlled(mob)) {
            Mob enemy = orders.get(mob);
            event.setNewTarget(valid(mob, enemy) ? enemy : null);
        }
    }

    @SubscribeEvent
    public static void projectile(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Projectile p && p.getOwner() instanceof Mob m && controlled(m))
            p.getPersistentData().putBoolean(SAFE_SHOT, true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void protect(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Entity author = event.getSource().getEntity(), direct = event.getSource().getDirectEntity();
        if ((author instanceof Mob m && controlled(m)) || (direct != null && direct.getPersistentData().getBoolean(SAFE_SHOT)))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof Mob mob)) return;
        if (controlled(mob)) SoulPossessionSync.send(player, controlState(mob, mob.getPersistentData().getLong(END)));
        else if (flights.containsKey(mob)) SoulPossessionSync.send(player, flights.get(mob));
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob) {
            flights.remove(mob);
            orders.remove(mob);
        }
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        flights.clear();
        orders.clear();
        lastAttack.clear();
    }
}
