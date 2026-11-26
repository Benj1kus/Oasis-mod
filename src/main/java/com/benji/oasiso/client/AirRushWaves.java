package com.benji.oasiso.client;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class AirRushWaves {
    private static final int DURATION = 12;
    private static final float RADIUS = 1.5F;

    private record Wave(double x, double y, double z, long start) {
    }

    private static final List<Wave> waves = new ArrayList<>();
    private static java.lang.ref.WeakReference<ClientLevel> owner = new java.lang.ref.WeakReference<>(null);
    private static ShaderInstance shader;

    @Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Shaders {
        @SubscribeEvent
        public static void register(RegisterShadersEvent event) throws IOException {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "air_rush_wave"), DefaultVertexFormat.POSITION_TEX_COLOR), loaded -> shader = loaded);
        }
    }

    public static void spawn(double x, double y, double z) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        if (owner.get() != level) {
            waves.clear();
            owner = new java.lang.ref.WeakReference<>(level);
        }
        waves.add(new Wave(x, y, z, level.getGameTime()));
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || waves.isEmpty()) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || level != owner.get()) {
            waves.clear();
            return;
        }
        long now = level.getGameTime();
        waves.removeIf(w -> now - w.start >= DURATION);
        if (waves.isEmpty() || shader == null) return;
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f matrix = event.getPoseStack().last().pose();
        var previous = RenderSystem.getShader();
        RenderSystem.setShader(() -> shader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        try {
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (Wave w : waves) {
                float t = (now - w.start + event.getPartialTick()) / DURATION;
                float r = 0.22F + (RADIUS - 0.22F) * t * (2 - t);
                int alpha = Math.round(255 * (1 - t) * (1 - t));
                float x = (float) (w.x - camera.x), y = (float) (w.y - camera.y), z = (float) (w.z - camera.z);
                b.vertex(matrix, x - r, y, z - r).uv(0, 0).color(255, 255, 255, alpha).endVertex();
                b.vertex(matrix, x - r, y, z + r).uv(0, 1).color(255, 255, 255, alpha).endVertex();
                b.vertex(matrix, x + r, y, z + r).uv(1, 1).color(255, 255, 255, alpha).endVertex();
                b.vertex(matrix, x + r, y, z - r).uv(1, 0).color(255, 255, 255, alpha).endVertex();
            }
            BufferUploader.drawWithShader(b.end());
        } finally {
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            if (previous != null) RenderSystem.setShader(() -> previous);
        }
    }
}
