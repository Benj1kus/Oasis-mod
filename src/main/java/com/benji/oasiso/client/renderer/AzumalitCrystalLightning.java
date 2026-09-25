package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.AzumalitCrystalBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.io.IOException;
import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class AzumalitCrystalLightning {

    public static final boolean DEBUG_ARCS = true;
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static int stages, draws;
    private static Matrix4f worldView, worldProjection;
    private static ClientLevel matrixWorld;
    private static final double VIEW_RANGE = 48.0;
    private static final int MAX_VISIBLE_ARCS = 12;
    private static final float BOLT_WIDTH = .10F;
    private static final List<AzumalitCrystalBlockEntity> CRYSTALS = new ArrayList<>();
    private static final Map<AzumalitCrystalBlockEntity, Geometry> CACHE = new WeakHashMap<>();
    private static final MultiBufferSource.BufferSource BUFFER = MultiBufferSource.immediate(new BufferBuilder(8192));
    private static ClientLevel world;
    private static int ticks;
    private static ShaderInstance shader;

    private AzumalitCrystalLightning() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != world) {
            world = mc.level;
            CRYSTALS.clear();
            CACHE.clear();
            ticks = 0;
            stages = draws = 0;
            worldView = worldProjection = null;
            matrixWorld = null;
        }
        if (world == null || mc.player == null || mc.isPaused()) return;
        if (ticks++ % 5 != 0) return;
        CRYSTALS.clear();
        Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
        BlockPos camera = BlockPos.containing(eye);

        for (int x = (camera.getX() - 48) >> 4; x <= (camera.getX() + 48) >> 4; x++) {
            for (int z = (camera.getZ() - 48) >> 4; z <= (camera.getZ() + 48) >> 4; z++) {
                var chunk = world.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                for (var be : chunk.getBlockEntities().values()) {
                    if (be instanceof AzumalitCrystalBlockEntity crystal && !crystal.isRemoved() && Vec3.atCenterOf(crystal.getBlockPos()).distanceToSqr(eye) < VIEW_RANGE * VIEW_RANGE) {
                        CRYSTALS.add(crystal);
                    }
                }
            }
        }
        // log debug
        CACHE.keySet().retainAll(CRYSTALS);
        if (DEBUG_ARCS && ticks % 200 == 1) {
            long valid = CRYSTALS.stream().filter(c -> c.strikeStart() >= 0 && c.path().size() >= 2).count();
            long active = CRYSTALS.stream().filter(c -> c.visualAge(0) < AzumalitCrystalBlockEntity.LIFE_TICKS).count();
            LOGGER.info("[AzumalitArc] shader={}, crystals={}, withPath={}, active={}, stages={}, draws={}", shader != null, CRYSTALS.size(), valid, active, stages, draws);
            for (var c : CRYSTALS)
                if (c.strikeStart() >= 0) {
                    LOGGER.info("[AzumalitArc] pos={}, points={}, serverStart={}, clientTime={}, visualAge={}", c.getBlockPos(), c.path().size(), c.strikeStart(), world.getGameTime(), c.visualAge(0));
                    break;
                }
            stages = draws = 0;
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            worldView = new Matrix4f(event.getPoseStack().last().pose());
            worldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
            matrixWorld = mc.level;
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        stages++;
        Matrix4f view = worldView, projection = worldProjection;
        ClientLevel capturedWorld = matrixWorld;
        worldView = worldProjection = null;
        matrixWorld = null;
        if (shader == null || world == null || world != mc.level || capturedWorld != world || view == null || projection == null)
            return;

        Vec3 eye = event.getCamera().getPosition();
        float partial = mc.isPaused() ? mc.getFrameTime() : event.getPartialTick();

        List<AzumalitCrystalBlockEntity> active = new ArrayList<>();
        for (var crystal : CRYSTALS) {
            if (crystal.isRemoved() || crystal.getLevel() != world || crystal.strikeStart() < 0) continue;
            float age = crystal.visualAge(partial);
            if (age >= 0 && age < AzumalitCrystalBlockEntity.LIFE_TICKS && crystal.path().size() >= 2 && event.getFrustum().isVisible(crystal.getRenderBoundingBox())) {
                active.add(crystal);
            }
        }
        if (active.isEmpty()) return;

        active.sort(Comparator.comparingDouble(c -> Vec3.atCenterOf(c.getBlockPos()).distanceToSqr(eye)));

        shader.safeGetUniform("ArcProjection").set(projection);
        ShaderInstance previousShader = RenderSystem.getShader();
        VertexConsumer out = BUFFER.getBuffer(ArcRenderType.ARC);
        try {
            for (int i = 0; i < Math.min(MAX_VISIBLE_ARCS, active.size()); i++) {
                var crystal = active.get(i);
                Geometry geometry = CACHE.get(crystal);
                if (geometry == null || geometry.start != crystal.strikeStart() || geometry.seed != crystal.seed()) {
                    geometry = new Geometry(crystal);
                    CACHE.put(crystal, geometry);
                }
                float age = crystal.visualAge(partial);
                draw(out, view, eye, geometry, age);
                draws++;
            }
        } finally {
            BUFFER.endBatch(ArcRenderType.ARC);
            if (previousShader != null) RenderSystem.setShader(() -> previousShader);
        }
    }

    private static void draw(VertexConsumer out, Matrix4f view, Vec3 eye, Geometry g, float age) {
        int ageByte = Math.round(Mth.clamp(age / AzumalitCrystalBlockEntity.LIFE_TICKS, 0F, 1F) * 255);
        int seedByte = (int) (g.seed & 255);
        float distanceFade = Mth.clamp((48F - (float) g.points.get(0).distanceTo(eye)) / 10F, 0, 1);
        int alpha = Math.round(distanceFade * 255);
        if (alpha <= 0) return;

        float growth = Mth.clamp(age / AzumalitCrystalBlockEntity.IMPACT_TICKS, 0, 1);
        double reached = g.length * growth;
        double walked = 0;

        if (age < 14) {
            for (int i = 1; i < g.points.size(); i++) {
                Vec3 a = g.points.get(i - 1), b = g.points.get(i);
                double length = a.distanceTo(b);
                if (length < .0001) continue;
                double portion = Mth.clamp((reached - walked) / length, 0, 1);
                if (portion <= 0) break;
                ribbon(out, view, eye, a, a.lerp(b, portion), BOLT_WIDTH, (float) (walked / g.length), (float) ((walked + length * portion) / g.length), ageByte, 0, seedByte, alpha);
                walked += length;
            }

            for (Branch branch : g.branches) {
                if (growth > branch.at) {
                    ribbon(out, view, eye, branch.from, branch.to, .14F, branch.at, branch.at + .08F, ageByte, 64, seedByte, alpha);
                }
            }
        }

        float since = age - AzumalitCrystalBlockEntity.IMPACT_TICKS;
        if (since < 0) return;

        if (since < 10) {
            float radius = .12F + .72F * Mth.clamp(since / 7F, 0, 1);
            quad(out, view, eye, g.impact, g.u.scale(radius), g.v.scale(radius), ageByte, 255, seedByte, alpha);
        }

        for (Spark spark : g.sparks) {
            float local = since - spark.delay;
            if (local < 0 || local > spark.life) continue;
            float seconds = local / 20F;
            Vec3 point = spark.origin.add(spark.velocity.scale(seconds)).add(0, -.50 * seconds * seconds, 0);
            Vec3 direction = spark.velocity.normalize();
            float shrink = 1 - Mth.clamp(local / spark.life, 0, 1);
            float length = spark.size * (.6F + .4F * shrink);
            int opacity = Math.round(alpha * shrink);
            ribbon(out, view, eye, point, point.add(direction.scale(length)), length * .62F, 0, 1, ageByte, 170, seedByte, opacity);
        }
    }

    private static void ribbon(VertexConsumer out, Matrix4f view, Vec3 eye, Vec3 a, Vec3 b, float width, float v0, float v1, int age, int kind, int seed, int alpha) {
        Vec3 direction = b.subtract(a);
        if (direction.lengthSqr() < .0000001) return;
        Vec3 side = direction.cross(a.lerp(b, .5).subtract(eye));
        if (side.lengthSqr() < .0000001) side = direction.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < .0000001) side = direction.cross(new Vec3(1, 0, 0));
        side = side.normalize().scale(width * .5);

        vertex(out, view, eye, a.subtract(side), 0, v0, age, kind, seed, alpha);
        vertex(out, view, eye, a.add(side), 1, v0, age, kind, seed, alpha);
        vertex(out, view, eye, b.add(side), 1, v1, age, kind, seed, alpha);
        vertex(out, view, eye, b.subtract(side), 0, v1, age, kind, seed, alpha);
    }

    private static void quad(VertexConsumer out, Matrix4f view, Vec3 eye, Vec3 center, Vec3 u, Vec3 v, int age, int kind, int seed, int alpha) {
        vertex(out, view, eye, center.subtract(u).subtract(v), 0, 0, age, kind, seed, alpha);
        vertex(out, view, eye, center.add(u).subtract(v), 1, 0, age, kind, seed, alpha);
        vertex(out, view, eye, center.add(u).add(v), 1, 1, age, kind, seed, alpha);
        vertex(out, view, eye, center.subtract(u).add(v), 0, 1, age, kind, seed, alpha);
    }

    private static void vertex(VertexConsumer out, Matrix4f view, Vec3 eye, Vec3 point, float u, float v, int age, int kind, int seed, int alpha) {
        out.vertex(view, (float) (point.x - eye.x), (float) (point.y - eye.y), (float) (point.z - eye.z)).color(age, kind, seed, alpha).uv(u, v).endVertex();
    }

    private record Branch(Vec3 from, Vec3 to, float at) {
    }

    private record Spark(Vec3 origin, Vec3 velocity, float size, float delay, float life) {
    }

    private static final class Geometry {
        final long seed, start;
        final List<Vec3> points;
        final List<Branch> branches = new ArrayList<>();
        final List<Spark> sparks = new ArrayList<>();
        final Vec3 impact, u, v;
        final double length;

        Geometry(AzumalitCrystalBlockEntity crystal) {
            seed = crystal.seed();
            start = crystal.strikeStart();
            points = crystal.path();
            impact = points.get(points.size() - 1);
            Vec3 normal = Vec3.atLowerCornerOf(crystal.hitFace().getNormal());
            u = normal.cross(Math.abs(normal.y) > .9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
            v = normal.cross(u).normalize();
            RandomSource random = RandomSource.create(seed);
            double total = 0;
            for (int i = 1; i < points.size(); i++) total += points.get(i - 1).distanceTo(points.get(i));
            length = Math.max(.001, total);
            double walked = 0;
            for (int i = 1; i < points.size() - 1; i++) {
                walked += points.get(i - 1).distanceTo(points.get(i));
                Vec3 point = points.get(i);
                Vec3 randomDirection = new Vec3(random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1).normalize();
                if (i % 3 == 1)
                    branches.add(new Branch(point, point.add(randomDirection.scale(.18 + random.nextFloat() * .28)), (float) (walked / length)));
                sparks.add(new Spark(point, randomDirection.scale(.45 + random.nextFloat() * .65).add(0, .45, 0), .12F + random.nextFloat() * .12F, 1.0F + i * .16F, 12 + random.nextFloat() * 7));
            }
            for (int i = 0; i < 9; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                Vec3 radial = u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle)));
                Vec3 velocity = normal.scale(.6 + random.nextDouble() * .8).add(radial.scale(.5 + random.nextDouble() * 1.2));
                sparks.add(new Spark(impact, velocity, .12F + random.nextFloat() * .18F, random.nextFloat() * .8F, 12 + random.nextFloat() * 8));
            }
        }
    }
//debug=check
    public static void registerArcShader(RegisterShadersEvent event) throws IOException {
        LOGGER.info("[AzumalitArc] RegisterShadersEvent received");

        shader = null;
        CACHE.clear();
        worldView = worldProjection = null;
        matrixWorld = null;

        event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "azumalit_crystal_arc"), DefaultVertexFormat.POSITION_COLOR_TEX), loaded -> {
            shader = loaded;

            LOGGER.info("[AzumalitArc] Shader assigned; ArcProjection={}", loaded.getUniform("ArcProjection") != null);
        });
    }

    private static final class ArcRenderType extends RenderType {
        private static final RenderType ARC = create("oasiso_azumalit_arc", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 8192, false, true, CompositeState.builder().setShaderState(new ShaderStateShard(() -> shader)).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));

        private ArcRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort, Runnable setup, Runnable clear) {
            super(name, format, mode, size, crumbling, sort, setup, clear);
        }
    }
}