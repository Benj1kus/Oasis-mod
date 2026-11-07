package com.benji.oasiso.common.soulcast;

import com.benji.oasiso.registry.ModItems;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = "oasiso")
public final class SoulCast {
    public static final int DURATION = 40;
    private static final int COOLDOWN_TICKS = 20 * 20;

    private record Cast(long start, float yaw,
                        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {
    }

    private static final Map<UUID, Cast> ACTIVE = new HashMap<>();

    public static boolean fullSet(Player p) {
        return p.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.SOUL_HELMET.get()) && p.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.SOUL_CHESTPLATE.get()) && p.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.SOUL_LEGGINGS.get()) && p.getItemBySlot(EquipmentSlot.FEET).is(ModItems.SOUL_BOOTS.get());
    }

    public static void start(ServerPlayer p) {
        var chestplate = ModItems.SOUL_CHESTPLATE.get();
        var cooldowns = p.getCooldowns();

        if (cooldowns.isOnCooldown(chestplate) || ACTIVE.containsKey(p.getUUID()) || !fullSet(p) || !p.isAlive() || p.isSpectator() || p.isPassenger() || p.isSleeping() || p.isFallFlying()) {
            return;
        }

        Cast c = new Cast(p.serverLevel().getGameTime(), p.getYRot(), p.level().dimension());
        ACTIVE.put(p.getUUID(), c);
        cooldowns.addCooldown(chestplate, COOLDOWN_TICKS);
        SoulCastNetwork.broadcast(p, new SoulCastNetwork.State(p.getUUID(), c.start, c.yaw, true));
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        Cast c = ACTIVE.get(p.getUUID());
        if (c == null) return;
        if (!p.isAlive() || !fullSet(p) || p.isPassenger() || p.isSleeping() || p.isFallFlying() || p.level().dimension() != c.dimension || p.serverLevel().getGameTime() - c.start >= DURATION) {
            ACTIVE.remove(p.getUUID());
            SoulCastNetwork.broadcast(p, new SoulCastNetwork.State(p.getUUID(), c.start, c.yaw, false));
            return;
        }
        if (p.serverLevel().getGameTime() - c.start == SoulPossession.RELEASE_TICK) {
            SoulPossession.release(p);
        }
        var velocity = p.getDeltaMovement();
        p.setDeltaMovement(0, velocity.y, 0);
    }

    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking e) {
        if (!(e.getEntity() instanceof ServerPlayer observer) || !(e.getTarget() instanceof ServerPlayer p)) return;
        Cast c = ACTIVE.get(p.getUUID());
        if (c != null) SoulCastNetwork.send(observer, new SoulCastNetwork.State(p.getUUID(), c.start, c.yaw, true));
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        ACTIVE.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent e) {
        ACTIVE.clear();
    }
}
