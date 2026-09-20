package com.benji.oasiso.client.gui;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.MiniBossHealthBar;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.IdentityHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class MiniBossHealthBarClient {
    private static final ResourceLocation FRAME = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/mini_hotbar_frame.png");
    private static final ResourceLocation PROGRESS = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/mini_hotbar_progress.png");

    private static final float TEXTURE_WIDTH = 60;
    private static final float TEXTURE_HEIGHT = 20;
    private static final float FILL_X = 3;
    private static final float FILL_Y = 3;
    private static final float FILL_WIDTH = 54;
    private static final float FILL_HEIGHT = 14;

    private static final double MAX_DISTANCE = 48;
    private static final int SLIDE_TICKS = 8;
    private static final int FEEDBACK_TICKS = 8;

    private static final Map<LivingEntity, BarState> BARS = new IdentityHashMap<>();
    private static ClientLevel currentLevel;
    private static long updateStamp;
    private static Matrix4f healthBarView;

    private MiniBossHealthBarClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (currentLevel != mc.level || mc.level == null || mc.player == null) {
            BARS.clear();
            currentLevel = mc.level;
            updateStamp = 0;
        }
        if (mc.level == null || mc.player == null || mc.isPaused()) return;

        long stamp = ++updateStamp;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || !eligible(living)) continue;
            BarState state = BARS.computeIfAbsent(living, mob -> new BarState(mob.getHealth(), mob.getMaxHealth()));
            state.tick(living.getHealth(), living.getMaxHealth());
            state.lastSeen = stamp;
        }
        BARS.values().removeIf(state -> state.lastSeen != stamp);
    }

    private static boolean eligible(LivingEntity entity) {
        return entity instanceof MiniBossHealthBar bar && bar.showMiniBossHealthBar() && entity.isAlive() && !entity.isRemoved() && !entity.isInvisible();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            healthBarView = new Matrix4f(event.getPoseStack().last().pose());
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Matrix4f view = healthBarView;
        healthBarView = null;
        if (view == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level != currentLevel || mc.player == null || mc.options.hideGui || BARS.isEmpty())
            return;

        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = new PoseStack();
        pose.mulPoseMatrix(view);

        float partial = Mth.clamp(event.getPartialTick(), 0, 1);

        RenderSystem.enableDepthTest(); // Не показываем полоску сквозь стены.
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            for (Map.Entry<LivingEntity, BarState> entry : BARS.entrySet()) {
                LivingEntity entity = entry.getKey();
                if (!eligible(entity) || entity.level() != mc.level) continue;
                double distance = entity.distanceToSqr(camera);
                if (distance > MAX_DISTANCE * MAX_DISTANCE) continue;
                MiniBossHealthBar bar = (MiniBossHealthBar) entity;

                float scale = bar.getMiniBossHealthBarScale();
                double offset = bar.getMiniBossHealthBarOffset();
                if (!Float.isFinite(scale) || scale <= 0 || !Double.isFinite(offset)) continue;

                float alpha = (float) (1 - smooth((Math.sqrt(distance) - (MAX_DISTANCE - 6)) / 6));
                renderBar(entity, bar, entry.getValue(), pose, camera, partial, alpha);
            }
        } finally {
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
        }
    }

    private static void renderBar(LivingEntity entity, MiniBossHealthBar bar, BarState state, PoseStack pose, Vec3 camera, float partial, float alpha) {
        double x = Mth.lerp(partial, entity.xo, entity.getX()) - camera.x;
        double y = Mth.lerp(partial, entity.yo, entity.getY()) - camera.y;
        double z = Mth.lerp(partial, entity.zo, entity.getZ()) - camera.z;

        float fill = Mth.clamp(Mth.lerp(partial, state.previousFill, state.fill), 0, 1);
        float feedback = (float) (1 - smooth((state.feedbackAge + partial) / FEEDBACK_TICKS));

        double phase = (state.feedbackAge + partial) * 2.5 + entity.getId() * 0.7;

        pose.pushPose();
        try {
            pose.translate(x, y + entity.getBbHeight() + bar.getMiniBossHealthBarOffset(), z);
            pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
            float scale = bar.getMiniBossHealthBarScale();
            pose.scale(-scale, -scale, scale);
            pose.translate(Math.sin(phase) * state.shakePixels * feedback, Math.cos(phase * 1.37) * state.shakePixels * 0.55 * feedback, 0);
            Matrix4f matrix = pose.last().pose();

            MiniBossHealthBarShader.draw(matrix, FRAME, -TEXTURE_WIDTH / 2, -TEXTURE_HEIGHT / 2, TEXTURE_WIDTH / 2, TEXTURE_HEIGHT / 2, 0, 0, 1, 1, 0, alpha);

            if (fill > 0.0001F) {
                float right = FILL_X + FILL_WIDTH * fill;

                MiniBossHealthBarShader.draw(matrix, PROGRESS, FILL_X - TEXTURE_WIDTH / 2, FILL_Y - TEXTURE_HEIGHT / 2, right - TEXTURE_WIDTH / 2, FILL_Y + FILL_HEIGHT - TEXTURE_HEIGHT / 2, FILL_X / TEXTURE_WIDTH, FILL_Y / TEXTURE_HEIGHT, right / TEXTURE_WIDTH, (FILL_Y + FILL_HEIGHT) / TEXTURE_HEIGHT, feedback, alpha);
            }
        } finally {
            pose.popPose();
        }
    }

    private static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    static final class BarState {
        float lastHealth;
        float fill;
        float previousFill;
        float fromFill;
        float targetFill;
        float shakePixels;
        int slideAge = SLIDE_TICKS;
        int feedbackAge = FEEDBACK_TICKS;
        long lastSeen;

        BarState(float health, float maxHealth) {
            lastHealth = safeHealth(health);
            fill = previousFill = fromFill = targetFill = fraction(health, maxHealth);
        }

        void tick(float health, float maxHealth) {
            health = safeHealth(health);
            previousFill = fill;

            if (health < lastHealth - 0.0001F) {
                feedbackAge = 0;
                float loss = fraction(lastHealth - health, maxHealth);
                shakePixels = 1.0F + Math.min(1, loss * 5) * 1.5F;
            } else if (feedbackAge < FEEDBACK_TICKS) {
                feedbackAge++;
            }
            lastHealth = health;
            float target = fraction(health, maxHealth);
            if (Math.abs(target - targetFill) > 0.000001F) {
                fromFill = fill;
                targetFill = target;
                slideAge = 0;
            }
            if (slideAge < SLIDE_TICKS) slideAge++;
            fill = Mth.lerp((float) smooth(slideAge / (double) SLIDE_TICKS), fromFill, targetFill);
        }

        private static float safeHealth(float health) {
            return Float.isFinite(health) ? Math.max(0, health) : 0;
        }

        private static float fraction(float health, float maxHealth) {
            if (!Float.isFinite(maxHealth) || maxHealth <= 0) return 0;
            return Mth.clamp(safeHealth(health) / maxHealth, 0, 1);
        }
    }
}
