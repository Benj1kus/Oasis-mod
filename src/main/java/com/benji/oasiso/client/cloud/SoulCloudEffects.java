package com.benji.oasiso.client.cloud;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.SoulCloudBlock;
import com.benji.oasiso.client.soul.VibritSoulEffects;
import com.benji.oasiso.client.soul.VibritSoulShaders;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class SoulCloudEffects {
    public static final int BEAM_RADIUS = 40, AMBIENT_RADIUS = 40;
    public static final float BEAM_HEIGHT = 40F;
    public static final float OUTER_WIDTH = .32F, INNER_WIDTH = .115F;
    public static final int CYAN = 0x07F09D;
    public static final int SCAN_BUDGET = 4096, MAX_RIBBONS = 96, MAX_SOULS = 24;
    private static final int DIAMETER = BEAM_RADIUS * 2 + 1;
    private static final int SCAN_VOLUME = DIAMETER * DIAMETER * DIAMETER;
    private static final BufferBuilder BUFFER = new BufferBuilder(262144);
    public static final int BEAM_FADE_TICKS = 20;
    private static final Set<BlockPos> LIT_BEAMS = new HashSet<>();
    private static final Map<BlockPos, Long> FADING_BEAMS = new HashMap<>();
    private static final Set<BlockPos> SOURCES = new HashSet<>();
    private static final List<Flight> RIBBONS = new ArrayList<>(), SOULS = new ArrayList<>();
    private static ClientLevel world;
    private static BlockPos scanCenter;
    private static int cursor;
    private static long clock;
    private static Matrix4f view, projection;

    private record Flight(Vec3 origin, long birth, int life, int seed, float rise, float width, float length,
                          boolean white) {
    }

    private static void clear() {
        SOURCES.clear();
        LIT_BEAMS.clear();
        FADING_BEAMS.clear();
        RIBBONS.clear();
        SOULS.clear();
        world = null;
        scanCenter = null;
        cursor = 0;
        clock = 0;
        view = projection = null;
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() == world) clear();
    }

    private static boolean exposed(BlockPos pos) {
        if (world == null || !world.hasChunkAt(pos) || !(world.getBlockState(pos).getBlock() instanceof SoulCloudBlock))
            return false;
        BlockPos above = pos.above();
        var state = world.getBlockState(above);
        return !(state.getBlock() instanceof SoulCloudBlock) && state.getCollisionShape(world, above).isEmpty() && world.getFluidState(above).isEmpty();
    }

    private static boolean active(BlockPos pos) {
        return exposed(pos) && !world.getBlockState(pos).getValue(SoulCloudBlock.OFF);
    }

    private static float beamPower(BlockPos pos, double now) {
        if (!world.getBlockState(pos).getValue(SoulCloudBlock.OFF)) {
            LIT_BEAMS.add(pos);
            FADING_BEAMS.remove(pos);
            return 1F;
        }
        if (LIT_BEAMS.remove(pos)) FADING_BEAMS.put(pos, clock);
        Long started = FADING_BEAMS.get(pos);
        return started == null ? 0F : 1F - CloudMotion.smooth((float) ((now - started) / BEAM_FADE_TICKS));
    }

    private static Vec3 top(BlockPos p) {
        return new Vec3(p.getX() + .5, p.getY() + 1.01, p.getZ() + .5);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            if (world != null) clear();
            return;
        }
        if (world != mc.level) {
            clear();
            world = mc.level;
        }
        if (mc.isPaused()) return;
        clock++;
        Vec3 player = mc.player.position();
        BlockPos center = mc.player.blockPosition();
        if (scanCenter == null || scanCenter.distSqr(center) > 32 * 32) {
            SOURCES.clear();
            LIT_BEAMS.clear();
            FADING_BEAMS.clear();
            RIBBONS.clear();
            SOULS.clear();
            scanCenter = center;
            cursor = 0;
        }
        for (int i = 0; i < SCAN_BUDGET; i++) {
            if (cursor >= SCAN_VOLUME) {
                cursor = 0;
                scanCenter = center;
            }
            int n = cursor++;
            BlockPos pos = scanCenter.offset(n % DIAMETER - BEAM_RADIUS, n / DIAMETER % DIAMETER - BEAM_RADIUS, n / (DIAMETER * DIAMETER) - BEAM_RADIUS);
            if (pos.getY() < world.getMinBuildHeight() || pos.getY() >= world.getMaxBuildHeight()) continue;
            if (top(pos).distanceToSqr(player) <= BEAM_RADIUS * BEAM_RADIUS && exposed(pos)) SOURCES.add(pos);
            else SOURCES.remove(pos);
        }
        if (clock % 10 == 0)
            SOURCES.removeIf(p -> top(p).distanceToSqr(player) > BEAM_RADIUS * BEAM_RADIUS || !exposed(p));
        LIT_BEAMS.removeIf(p -> !exposed(p) || top(p).distanceToSqr(player) >= BEAM_RADIUS * BEAM_RADIUS);
        FADING_BEAMS.entrySet().removeIf(e -> clock - e.getValue() >= BEAM_FADE_TICKS || !exposed(e.getKey()) || top(e.getKey()).distanceToSqr(player) >= BEAM_RADIUS * BEAM_RADIUS);
        RIBBONS.removeIf(f -> clock - f.birth >= f.life);
        SOULS.removeIf(f -> clock - f.birth >= f.life);
        var random = world.random;
        boolean ribbon = RIBBONS.size() < MAX_RIBBONS && random.nextFloat() < .07F;
        boolean soul = SOULS.size() < MAX_SOULS && random.nextFloat() < .035F;
        if (!ribbon && !soul) return;
        List<BlockPos> nearby = SOURCES.stream().filter(p -> top(p).distanceToSqr(player) <= AMBIENT_RADIUS * AMBIENT_RADIUS && active(p)).toList();
        if (nearby.isEmpty()) return;
        if (ribbon) emit(nearby.get(random.nextInt(nearby.size())), false);
        if (soul) emit(nearby.get(random.nextInt(nearby.size())), true);
    }

    private static void emit(BlockPos block, boolean soul) {
        var r = world.random;
        Vec3 origin = top(block).add((r.nextDouble() - .5) * .7, 0, (r.nextDouble() - .5) * .7);
        float ribbonSize = .6F + r.nextFloat() * 1.4F;

        Flight flight = new Flight(origin, clock, soul ? 100 + r.nextInt(41) : 55 + r.nextInt(36), r.nextInt(), soul ? 6 + r.nextFloat() * 3 : 2.5F + r.nextFloat() * 2, soul ? .05F + r.nextFloat() * .09F : .075F * ribbonSize, soul ? .35F + r.nextFloat() * .65F : .95F * ribbonSize, r.nextBoolean());
        (soul ? SOULS : RIBBONS).add(flight);
    }

    private static Vec3 point(Flight f, double age, boolean soul) {
        var p = CloudMotion.sample(f.seed, age / f.life, f.rise, soul);
        return f.origin.add(p.x(), p.y(), p.z());
    }

    private static Vector3f toView(Vec3 p, Matrix4f matrix, Vec3 camera) {
        return matrix.transformPosition(new Vector3f((float) (p.x - camera.x), (float) (p.y - camera.y), (float) (p.z - camera.z)));
    }

    private static float distanceFade(Vec3 origin, Vec3 player, int radius) {
        double distance = origin.distanceTo(player);
        return 1 - CloudMotion.smooth((float) ((distance - radius + 2) / 2));
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGH)
    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (world == null || mc.level != world || mc.player == null) return;
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            view = new Matrix4f(event.getPoseStack().last().pose());
            projection = new Matrix4f(RenderSystem.getProjectionMatrix());
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;
        Matrix4f matrix = view, proj = projection;
        view = projection = null;
        if (matrix == null || proj == null) return;
        Vec3 camera = event.getCamera().getPosition(), player = mc.player.position();
        float partial = mc.isPaused() ? 0 : event.getPartialTick();
        double now = clock + partial;
        try (CloudRenderState ignored = new CloudRenderState()) {
            mc.getMainRenderTarget().bindWrite(true);
            RenderSystem.disableScissor();
            RenderSystem.colorMask(true, true, true, false);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            var shader = SoulCloudShaders.shader;
            if (shader != null) {
                shader.safeGetUniform("CloudProjection").set(proj);
                shader.safeGetUniform("CloudKind").set(0F);
                RenderSystem.setShader(() -> shader);
                BUFFER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
                // Stable back-to-front order for translucent overlapping columns.
                List<BlockPos> beams = SOURCES.stream().filter(p -> exposed(p) && world.getBlockState(p).getValue(SoulCloudBlock.SOULS) && (!world.getBlockState(p).getValue(SoulCloudBlock.OFF) || LIT_BEAMS.contains(p) || FADING_BEAMS.containsKey(p)) && top(p).distanceToSqr(player) < BEAM_RADIUS * BEAM_RADIUS).sorted(Comparator.<BlockPos>comparingDouble(p -> top(p).distanceToSqr(camera)).reversed().thenComparingLong(BlockPos::asLong)).toList();
                for (BlockPos pos : beams) {
                    Vec3 base = top(pos);
                    long hash = pos.asLong() * 6364136223846793005L;
                    double period = 11 + (hash & 15), offset = ((hash >>> 16) & 65535) / 65535D * period;
                    double cycle = (now / 20D + offset) % period;
                    float pulse = cycle < 1.8 ? (float) Math.pow(Math.sin(cycle / 1.8 * Math.PI), 2) : 0;
                    float fade = distanceFade(base, player, BEAM_RADIUS) * beamPower(pos, now);
                    beam(base, OUTER_WIDTH * (1 + pulse * .12F), CYAN, (.27F + pulse * .08F) * fade, matrix, camera);
                    beam(base, INNER_WIDTH * (1 + pulse * .06F), 0xFFFFFF, (.83F + pulse * .12F) * fade, matrix, camera);
                }
                BufferUploader.drawWithShader(BUFFER.end());
                shader.safeGetUniform("CloudKind").set(1F);
                BUFFER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
                for (Flight f : RIBBONS) {
                    float age = (float) (now - f.birth);
                    if (age < 0 || age >= f.life) continue;
                    float alpha = CloudMotion.opacity(age, f.life) * distanceFade(f.origin, player, AMBIENT_RADIUS) * .8F;
                    if (alpha <= .005F) continue;
                    drawRibbon(f, age, alpha, matrix, camera);
                }
                BufferUploader.drawWithShader(BUFFER.end());
            }
            if (VibritSoulShaders.shader != null && !SOULS.isEmpty()) {
                var soulShader = VibritSoulShaders.shader;
                soulShader.safeGetUniform("SoulProjection").set(proj);
                RenderSystem.setShader(() -> soulShader);
                RenderSystem.setShaderTexture(0, VibritSoulEffects.HEAD);
                BUFFER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
                for (Flight f : SOULS) drawSoul(f, now, player, matrix, camera);
                BufferUploader.drawWithShader(BUFFER.end());
            }
        }
    }

    private static void beam(Vec3 base, float width, int color, float alpha, Matrix4f matrix, Vec3 camera) {
        double h = width * .5;
        Vec3[] corners = {base.add(-h, 0, -h), base.add(h, 0, -h), base.add(h, 0, h), base.add(-h, 0, h)};
        for (int face = 0; face < 4; face++) {
            Vec3 a = corners[face], b = corners[(face + 1) % 4];
            vertex(toView(a, matrix, camera), 0, 0, color, alpha);
            vertex(toView(b, matrix, camera), 1, 0, color, alpha);
            vertex(toView(b.add(0, BEAM_HEIGHT, 0), matrix, camera), 1, 1, color, alpha);
            vertex(toView(a.add(0, BEAM_HEIGHT, 0), matrix, camera), 0, 1, color, alpha);
        }
    }

    private static void drawRibbon(Flight f, float age, float alpha, Matrix4f matrix, Vec3 camera) {
        final int segments = 24;

        Vector3f[] points = new Vector3f[segments + 1];
        Vector3f[] sides = new Vector3f[segments + 1];
        double trailTicks = Math.min(f.life * .65, f.length / f.rise * f.life);

        double first = Math.max(0, age - trailTicks);

        for (int i = 0; i <= segments; i++) {
            double t = i / (double) segments;
            double pathAge = first + (age - first) * t;
            points[i] = toView(point(f, pathAge, true), matrix, camera);
        }

        Vector3f previous = new Vector3f(1, 0, 0);

        for (int i = 0; i <= segments; i++) {
            Vector3f tangent = new Vector3f(points[Math.min(segments, i + 1)]).sub(points[Math.max(0, i - 1)]);

            Vector3f side = new Vector3f(-tangent.y, tangent.x, 0);

            if (side.lengthSquared() < .000001F) side.set(previous);
            else side.normalize();

            if (side.dot(previous) < 0) side.negate();

            previous.set(side);
            sides[i] = side.mul(f.width * .5F);
        }

        int color = f.white ? 0xFFFFFF : CYAN;

        for (int i = 0; i < segments; i++) {
            float a = i / (float) segments;
            float b = (i + 1F) / segments;

            vertex(new Vector3f(points[i]).sub(sides[i]), 0, a, color, alpha);
            vertex(new Vector3f(points[i]).add(sides[i]), 1, a, color, alpha);
            vertex(new Vector3f(points[i + 1]).add(sides[i + 1]), 1, b, color, alpha);
            vertex(new Vector3f(points[i + 1]).sub(sides[i + 1]), 0, b, color, alpha);
        }
    }

    private static void vertex(Vector3f p, float u, float v, int color, float alpha) {
        BUFFER.vertex(p.x, p.y, p.z).color((color >> 16) & 255, (color >> 8) & 255, color & 255, Math.max(0, Math.min(255, (int) (alpha * 255)))).uv(u, v).endVertex();
    }

    private static void soulVertex(Vector3f p, float u, float v, float progress, int kind, int seed, float alpha) {
        int encoded = ((Math.max(0, Math.min(255, (int) (progress * 255)))) << 16) | (kind << 8) | (seed & 255);
        vertex(p, u, v, encoded, alpha);
    }

    private static void drawSoul(Flight f, double now, Vec3 player, Matrix4f matrix, Vec3 camera) {
        float age = (float) (now - f.birth), progress = age / f.life;
        if (age < 0 || progress >= 1) return;
        float alpha = CloudMotion.opacity(age, f.life) * distanceFade(f.origin, player, AMBIENT_RADIUS);
        if (alpha <= .005F) return;
        drawChips(f, age, progress, alpha, matrix, camera);
        final int segments = 32;

        Vector3f[] points = new Vector3f[segments + 1], sides = new Vector3f[segments + 1];

        double first = Math.max(0, age - 36);
        for (int i = 0; i <= segments; i++)
            points[i] = toView(point(f, first + (age - first) * i / segments, true), matrix, camera);
        Vector3f previous = new Vector3f(1, 0, 0);
        for (int i = 0; i <= segments; i++) {
            Vector3f tangent = new Vector3f(points[Math.min(segments, i + 1)]).sub(points[Math.max(0, i - 1)]);
            Vector3f side = new Vector3f(-tangent.y, tangent.x, 0);
            if (side.lengthSquared() < .000001F) side.set(previous);
            else side.normalize();
            if (side.dot(previous) < 0) side.negate();
            previous.set(side);
            sides[i] = side.mul(.23F * (.55F + .45F * i / segments));
        }
        for (int i = 0; i < segments; i++) {
            float a = i / (float) segments, b = (i + 1F) / segments;
            soulVertex(new Vector3f(points[i]).sub(sides[i]), 0, a, progress, 0, f.seed, alpha);
            soulVertex(new Vector3f(points[i]).add(sides[i]), 1, a, progress, 0, f.seed, alpha);
            soulVertex(new Vector3f(points[i + 1]).add(sides[i + 1]), 1, b, progress, 0, f.seed, alpha);
            soulVertex(new Vector3f(points[i + 1]).sub(sides[i + 1]), 0, b, progress, 0, f.seed, alpha);
        }
        Vector3f head = points[segments];
        Vector3f up = new Vector3f(head).sub(points[segments - 1]);
        up.z = 0;
        if (up.lengthSquared() < .000001F) up.set(0, 1, 0);
        else up.normalize();
        Vector3f side = new Vector3f(up.y, -up.x, 0).mul(.29F);
        up.mul(.29F);
        soulVertex(new Vector3f(head).sub(side).add(up), 0, 0, progress, 255, f.seed, alpha);
        soulVertex(new Vector3f(head).sub(side).sub(up), 0, 1, progress, 255, f.seed, alpha);
        soulVertex(new Vector3f(head).add(side).sub(up), 1, 1, progress, 255, f.seed, alpha);
        soulVertex(new Vector3f(head).add(side).add(up), 1, 0, progress, 255, f.seed, alpha);
    }

    private static void drawChips(Flight f, float age, float progress, float alpha, Matrix4f matrix, Vec3 camera) {
        int last = (int) Math.floor(age / 3);
        int first = Math.max(4, (int) Math.ceil((age - 14) / 3));
        for (int emission = first; emission <= last; emission++) {
            float elapsed = age - emission * 3;
            int key = f.seed ^ emission * 73428767;
            float rx = noise(key), ry = noise(key + 1), rz = noise(key + 2);
            Vec3 origin = point(f, Math.max(0, emission * 3 - 12), true);
            Vec3 centre = origin.add((rx - .5) * (.20 + elapsed * .02), .04 + elapsed * (.01 + ry * .015), (rz - .5) * (.20 + elapsed * .02));
            float fade = CloudMotion.smooth(elapsed / 2) * (1 - CloudMotion.smooth(elapsed / 14));
            cube(centre, .025F + .035F * ry, rx * 6.28F + elapsed * .03F, rz + elapsed * .02F, progress, alpha * fade, matrix, camera);
        }
    }

    private static float noise(int key) {
        key ^= key >>> 16;
        key *= 0x7feb352d;
        key ^= key >>> 15;
        key *= 0x846ca68b;
        key ^= key >>> 16;
        return (key & 0xFFFFFF) / 16777216F;
    }

    private static final int[][] CUBE_FACES = {{0, 4, 6, 2}, {1, 3, 7, 5}, {0, 1, 5, 4}, {2, 6, 7, 3}, {0, 2, 3, 1}, {4, 5, 7, 6}};
    private static final int[] CUBE_SHADE = {170, 210, 145, 255, 180, 225};

    private static void cube(Vec3 centre, float size, float yaw, float pitch, float progress, float alpha, Matrix4f matrix, Vec3 camera) {
        if (alpha < .01F) return;
        Vector3f[] corners = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            Vector3f offset = new Vector3f((i & 1) == 0 ? -size : size, (i & 2) == 0 ? -size : size, (i & 4) == 0 ? -size : size).rotateX(pitch).rotateY(yaw);
            corners[i] = toView(centre.add(offset.x, offset.y, offset.z), matrix, camera);
        }
        for (int face = 0; face < 6; face++) {
            int[] q = CUBE_FACES[face];
            Vector3f normal = new Vector3f(corners[q[1]]).sub(corners[q[0]]).cross(new Vector3f(corners[q[2]]).sub(corners[q[0]]));
            if (normal.dot(corners[q[0]]) >= 0) continue;
            soulVertex(corners[q[0]], 0, 0, progress, 128, CUBE_SHADE[face], alpha);
            soulVertex(corners[q[1]], 1, 0, progress, 128, CUBE_SHADE[face], alpha);
            soulVertex(corners[q[2]], 1, 1, progress, 128, CUBE_SHADE[face], alpha);
            soulVertex(corners[q[3]], 0, 1, progress, 128, CUBE_SHADE[face], alpha);
        }
    }

    private SoulCloudEffects() {
    }
}
