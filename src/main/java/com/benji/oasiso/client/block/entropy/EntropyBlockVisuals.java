package com.benji.oasiso.client.block.entropy;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.EntropyBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EntropyBlockVisuals {
    public static final int CYCLE_TICKS = 80;
    public static final float PULSE_SIZE = 0.14F;
    public static final float SHAKE_SIZE = 0.045F;
    public static final int LIGHTNING_PERCENT = 22;
    private static final double SOUND_RANGE = 24.0;
    private static final int MAX_GROUP_SIZE = 2048;
    private static final Set<BlockPos> KNOWN = ConcurrentHashMap.newKeySet();
    private static final AtomicBoolean DIRTY = new AtomicBoolean();
    static final List<Group> GROUPS = new ArrayList<>();
    private static final Map<BlockPos, Long> LAST_CYCLE = new HashMap<>();
    private static ClientLevel world;
    private static int refresh;

    private EntropyBlockVisuals() {
    }

    public static void track(BlockPos pos) {
        if (KNOWN.add(pos.immutable())) DIRTY.set(true);
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (!event.getLevel().isClientSide()) return;
        KNOWN.clear();
        GROUPS.clear();
        LAST_CYCLE.clear();
        world = null;
        EntropyBlockRender.releaseTargets();
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != world) {
            world = mc.level;
            GROUPS.clear();
            LAST_CYCLE.clear();
            refresh = 0;
        }
        if (world == null || mc.isPaused()) return;
        if (DIRTY.getAndSet(false) | (refresh++ % 4 == 0)) rebuild();
        long now = world.getGameTime();
        Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
        Set<BlockPos> live = new HashSet<>();
        for (Group group : GROUPS) {
            live.add(group.anchor);
            long cycle = group.cycle(now);
            Long previous = LAST_CYCLE.put(group.anchor, cycle);
            if (previous == null || previous == cycle) continue;
            if (group.phase(now) > 2 || group.nearest(eye).distToCenterSqr(eye) > SOUND_RANGE * SOUND_RANGE) continue;
            boolean lightning = group.lightning(cycle);
            BlockPos source = group.nearest(eye);
            world.playLocalSound(source.getX() + .5, source.getY() + .5, source.getZ() + .5, lightning ? ModSounds.ENTROPY_LIGHTNING.get() : ModSounds.ENTROPY_PULSE.get(), SoundSource.BLOCKS, .85F, .96F + unit(group.seed ^ cycle) * .08F, false);
            if (lightning) spawnLightning(group, cycle);
        }
        LAST_CYCLE.keySet().retainAll(live);
    }

    private static boolean entropy(BlockPos pos) {
        return world.hasChunkAt(pos) && world.getBlockState(pos).getBlock() instanceof EntropyBlock;
    }

    private static void rebuild() {
        KNOWN.removeIf(pos -> !entropy(pos));
        Set<BlockPos> remaining = new HashSet<>(KNOWN);
        List<BlockPos> starts = new ArrayList<>(remaining);
        starts.sort(Comparator.comparingInt((BlockPos p) -> p.getX()).thenComparingInt(p -> p.getY()).thenComparingInt(p -> p.getZ()));
        GROUPS.clear();
        Set<BlockPos> assigned = new HashSet<>();
        for (BlockPos start : starts) {
            if (!remaining.remove(start)) continue;
            List<BlockPos> members = new ArrayList<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            Set<BlockPos> visited = new HashSet<>();
            visited.add(start);
            assigned.add(start);
            queue.add(start);
            while (!queue.isEmpty()) {
                BlockPos pos = queue.removeFirst();
                members.add(pos);
                for (Direction direction : Direction.values()) {
                    BlockPos next = pos.relative(direction);
                    if (visited.size() >= MAX_GROUP_SIZE || assigned.contains(next) || !entropy(next)) continue;
                    visited.add(next);
                    assigned.add(next);
                    queue.add(next);
                    remaining.remove(next);
                    KNOWN.add(next);
                }
            }
            GROUPS.add(new Group(members));
        }
    }

    private static void spawnLightning(Group group, long cycle) {
        Random random = new Random(mix(group.seed ^ cycle));
        for (int i = 0; i < 3; i++) {
            BlockPos pos = group.members.get(random.nextInt(group.members.size()));
            Direction face = Direction.UP;
            for (int attempt = 0; attempt < 24; attempt++) {
                BlockPos candidate = group.members.get(random.nextInt(group.members.size()));
                Direction direction = Direction.values()[random.nextInt(6)];
                BlockPos next = candidate.relative(direction);
                if (!group.cells.contains(next) && world.getBlockState(next).getCollisionShape(world, next).isEmpty()) {
                    pos = candidate;
                    face = direction;
                    break;
                }
            }
            Vec3 point = Vec3.atCenterOf(pos).add(face.getStepX() * .72, face.getStepY() * .72, face.getStepZ() * .72);
            point = point.add(face.getAxis() == Direction.Axis.X ? 0 : (random.nextDouble() - .5) * .75, face.getAxis() == Direction.Axis.Y ? 0 : (random.nextDouble() - .5) * .75, face.getAxis() == Direction.Axis.Z ? 0 : (random.nextDouble() - .5) * .75);
            world.addParticle(Oasiso.ENTROPY_LIGHTNING.get(), point.x, point.y, point.z, 0, 0, 0);
        }
    }

    static final class Group {
        final List<BlockPos> members;
        final Set<BlockPos> cells;
        final BlockPos anchor;
        final Vec3 center;
        final AABB bounds;
        final long seed;
        final int offset;

        Group(List<BlockPos> blocks) {
            members = List.copyOf(blocks);
            cells = Set.copyOf(blocks);
            anchor = blocks.stream().min(Comparator.comparingInt((BlockPos p) -> p.getX()).thenComparingInt(p -> p.getY()).thenComparingInt(p -> p.getZ())).orElseThrow();
            int minX = Integer.MAX_VALUE, minY = minX, minZ = minX, maxX = Integer.MIN_VALUE, maxY = maxX, maxZ = maxX;
            for (BlockPos p : blocks) {
                minX = Math.min(minX, p.getX());
                minY = Math.min(minY, p.getY());
                minZ = Math.min(minZ, p.getZ());
                maxX = Math.max(maxX, p.getX() + 1);
                maxY = Math.max(maxY, p.getY() + 1);
                maxZ = Math.max(maxZ, p.getZ() + 1);
            }
            bounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
            center = bounds.getCenter();
            seed = mix(anchor.asLong());
            offset = (int) Math.floorMod(seed, (long) CYCLE_TICKS);
        }

        long cycle(long tick) {
            return Math.floorDiv(tick + offset, CYCLE_TICKS);
        }

        int phase(long tick) {
            return (int) Math.floorMod(tick + offset, CYCLE_TICKS);
        }

        boolean lightning(long cycle) {
            return Math.floorMod(mix(seed ^ cycle), 100) < LIGHTNING_PERCENT;
        }

        BlockPos nearest(Vec3 point) {
            return members.stream().min(Comparator.comparingDouble(p -> p.distToCenterSqr(point))).orElse(anchor);
        }

        AABB renderBounds() {
            return bounds.inflate(Math.max(bounds.getXsize(), Math.max(bounds.getYsize(), bounds.getZsize())) * PULSE_SIZE + .15);
        }

        void transform(PoseStack pose, Vec3 eye, long tick, float partial) {
            float age = phase(tick) + partial;
            pose.translate(center.x - eye.x, center.y - eye.y, center.z - eye.z);
            if (lightning(cycle(tick))) {
                float envelope = (1 - smooth(0, 15, age)) * smooth(0, 1.2F, age);
                double t = age * 2.6;
                pose.translate(Math.sin(t + unit(seed) * 6) * SHAKE_SIZE * envelope, Math.sin(t * 1.31 + 2) * SHAKE_SIZE * envelope, Math.sin(t * 1.73 + 4) * SHAKE_SIZE * envelope);
            } else {
                float pulse = smooth(0, 3, age) * (1 - smooth(3, 23, age));
                float scale = 1 + PULSE_SIZE * pulse;
                pose.scale(scale, scale, scale);
            }
        }
    }

    static float smooth(float lo, float hi, float x) {
        float t = Mth.clamp((x - lo) / (hi - lo), 0, 1);
        return t * t * (3 - 2 * t);
    }

    static long mix(long x) {
        x = (x ^ (x >>> 30)) * 0xbf58476d1ce4e5b9L;
        x = (x ^ (x >>> 27)) * 0x94d049bb133111ebL;
        return x ^ (x >>> 31);
    }

    private static float unit(long x) {
        return (mix(x) >>> 40) / (float) (1 << 24);
    }
}
