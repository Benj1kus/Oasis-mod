package com.benji.oasiso.client.light;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class OasisoLocalLights {
    private static final Map<BlockPos, Source> KNOWN = new HashMap<>();
    private static final List<Source> ACTIVE = new ArrayList<>();
    private static ClientLevel currentLevel;
    private static long ticks;
    private static int roundRobin;
    private static boolean failed;
    private static Matrix4f worldView, worldProjection;
    private static ClientLevel matrixLevel;

    private OasisoLocalLights() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != currentLevel) {
            KNOWN.clear();
            ACTIVE.clear();
            currentLevel = mc.level;
            ticks = 0;
            roundRobin = 0;
            worldView = worldProjection = null;
            matrixLevel = null;
        }
        if (mc.level == null || mc.player == null || !OasisoLightStyles.ENABLED) {
            KNOWN.clear();
            ACTIVE.clear();
            return;
        }
        if (mc.isPaused()) return;
        if ((ticks++ % 10L) == 0L) discover(mc);
        Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
        KNOWN.values().removeIf(s -> !mc.level.hasChunkAt(s.pos) || mc.level.getBlockState(s.pos).getBlock() != s.block);
        List<Source> nearest = new ArrayList<>(KNOWN.values());
        nearest.sort(Comparator.comparingDouble(s -> s.center.distanceToSqr(eye)));
        ACTIVE.clear();
        for (Source source : nearest) {
            if (source.center.distanceToSqr(eye) > 24.0 * 24.0) continue;
            ACTIVE.add(source);
            if (ACTIVE.size() >= OasisoLightStyles.MAX_LIGHTS) break;
        }
        for (int i = 0; i < OasisoLightStyles.RAYS_PER_TICK && !ACTIVE.isEmpty(); ++i) {
            Source source = ACTIVE.get(Math.floorMod(roundRobin++, ACTIVE.size()));
            if (source.cursor == 0 && source.ready && ticks - source.lastBuilt < 20) continue;
            source.traceOne(mc);
        }
    }

    private static void discover(Minecraft mc) {
        Map<Block, OasisoLightStyles.Style> styles = OasisoLightStyles.resolved();
        if (styles.isEmpty()) {
            KNOWN.clear();
            return;
        }
        var level = mc.level;
        BlockPos camera = BlockPos.containing(mc.gameRenderer.getMainCamera().getPosition());
        int radius = OasisoLightStyles.SEARCH_RADIUS;
        Set<BlockPos> found = new HashSet<>();
        BlockPos.MutableBlockPos test = new BlockPos.MutableBlockPos();
        int low = Math.max(0, (camera.getY() - radius - level.getMinBuildHeight()) >> 4);
        for (int cx = (camera.getX() - radius) >> 4; cx <= (camera.getX() + radius) >> 4; ++cx) {
            for (int cz = (camera.getZ() - radius) >> 4; cz <= (camera.getZ() + radius) >> 4; ++cz) {
                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                int high = Math.min(chunk.getSections().length - 1, (camera.getY() + radius - level.getMinBuildHeight()) >> 4);
                for (int sectionIndex = low; sectionIndex <= high; ++sectionIndex) {
                    var section = chunk.getSections()[sectionIndex];
                    if (section.hasOnlyAir() || !section.maybeHas(state -> styles.containsKey(state.getBlock())))
                        continue;
                    int by = level.getMinBuildHeight() + sectionIndex * 16;
                    for (int y = 0; y < 16; ++y)
                        for (int z = 0; z < 16; ++z)
                            for (int x = 0; x < 16; ++x) {
                                Block block = section.getBlockState(x, y, z).getBlock();
                                var style = styles.get(block);
                                if (style == null) continue;
                                test.set(cx * 16 + x, by + y, cz * 16 + z);
                                if (test.distSqr(camera) > radius * radius) continue;
                                BlockPos pos = test.immutable();
                                found.add(pos);
                                Source old = KNOWN.get(pos);
                                if (old == null || old.block != block || !old.style.equals(style))
                                    KNOWN.put(pos, new Source(pos, block, style));
                            }
                }
            }
        }
        KNOWN.keySet().retainAll(found);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            worldView = new Matrix4f(event.getPoseStack().last().pose());
            worldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
            matrixLevel = mc.level;
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Matrix4f view = worldView, projection = worldProjection;
        ClientLevel capturedLevel = matrixLevel;
        worldView = worldProjection = null;
        matrixLevel = null;
        if (failed || !OasisoLightStyles.ENABLED || mc.level == null || mc.level != currentLevel || view == null || projection == null || capturedLevel != mc.level || ACTIVE.isEmpty() || !OasisoLightShaders.ready())
            return;
        var camera = event.getCamera();
        List<Source> visible = new ArrayList<>();
        for (Source source : ACTIVE) {
            if (source.ready && mc.level.getBlockState(source.pos).getBlock() == source.block && event.getFrustum().isVisible(source.bounds))
                visible.add(source);
        }
        if (visible.isEmpty()) return;
        Matrix4f inverse = new Matrix4f(projection).mul(view).invert();
        if (!inverse.isFinite()) return;
        float time = ((mc.level.getGameTime() % 1_000_000L) + event.getPartialTick()) / 20.0F;
        try {
            OasisoLightPass.draw(visible, camera.getPosition(), inverse, time, ticks);
        } catch (RuntimeException exception) {
            failed = true;
        }
    }

    static void resetFailure() {
        failed = false;
    }

    static final class Source {
        final BlockPos pos;
        final Block block;
        final OasisoLightStyles.Style style;
        final Vec3 center;
        final AABB bounds;
        float[] shadow = new float[OasisoLightStyles.SHADOW_SIZE * OasisoLightStyles.SHADOW_SIZE * 6];
        float[] next = new float[shadow.length];
        int cursor;
        boolean ready;
        long lastBuilt, firstReady;

        Source(BlockPos pos, Block block, OasisoLightStyles.Style style) {
            this.pos = pos;
            this.block = block;
            this.style = style;
            center = new Vec3(pos.getX() + 0.5, pos.getY() + style.height(), pos.getZ() + 0.5);
            bounds = new AABB(center.x, center.y, center.z, center.x, center.y, center.z).inflate(style.radius());
        }

        void traceOne(Minecraft mc) {
            int size = OasisoLightStyles.SHADOW_SIZE;
            int face = cursor / (size * size), cell = cursor % (size * size);
            double u = (cell % size + 0.5) / size * 2 - 1, v = (cell / size + 0.5) / size * 2 - 1;
            Vec3 direction = switch (face) {
                case 0 -> new Vec3(1, -v, -u);
                case 1 -> new Vec3(-1, -v, u);
                case 2 -> new Vec3(u, 1, v);
                case 3 -> new Vec3(u, -1, -v);
                case 4 -> new Vec3(u, -v, 1);
                default -> new Vec3(-u, -v, -1);
            };
            Vec3 end = center.add(direction.normalize().scale(style.radius()));
            ClipContext context = new ClipContext(center, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player) {
                @Override
                public VoxelShape getBlockShape(BlockState state, BlockGetter level, BlockPos sample) {
                    return sample.equals(pos) ? Shapes.empty() : super.getBlockShape(state, level, sample);
                }
            };
            var hit = mc.level.clip(context);
            next[cursor++] = hit.getType() == HitResult.Type.MISS ? style.radius() : (float) center.distanceTo(hit.getLocation());
            if (cursor == next.length) {
                float[] swap = shadow;
                shadow = next;
                next = swap;
                cursor = 0;
                lastBuilt = ticks;
                if (!ready) firstReady = ticks;
                ready = true;
            }
        }
    }
}
