package com.benji.oasiso.client.cutscene;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.cutscene.CutsceneNetwork;
import com.benji.oasiso.common.cutscene.CutsceneTiming;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.*;
import org.joml.*;

import java.lang.Math;

import org.lwjgl.glfw.GLFW;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class CutsceneScreen extends Screen {
    private static CutsceneScreen active;
    final CutsceneNetwork.Start packet;
    final JsonObject config;
    final ClientLevel world;

    static CutsceneScreen current() {
        return active;
    }

    boolean hasCamera() {
        return attached && !restored;
    }

    Entity subject() {
        return boss;
    }

    double flareX, flareY;
    boolean flareValid;

    @SubscribeEvent
    public static void projectFlare(RenderLevelStageEvent event) {
        CutsceneScreen scene = active;
        if (scene == null) return;
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) scene.flareValid = false;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !scene.hasCamera() || scene.boss == null)
            return;
        Entity actor = scene.boss;
        var eye = event.getCamera().getPosition();
        float partial = event.getPartialTick();
        double x = net.minecraft.util.Mth.lerp(partial, actor.xo, actor.getX()) - eye.x;
        double y = net.minecraft.util.Mth.lerp(partial, actor.yo, actor.getY()) - eye.y + actor.getBbHeight() * .55;
        double z = net.minecraft.util.Mth.lerp(partial, actor.zo, actor.getZ()) - eye.z;
        Vector4f point = new Vector4f((float) x, (float) y, (float) z, 1);
        new Matrix4f(event.getProjectionMatrix()).mul(com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix()).mul(event.getPoseStack().last().pose()).transform(point);
        if (point.w <= .001F || !Float.isFinite(point.w)) return;
        double nx = point.x / point.w, ny = point.y / point.w;
        if (!Double.isFinite(nx) || !Double.isFinite(ny)) return;
        scene.flareX = (nx * .5 + .5) * scene.width;
        scene.flareY = (.5 - ny * .5) * scene.height;
        scene.flareValid = true;
    }

    private CutsceneMusic music;
    private Entity boss, previous;
    private ArmorStand anchor;
    private CameraType previousType;
    private long exitAt = -1, spaceSince = -1;
    private boolean attached, restored, finished;
    private float yaw, pitch, roll;
    private double localAge;
    private long opened = System.nanoTime();

    private CutsceneScreen(CutsceneNetwork.Start p) {
        super(Component.literal("Cutscene"));
        packet = p;
        config = JsonParser.parseString(p.json()).getAsJsonObject();
        world = Minecraft.getInstance().level;
    }

    public static void open(CutsceneNetwork.Start p) {
        try {
            if (active != null) active.cleanup();
            active = new CutsceneScreen(p);
            Minecraft.getInstance().setScreen(active);
            com.benji.oasiso.client.gui.CutsceneFightOverlay.prepare();
            active.music = new CutsceneMusic();
            Minecraft.getInstance().getSoundManager().play(active.music);
        } catch (Exception ex) {
            if (active != null) {
                active.cleanup();
                Minecraft.getInstance().setScreen(null);
            }
            com.mojang.logging.LogUtils.getLogger().error("Cannot start cutscene", ex);
            CutsceneNetwork.finish(p.token());
        }
    }

    public static void stop(UUID token) {
        if (active != null && active.packet.token().equals(token)) {
            active.cleanup();
            Minecraft.getInstance().setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    double number(String key, double fallback) {
        return config.has(key) ? config.get(key).getAsDouble() : fallback;
    }

    String string(String key, String fallback) {
        return config.has(key) ? config.get(key).getAsString() : fallback;
    }

    double elapsed() {
        return Math.max(0, (world.getGameTime() + Minecraft.getInstance().getFrameTime() - packet.start()) / 20.0);
    }

    boolean title() {
        return elapsed() >= number("duration", 2.375);
    }

    private void leave() {
        if (exitAt < 0) {
            exitAt = System.nanoTime();
            if (music != null) music.fadeOut();
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (title()) leave();
        else if (key == GLFW.GLFW_KEY_SPACE && spaceSince < 0) spaceSince = System.nanoTime();
        return true;
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_SPACE) spaceSince = -1;
        return true;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (title()) leave();
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (title()) leave();
        return true;
    }

    @Override
    public void onClose() {
        if (title()) leave();
    }

    @Override
    public void tick() {
        if (minecraft.level != world || minecraft.player == null || !minecraft.player.isAlive()) {
            cleanup();
            minecraft.setScreen(null);
            return;
        }
        if (boss == null) for (Entity e : world.entitiesForRendering())
            if (e.getUUID().equals(packet.boss())) {
                boss = e;
                break;
            }
        if (boss == null && (System.nanoTime() - opened) > 5_000_000_000L) {
            cleanup();
            minecraft.setScreen(null);
            return;
        }
        if (boss != null && !boss.isAlive()) {
            cleanup();
            minecraft.setScreen(null);
            return;
        }
        if (!minecraft.isWindowActive()) spaceSince = -1;
        if (spaceSince >= 0 && (System.nanoTime() - spaceSince) >= 3_000_000_000L) leave();
        if (exitAt >= 0 && (System.nanoTime() - exitAt) / 1e9 > CutsceneTiming.total(config)) {
            cleanup();
            minecraft.setScreen(null);
            com.benji.oasiso.client.gui.CutsceneFightOverlay.afterCutscene(boss);
        }
    }

    private void restore() {
        if (restored) return;
        restored = true;
        if (attached) {
            minecraft.setCameraEntity(previous != null && !previous.isRemoved() ? previous : minecraft.player);
            minecraft.options.setCameraType(previousType);
        }
        KeyMapping.releaseAll();
    }

    private void cleanup() {
        if (music != null) music.fadeOut();
        restore();
        CutsceneForeground.release();
        if (!finished) {
            finished = true;
            if (Minecraft.getInstance().getConnection() != null) CutsceneNetwork.finish(packet.token());
        }
        if (active == this) active = null;
    }

    @Override
    public void removed() {
        cleanup();
    }

    private void camera() {
        if (boss == null || restored) return;
        double opening = (System.nanoTime() - opened) / 1e9;
        if (opening < CutsceneTiming.half(config)) return;
        if (exitAt >= 0 && (System.nanoTime() - exitAt) / 1e9 >= CutsceneTiming.half(config)) {
            restore();
            return;
        }
        if (!attached) {
            previous = minecraft.getCameraEntity();
            previousType = minecraft.options.getCameraType();
            anchor = new ArmorStand(world, 0, 0, 0);
            anchor.setInvisible(true);
            attached = true;
            KeyMapping.releaseAll();
            minecraft.setCameraEntity(anchor);
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        }
        JsonObject track = config.getAsJsonObject("camera");
        double time = Math.min(elapsed(), number("duration", 2.375));
        Vector3f pos = CameraTrack.sample(track.getAsJsonArray("position"), time), rot = CameraTrack.sample(track.getAsJsonArray("rotation"), time);
        if (track.has("pivot")) {
            var pivot = track.getAsJsonArray("pivot");
            pos.add(pivot.get(0).getAsFloat(), pivot.get(1).getAsFloat(), pivot.get(2).getAsFloat());
        }
        float heading = (float) Math.toRadians(180 - boss.getYRot());
        Vector3f offset = new Vector3f(-pos.x, pos.y, pos.z).mul((float) (number("model_scale", 1) / 16)).rotateY(heading);
        Quaternionf q = new Quaternionf().rotateY(heading).rotateZYX((float) Math.toRadians(rot.z), (float) Math.toRadians(-rot.y), (float) Math.toRadians(-rot.x));
        Vector3f forward = q.transform(new Vector3f(0, 0, -1)), up = q.transform(new Vector3f(0, 1, 0));
        yaw = (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
        pitch = (float) Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, -forward.y))));
        Vector3f right = new Vector3f((float) Math.cos(Math.toRadians(yaw)), 0, (float) Math.sin(Math.toRadians(yaw)));
        Vector3f normalUp = new Vector3f(forward).cross(right).normalize();
        roll = (float) Math.toDegrees(Math.atan2(up.dot(right), up.dot(normalUp)));
        double x = boss.getX() + offset.x, y = boss.getY() + offset.y - anchor.getEyeHeight(), z = boss.getZ() + offset.z;
        anchor.setPos(x, y, z);
        anchor.xo = anchor.xOld = x;
        anchor.yo = anchor.yOld = y;
        anchor.zo = anchor.zOld = z;
        anchor.setYRot(yaw);
        anchor.yRotO = yaw;
        anchor.setXRot(pitch);
        anchor.xRotO = pitch;
    }

    @SubscribeEvent
    public static void angles(ViewportEvent.ComputeCameraAngles e) {
        if (active != null && active.attached && !active.restored) {
            e.setYaw(active.yaw);
            e.setPitch(active.pitch);
            e.setRoll(active.roll);
        }
    }

    @SubscribeEvent
    public static void fov(ViewportEvent.ComputeFov e) {
        if (active != null && active.attached && !active.restored) {
            double aspect = active.number("aspect", 16.0 / 9);
            double h = Math.min(active.height, active.width / aspect);
            e.setFOV(Math.toDegrees(2 * Math.atan(Math.tan(Math.toRadians(active.number("fov", 70)) / 2) * active.height / h)));
        }
    }

    @SubscribeEvent
    public static void renderTick(net.minecraftforge.event.TickEvent.RenderTickEvent e) {
        if (e.phase == net.minecraftforge.event.TickEvent.Phase.START && active != null && Minecraft.getInstance().screen == active)
            active.camera();
    }

    @SubscribeEvent
    public static void hand(RenderHandEvent e) {
        if (active != null && active.attached && !active.restored) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void hud(RenderGuiOverlayEvent.Pre e) {
        if (active != null) e.setCanceled(true);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        localAge = (System.nanoTime() - opened) / 1e9;
        if (attached && !restored) {

            g.flush();
            try (CutsceneRenderState saved = new CutsceneRenderState()) {
                if (CutsceneForeground.available(this)) {
                    double dim = Math.max(0, Math.min(.9, number("background_dim", .50)));
                    g.fill(0, 0, width, height, ((int) (255 * dim) << 24));
                    if (title()) CutsceneVisuals.title(g, this, mx, my, elapsed() - number("duration", 2.375));
                    g.flush();
                    CutsceneForeground.composite(g, this);
                }
                if (title()) CutsceneVisuals.edges(g, this, elapsed() - number("duration", 2.375));
                double aspect = number("aspect", 16.0 / 9);
                int vw = (int) Math.min(width, height * aspect), vh = (int) (vw / aspect);
                int l = (width - vw) / 2, t = (height - vh) / 2;
                g.fill(0, 0, width, t, 0xff000000);
                g.fill(0, height - t, width, height, 0xff000000);
                g.fill(0, 0, l, height, 0xff000000);
                g.fill(width - l, 0, width, height, 0xff000000);
                boolean held = spaceSince >= 0;
                float progress = held ? (float) Math.min(1, (System.nanoTime() - spaceSince) / 3e9) : 0;
                float scale = (float) Math.max(.5, Math.min(3, number("skip_scale", 1)));
                int sw = (int) Math.ceil(39 * scale), sh = (int) Math.ceil(16 * scale);
                int dx = width - sw - 12 + (held ? (int) (Math.sin(localAge * 85) * 1.2) : 0), dy = height - sh - 12;
                ResourceLocation tex = ResourceLocation.tryParse(string(held ? "skip_hold" : "skip", held ? "oasiso:textures/gui/space_skip_hold.png" : "oasiso:textures/gui/space_skip.png"));
                com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
                g.pose().pushPose();
                g.pose().translate(dx, dy, 0);
                g.pose().scale(scale, scale, 1);
                g.blit(tex, 0, 0, 0, 0, 39, 16, 39, 16);
                g.pose().popPose();
                if (held) g.fill(dx, dy + sh + 1, dx + (int) (sw * progress), dy + sh + 2, 0xffa9f5ff);
                g.flush();
            }
        }
        double half = CutsceneTiming.half(config), hold = CutsceneTiming.hold(config);
        double coverage = CutsceneTiming.coverage(localAge, half, hold);
        if (boss == null && localAge >= half) coverage = 1;
        if (exitAt >= 0) coverage = CutsceneTiming.coverage((System.nanoTime() - exitAt) / 1e9, half, hold);
        CutsceneVisuals.wipe(g, width, height, Math.max(0, Math.min(1, coverage)));
    }
}
