package com.benji.oasiso.client.factory;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.FactoryCoreBlockEntity;
import com.benji.oasiso.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class FactoryCoreVisuals {
    public static final int RANGE = 30, MAX_STREAMS = 64;

    public record Core(BlockPos pos, Matrix4f matrix, Vec3 center) {
    }

    public record Target(BlockPos pos, Vec3 point) {
    }

    public record Link(Vec3 start, Vec3 end, int seed) {
        public Vec3 center() {
            return start.add(end).scale(.5);
        }

        public double length() {
            return start.distanceTo(end);
        }
    }

    private static final Map<BlockPos, Core> CORES = new LinkedHashMap<>();
    private static final List<Target> TARGETS = new ArrayList<>();
    private static ClientLevel world;
    private static long scanAt, particleTick;
    private static int particles;

    private static void checkWorld() {
        ClientLevel current = Minecraft.getInstance().level;
        if (world != current) {
            world = current;
            CORES.clear();
            TARGETS.clear();
            scanAt = 0;
            particles = 0;
        }
    }

    @SubscribeEvent
    public static void frame(RenderLevelStageEvent e) {
        if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            checkWorld();
            CORES.clear();
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        checkWorld();
        var mc = Minecraft.getInstance();
        if (world == null || mc.isPaused() || mc.player == null || world.getGameTime() < scanAt) return;
        scanAt = world.getGameTime() + 20;
        TARGETS.clear();
        BlockPos camera = BlockPos.containing(mc.gameRenderer.getMainCamera().getPosition());
        int cx = camera.getX() >> 4, cz = camera.getZ() >> 4;
        for (int x = cx - 6; x <= cx + 6; x++)
            for (int z = cz - 6; z <= cz + 6; z++) {
                var chunk = world.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                for (var entry : chunk.getBlockEntities().entrySet()) {
                    var state = entry.getValue().getBlockState();
                    double height = state.is(ModBlocks.WINDMILL.get()) ? 3.4375 : state.is(ModBlocks.ENTROPY_WELL.get()) ? 2.5 : -1;
                    if (height > 0) {
                        var p = entry.getKey();
                        TARGETS.add(new Target(p.immutable(), new Vec3(p.getX() + .5, p.getY() + height, p.getZ() + .5)));
                    }
                }
            }
    }

    public static void capture(FactoryCoreBlockEntity b, Matrix4f matrix) {
        checkWorld();
        if (world == null || b.getLevel() != world) return;
        var pos = b.getBlockPos();
        var mc = Minecraft.getInstance();
        Vector3f p = matrix.transformPosition(new Vector3f());
        Vec3 center = new Vec3(pos.getX() + p.x, pos.getY() + p.y, pos.getZ() + p.z);
        if (center.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition()) > 64 * 64) return;
        CORES.put(pos, new Core(pos, matrix, center));
        long tick = world.getGameTime();
        if (mc.isPaused() || tick < b.nextLightning || center.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition()) > 32 * 32)
            return;
        b.nextLightning = tick + 20 + world.random.nextInt(21);
        if (particleTick != tick) {
            particleTick = tick;
            particles = 0;
        }
        for (int i = 0; i < 2 && particles < 16; i++, particles++) {
            Vector3f spark = new Vector3f((world.random.nextFloat() - .5F) * .8F, (world.random.nextFloat() - .5F) * .8F, (world.random.nextFloat() - .5F) * .8F);
            matrix.transformPosition(spark);
            world.addParticle(Oasiso.ENTROPY_LIGHTNING.get(), pos.getX() + spark.x, pos.getY() + spark.y, pos.getZ() + spark.z, 0, 0, 0);
        }
    }

    public static Collection<Core> cores() {
        checkWorld();
        return CORES.values();
    }

    public static List<Link> links(Vec3 camera) {
        checkWorld();
        List<Link> result = new ArrayList<>();
        if (world == null) return result;
        for (Core c : CORES.values())
            for (Target t : TARGETS) {
                if (c.pos.distSqr(t.pos) > RANGE * RANGE || !world.hasChunkAt(t.pos)) continue;
                var s = world.getBlockState(t.pos);
                if (!s.is(ModBlocks.WINDMILL.get()) && !s.is(ModBlocks.ENTROPY_WELL.get())) continue;
                if (c.center.distanceToSqr(t.point) < .01) continue;
                result.add(new Link(c.center, t.point, (int) mix(c.pos.asLong() ^ Long.rotateLeft(t.pos.asLong(), 23))));
            }
        result.sort(Comparator.comparingDouble(l -> l.center().distanceToSqr(camera)));
        if (result.size() > MAX_STREAMS) return new ArrayList<>(result.subList(0, MAX_STREAMS));
        return result;
    }

    public static float pulse(FactoryCoreBlockEntity b, float partial) {
        long tick = b.getLevel() == null ? 0 : b.getLevel().getGameTime();
        long seed = mix(b.getBlockPos().asLong());
        float age = Math.floorMod(tick + Math.floorMod(seed, 80), 80) + partial;
        return 1 + .14F * smooth(0, 3, age) * (1 - smooth(3, 23, age));
    }

    private static float smooth(float a, float b, float x) {
        float t = Math.max(0, Math.min(1, (x - a) / (b - a)));
        return t * t * (3 - 2 * t);
    }

    private static long mix(long x) {
        x = (x ^ (x >>> 30)) * 0xbf58476d1ce4e5b9L;
        x = (x ^ (x >>> 27)) * 0x94d049bb133111ebL;
        return x ^ (x >>> 31);
    }

    private FactoryCoreVisuals() {
    }
}
