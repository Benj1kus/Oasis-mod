package com.benji.oasiso.common.wings;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.item.ApolWingsArmorItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ApolWingsMechanics {
    public static final String JUMP_UNTIL = "OasisoWingsJumpUntil";
    public static final int HOLD_THRESHOLD = 6, FRAME_TICKS = 8, WINDUP_TICKS = 20;
    private static final int[] FRAMES = {0, 1, 2, 3, 2, 1};
    private static final double[] HEIGHTS = {0, 3, 10, 22};
    private static final Map<UUID, Charge> ACTIVE = new HashMap<>();

    private static final class Charge {
        final long start;
        final Object level;
        final ItemStack stack;
        long launch = -1;
        int stage;

        Charge(ServerPlayer p) {
            start = p.level().getGameTime();
            level = p.level();
            stack = p.getItemBySlot(EquipmentSlot.CHEST);
        }
    }

    public static boolean wearing(Player p) {
        ItemStack stack = p.getItemBySlot(EquipmentSlot.CHEST);
        return stack.getItem() instanceof ApolWingsArmorItem wings && wings.canElytraFly(stack, p);
    }

    public static boolean canCharge(Player p) {
        return wearing(p) && p.isAlive() && !p.isSpectator() && !p.getAbilities().flying && p.onGround() && !p.isPassenger() && !p.isInWaterOrBubble() && !p.isInLava() && !p.isFallFlying() && !p.onClimbable();
    }

    public static int frame(long held) {
        if (held < HOLD_THRESHOLD) return 0;
        return FRAMES[(int) (((held - HOLD_THRESHOLD) / FRAME_TICKS) % FRAMES.length)];
    }

    public static void input(ServerPlayer p, int action) {
        Charge state = ACTIVE.get(p.getUUID());
        if (action == 0) {
            if (state != null || !canCharge(p)) return;
            state = new Charge(p);
            ACTIVE.put(p.getUUID(), state);
            ApolWingsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), packet(p, 0, state.start, 0));
        } else if (action == 2) {
            cancel(p);
        } else if (action == 1 && state != null && state.launch < 0) {
            if (!valid(p, state)) {
                cancel(p);
                return;
            }
            long held = p.level().getGameTime() - state.start;
            state.stage = frame(held);
            if (state.stage == 0) {
                cancel(p);
                return;
            }
            state.launch = p.level().getGameTime() + WINDUP_TICKS;
            p.getPersistentData().putLong(JUMP_UNTIL, state.launch);
            broadcast(p, packet(p, 1, p.level().getGameTime(), 0));
        }
    }

    private static boolean valid(ServerPlayer p, Charge c) {
        return p.level() == c.level && canCharge(p) && p.getItemBySlot(EquipmentSlot.CHEST) == c.stack;
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)) return;
        Charge c = ACTIVE.get(p.getUUID());
        if (c == null) return;
        if (!valid(p, c)) {
            cancel(p);
            return;
        }
        if (c.launch >= 0 && p.level().getGameTime() >= c.launch) {
            ACTIVE.remove(p.getUUID());
            p.getPersistentData().remove(JUMP_UNTIL);
            double velocity = velocityForHeight(HEIGHTS[c.stage]);
            Vec3 old = p.getDeltaMovement();
            p.setDeltaMovement(old.x, velocity, old.z);
            p.setOnGround(false);
            p.fallDistance = 0;
            p.hurtMarked = true;
            p.hasImpulse = true;
            broadcast(p, packet(p, 2, p.level().getGameTime(), velocity));
        }
    }

    public static double velocityForHeight(double height) {
        double low = 0, high = 4;
        for (int i = 0; i < 48; i++) {
            double mid = (low + high) * .5, y = 0, v = mid;
            for (int tick = 0; tick < 200 && v > 0; tick++) {
                y += v;
                v = (v - .08) * .98;
            }
            if (y < height) low = mid;
            else high = mid;
        }
        return (low + high) * .5;
    }

    private static ApolWingsNetwork.Fx packet(ServerPlayer p, int kind, long time, double velocity) {
        return new ApolWingsNetwork.Fx(kind, p.getId(), p.getUUID(), time, p.position(), velocity);
    }

    private static void broadcast(ServerPlayer p, ApolWingsNetwork.Fx packet) {
        ApolWingsNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), packet);
    }

    private static void cancel(ServerPlayer p) {
        Charge old = ACTIVE.remove(p.getUUID());
        p.getPersistentData().remove(JUMP_UNTIL);
        if (old != null) broadcast(p, packet(p, 3, p.level().getGameTime(), 0));
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        ACTIVE.remove(e.getEntity().getUUID());
        e.getEntity().getPersistentData().remove(JUMP_UNTIL);
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent e) {
        ACTIVE.clear();
    }

    private ApolWingsMechanics() {
    }
}
