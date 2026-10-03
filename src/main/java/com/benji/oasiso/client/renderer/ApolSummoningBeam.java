package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ApolSummoningEntity;
import com.benji.oasiso.common.block.entity.ApolAltarBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class ApolSummoningBeam {
    public static final float RADIUS = 6, HEIGHT = 10, DURATION = 120;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation SHADER_ID = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "apol_summoning_world");
    private static ShaderInstance shader;
    private static boolean attemptedLazyLoad, ownsShader;
    private static ClientLevel world;
    private static final BufferBuilder BUILDER = new BufferBuilder(262144);
    private static final LinkedHashMap<Long, Beam> BEAMS = new LinkedHashMap<>();
    private static final Set<Long> DRAWN = new HashSet<>();

    private record Beam(Vec3 base, long start, int seed) {
    }

    private static Matrix4f frameView, frameProjection;
    private static Vec3 frameCamera;

    private static void checkWorld() {
        ClientLevel current = Minecraft.getInstance().level;
        if (world != current) {
            world = current;
            BEAMS.clear();
            DRAWN.clear();
            frameView = frameProjection = null;
            frameCamera = null;
        }
    }

    private static void remember(long key, Vec3 base, long start, boolean fromAltar) {
        if (world == null || start < 0) return;
        Beam old = BEAMS.get(key);
        if (old == null || start > old.start + 120) {
            if (BEAMS.size() >= 64 && old == null) {
                Long first = BEAMS.keySet().iterator().next();
                BEAMS.remove(first);
                DRAWN.remove(first);
            }
            BEAMS.put(key, new Beam(base, start, (int) ((key ^ (key >>> 32)) & 1023)));
            DRAWN.remove(key);
            LOGGER.info("[ApolBeam] tracked {} at {}, start={}", fromAltar ? "altar" : "ritual", base, start);
        }
    }

    public static void trackAltar(ApolAltarBlockEntity altar) {
        checkWorld();
        if (world == null || altar.getLevel() != world || !altar.isActivated()) return;
        remember(altar.getBlockPos().asLong(), Vec3.atBottomCenterOf(altar.getBlockPos()).add(0, .03, 0), altar.getActivationTime() + 20, true);
    }

    public static void render(ApolSummoningEntity ritual, float partial, PoseStack ignored) {
        trackRitual(ritual);
    }

    private static void trackRitual(ApolSummoningEntity ritual) {
        checkWorld();
        if (world == null || ritual.level() != world || !ritual.settled()) return;
        long start = ritual.beamStartTime();
        remember(ritual.raisedAltar().asLong(), ritual.beamBase(), start, false);
    }

    private static boolean ensureShader() {
        if (shader != null) return true;
        if (attemptedLazyLoad) return false;
        attemptedLazyLoad = true;
        try {
            shader = new ShaderInstance(Minecraft.getInstance().getResourceManager(), SHADER_ID, DefaultVertexFormat.POSITION_COLOR_TEX);
            ownsShader = true;
            LOGGER.info("[ApolBeam] shader loaded on demand: {}", SHADER_ID);
            return true;
        } catch (IOException | RuntimeException error) {
            LOGGER.error("[ApolBeam] shader could not load; check assets/oasiso/shaders/core/apol_summoning_world.*", error);
            return false;
        }
    }

    @SubscribeEvent
    public static void drawWorld(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            checkWorld();
            frameView = new Matrix4f(event.getPoseStack().last().pose());
            frameProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
            frameCamera = event.getCamera().getPosition();
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        checkWorld();
        Matrix4f view = frameView, projection = frameProjection;
        Vec3 camera = frameCamera;
        frameView = frameProjection = null;
        frameCamera = null;
        if (world == null || view == null || projection == null || camera == null) return;
        for (var entity : world.entitiesForRendering())
            if (entity instanceof ApolSummoningEntity ritual) trackRitual(ritual);
        long now = world.getGameTime();
        BEAMS.entrySet().removeIf(entry -> {
            if (now - entry.getValue().start > 240) {
                DRAWN.remove(entry.getKey());
                return true;
            }
            return false;
        });
        float partial = Minecraft.getInstance().isPaused() ? Minecraft.getInstance().getFrameTime() : event.getPartialTick();
        List<Map.Entry<Long, Beam>> active = new ArrayList<>();
        for (var entry : BEAMS.entrySet()) {
            float age = now - entry.getValue().start + partial;
            if (age >= 0 && age < DURATION && entry.getValue().base.distanceToSqr(camera) < 192 * 192)
                active.add(entry);
        }
        if (active.isEmpty() || !ensureShader()) return;
        active.sort(Comparator.comparingDouble((Map.Entry<Long, Beam> e) -> e.getValue().base.distanceToSqr(camera)).reversed());
        shader.safeGetUniform("BeamViewProjection").set(new Matrix4f(projection).mul(view));
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND), depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean mask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int func = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        ShaderInstance previous = RenderSystem.getShader();
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.setShader(() -> shader);
            for (var entry : active) {
                Beam beam = entry.getValue();
                float age = now - beam.start + partial;
                shader.safeGetUniform("Age").set(age / 20F);
                shader.safeGetUniform("Seed").set((float) beam.seed);
                draw(beam.base.subtract(camera), age, beam.seed);
                if (age > 0 && DRAWN.add(entry.getKey()))
                    LOGGER.info("[ApolBeam] submitted beam geometry, age={} ticks, shader={}", age, SHADER_ID);
            }
        } finally {
            RenderSystem.depthMask(mask);
            RenderSystem.depthFunc(func);
            if (depth) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull();
            else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            if (blend) RenderSystem.enableBlend();
            else RenderSystem.disableBlend();
            if (previous != null) RenderSystem.setShader(() -> previous);
        }
    }

    private static void draw(Vec3 origin, float age, int seed) {
        List<ApolBeamGeometry.Face> faces = ApolBeamGeometry.build(age / 20.0, seed);
        faces.sort(Comparator.comparingDouble((ApolBeamGeometry.Face f) -> {
            double x = origin.x + f.centerX(), y = origin.y + f.centerY(), z = origin.z + f.centerZ();
            return x * x + y * y + z * z;
        }).reversed());
        BUILDER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        for (ApolBeamGeometry.Face face : faces) {
            for (ApolBeamGeometry.Point p : face.points()) {
                BUILDER.vertex(origin.x + p.x(), origin.y + p.y(), origin.z + p.z()).color(face.kind() / 4F, face.tint(), 0F, face.alpha()).uv(p.u(), p.v()).endVertex();
            }
        }
        BufferUploader.drawWithShader(BUILDER.end());
    }

    @Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Shaders {
        @SubscribeEvent
        public static void register(RegisterShadersEvent event) throws IOException {
            if (ownsShader && shader != null) shader.close();
            shader = null;
            ownsShader = false;
            attemptedLazyLoad = false;
            event.registerShader(new ShaderInstance(event.getResourceProvider(), SHADER_ID, DefaultVertexFormat.POSITION_COLOR_TEX), loaded -> {
                shader = loaded;
                LOGGER.info("[ApolBeam] shader registered: {}", SHADER_ID);
            });
        }
    }

    private ApolSummoningBeam() {
    }
}
