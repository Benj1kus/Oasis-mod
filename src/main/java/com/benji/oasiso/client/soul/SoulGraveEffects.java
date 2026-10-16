package com.benji.oasiso.client.soul;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.GraveBlock;
import com.benji.oasiso.common.entity.SoulGiverEntity;
import com.benji.oasiso.common.soul.SoulGraveNetwork;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SoulGraveEffects {
    private static final ResourceLocation HEAD_TEXTURE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/soul_giver.png");
    private static final Map<UUID, Head> HEADS = new LinkedHashMap<>();
    private static final List<Strike> STRIKES = new ArrayList<>();
    private static final List<Bubble> BUBBLES = new ArrayList<>();
    private static final Map<UUID, long[]> EMITTERS = new HashMap<>();
    private static final BufferBuilder BUFFER = new BufferBuilder(65536);
    private static TextureTarget mask;
    private static ClientLevel world;
    private static boolean collecting;
    private static Matrix4f frameView, frameProjection;

    private record Head(SoulGiverEntity entity, SoulMesh mesh) {
    }

    private record Bubble(Vec3 origin, Vec3 drift, double start, int life, float radius, int kind, int seed) {
    }

    private record Strike(SoulGraveNetwork.Strike data, long start) {
    }

    private SoulGraveEffects() {
    }

    public static void capture(SoulGiverEntity entity, SoulMesh mesh) {
        Minecraft mc = Minecraft.getInstance();
        if (collecting && mc.level == entity.level() && HEADS.size() < 32 && entity.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition()) < 64 * 64)
            HEADS.put(entity.getUUID(), new Head(entity, mesh));
    }

    public static void receive(SoulGraveNetwork.Strike packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().location().equals(packet.dimension())) return;
        ensureWorld(mc.level);
        if (STRIKES.size() >= 64) STRIKES.remove(0);
        STRIKES.add(new Strike(packet, mc.level.getGameTime()));
    }

    private static void ensureWorld(ClientLevel level) {
        if (world != level) {
            HEADS.clear();
            STRIKES.clear();
            BUBBLES.clear();
            EMITTERS.clear();
            collecting = false;
            frameView = frameProjection = null;
            world = level;
        }
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() == world) {
            HEADS.clear();
            STRIKES.clear();
            BUBBLES.clear();
            EMITTERS.clear();
            world = null;
            collecting = false;
            frameView = frameProjection = null;
            RenderSystem.recordRenderCall(() -> {
                if (mask != null) {
                    mask.destroyBuffers();
                    mask = null;
                }
            });
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ensureWorld(mc.level);
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            HEADS.clear();
            frameView = frameProjection = null;
            collecting = true;
            return;
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            collecting = false;
            frameView = new Matrix4f(event.getPoseStack().last().pose());
            frameProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Matrix4f view = frameView, projection = frameProjection;
        frameView = frameProjection = null;
        if (view == null || projection == null) {
            HEADS.clear();
            return;
        }
        if (SoulFxShaders.mask == null || SoulFxShaders.outline == null || SoulFxShaders.arc == null) {
            HEADS.clear();
            return;
        }
        float partial = event.getPartialTick();
        float time = ((mc.level.getGameTime() % 24000) + partial) / 20F;
        Vec3 camera = event.getCamera().getPosition();
        RenderTarget main = mc.getMainRenderTarget();
        SavedState saved = new SavedState();
        try {
            if (mask == null || mask.width != main.width || mask.height != main.height) {
                if (mask != null) mask.destroyBuffers();
                mask = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
                mask.setClearColor(0, 0, 0, 0);
            }
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            for (Head head : HEADS.values()) {
                float depth = Math.max(1F, -head.mesh.center().z);
                float pixels = Math.max(1.5F, Math.min(24F, projection.m11() * main.height * 0.5F * 0.11F / depth));
                silhouette(head.mesh, HEAD_TEXTURE, projection, pixels, time, head.entity.getId(), 0, 1F, main);
            }
            STRIKES.removeIf(s -> mc.level.getGameTime() - s.start >= 24 || mc.level.getGameTime() < s.start);
            for (Strike strike : STRIKES) {
                if (!mc.level.hasChunkAt(strike.data.grave()) || !(mc.level.getBlockState(strike.data.grave()).getBlock() instanceof GraveBlock))
                    continue;
                float age = (float) (mc.level.getGameTime() - strike.start) + partial;
                SoulMesh grave = graveMesh(strike, view, camera);
                float fade = (float) Math.pow(Math.max(0, 1 - age / 24F), 2);
                float depth = Math.max(1F, -toView(Vec3.atCenterOf(strike.data.grave()), view, camera).z);
                float pixels = Math.max(2F, Math.min(28F, projection.m11() * main.height * 0.5F * 0.13F / depth));
                silhouette(grave, TextureAtlas.LOCATION_BLOCKS, projection, pixels, time, strike.data.seed(), 1, fade, main);
            }
            main.bindWrite(true);
            RenderSystem.disableScissor();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            ShaderInstance arc = SoulFxShaders.arc;
            arc.safeGetUniform("ArcProjection").set(projection);
            RenderSystem.setShader(() -> arc);
            BUFFER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
            double now = mc.level.getGameTime() + partial;
            for (Head head : HEADS.values()) emitBubbles(head, view, camera, now);
            BUBBLES.removeIf(b -> now - b.start >= b.life || now < b.start);
            EMITTERS.entrySet().removeIf(e -> mc.level.getGameTime() - e.getValue()[2] > 200);
            for (Bubble b : BUBBLES) {
                float phase = (float) ((now - b.start) / b.life);
                Vec3 pos = b.origin.add(b.drift.scale(phase)).add(0, phase * phase * 0.95, 0);
                pos = pos.add(Math.sin(phase * 7 + b.seed) * 0.04 * phase, 0, Math.cos(phase * 5 + b.seed) * 0.04 * phase);
                billboard(toView(pos, view, camera), b.radius, phase, b.kind, b.seed, 1F);
            }
            for (Strike strike : STRIKES) {
                float age = (float) (mc.level.getGameTime() - strike.start) + partial;
                if (age > 11 || !mc.level.hasChunkAt(strike.data.grave()) || !(mc.level.getBlockState(strike.data.grave()).getBlock() instanceof GraveBlock))
                    continue;
                Head head = HEADS.get(strike.data.uuid());
                Vector3f from = head != null ? head.mesh.center() : toView(strike.data.origin(), view, camera);
                Vector3f to = toView(Vec3.atCenterOf(strike.data.grave()).add(0, 0.05, 0), view, camera);
                bolt(from, to, age, strike.data.seed());
                billboard(to, 0.75F, age / 24F, 240, strike.data.seed(), 1F);
            }
            BufferUploader.drawWithShader(BUFFER.end());
        } finally {
            main.bindWrite(true);
            saved.restore();
            HEADS.clear();
        }
    }

    private static Vector3f toView(Vec3 p, Matrix4f view, Vec3 camera) {
        return view.transformPosition(new Vector3f((float) (p.x - camera.x), (float) (p.y - camera.y), (float) (p.z - camera.z)));
    }

    private static SoulMesh graveMesh(Strike strike, Matrix4f view, Vec3 camera) {
        Minecraft mc = Minecraft.getInstance();
        var pos = strike.data.grave();
        var state = mc.level.getBlockState(pos);
        var model = mc.getBlockRenderer().getBlockModel(state);
        SoulMesh mesh = new SoulMesh();
        for (int face = -1; face < 6; face++) {
            Direction direction = face < 0 ? null : Direction.values()[face];
            var quads = model.getQuads(state, direction, RandomSource.create(42), ModelData.EMPTY, null);
            for (var quad : quads) {
                int[] data = quad.getVertices();
                int stride = data.length / 4;
                for (int i = 0; i < 4; i++) {
                    int k = i * stride;
                    var p = toView(new Vec3(pos.getX() + Float.intBitsToFloat(data[k]), pos.getY() + Float.intBitsToFloat(data[k + 1]), pos.getZ() + Float.intBitsToFloat(data[k + 2])), view, camera);
                    mesh.vertices.add(new SoulMesh.Vertex(p.x, p.y, p.z, Float.intBitsToFloat(data[k + 4]), Float.intBitsToFloat(data[k + 5])));
                }
            }
        }
        return mesh;
    }

    private static void silhouette(SoulMesh mesh, ResourceLocation texture, Matrix4f projection, float pixels, float time, int seed, int mode, float amount, RenderTarget main) {
        if (mesh.vertices.isEmpty()) return;
        float minX = main.width, minY = main.height, maxX = 0, maxY = 0;
        boolean nearPlane = false;
        for (var v : mesh.vertices) {
            Vector4f p = projection.transform(new Vector4f(v.x(), v.y(), v.z(), 1));
            if (p.w <= 0.05F) {
                nearPlane = true;
                continue;
            }
            float x = (p.x / p.w * 0.5F + 0.5F) * main.width, y = (p.y / p.w * 0.5F + 0.5F) * main.height;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        if (nearPlane) {
            minX = 0;
            minY = 0;
            maxX = main.width;
            maxY = main.height;
        }
        int pad = (int) (pixels * 2.4F + 4);
        int x = Math.max(0, (int) Math.floor(minX) - pad), y = Math.max(0, (int) Math.floor(minY) - pad);
        int right = Math.min(main.width, (int) Math.ceil(maxX) + pad), top = Math.min(main.height, (int) Math.ceil(maxY) + pad);
        if (right <= x || top <= y) return;
        RenderSystem.disableScissor();
        mask.clear(Minecraft.ON_OSX);
        mask.copyDepthFrom(main);
        mask.bindWrite(true);
        RenderSystem.enableScissor(x, y, right - x, top - y);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset(-1F, -1F);
        ShaderInstance shader = SoulFxShaders.mask;
        shader.safeGetUniform("FxProjection").set(projection);
        final ShaderInstance maskShader = shader;
        RenderSystem.setShader(() -> maskShader);
        RenderSystem.setShaderTexture(0, texture);
        BUFFER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        for (var v : mesh.vertices) BUFFER.vertex(v.x(), v.y(), v.z()).uv(v.u(), v.v()).endVertex();
        BufferUploader.drawWithShader(BUFFER.end());
        RenderSystem.disablePolygonOffset();
        main.bindWrite(true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        shader = SoulFxShaders.outline;
        shader.safeGetUniform("ScreenSize").set((float) main.width, (float) main.height);
        shader.safeGetUniform("Radius").set(pixels);
        shader.safeGetUniform("Time").set(time);
        shader.safeGetUniform("Seed").set((float) (seed & 1023));
        shader.safeGetUniform("Mode").set((float) mode);
        shader.safeGetUniform("Amount").set(amount);
        final ShaderInstance outline = shader;
        RenderSystem.setShader(() -> outline);
        RenderSystem.setShaderTexture(0, mask.getColorTextureId());
        RenderSystem.setShaderTexture(1, mask.getDepthTextureId());
        BUFFER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        BUFFER.vertex(-1, -1, 0).uv(0, 0).endVertex();
        BUFFER.vertex(1, -1, 0).uv(1, 0).endVertex();
        BUFFER.vertex(1, 1, 0).uv(1, 1).endVertex();
        BUFFER.vertex(-1, 1, 0).uv(0, 1).endVertex();
        BufferUploader.drawWithShader(BUFFER.end());
    }

    private static void bolt(Vector3f from, Vector3f to, float age, int seed) {
        RandomSource random = RandomSource.create(seed + ((int) (age / 2)) * 17L);
        Vector3f delta = new Vector3f(to).sub(from);
        Vector3f side = new Vector3f(-delta.y, delta.x, 0);
        if (side.lengthSquared() < 0.0001F) side.set(1, 0, 0);
        else side.normalize();
        Vector3f other = new Vector3f(delta).cross(side).normalize();
        int segments = 14;
        Vector3f previous = new Vector3f(from);
        for (int i = 1; i <= segments; i++) {
            float t = i / (float) segments;
            float offset = (float) Math.sin(t * Math.PI) * 0.23F;
            Vector3f p = new Vector3f(from).fma(t, delta).fma((random.nextFloat() * 2 - 1) * offset, side).fma((random.nextFloat() * 2 - 1) * offset * 0.45F, other);
            ribbon(previous, p, 0.065F, age / 24F, 0, seed, 1F);
            if (i == 5 || i == 9) {
                Vector3f tip = new Vector3f(p).fma((random.nextBoolean() ? 1 : -1) * 0.5F, side).fma(0.09F, delta);
                ribbon(p, tip, 0.03F, age / 24F, 60, seed, 0.75F);
            }
            previous = p;
        }
    }

    private static void ribbon(Vector3f a, Vector3f b, float width, float age, int kind, int seed, float alpha) {
        Vector3f d = new Vector3f(b).sub(a), n = new Vector3f(-d.y, d.x, 0);
        if (n.lengthSquared() < 0.00001F) n.set(1, 0, 0);
        else n.normalize();
        n.mul(width);
        vertex(new Vector3f(a).sub(n), 0, 0, age, kind, seed, alpha);
        vertex(new Vector3f(a).add(n), 1, 0, age, kind, seed, alpha);
        vertex(new Vector3f(b).add(n), 1, 1, age, kind, seed, alpha);
        vertex(new Vector3f(b).sub(n), 0, 1, age, kind, seed, alpha);
    }

    private static void emitBubbles(Head head, Matrix4f view, Vec3 camera, double now) {
        if (head.mesh.headPoints.isEmpty()) return;
        long tick = (long) now;
        long[] timer = EMITTERS.computeIfAbsent(head.entity.getUUID(), id -> new long[]{tick, tick + 5, tick});
        timer[2] = tick;
        Matrix4f inverse = new Matrix4f(view).invert();
        for (int layer = 0; layer < 2; layer++) {
            if (tick < timer[layer]) continue;
            RandomSource rand = RandomSource.create(head.entity.getId() * 741L + tick * 97L + layer * 1531L);
            timer[layer] = tick + 8 + rand.nextInt(layer == 0 ? 7 : 11);
            var v = head.mesh.vertices.get(rand.nextInt(head.mesh.vertices.size()));
            Vector3f origin = new Vector3f(v.x(), v.y(), v.z());
            Vector3f outward = new Vector3f(origin).sub(head.mesh.center());
            if (outward.lengthSquared() < .001F) outward.set(1, 0, 0);
            else outward.normalize();
            origin.fma(.04F, outward);
            inverse.transformPosition(origin);
            inverse.transformDirection(outward).mul(.18F + rand.nextFloat() * .15F);
            float radius = (layer == 0 ? .075F : .12F) * (.75F + rand.nextFloat() * .5F);
            if (BUBBLES.size() >= 512) BUBBLES.remove(0);
            BUBBLES.add(new Bubble(new Vec3(origin.x + camera.x, origin.y + camera.y, origin.z + camera.z), new Vec3(outward.x, outward.y, outward.z), now, 40 + rand.nextInt(25), radius, layer == 0 ? 160 : 180, rand.nextInt(256)));
        }
    }

    private static void billboard(Vector3f p, float radius, float age, int kind, int seed, float alpha) {
        vertex(new Vector3f(p).add(-radius, -radius, 0), 0, 0, age, kind, seed, alpha);
        vertex(new Vector3f(p).add(radius, -radius, 0), 1, 0, age, kind, seed, alpha);
        vertex(new Vector3f(p).add(radius, radius, 0), 1, 1, age, kind, seed, alpha);
        vertex(new Vector3f(p).add(-radius, radius, 0), 0, 1, age, kind, seed, alpha);
    }

    private static void vertex(Vector3f p, float u, float v, float age, int kind, int seed, float alpha) {
        BUFFER.vertex(p.x, p.y, p.z).color(Math.min(255, Math.max(0, (int) (age * 255))), kind, seed & 255, Math.min(255, Math.max(0, (int) (alpha * 255)))).uv(u, v).endVertex();
    }

    private static final class SavedState {
        final ShaderInstance shader = RenderSystem.getShader();
        final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), blend = GL11.glIsEnabled(GL11.GL_BLEND), cull = GL11.glIsEnabled(GL11.GL_CULL_FACE), scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST), write = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        final int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        final int src = GL11.glGetInteger(0x80C9), dst = GL11.glGetInteger(0x80C8), srcA = GL11.glGetInteger(0x80CB), dstA = GL11.glGetInteger(0x80CA);
        final int tex0 = RenderSystem.getShaderTexture(0), tex1 = RenderSystem.getShaderTexture(1);
        final int[] box = new int[4];

        SavedState() {
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, box);
        }

        void restore() {
            RenderSystem.disablePolygonOffset();
            RenderSystem.polygonOffset(0, 0);
            if (depth) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            if (blend) RenderSystem.enableBlend();
            else RenderSystem.disableBlend();
            if (cull) RenderSystem.enableCull();
            else RenderSystem.disableCull();
            if (scissor) RenderSystem.enableScissor(box[0], box[1], box[2], box[3]);
            else RenderSystem.disableScissor();
            RenderSystem.depthMask(write);
            RenderSystem.depthFunc(depthFunc);
            RenderSystem.blendFuncSeparate(src, dst, srcA, dstA);
            RenderSystem.setShaderTexture(0, tex0);
            RenderSystem.setShaderTexture(1, tex1);
            if (shader != null) RenderSystem.setShader(() -> shader);
        }
    }
}
