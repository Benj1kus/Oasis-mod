package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ApolSummoningEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;

public final class ApolSummoningBeam {
    public static final float RADIUS = 6, HEIGHT = 10, DURATION = 120;
    private static ShaderInstance shader;
    private static final MultiBufferSource.BufferSource BUFFER = MultiBufferSource.immediate(new BufferBuilder(262144));

    private record Face(Vec3 a, Vec3 b, Vec3 c, Vec3 d, float u0, float u1, float v0, float v1, float layer,
                        double distance) {
    }

    public static void render(ApolSummoningEntity ritual, float partial, PoseStack pose) {
        float age = ritual.beamAge(partial);
        if (age < 0 || age >= DURATION || shader == null) return;
        ShaderInstance previous = RenderSystem.getShader();
        shader.safeGetUniform("Age").set(age / 20F);
        shader.safeGetUniform("Seed").set((float) (ritual.getId() % 997));
        Vec3 base = ritual.beamBase().subtract(ritual.position());
        Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(ritual.position());
        try {
            draw(pose.last().pose(), eye, base, age);
        } finally {
            BUFFER.endBatch(BeamType.TYPE);
            if (previous != null) RenderSystem.setShader(() -> previous);
        }
    }

    private static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    private static Vec3 point(Vec3 base, double angle, double radius, double tip, double heightFraction, double spread) {
        double r = radius * (spread + (Math.sqrt(spread) - spread) * Math.min(1, heightFraction * 3));
        return base.add(Math.cos(angle) * r, tip * heightFraction, Math.sin(angle) * r);
    }

    private static void draw(Matrix4f matrix, Vec3 eye, Vec3 base, float age) {
        double height = HEIGHT * smooth(age / 14.0), spread = .025 + .975 * smooth(age / 26.0);
        if (height < .001) return;
        double[] radii = {.85, 1.8, 3.3, 4.7, RADIUS};
        ArrayList<Face> faces = new ArrayList<>(3000);
        for (int layer = 0; layer < 5; layer++) {
            for (int i = 0; i < 64; i++) {
                double a = i * Math.PI * 2 / 64, b = (i + 1) * Math.PI * 2 / 64;
                double tipA = height * (.84 + .16 * Math.abs(Math.sin(i * 2.43 + layer)));
                double tipB = height * (.84 + .16 * Math.abs(Math.sin(((i + 1) % 64) * 2.43 + layer)));
                if (layer == 0) {
                    tipA = height;
                    tipB = height;
                }
                for (int j = 0; j < 8; j++) {
                    float lo = j / 8F, hi = (j + 1) / 8F;
                    Vec3 p0 = point(base, a, radii[layer], tipA, lo, spread), p1 = point(base, b, radii[layer], tipB, lo, spread);
                    Vec3 p2 = point(base, b, radii[layer], tipB, hi, spread), p3 = point(base, a, radii[layer], tipA, hi, spread);
                    faces.add(new Face(p0, p1, p2, p3, i / 64F, (i + 1) / 64F, lo, hi, layer, p0.add(p2).scale(.5).distanceToSqr(eye)));
                }
                double inner = layer == 0 ? 0 : radii[layer - 1] * spread, outer = radii[layer] * spread;
                Vec3 p0 = base.add(Math.cos(a) * inner, .025, Math.sin(a) * inner), p1 = base.add(Math.cos(a) * outer, .025, Math.sin(a) * outer);
                Vec3 p2 = base.add(Math.cos(b) * outer, .025, Math.sin(b) * outer), p3 = base.add(Math.cos(b) * inner, .025, Math.sin(b) * inner);
                faces.add(new Face(p0, p1, p2, p3, i / 64F, (i + 1) / 64F, .03F, .09F, layer, p0.add(p2).scale(.5).distanceToSqr(eye)));
            }
        }
        faces.sort(Comparator.comparingDouble(Face::distance).reversed());
        VertexConsumer out = BUFFER.getBuffer(BeamType.TYPE);
        for (Face f : faces) {
            vertex(out, matrix, f.a, f.u0, f.v0, f.layer);
            vertex(out, matrix, f.b, f.u1, f.v0, f.layer);
            vertex(out, matrix, f.c, f.u1, f.v1, f.layer);
            vertex(out, matrix, f.d, f.u0, f.v1, f.layer);
        }
        BUFFER.endBatch(BeamType.TYPE);
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, Vec3 p, float u, float v, float layer) {
        out.vertex(matrix, (float) p.x, (float) p.y, (float) p.z).color(layer / 4, 0, 0, 1).uv(u, v).endVertex();
    }

    @Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Shaders {
        @SubscribeEvent
        public static void register(RegisterShadersEvent event) throws IOException {
            shader = null;
            event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "apol_altar_column"), DefaultVertexFormat.POSITION_COLOR_TEX), loaded -> shader = loaded);
        }
    }

    private static final class BeamType extends RenderType {
        static final RenderType TYPE = create("oasiso_apol_altar_column", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 262144, false, false, CompositeState.builder().setShaderState(new ShaderStateShard(() -> shader)).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).setOutputState(ITEM_ENTITY_TARGET).createCompositeState(false));

        private BeamType(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sorted, Runnable setup, Runnable clear) {
            super(name, format, mode, size, crumbling, sorted, setup, clear);
        }
    }

    private ApolSummoningBeam() {
    }
}
