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
    private static final double[] AIR_IMPULSE = {0, .45, .95, 1.55};
    private static final double AIR_MAX_SPEED = 3.5;
    private static final int RELEASE_TOLERANCE_TICKS = 4;
    private static final Map<UUID, Charge> ACTIVE = new HashMap<>();

    private static final class Charge {
        final long start;
        final Object level;
        final ItemStack stack;
        final boolean air;
        final int requestId;
        long launch = -1;
        int stage;

        Charge(ServerPlayer p, boolean air, int requestId) {
            this.air = air;
            this.requestId = requestId;
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

    public static boolean canAirCharge(Player p) {
        return wearing(p) && p.isAlive() && !p.isSpectator() && !p.getAbilities().flying && p.isFallFlying() && !p.onGround() && !p.isPassenger() && !p.isInWaterOrBubble() && !p.isInLava() && !p.onClimbable();
    }

    public static int frame(long held) {
        if (held < HOLD_THRESHOLD) return 0;
        return FRAMES[(int) (((held - HOLD_THRESHOLD) / FRAME_TICKS) % FRAMES.length)];
    }

    public static void input(ServerPlayer p, int action, long clientHeldTicks, int requestId) {
        Charge state = ACTIVE.get(p.getUUID());
        if (action == 0 || action == 3) {
            boolean air = action == 3;
            if (state != null) return;
            if (!(air ? canAirCharge(p) : canCharge(p))) {
                ApolWingsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), packet(p, 3, p.level().getGameTime(), 0, requestId));
                return;
            }
            state = new Charge(p, air, requestId);
            ACTIVE.put(p.getUUID(), state);
            ApolWingsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), packet(p, 0, state.start, 0, state.requestId));
        } else if (action == 2) {
            if (state != null && state.requestId == requestId) cancel(p);
        } else if (action == 1 && state != null && state.requestId == requestId && state.launch < 0) {
            if (!valid(p, state)) {
                cancel(p);
                return;
            }
            long held = Math.max(0, p.level().getGameTime() - state.start);
            if (clientHeldTicks >= Math.max(0, held - RELEASE_TOLERANCE_TICKS) && clientHeldTicks <= held + RELEASE_TOLERANCE_TICKS) {
                held = clientHeldTicks;
            }
            state.stage = frame(held);
            if (state.stage == 0) {
                cancel(p);
                return;
            }
            if (state.air) {
                airBoost(p, state.stage, state.requestId);
                return;
            }
            state.launch = p.level().getGameTime() + WINDUP_TICKS;
            p.getPersistentData().putLong(JUMP_UNTIL, state.launch);
            broadcast(p, packet(p, 1, p.level().getGameTime(), 0, state.requestId));
        }
    }

    private static boolean valid(ServerPlayer p, Charge c) {
        return p.level() == c.level && (c.air ? canAirCharge(p) : canCharge(p)) && p.getItemBySlot(EquipmentSlot.CHEST) == c.stack;
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
            broadcast(p, packet(p, 2, p.level().getGameTime(), velocity, c.requestId));
        }
    }

    private static void airBoost(ServerPlayer p, int stage, int requestId) {
        ACTIVE.remove(p.getUUID());
        p.getPersistentData().remove(JUMP_UNTIL);
        Vec3 direction = p.getLookAngle().normalize();
        Vec3 old = p.getDeltaMovement();
        Vec3 motion = old.add(direction.scale(AIR_IMPULSE[stage]));
        double limit = Math.max(AIR_MAX_SPEED, old.length());
        if (motion.lengthSqr() > limit * limit) motion = motion.normalize().scale(limit);
        p.setDeltaMovement(motion);
        p.fallDistance = 0;
        p.hasImpulse = true;
        Vec3 origin = p.position().add(0, p.getBbHeight() * .5, 0).subtract(direction.scale(1.25));
        broadcast(p, new ApolWingsNetwork.Fx(4, p.getId(), p.getUUID(), p.level().getGameTime(), origin, 0, direction, motion, stage, requestId));
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

    private static ApolWingsNetwork.Fx packet(ServerPlayer p, int kind, long time, double velocity, int requestId) {
        return new ApolWingsNetwork.Fx(kind, p.getId(), p.getUUID(), time, p.position(), velocity, requestId);
    }

    private static void broadcast(ServerPlayer p, ApolWingsNetwork.Fx packet) {
        ApolWingsNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), packet);
    }

    private static void cancel(ServerPlayer p) {
        Charge old = ACTIVE.remove(p.getUUID());
        p.getPersistentData().remove(JUMP_UNTIL);
        if (old != null) broadcast(p, packet(p, 3, p.level().getGameTime(), 0, old.requestId));
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
