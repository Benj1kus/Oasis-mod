package com.benji.oasiso.client.gravesoul;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.ModSounds;
import com.benji.oasiso.common.gravesoul.GraveSoulNetwork;
import com.benji.oasiso.client.cutscene.CameraTrack;
import com.google.gson.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import org.joml.Vector3f;
import org.joml.Quaternionf;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class GraveSoulScene extends Screen {
    public static final int TYPE_TICKS = 2, READ_HOLD = 20, CLOSE_TICKS = 25, COVER_HOLD = 6, OPEN_TICKS = 20;
    public static final float MODEL_SCALE = 1, FOV = 70, ASPECT = 16F / 9F;
    private static final ResourceLocation OVERLAY = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/soul_overlay.png");
    private static GraveSoulScene active;
    private final GraveSoulNetwork.Scene packet;
    private final ClientLevel world;
    private final JsonObject track;
    private final int[] chars;
    private final int attackTicks, closeAt, restoreAt, endAt;
    private Entity soul, previous;
    private ArmorStand anchor;
    private CameraType previousType;
    private Vec3 initial;
    private float initialYaw, initialPitch, yaw, pitch, roll;
    private int age, voiced, wait;
    private boolean attached, restored, finished;

    private GraveSoulScene(GraveSoulNetwork.Scene p) throws Exception {
        super(Component.literal("Grave soul"));
        packet = p;
        world = Minecraft.getInstance().level;
        try (var reader = Minecraft.getInstance().getResourceManager().openAsReader(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "grave_soul/camera.json"))) {
            track = JsonParser.parseReader(reader).getAsJsonObject();
        }
        chars = Component.translatable("dialogue.oasiso.grave_soul." + p.phrase()).getString().codePoints().limit(200).toArray();
        attackTicks = (int) Math.ceil(track.get("duration").getAsDouble() * 20);
        closeAt = attackTicks + chars.length * TYPE_TICKS + READ_HOLD;
        restoreAt = closeAt + CLOSE_TICKS + COVER_HOLD;
        endAt = restoreAt + OPEN_TICKS;
    }

    public static void receive(GraveSoulNetwork.Scene p) {
        Minecraft mc = Minecraft.getInstance();
        if (p.stop()) {
            if (active != null && active.packet.token().equals(p.token())) active.closeScene();
            return;
        }
        if (mc.player == null || mc.level == null || mc.screen != null || active != null) {
            GraveSoulNetwork.finish(p.token());
            return;
        }
        try {
            active = new GraveSoulScene(p);
            mc.setScreen(active);
            KeyMapping.releaseAll();
        } catch (Exception ex) {
            com.mojang.logging.LogUtils.getLogger().error("Cannot open grave soul scene", ex);
            active = null;
            GraveSoulNetwork.finish(p.token());
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

    @Override
    public void onClose() {
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
    }

    @Override
    public void tick() {
        if (minecraft.level != world || minecraft.player == null || !minecraft.player.isAlive()) {
            closeScene();
            return;
        }
        if (soul == null) {
            for (Entity e : world.entitiesForRendering())
                if (e.getUUID().equals(packet.soul())) {
                    soul = e;
                    break;
                }
            if (soul == null) {
                if (++wait > 100) closeScene();
                return;
            }
        }
        if (!soul.isAlive() || soul.isRemoved()) {
            closeScene();
            return;
        }
        age++;
        int visible = visible(age);
        for (int i = voiced; i < visible; i++)
            if (Character.isLetterOrDigit(chars[i]))
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.SOUL_VOICE.get(), .94F + world.random.nextFloat() * .12F, .6F));
        voiced = visible;
        if (age == closeAt + CLOSE_TICKS) {
            if (soul instanceof com.benji.oasiso.common.entity.GraveSoulEntity grave) grave.stopAttackPose();
            GraveSoulNetwork.stopPose(packet.token());
        }
        if (age >= restoreAt) restore();
        if (age >= endAt) closeScene();
    }

    private int visible(float t) {
        return Mth.clamp((int) ((t - attackTicks) / TYPE_TICKS), 0, chars.length);
    }

    private void restore() {
        if (restored) return;
        restored = true;
        if (attached) {
            minecraft.setCameraEntity(minecraft.level == world && previous != null && !previous.isRemoved() ? previous : minecraft.player);
            minecraft.options.setCameraType(previousType);
        }
        KeyMapping.releaseAll();
    }

    private void cleanup() {
        restore();
        if (!finished) {
            finished = true;
            if (Minecraft.getInstance().getConnection() != null) GraveSoulNetwork.finish(packet.token());
        }
        if (active == this) active = null;
    }

    private void closeScene() {
        cleanup();
        if (minecraft.screen == this) minecraft.setScreen(null);
    }

    @Override
    public void removed() {
        cleanup();
    }

    private void camera() {
        if (soul == null || restored || minecraft.level != world) return;
        if (!attached) {
            previous = minecraft.getCameraEntity();
            previousType = minecraft.options.getCameraType();
            var camera = minecraft.gameRenderer.getMainCamera();
            initial = camera.getPosition();
            initialYaw = camera.getYRot();
            initialPitch = camera.getXRot();
            anchor = new ArmorStand(world, 0, 0, 0);
            anchor.setInvisible(true);
            attached = true;
            minecraft.setCameraEntity(anchor);
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        }
        double t = Math.min((age + minecraft.getFrameTime()) / 20D, track.get("duration").getAsDouble());
        Vector3f pos = CameraTrack.sample(track.getAsJsonArray("position"), t), rot = CameraTrack.sample(track.getAsJsonArray("rotation"), t);
        float heading = (float) Math.toRadians(180 - soul.getYRot());
        Vector3f offset = new Vector3f(-pos.x, pos.y, pos.z).mul(MODEL_SCALE / 16).rotateY(heading);
        Quaternionf q = new Quaternionf().rotateY(heading).rotateZYX((float) Math.toRadians(rot.z), (float) Math.toRadians(-rot.y), (float) Math.toRadians(-rot.x));
        Vector3f forward = q.transform(new Vector3f(0, 0, -1)), up = q.transform(new Vector3f(0, 1, 0));
        float targetYaw = (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
        float targetPitch = (float) Math.toDegrees(Math.asin(Mth.clamp(-forward.y, -1, 1)));
        Vector3f right = new Vector3f((float) Math.cos(Math.toRadians(targetYaw)), 0, (float) Math.sin(Math.toRadians(targetYaw)));
        float targetRoll = (float) Math.toDegrees(Math.atan2(up.dot(right), up.dot(new Vector3f(forward).cross(right).normalize())));
        float blend = Mth.clamp((age + minecraft.getFrameTime()) / 10F, 0, 1);
        blend = blend * blend * (3 - 2 * blend);
        Vec3 eye = initial.lerp(soul.position().add(offset.x, offset.y, offset.z), blend);
        yaw = initialYaw + Mth.wrapDegrees(targetYaw - initialYaw) * blend;
        pitch = Mth.lerp(blend, initialPitch, targetPitch);
        roll = targetRoll * blend;
        double x = eye.x, y = eye.y - anchor.getEyeHeight(), z = eye.z;
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
    public static void renderTick(TickEvent.RenderTickEvent e) {
        if (e.phase == TickEvent.Phase.START && active != null && Minecraft.getInstance().screen == active)
            active.camera();
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
            double h = Math.min(active.height, active.width / ASPECT);
            double target = Math.toDegrees(2 * Math.atan(Math.tan(Math.toRadians(FOV) / 2) * active.height / Math.max(1, h)));
            float t = Mth.clamp((active.age + Minecraft.getInstance().getFrameTime()) / 10F, 0, 1);
            t = t * t * (3 - 2 * t);
            e.setFOV(e.getFOV() + (target - e.getFOV()) * t);
        }
    }

    @SubscribeEvent
    public static void hand(RenderHandEvent e) {
        if (active != null && !active.restored) e.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void overlay(ScreenEvent.Render.Post e) {
        if (active != null && e.getScreen() == active) active.paint(e.getGuiGraphics(), e.getPartialTick());
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut e) {
        if (active != null) active.cleanup();
    }

    private void paint(GuiGraphics g, float partial) {
        float t = age + partial;
        g.flush();
        try (SoulRenderState saved = new SoulRenderState()) {
            RenderSystem.colorMask(true, true, true, false);
            RenderSystem.depthMask(false);
            g.pose().pushPose();
            try {
                g.pose().translate(0, 0, 600);
                float opacity = restored ? Mth.clamp(1 - (t - restoreAt) / OPEN_TICKS, 0, 1) : Mth.clamp(t / 10F, 0, 1);
                int vw = (int) Math.min(width, height * ASPECT), vh = (int) (vw / ASPECT), left = (width - vw) / 2, top = (height - vh) / 2;
                int black = ((int) (opacity * 255) << 24);
                g.fill(0, 0, width, top, black);
                g.fill(0, top + vh, width, height, black);
                g.fill(0, top, left, top + vh, black);
                g.fill(left + vw, top, width, top + vh, black);
                g.flush();
                RenderSystem.disableDepthTest();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1, 1, 1, opacity);
                float scale = Math.max(width / 1920F, height / 1080F);
                int ow = (int) Math.ceil(1920 * scale), oh = (int) Math.ceil(1080 * scale);
                g.blit(OVERLAY, (width - ow) / 2, (height - oh) / 2, ow, oh, 0F, 0F, 1920, 1080, 1920, 1080);
                RenderSystem.setShaderColor(1, 1, 1, 1);
                if (t >= attackTicks && t < restoreAt) {
                    String text = new String(chars, 0, visible(t));
                    var lines = font.split(Component.literal(text), Math.max(40, Math.min(vw - 32, 420)));
                    int y = Math.min(height - 60, top + vh - 24) - lines.size() * 11;
                    int max = 0;
                    for (var line : lines) max = Math.max(max, font.width(line));
                    if (!text.isEmpty())
                        g.fill((width - max) / 2 - 7, y - 5, (width + max) / 2 + 7, y + lines.size() * 11 + 4, 0xA6000000);
                    for (var line : lines) {
                        g.drawString(font, line, (width - font.width(line)) / 2, y, 0x5FFFCC, true);
                        y += 11;
                    }
                }
                if (t >= closeAt) {
                    float progress = t < restoreAt ? Mth.clamp((t - closeAt) / CLOSE_TICKS, 0, 1) : 1 - Mth.clamp((t - restoreAt) / OPEN_TICKS, 0, 1);
                    int cell = Math.max(8, (int) Math.ceil(width / 44F));
                    for (int y = 0; y < height; y += cell)
                        for (int x = 0; x < width; x += cell) {
                            float nx = (x + cell * .5F) / width, ny = (y + cell * .5F) / height;
                            float distance = (Math.min(nx, 1 - nx) + Math.min(ny, 1 - ny));
                            float noise = ((x / cell * 73856093L ^ y / cell * 19349663L) & 1023) / 1023F;
                            float threshold = .06F + .77F * distance + .15F * noise;
                            if (progress >= threshold) {
                                float since = (progress - threshold) * CLOSE_TICKS;
                                boolean white = t < restoreAt && since < 4 && ((int) since % 2 == 0);
                                g.fill(x, y, Math.min(width, x + cell), Math.min(height, y + cell), white ? 0xFFFFFFFF : 0xFF5FFFCC);
                            }
                        }
                }
                g.flush();
            } finally {
                g.pose().popPose();
            }
        }
    }
}
