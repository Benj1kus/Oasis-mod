package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.GeoQuad;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ApollyonTeleportFx {
    public static final float CELL_SIZE = .15F;
    public static final float SCATTER = 3.35F;
    private static final int[] COLORS = {0x62E3FF, 0xEF82D8, 0x9E62EF};
    private static ShaderInstance shader;

    private ApollyonTeleportFx() {
    }

    public static boolean ready() {
        return shader != null;
    }

    public static void prepare(Matrix4f clip, float amount) {
        shader.safeGetUniform("ClipMatrix").set(clip);
        shader.safeGetUniform("Strength").set(amount);
    }

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) throws IOException {
        shader = null;
        FxType.TYPES.clear();
        event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "apollyon_teleport"), DefaultVertexFormat.POSITION_COLOR_TEX), value -> shader = value);
    }

    public static void emit(GeoQuad quad, Matrix4f pose, Matrix4f root, MultiBufferSource buffers, ResourceLocation texture, float amount, float age, int boneSeed) {
        var vertices = quad.vertices();
        Vector3f a = vertices[0].position(), b = vertices[1].position();
        Vector3f c = vertices[2].position(), d = vertices[3].position();
        int nx = Mth.clamp((int) Math.ceil(a.distance(b) / CELL_SIZE), 1, 8);
        int ny = Mth.clamp((int) Math.ceil(a.distance(d) / CELL_SIZE), 1, 8);
        int faceSeed = boneSeed;
        for (var v : vertices) {
            faceSeed = 31 * faceSeed + Float.floatToIntBits(v.position().x());
            faceSeed = 31 * faceSeed + Float.floatToIntBits(v.position().y());
            faceSeed = 31 * faceSeed + Float.floatToIntBits(v.position().z());
        }
        VertexConsumer out = buffers.getBuffer(FxType.get(texture));
        Vector4f center = root.transform(new Vector4f(0, 1.5F, 0, 1));
        float scatter = amount * amount * SCATTER;
        for (int y = 0; y < ny; y++)
            for (int x = 0; x < nx; x++) {

                int seed = mix(faceSeed + x * 7349 + y * 9151);
                float threshold = .22F + hash(seed) * .57F;
                float alpha = 1 - smooth(threshold - .10F, threshold + .10F, amount);
                if (alpha < .015F) continue;

                float u0 = (float) x / nx, u1 = (float) (x + 1) / nx, v0 = (float) y / ny, v1 = (float) (y + 1) / ny;
                Vector3f mid = point(a, b, c, d, (u0 + u1) * .5F, (v0 + v1) * .5F);
                Vector4f transformed = pose.transform(new Vector4f(mid, 1));
                Vector3f radial = new Vector3f(transformed.x - center.x, transformed.y - center.y, transformed.z - center.z);
                if (radial.lengthSquared() > .0001F) radial.normalize();

                float bandJitter = hash(faceSeed + y * 97 + (int) (age * 2) * 131) * 2 - 1;
                Vector3f random = new Vector3f(hash(seed + 17) * 2 - 1, hash(seed + 31) * 1.4F - .25F, hash(seed + 59) * 2 - 1);
                root.transformDirection(random);
                Vector3f side = root.transformDirection(new Vector3f(bandJitter * .45F, 0, 0));
                Vector3f offset = radial.mul(scatter).add(random.mul(scatter * .8F)).add(side.mul(amount));
                int color = COLORS[Math.floorMod(seed, 3)];
                vertex(out, quad, pose, a, b, c, d, u0, v0, offset, color, alpha);
                vertex(out, quad, pose, a, b, c, d, u1, v0, offset, color, alpha);
                vertex(out, quad, pose, a, b, c, d, u1, v1, offset, color, alpha);
                vertex(out, quad, pose, a, b, c, d, u0, v1, offset, color, alpha);
            }
    }

    private static void vertex(VertexConsumer out, GeoQuad quad, Matrix4f pose, Vector3f a, Vector3f b, Vector3f c, Vector3f d, float u, float v, Vector3f offset, int color, float alpha) {
        Vector3f local = point(a, b, c, d, u, v);
        Vector4f p = pose.transform(new Vector4f(local, 1));
        var q = quad.vertices();
        float texU = Mth.lerp(v, Mth.lerp(u, q[0].texU(), q[1].texU()), Mth.lerp(u, q[3].texU(), q[2].texU()));
        float texV = Mth.lerp(v, Mth.lerp(u, q[0].texV(), q[1].texV()), Mth.lerp(u, q[3].texV(), q[2].texV()));
        out.vertex(p.x + offset.x, p.y + offset.y, p.z + offset.z).color((color >> 16) & 255, (color >> 8) & 255, color & 255, Math.round(alpha * 255)).uv(texU, texV).endVertex();
    }

    private static Vector3f point(Vector3f a, Vector3f b, Vector3f c, Vector3f d, float u, float v) {
        return new Vector3f(a).lerp(b, u).lerp(new Vector3f(d).lerp(c, u), v);
    }

    private static float smooth(float min, float max, float v) {
        float t = Mth.clamp((v - min) / (max - min), 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static int mix(int v) {
        v ^= v >>> 16;
        v *= 0x7feb352d;
        v ^= v >>> 15;
        v *= 0x846ca68b;
        return v ^ (v >>> 16);
    }

    private static float hash(int v) {
        return (mix(v) & 0x7fffffff) / (float) Integer.MAX_VALUE;
    }

    private static final class FxType extends RenderType {
        static final Map<ResourceLocation, RenderType> TYPES = new HashMap<>();

        static RenderType get(ResourceLocation texture) {
            return TYPES.computeIfAbsent(texture, key -> create("oasiso_apollyon_teleport", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 131072, false, false, CompositeState.builder().setShaderState(new ShaderStateShard(() -> shader)).setTextureState(new TextureStateShard(key, false, false)).setTransparencyState(NO_TRANSPARENCY).setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(false)));
        }

        private FxType(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort, Runnable setup, Runnable clear) {
            super(name, format, mode, size, crumbling, sort, setup, clear);
        }
    }
}
