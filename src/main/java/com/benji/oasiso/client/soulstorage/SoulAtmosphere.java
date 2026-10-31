package com.benji.oasiso.client.soulstorage;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import com.benji.oasiso.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.*;

@Mod.EventBusSubscriber(modid = "oasiso", value = Dist.CLIENT)
public final class SoulAtmosphere {
    private static final float AMBIENT_VOLUME = 0.45F;
    private static final float FOG_START = 7F;
    private static final float FOG_END = 12F;
    private static final float FOG_COLOR_BLEND = 1.0F;
    private static final float FOG_RED = 0.025F, FOG_GREEN = 0.24F, FOG_BLUE = 0.19F;
    private static SoulLoop ambient;
    private static ShaderInstance shader;
    private static ClientLevel world;
    private static AABB region;
    private static boolean wanted;
    private static float blend, previous;
    private static long clock, packetAt;
    private static final Random RANDOM = new Random();
    private static final List<Ash> ASH = new ArrayList<>();

    private record Ash(Vec3 origin, long born, int life, float size, float bright, float phase) {
    }

    public static void area(AABB box) {
        ClientLevel now = Minecraft.getInstance().level;
        if (now != world) reset(now);
        wanted = box != null;
        if (box != null) {
            region = box;
        }
        packetAt = clock;
    }

    private static void reset(ClientLevel now) {
        if (ambient != null) Minecraft.getInstance().getSoundManager().stop(ambient);
        ambient = null;
        world = now;
        region = null;
        wanted = false;
        blend = previous = 0;
        clock = packetAt = 0;
        ASH.clear();
    }

    public static float strength() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level != world || shader == null) return 0;
        return previous + (blend - previous) * mc.getFrameTime();
    }

    @Mod.EventBusSubscriber(modid = "oasiso", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Shaders {
        @SubscribeEvent
        public static void register(RegisterShadersEvent e) throws IOException {
            e.registerShader(new ShaderInstance(e.getResourceProvider(), ResourceLocation.fromNamespaceAndPath("oasiso", "soul_atmosphere"), DefaultVertexFormat.POSITION_COLOR_TEX), s -> shader = s);
            SoulSourceFog.registerShader(e);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != world || mc.player == null) {
            reset(mc.level);
            return;
        }
        if (mc.isPaused()) return;
        clock++;
        previous = blend;
        if (clock - packetAt > 40) wanted = false;
        float target = wanted && mc.player.isAlive() ? 1 : 0;
        blend = Mth.approach(blend, target, 1F / 40F);
        updateAmbient();
        ASH.removeIf(a -> clock - a.born > a.life);
        if (blend <= 0) {
            ASH.clear();
            return;
        }
        if (wanted && region != null && ASH.size() < 110) {
            Vec3 p = mc.player.position().add((RANDOM.nextDouble() - .5) * 24, RANDOM.nextDouble() * 10 - 2, (RANDOM.nextDouble() - .5) * 24);
            if (region.contains(p))
                ASH.add(new Ash(p, clock, 100 + RANDOM.nextInt(100), .018F + RANDOM.nextFloat() * .04F, .35F + RANDOM.nextFloat() * .65F, RANDOM.nextFloat() * 6.283F));
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent e) {
        boolean sky = e.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY;
        if (!sky && e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        float alpha = strength();
        if (alpha <= 0 || shader == null) return;
        Minecraft mc = Minecraft.getInstance();
        Vec3 camera = e.getCamera().getPosition();
        Matrix4f matrix = e.getPoseStack().last().pose();
        try (SoulRenderState state = new SoulRenderState()) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.colorMask(true, true, true, false);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.setShader(() -> shader);
            shader.safeGetUniform("View").set(matrix);
            shader.safeGetUniform("Projection").set(e.getProjectionMatrix());
            shader.safeGetUniform("Time").set((clock + e.getPartialTick()) / 20F);
            shader.safeGetUniform("Fade").set(alpha);
            if (sky) {
                RenderSystem.disableDepthTest();
                drawSky();
                return;
            }
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(515);
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            shader.safeGetUniform("Kind").set(2F);
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
            Vector3f right = new Vector3f(1, 0, 0).rotate(e.getCamera().rotation()), up = new Vector3f(0, 1, 0).rotate(e.getCamera().rotation());
            for (Ash ash : ASH) {
                float age = (clock - ash.born + e.getPartialTick()), t = age / ash.life;
                Vec3 p = ash.origin.add(Math.sin(age * .022 + ash.phase) * .6, age * .006, Math.cos(age * .018 + ash.phase) * .6);
                float a = Math.min(1, age / 15) * Math.min(1, (1 - t) * 5) * ash.bright;
                for (int[] corner : new int[][]{{-1, -1}, {1, -1}, {1, 1}, {-1, 1}}) {
                    float x = corner[0] * ash.size, y = corner[1] * ash.size;
                    b.vertex(p.x - camera.x + right.x * x + up.x * y, p.y - camera.y + right.y * x + up.y * y, p.z - camera.z + right.z * x + up.z * y).color(0F, ash.bright, ash.bright * .65F, Math.max(0, a)).uv(0, 0).endVertex();
                }
            }
            BufferUploader.drawWithShader(b.end());
        }
    }

    private static void drawSky() {
        shader.safeGetUniform("Kind").set(0F);
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        int[][] vertices = {{-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1}, {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}};
        int[][] faces = {{0, 1, 2, 3}, {5, 4, 7, 6}, {4, 0, 3, 7}, {1, 5, 6, 2}, {3, 2, 6, 7}, {4, 5, 1, 0}};
        for (int[] face : faces)
            for (int i : face) {
                int[] v = vertices[i];
                b.vertex(v[0] * 100F, v[1] * 100F, v[2] * 100F).color(1F, 1F, 1F, 1F).uv(0, 0).endVertex();
            }
        BufferUploader.drawWithShader(b.end());
    }

    private static void updateAmbient() {
        Minecraft mc = Minecraft.getInstance();
        if (ambient != null && ambient.isStopped()) ambient = null;
        if (blend > 0 && ambient == null && mc.options.getSoundSourceVolume(SoundSource.AMBIENT) > 0 && mc.options.getSoundSourceVolume(SoundSource.MASTER) > 0) {
            ambient = new SoulLoop(world);
            mc.getSoundManager().play(ambient);
        }
        if (ambient != null && (mc.options.getSoundSourceVolume(SoundSource.AMBIENT) <= 0 || mc.options.getSoundSourceVolume(SoundSource.MASTER) <= 0)) {
            mc.getSoundManager().stop(ambient);
            ambient = null;
        }
    }

    private static final class SoulLoop extends AbstractTickableSoundInstance {
        private final ClientLevel owner;

        private SoulLoop(ClientLevel owner) {
            super(ModSounds.SOUL_AMBIENT.get(), SoundSource.AMBIENT, RandomSource.create());
            this.owner = owner;
            looping = true;
            delay = 0;
            relative = true;
            attenuation = SoundInstance.Attenuation.NONE;
            volume = 0F;
            pitch = 1F;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != owner || mc.player == null) {
                stop();
                return;
            }
            volume = blend * AMBIENT_VOLUME;
            if (blend <= 0) stop();
        }
    }

    private static boolean clearAir(net.minecraft.client.Camera camera) {
        if (camera.getFluidInCamera() != FogType.NONE) return false;
        if (camera.getEntity() instanceof LivingEntity living && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS)))
            return false;
        return true;
    }

    @SubscribeEvent
    public static void distantFog(ViewportEvent.RenderFog e) {
        float a = strength();
        if (a <= 0 || e.getMode() != FogRenderer.FogMode.FOG_TERRAIN || !clearAir(e.getCamera())) return;
        float end = Math.min(e.getFarPlaneDistance(), FOG_END);
        float start = Math.min(FOG_START, Math.max(0F, end - 1F));
        e.setNearPlaneDistance(Mth.lerp(a, e.getNearPlaneDistance(), start));
        e.setFarPlaneDistance(Mth.lerp(a, e.getFarPlaneDistance(), end));
        e.setCanceled(true);
    }

    @SubscribeEvent
    public static void distantFogColor(ViewportEvent.ComputeFogColor e) {
        float a = strength() * FOG_COLOR_BLEND;
        if (a <= 0 || !clearAir(e.getCamera())) return;
        e.setRed(Mth.lerp(a, e.getRed(), FOG_RED));
        e.setGreen(Mth.lerp(a, e.getGreen(), FOG_GREEN));
        e.setBlue(Mth.lerp(a, e.getBlue(), FOG_BLUE));
    }
}
