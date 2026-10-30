package com.benji.oasiso.common.portal;

import com.benji.oasiso.common.block.SoulPortalBlock;
import com.benji.oasiso.common.block.SoulPortalFrames;
import com.benji.oasiso.registry.ModBlocks;
import com.benji.oasiso.common.portal.SoulStorageData.Visit;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.*;

@Mod.EventBusSubscriber(modid = "oasiso")
public final class SoulPortalTravel {
    public static final int ENTER_TICKS = 80;
    public static final int HEIGHT = 45;
    private static final int ERASE_BUDGET = 2048;
    private static final ResourceLocation TEMPLATE = ResourceLocation.fromNamespaceAndPath("oasiso", "soul_storage");
    private static final String LOCK = "OasisoSoulPortalExitLock";
    private static final String ENTITY_TAG = "oasiso_storage:";
    private static final Map<UUID, Charge> CHARGES = new HashMap<>();

    private static final class Charge {
        ServerLevel level;
        long last;
        int ticks;
        BlockPos portal;
    }

    public static void inside(Level world, BlockPos pos, Entity entity) {
        if (!(world instanceof ServerLevel level) || !(entity instanceof ServerPlayer p) || !p.isAlive() || p.isSpectator() || p.getPersistentData().getBoolean(LOCK))
            return;
        long now = level.getGameTime();
        Charge c = CHARGES.computeIfAbsent(p.getUUID(), k -> new Charge());
        if (c.level == level && c.last == now) return;
        if (c.level != level || c.last != now - 1 || c.portal == null || c.portal.distSqr(pos) > 36) c.ticks = 0;
        c.level = level;
        c.last = now;
        c.portal = pos.immutable();
        if (++c.ticks == 1) {
            p.playNotifySound(SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 0.35F, 0.9F + p.getRandom().nextFloat() * 0.2F);
        }

        if (c.ticks < ENTER_TICKS) return;
        CHARGES.remove(p.getUUID());
        p.getPersistentData().putBoolean(LOCK, true);
        travel(level, p, pos);
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        if (!touchingPortal(p)) {
            p.getPersistentData().remove(LOCK);
            CHARGES.remove(p.getUUID());
        }
    }

    private static boolean touchingPortal(ServerPlayer p) {
        AABB b = p.getBoundingBox().deflate(0.001);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(b.minX, b.minY, b.minZ), BlockPos.containing(b.maxX, b.maxY, b.maxZ)))
            if (p.level().getBlockState(pos).is(ModBlocks.SOUL_PORTAL.get())) return true;
        return false;
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        CHARGES.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent e) {
        CHARGES.clear();
    }

    private static void fail(ServerPlayer p, String reason) {
        p.displayClientMessage(Component.translatable("message.oasiso_portal." + reason), false);
    }

    private static void travel(ServerLevel level, ServerPlayer p, BlockPos portal) {
        SoulStorageData data = SoulStorageData.get(level);
        for (Visit v : data.visits.values())
            if (v.bounds().contains(Vec3.atCenterOf(portal))) {
                if (v.closing) {
                    fail(p, "closing");
                    return;
                }
                leave(level, data, v, p);
                return;
            }
        Direction.Axis axis = level.getBlockState(portal).getValue(SoulPortalBlock.AXIS);
        BlockPos source = SoulPortalFrames.findFrame(level, portal, axis);
        if (source == null) {
            fail(p, "frame");
            return;
        }
        Visit visit = null;
        for (Visit v : data.visits.values())
            if (v.source.equals(source) && v.sourceAxis == axis) {
                visit = v;
                break;
            }
        if (visit != null && visit.closing) {
            fail(p, "closing");
            return;
        }
        if (visit == null) visit = create(level, data, source, axis, p);
        if (visit == null) return;
        loadChunks(level, visit.origin, visit.max());
        if (visit.exit == null || !SoulPortalFrames.activate(level, visit.exit, visit.exitAxis)) {
            fail(p, "exit");
            if (visit.visitors.isEmpty()) {
                visit.closing = true;
                data.setDirty();
            }
            return;
        }
        Vec3 target = landing(level, p, visit.exit, visit.exitAxis);
        if (target == null) {
            fail(p, "blocked");
            return;
        }
        visit.visitors.add(p.getUUID());
        data.setDirty();
        Vec3 middle = visit.bounds().getCenter();
        float yaw = visit.exitAxis == Direction.Axis.X ? (middle.z >= target.z ? 0 : 180) : (middle.x >= target.x ? -90 : 90);
        move(level, p, target, yaw);
    }

    private static Visit create(ServerLevel level, SoulStorageData data, BlockPos source, Direction.Axis axis, ServerPlayer p) {
        Optional<StructureTemplate> found;
        try {
            found = level.getStructureManager().get(TEMPLATE);
        } catch (RuntimeException ex) {
            LogUtils.getLogger().error("Cannot read soul_storage", ex);
            fail(p, "template");
            return null;
        }
        if (found.isEmpty()) {
            fail(p, "template");
            return null;
        }
        StructureTemplate template = found.get();
        Vec3i dimensions = template.getSize();
        if (dimensions.getX() != 48 || dimensions.getY() != 15 || dimensions.getZ() != 48) {
            fail(p, "size");
            return null;
        }
        BlockPos size = new BlockPos(dimensions);
        Vec3 center = center(source, axis);
        Visit v = null;
        for (int offset : new int[]{0, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5}) {
            BlockPos origin = new BlockPos((int) Math.floor(center.x) - size.getX() / 2, source.getY() + HEIGHT + offset, (int) Math.floor(center.z) - size.getZ() / 2);
            Visit candidate = new Visit(UUID.randomUUID(), source, axis, origin, size);
            if (free(level, data, candidate)) {
                v = candidate;
                break;
            }
        }
        if (v == null) {
            fail(p, "space");
            return null;
        }
        Visit placed = v;
        data.visits.put(v.id, v);
        data.setDirty();
        boolean[] ok = {false};
        try {
            SoulPortalFrames.withoutChecks(() -> ok[0] = template.placeInWorld(level, placed.origin, placed.origin, new StructurePlaceSettings().setIgnoreEntities(false).setFinalizeEntities(true).setKeepLiquids(false), level.random, 18));
            if (ok[0]) findExit(level, v);
        } catch (RuntimeException ex) {
            LogUtils.getLogger().error("Cannot place soul_storage at {}", v.origin, ex);
        }
        if (!ok[0] || v.exit == null) {
            v.closing = true;
            data.setDirty();
            fail(p, "exit");
            return null;
        }
        data.setDirty();
        return v;
    }

    private static boolean free(ServerLevel level, SoulStorageData data, Visit v) {
        if (v.origin.getY() < level.getMinBuildHeight() || v.max().getY() >= level.getMaxBuildHeight() || !level.getWorldBorder().isWithinBounds(v.origin) || !level.getWorldBorder().isWithinBounds(v.max()))
            return false;
        for (Visit other : data.visits.values()) if (other.bounds().inflate(1).intersects(v.bounds())) return false;
        loadChunks(level, v.origin, v.max());
        for (BlockPos pos : BlockPos.betweenClosed(v.origin, v.max()))
            if (!level.getBlockState(pos).isAir()) return false;
        return level.getEntitiesOfClass(Entity.class, v.bounds(), e -> !e.isRemoved()).isEmpty();
    }

    private static void findExit(ServerLevel level, Visit v) {
        for (BlockPos core : BlockPos.betweenClosed(v.origin, v.max())) {
            if (!level.getBlockState(core).is(ModBlocks.SOUL_PCORE.get())) continue;
            for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
                BlockPos frame = axis == Direction.Axis.X ? core.offset(-2, -5, 0) : core.offset(0, -5, -2);
                BlockPos end = axis == Direction.Axis.X ? frame.offset(4, 5, 0) : frame.offset(0, 5, 4);
                if (!v.bounds().contains(Vec3.atCenterOf(frame)) || !v.bounds().contains(Vec3.atCenterOf(end)))
                    continue;
                if (SoulPortalFrames.activate(level, frame, axis)) {
                    v.exit = frame;
                    v.exitAxis = axis;
                    return;
                }
            }
        }
    }

    private static void leave(ServerLevel level, SoulStorageData data, Visit v, ServerPlayer p) {
        loadChunks(level, v.source.offset(-5, -5, -5), v.source.offset(9, 9, 9));
        SoulPortalFrames.activate(level, v.source, v.sourceAxis);
        Vec3 target = landing(level, p, v.source, v.sourceAxis);
        if (target == null) {
            fail(p, "return_blocked");
            return;
        }
        move(level, p, target, p.getYRot());
        v.visitors.remove(p.getUUID());
        data.setDirty();
    }

    private static void move(ServerLevel level, ServerPlayer p, Vec3 target, float yaw) {
        p.stopRiding();
        p.getPersistentData().putBoolean(LOCK, true);
        CHARGES.remove(p.getUUID());
        p.teleportTo(level, target.x, target.y, target.z, yaw, 0);
        p.playNotifySound(SoundEvents.PORTAL_TRAVEL, SoundSource.BLOCKS, 0.5F, 0.9F + p.getRandom().nextFloat() * 0.2F);
        p.setDeltaMovement(Vec3.ZERO);
        p.fallDistance = 0;
    }

    private static Vec3 center(BlockPos frame, Direction.Axis axis) {
        return axis == Direction.Axis.X ? new Vec3(frame.getX() + 2.5, frame.getY() + 1, frame.getZ() + 0.5) : new Vec3(frame.getX() + 0.5, frame.getY() + 1, frame.getZ() + 2.5);
    }

    private static Vec3 landing(ServerLevel level, ServerPlayer p, BlockPos frame, Direction.Axis axis) {
        Vec3 point = center(frame, axis);
        if (safe(level, p, point)) return point;
        for (int radius = 1; radius <= 4; radius++)
            for (int y : new int[]{0, 1, -1, 2, -2, 3, -3, 4, -4})
                for (int x = -radius; x <= radius; x++)
                    for (int z = -radius; z <= radius; z++) {
                        if (Math.max(Math.abs(x), Math.abs(z)) != radius) continue;
                        Vec3 candidate = point.add(x, y, z);
                        if (safe(level, p, candidate)) return candidate;
                    }
        return null;
    }

    private static boolean safe(ServerLevel level, ServerPlayer p, Vec3 point) {
        BlockPos feet = BlockPos.containing(point);
        if (!level.getWorldBorder().isWithinBounds(feet) || feet.getY() < level.getMinBuildHeight() + 1 || point.y + p.getBbHeight() >= level.getMaxBuildHeight())
            return false;
        if (!level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)) return false;
        if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()) return false;
        return level.noCollision(p, p.getBoundingBox().move(point.x - p.getX(), point.y - p.getY(), point.z - p.getZ()));
    }

    private static void loadChunks(ServerLevel level, BlockPos min, BlockPos max) {
        for (int x = min.getX() >> 4; x <= max.getX() >> 4; x++)
            for (int z = min.getZ() >> 4; z <= max.getZ() >> 4; z++) level.getChunk(x, z);
    }

    @SubscribeEvent
    public static void entityJoin(EntityJoinLevelEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level) || e.getEntity() instanceof Player) return;
        Entity entity = e.getEntity();
        SoulStorageData data = SoulStorageData.get(level);
        for (String tag : entity.getTags())
            if (tag.startsWith(ENTITY_TAG)) {
                try {
                    Visit v = data.visits.get(UUID.fromString(tag.substring(ENTITY_TAG.length())));
                    if (v == null || v.closing) {
                        e.setCanceled(true);
                        return;
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        for (Visit v : data.visits.values())
            if (v.bounds().contains(entity.position())) {
                if (v.closing) {
                    e.setCanceled(true);
                    return;
                }
                entity.addTag(ENTITY_TAG + v.id);
                break;
            }
    }

    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.level instanceof ServerLevel level)) return;
        SoulStorageData data = SoulStorageData.get(level);
        int[] budget = {ERASE_BUDGET};
        for (Visit v : new ArrayList<>(data.visits.values())) {
            if (!v.closing) {
                boolean changed = v.visitors.removeIf(id -> {
                    ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
                    return p != null && (!p.isAlive() || p.serverLevel() != level || !v.bounds().inflate(8).contains(p.position()));
                });
                if (changed) data.setDirty();
                if (v.visitors.isEmpty() && !occupied(level, v)) {
                    v.closing = true;
                    data.setDirty();
                }
            }
            if (!v.closing || occupied(level, v) || budget[0] <= 0) continue;
            loadChunks(level, v.origin, v.max());
            if (v.cursor == 0) removeEntities(level, v);
            SoulPortalFrames.withoutChecks(() -> {
                while (v.cursor < v.volume() && budget[0] > 0) {
                    BlockPos pos = v.cell(v.cursor++);
                    budget[0]--;
                    if (!level.getBlockState(pos).isAir()) {
                        level.removeBlockEntity(pos);
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
                    }
                }
            });
            data.setDirty();
            if (v.cursor == v.volume()) {
                removeEntities(level, v);
                data.visits.remove(v.id);
            }
        }
    }

    private static boolean occupied(ServerLevel level, Visit v) {
        return level.players().stream().anyMatch(p -> v.bounds().inflate(2).contains(p.position()));
    }

    private static void removeEntities(ServerLevel level, Visit v) {
        List<Entity> remove = new ArrayList<>();
        for (Entity entity : level.getAllEntities())
            if (!(entity instanceof Player) && (entity.getTags().contains(ENTITY_TAG + v.id) || v.bounds().contains(entity.position())))
                remove.add(entity);
        for (Entity entity : remove) entity.discard();
    }
}
