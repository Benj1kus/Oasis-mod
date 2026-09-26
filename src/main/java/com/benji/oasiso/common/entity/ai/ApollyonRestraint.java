package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ApollyonEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ApollyonRestraint {
    private static final Map<UUID, Hold> HELD = new HashMap<>();
    private static final int LEASE_TICKS = 5;

    private static final class Hold {
        final ApollyonEntity owner;
        final ServerPlayer player;
        Vec3 anchor;
        long expires;

        Hold(ApollyonEntity owner, ServerPlayer player) {
            this.owner = owner;
            this.player = player;
            this.anchor = player.position();
            this.expires = owner.level().getGameTime() + LEASE_TICKS;
        }
    }

    public static boolean acquire(ApollyonEntity owner, ServerPlayer player) {
        if (!eligible(owner, player)) return false;
        Hold previous = HELD.get(player.getUUID());
        if (previous != null) {
            if (valid(previous)) return false;
            release(previous.owner);
        }
        HELD.put(player.getUUID(), new Hold(owner, player));
        owner.setRestrainedPlayerId(player.getId());
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        return true;
    }

    public static boolean refresh(ApollyonEntity owner, Player player) {
        Hold hold = HELD.get(player.getUUID());
        if (hold == null || hold.owner != owner || !valid(hold)) {
            release(owner);
            return false;
        }
        hold.expires = owner.level().getGameTime() + LEASE_TICKS;
        enforce(hold);
        return true;
    }

    public static void push(ApollyonEntity owner, Player player, Vec3 movement) {
        Hold hold = HELD.get(player.getUUID());
        if (hold == null || hold.owner != owner || !valid(hold)) return;
        enforce(hold);
        Vec3 before = player.position();

        player.move(MoverType.SELF, movement);
        hold.anchor = player.position();
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        if (before.distanceToSqr(hold.anchor) > 1E-10) synchronize(hold);
    }

    public static void release(ApollyonEntity owner) {
        HELD.entrySet().removeIf(entry -> entry.getValue().owner == owner);
        owner.setRestrainedPlayerId(-1);
    }

    private static boolean eligible(ApollyonEntity owner, ServerPlayer player) {
        return owner.isAlive() && !owner.isRemoved() && player.isAlive() && !player.hasDisconnected() && !player.isSpectator() && !player.isCreative() && !player.isPassenger() && owner.level() == player.level();
    }

    private static boolean valid(Hold hold) {
        return eligible(hold.owner, hold.player) && hold.owner.isRestrainingPlayer(hold.player.getId()) && hold.owner.level().getGameTime() <= hold.expires;
    }

    private static void enforce(Hold hold) {
        ServerPlayer player = hold.player;
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        player.setSprinting(false);
        if (player.position().distanceToSqr(hold.anchor) > 1E-8) synchronize(hold);
    }

    private static void synchronize(Hold hold) {
        ServerPlayer player = hold.player;
        Vec3 p = hold.anchor;
        player.connection.teleport(p.x, p.y, p.z, player.getYRot(), player.getXRot());
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        Hold hold = HELD.get(player.getUUID());
        if (hold == null) return;
        if (!valid(hold)) release(hold.owner);
        else enforce(hold);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Hold hold = HELD.get(player.getUUID());
        if (hold != null) release(hold.owner);
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;
        HELD.entrySet().removeIf(entry -> {
            Hold hold = entry.getValue();
            if (hold.owner.level() != event.getLevel()) return false;
            hold.owner.setRestrainedPlayerId(-1);
            return true;
        });
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        HELD.clear();
    }

    private ApollyonRestraint() {
    }
}
