package com.benji.oasiso.common.wings;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.item.ApolWingsArmorItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ApolWingsTransit {
    public static final double ENTRY_SPEED = .55, MAX_THICKNESS = 10;
    public static final int WAIT_TICKS = 60, COST = 10, COOLDOWN = 20;
    private static final String RECOVERY = "OasisoTransitOldGravity";
    private static final Map<UUID, Travel> ACTIVE = new HashMap<>();
    private static final Map<UUID, Motion> MOTION = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

    private static final class Motion {
        Vec3 previous, velocity = Vec3.ZERO;
        ServerLevel level;
        long fresh, flightTick = -100;

        Motion(ServerPlayer p) {
            previous = p.position();
            level = p.serverLevel();
        }
    }

    private record Route(Vec3 exit, Vec3 surface, Vec3 normal) {
    }

    private record Travel(ServerLevel level, Vec3 anchor, Route route, Vec3 velocity, long end, boolean gravity,
                          boolean invisible, ItemStack wings) {
    }

    public static boolean active(Player p) {
        return ACTIVE.containsKey(p.getUUID());
    }

    private static boolean wings(ServerPlayer p) {
        ItemStack s = p.getItemBySlot(EquipmentSlot.CHEST);
        return s.getItem() instanceof ApolWingsArmorItem && !s.isEmpty() && s.getMaxDamage() - s.getDamageValue() >= COST;
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        Travel t = ACTIVE.get(p.getUUID());
        long now = p.level().getGameTime();
        if (t != null) {
            if (!p.isAlive() || p.serverLevel() != t.level()) {
                finish(p, t, false);
                return;
            }
            if (now >= t.end()) {
                finish(p, t, true);
                return;
            }
            if (now % 20 == 0) state(p, 0, (int) (t.end() - now), t.anchor(), Vec3.ZERO);
            p.setDeltaMovement(Vec3.ZERO);
            p.setInvisible(true);
            p.fallDistance = 0;
            if (p.position().distanceToSqr(t.anchor()) > .0025)
                p.connection.teleport(t.anchor().x, t.anchor().y, t.anchor().z, p.getYRot(), p.getXRot());
            return;
        }
        Motion m = MOTION.computeIfAbsent(p.getUUID(), k -> new Motion(p));
        if (m.level != p.serverLevel()) {
            MOTION.put(p.getUUID(), new Motion(p));
            return;
        }
        Vec3 delta = p.position().subtract(m.previous);
        m.previous = p.position();
        if (!wings(p) || p.isPassenger() || p.isSpectator() || !p.isAlive()) {
            m.velocity = Vec3.ZERO;
            return;
        }
        if (p.isFallFlying()) m.flightTick = now;
        else if (!p.onGround() || now - m.flightTick > 2) {
            m.velocity = Vec3.ZERO;
            return;
        }
        if (delta.lengthSqr() > 64) {
            m.velocity = Vec3.ZERO;
            return;
        }
        if (delta.lengthSqr() >= ENTRY_SPEED * ENTRY_SPEED) {
            m.velocity = delta;
            m.fresh = now;
        }
        if (now - m.fresh > 3 || m.velocity.lengthSqr() < ENTRY_SPEED * ENTRY_SPEED || COOLDOWNS.getOrDefault(p.getUUID(), 0L) > now)
            return;
        Vec3 direction = m.velocity.normalize();
        Vec3 center = p.position().add(0, .3, 0);
        BlockHitResult hit = clip(p, center, center.add(direction.scale(.48)));
        if (hit.getType() != HitResult.Type.BLOCK) return;
        var block = p.level().getBlockState(hit.getBlockPos());
        if (!Block.isShapeFullBlock(block.getCollisionShape(p.level(), hit.getBlockPos()))) return;
        Route route = route(p, hit, direction);
        if (route == null) {
            COOLDOWNS.put(p.getUUID(), now + 6);
            return;
        }
        ItemStack stack = p.getItemBySlot(EquipmentSlot.CHEST);
        Travel travel = new Travel(p.serverLevel(), p.position(), route, m.velocity, now + WAIT_TICKS, p.isNoGravity(), p.isInvisible(), stack);
        ACTIVE.put(p.getUUID(), travel);
        MOTION.remove(p.getUUID());
        p.getPersistentData().putBoolean(RECOVERY, p.isNoGravity());
        p.getPersistentData().putBoolean(RECOVERY + "Invisible", p.isInvisible());
        p.setInvisible(true);
        p.closeContainer();
        p.stopUsingItem();
        p.stopFallFlying();
        p.setNoGravity(true);
        p.setDeltaMovement(Vec3.ZERO);
        p.fallDistance = 0;
        state(p, 0, WAIT_TICKS, travel.anchor(), Vec3.ZERO);
        blob(p, hit.getLocation(), Vec3.atLowerCornerOf(hit.getDirection().getNormal()));
    }

    private static BlockHitResult clip(ServerPlayer p, Vec3 a, Vec3 b) {
        return p.level().clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
    }

    private static AABB standing(Vec3 feet) {
        return new AABB(feet.x - .31, feet.y, feet.z - .31, feet.x + .31, feet.y + 1.81, feet.z + .31);
    }

    private static boolean clear(ServerPlayer p, Vec3 feet) {
        AABB box = standing(feet);
        ServerLevel l = p.serverLevel();
        if (box.minY < l.getMinBuildHeight() || box.maxY > l.getMaxBuildHeight() || !l.getWorldBorder().isWithinBounds(box))
            return false;
        if (!l.hasChunksAt(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ)))
            return false;
        return l.noCollision(p, box) && !l.containsAnyLiquid(box);
    }

    private static Route route(ServerPlayer p, BlockHitResult entry, Vec3 direction) {
        Vec3 inside = entry.getLocation().add(direction.scale(.001));
        for (double distance = .125; distance <= MAX_THICKNESS + 2; distance += .125) {
            Vec3 center = entry.getLocation().add(direction.scale(distance));
            if (!p.level().hasChunkAt(BlockPos.containing(center))) return null;
            Vec3 feet = center.add(0, -.3, 0);
            if (!clear(p, feet)) continue;
            BlockHitResult back = clip(p, center, inside);
            if (back.getType() != HitResult.Type.BLOCK) return null;
            double thickness = back.getLocation().distanceTo(entry.getLocation());
            if (thickness <= 1.05) continue;
            if (thickness > MAX_THICKNESS + 1e-5) return null;
            return new Route(feet, back.getLocation(), Vec3.atLowerCornerOf(back.getDirection().getNormal()));
        }
        return null;
    }

    private static void finish(ServerPlayer p, Travel t, boolean success) {
        ACTIVE.remove(p.getUUID());
        MOTION.remove(p.getUUID());
        p.setNoGravity(t.gravity());
        p.setInvisible(t.invisible());
        p.getPersistentData().remove(RECOVERY);
        p.getPersistentData().remove(RECOVERY + "Invisible");
        success = success && p.isAlive() && p.serverLevel() == t.level() && clear(p, t.route().exit()) && p.getItemBySlot(EquipmentSlot.CHEST) == t.wings() && wings(p);
        Vec3 velocity = Vec3.ZERO;
        if (success) {
            Vec3 v = t.route().exit();
            p.connection.teleport(v.x, v.y, v.z, p.getYRot(), p.getXRot());
            ItemStack s = t.wings();
            int damage = s.getDamageValue() + COST;
            if (damage >= s.getMaxDamage()) {
                p.broadcastBreakEvent(EquipmentSlot.CHEST);
                s.shrink(1);
                s.setDamageValue(0);
            } else s.setDamageValue(damage);
            p.setOnGround(false);
            if (!s.isEmpty()) {
                p.startFallFlying();
                velocity = t.velocity().normalize().scale(Math.min(t.velocity().length(), 1.5));
            }
            blob(p, t.route().surface(), t.route().normal());
        }
        p.setDeltaMovement(velocity);
        p.hurtMarked = true;
        p.fallDistance = 0;
        COOLDOWNS.put(p.getUUID(), p.level().getGameTime() + COOLDOWN);
        state(p, 1, 0, p.position(), velocity);
    }

    private static void state(ServerPlayer p, int kind, int ticks, Vec3 pos, Vec3 velocity) {
        ApolWingsTransitNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), new ApolWingsTransitNetwork.Fx(kind, p.getUUID(), ticks, pos, Vec3.ZERO, velocity));
    }

    private static void blob(ServerPlayer p, Vec3 pos, Vec3 normal) {
        ApolWingsTransitNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(pos.x, pos.y, pos.z, 96, p.level().dimension())), new ApolWingsTransitNetwork.Fx(2, p.getUUID(), 100, pos, normal, Vec3.ZERO));
        var soil = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "entropy_soil")).defaultBlockState();
        Vec3 out = pos.add(normal.scale(.15));
        p.serverLevel().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, soil), out.x, out.y, out.z, 55, .3, .3, .3, .22);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        var p = e.getEntity();
        if (p.getPersistentData().contains(RECOVERY)) {
            p.setNoGravity(p.getPersistentData().getBoolean(RECOVERY));
            p.setInvisible(p.getPersistentData().getBoolean(RECOVERY + "Invisible"));
            p.getPersistentData().remove(RECOVERY);
            p.getPersistentData().remove(RECOVERY + "Invisible");
        }
    }

    @SubscribeEvent
    public static void breaking(net.minecraftforge.event.level.BlockEvent.BreakEvent e) {
        if (active(e.getPlayer())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void damage(LivingAttackEvent e) {
        if (e.getEntity() instanceof Player p && active(p)) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void interact(PlayerInteractEvent e) {
        if (active(e.getEntity()) && e.isCancelable()) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void attack(AttackEntityEvent e) {
        if (active(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void toss(ItemTossEvent e) {
        if (active(e.getPlayer())) {
            e.setCanceled(true);
            e.getPlayer().getInventory().placeItemBackInInventory(e.getEntity().getItem());
        }
    }

    @SubscribeEvent
    public static void track(PlayerEvent.StartTracking e) {
        if (e.getEntity() instanceof ServerPlayer viewer && e.getTarget() instanceof ServerPlayer p) {
            Travel t = ACTIVE.get(p.getUUID());
            if (t != null)
                ApolWingsTransitNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer), new ApolWingsTransitNetwork.Fx(0, p.getUUID(), (int) Math.max(1, t.end() - p.level().getGameTime()), t.anchor(), Vec3.ZERO, Vec3.ZERO));
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            Travel t = ACTIVE.get(p.getUUID());
            if (t != null) finish(p, t, false);
            MOTION.remove(p.getUUID());
            COOLDOWNS.remove(p.getUUID());
        }
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            Travel t = ACTIVE.get(p.getUUID());
            if (t != null) finish(p, t, false);
            MOTION.remove(p.getUUID());
        }
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent e) {
        ACTIVE.clear();
        MOTION.clear();
        COOLDOWNS.clear();
    }

    private ApolWingsTransit() {
    }
}
