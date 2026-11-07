package com.benji.oasiso.client.soulcast;

import com.benji.oasiso.common.soulcast.*;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.*;

@Mod.EventBusSubscriber(modid = "oasiso", value = Dist.CLIENT)
public final class SoulCastClient {
    private static final KeyMapping KEY = new KeyMapping("key.oasiso_soulcast.summon", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.oasiso_soulcast");
    private static final Map<UUID, SoulCastNetwork.State> STATES = new HashMap<>();
    private static final Map<UUID, SoulPlayerVisual> VISUALS = new HashMap<>();
    private static ClientLevel level;
    private static HeadCamera camera;
    private static Entity oldCamera;
    private static CameraType oldType, lastType;
    private static boolean viewChanged;
    private static float oldPitch, roll;
    private static long requestAt = -100;

    @Mod.EventBusSubscriber(modid = "oasiso", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Keys {
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent e) {
            e.register(KEY);
        }
    }

    public static void receive(SoulCastNetwork.State s) {
        checkWorld();
        if (s.active()) {
            var old = STATES.get(s.player());
            if (old == null || old.start() != s.start()) VISUALS.remove(s.player());
            STATES.put(s.player(), s);
        } else {
            var old = STATES.get(s.player());
            if (old == null || old.start() == s.start()) {
                STATES.remove(s.player());
                VISUALS.remove(s.player());
            }
        }
    }

    private static void checkWorld() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != level || mc.player == null) {
            restore();
            STATES.clear();
            VISUALS.clear();
            level = mc.level;
            requestAt = -100;
        }
    }

    private static SoulPlayerVisual visual(AbstractClientPlayer p) {
        var s = STATES.get(p.getUUID());
        if (s == null || !p.isAlive() || !SoulCast.fullSet(p) || p.level().getGameTime() - s.start() >= SoulCast.DURATION)
            return null;
        return VISUALS.computeIfAbsent(p.getUUID(), k -> new SoulPlayerVisual(p, s.start(), s.yaw()));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        checkWorld();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        STATES.values().removeIf(s -> mc.level.getGameTime() - s.start() >= SoulCast.DURATION);
        VISUALS.keySet().removeIf(id -> !STATES.containsKey(id));
        while (KEY.consumeClick()) {
            if (mc.screen == null && mc.getCameraEntity() == mc.player && !STATES.containsKey(mc.player.getUUID()) && SoulCast.fullSet(mc.player) && mc.level.getGameTime() - requestAt >= 5) {
                requestAt = mc.level.getGameTime();
                SoulCastNetwork.request();
            }
        }
        // A tracked player may be outside the view frustum at the marker.
        for (var id : STATES.keySet()) {
            if (mc.level.getPlayerByUUID(id) instanceof AbstractClientPlayer actor
                    && actor.distanceToSqr(mc.player) <= SoulStrikeEffects.VIEW_DISTANCE_SQR) {
                var cast = visual(actor);
                if (cast != null && !cast.strikePlayed) cast.prepare();
            }
        }
        if (visual(mc.player) == null) restore();
    }

    @SubscribeEvent
    public static void frame(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        checkWorld();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        SoulPlayerVisual a = visual(mc.player);
        if (a == null) {
            restore();
            return;
        }
        if (!a.strikePlayed) a.prepare();
        if (camera == null) {
            if (mc.getCameraEntity() != mc.player) return;
            oldCamera = mc.getCameraEntity();
            oldType = mc.options.getCameraType();
            oldPitch = mc.player.getXRot();
            camera = new HeadCamera(mc.level, mc.player.getEyeHeight());
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            lastType = CameraType.FIRST_PERSON;
            viewChanged = false;
            mc.setCameraEntity(camera);
        }
        if (mc.getCameraEntity() != camera) return;
        trackPerspective(mc);
        if (!mc.options.getCameraType().isFirstPerson()) {
            roll = 0;
            moveCamera(mc.player.getPosition(mc.getFrameTime()).add(0, mc.player.getEyeHeight(), 0), mc.player.getYRot(), mc.player.getXRot());
            return;
        }
        Matrix4f head = a.head();
        Vector3f eye = head.transformPosition(new Vector3f(0, 27.648F / 16F, 0));
        Vector3f forward = head.transformDirection(new Vector3f(0, 0, -1)).normalize();
        Vector3f up = head.transformDirection(new Vector3f(0, 1, 0)).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
        float pitch = (float) Math.toDegrees(-Math.asin(Mth.clamp(forward.y, -1, 1)));
        float age = (float) a.age(), mix = Math.min(1, Math.min(age / 3F, (SoulCast.DURATION - age) / 3F));
        mix = Mth.clamp(mix, 0, 1);
        mix = mix * mix * (3 - 2 * mix);
        float part = mc.getFrameTime();
        Vec3 body = mc.player.getPosition(part);
        Vec3 base = body.add(0, mc.player.getEyeHeight(), 0);
        Vec3 target = base.lerp(body.add(eye.x, eye.y, eye.z), mix);
        yaw = a.yaw + Mth.wrapDegrees(yaw - a.yaw) * mix;
        pitch = Mth.lerp(mix, oldPitch, pitch);
        double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
        Vector3f right = new Vector3f((float) -Math.cos(yr), 0, (float) -Math.sin(yr));
        Vector3f normalUp = new Vector3f((float) (-Math.sin(yr) * Math.sin(pr)), (float) Math.cos(pr), (float) (Math.cos(yr) * Math.sin(pr)));
        roll = (float) Math.toDegrees(Math.atan2(up.dot(right), up.dot(normalUp))) * mix;
        moveCamera(target, yaw, pitch);
        mc.player.setYRot(a.yaw);
        mc.player.setXRot(oldPitch);
    }

    private static void moveCamera(Vec3 eye, float yaw, float pitch) {
        camera.setPos(eye.x, eye.y - camera.eye, eye.z);
        camera.xo = camera.xOld = camera.getX();
        camera.yo = camera.yOld = camera.getY();
        camera.zo = camera.zOld = camera.getZ();
        camera.setYRot(yaw);
        camera.yRotO = yaw;
        camera.setXRot(pitch);
        camera.xRotO = pitch;
    }

    private static void trackPerspective(Minecraft mc) {
        CameraType current = mc.options.getCameraType();
        if (lastType != null && current != lastType) viewChanged = true;
        lastType = current;
    }

    @SubscribeEvent
    public static void angles(ViewportEvent.ComputeCameraAngles e) {
        if (camera != null && e.getCamera().getEntity() == camera && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            e.setYaw(camera.getYRot());
            e.setPitch(camera.getXRot());
            e.setRoll(roll);
        }
    }

    @SubscribeEvent
    public static void player(RenderPlayerEvent.Pre e) {
        if (!(e.getEntity() instanceof AbstractClientPlayer p)) return;
        SoulPlayerVisual a = visual(p);
        if (a == null) return;
        e.setCanceled(true);
        a.draw(e.getPoseStack(), e.getMultiBufferSource(), e.getPackedLight(), p == Minecraft.getInstance().player && camera != null && Minecraft.getInstance().getCameraEntity() == camera && Minecraft.getInstance().options.getCameraType().isFirstPerson());
    }

    @SubscribeEvent
    public static void hands(RenderHandEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && visual(mc.player) != null) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void input(MovementInputUpdateEvent e) {
        if (!(e.getEntity() instanceof AbstractClientPlayer p) || visual(p) == null) return;
        var i = e.getInput();
        i.forwardImpulse = 0;
        i.leftImpulse = 0;
        i.jumping = false;
        i.shiftKeyDown = false;
    }

    private static void restore() {
        Minecraft mc = Minecraft.getInstance();
        if (camera != null) {
            boolean owned = mc.getCameraEntity() == camera;
            if (owned) trackPerspective(mc);
            if (owned)
                mc.setCameraEntity(oldCamera != null && !oldCamera.isRemoved() && oldCamera.level() == mc.level ? oldCamera : mc.player);
            if (!viewChanged && oldType != null && (owned || mc.level != level || mc.player == null))
                mc.options.setCameraType(oldType);
        }
        camera = null;
        oldCamera = null;
        oldType = null;
        lastType = null;
        viewChanged = false;
        roll = 0;
    }

    private static final class HeadCamera extends Entity {
        final float eye;

        HeadCamera(ClientLevel level, float eye) {
            super(EntityType.ARMOR_STAND, level);
            this.eye = eye;
            refreshDimensions();
        }

        @Override
        protected float getEyeHeight(Pose p, EntityDimensions size) {
            return eye;
        }

        @Override
        protected void defineSynchedData() {
        }

        @Override
        protected void readAdditionalSaveData(CompoundTag n) {
        }

        @Override
        protected void addAdditionalSaveData(CompoundTag n) {
        }

        @Override
        public boolean isSpectator() {
            return true;
        }
    }
}
