package com.benji.oasiso.client.wings;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.wings.ApolWingsTransitNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ApolWingsTransitClient {
    private record Hidden(long until, Vec3 anchor) {
    }

    private record Blob(Vec3 pos, Vec3 normal, long start) {
    }

    private static final Map<UUID, Hidden> HIDDEN = new HashMap<>();
    private static final List<Blob> BLOBS = new ArrayList<>();
    private static final ResourceLocation BLACK = texture("gui/black_overlay.png");
    private static final ResourceLocation[] FRAMES = {texture("particle/entropy_blob1.png"), texture("particle/entropy_blob2.png"), texture("particle/entropy_blob3.png")};
    private static ClientLevel world;
    private static boolean locked, oldGravity;
    private static long start;

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/" + name);
    }

    private static void syncWorld() {
        Minecraft mc = Minecraft.getInstance();
        if (world != mc.level) {
            unlock();
            world = mc.level;
            HIDDEN.clear();
            BLOBS.clear();
        }
    }

    private static void unlock() {
        var p = Minecraft.getInstance().player;
        if (locked && p != null) p.setNoGravity(oldGravity);
        locked = false;
    }

    public static void receive(ApolWingsTransitNetwork.Fx p) {
        syncWorld();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        boolean self = mc.player != null && mc.player.getUUID().equals(p.player());
        if (p.kind() == 0) {
            HIDDEN.put(p.player(), new Hidden(now + p.ticks() + 60, p.pos()));
            if (self) {
                if (!locked) {
                    oldGravity = mc.player.isNoGravity();
                    start = now;
                }
                locked = true;
                mc.player.stopFallFlying();
                mc.player.stopUsingItem();
                mc.player.setNoGravity(true);
                mc.player.setDeltaMovement(Vec3.ZERO);
            }
        } else if (p.kind() == 1) {
            HIDDEN.remove(p.player());
            if (self) {
                unlock();
                mc.player.setDeltaMovement(p.motion());
                mc.player.fallDistance = 0;
                if (p.motion().lengthSqr() > 0) mc.player.startFallFlying();
            }
        } else if (p.kind() == 2) {
            if (BLOBS.size() >= 128) BLOBS.remove(0);
            BLOBS.add(new Blob(p.pos(), p.normal(), now));
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        syncWorld();
        Minecraft mc = Minecraft.getInstance();
        if (world == null) return;
        HIDDEN.entrySet().removeIf(v -> v.getValue().until() < world.getGameTime());
        BLOBS.removeIf(b -> world.getGameTime() - b.start() >= 120);
        if (mc.player == null) return;
        Hidden h = HIDDEN.get(mc.player.getUUID());
        if (h == null) {
            unlock();
            return;
        }
        mc.player.setDeltaMovement(Vec3.ZERO);
        mc.player.setNoGravity(true);
        mc.player.fallDistance = 0;
        mc.player.setPos(h.anchor().x, h.anchor().y, h.anchor().z);
        mc.options.keyAttack.setDown(false);
        mc.options.keyUse.setDown(false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void input(MovementInputUpdateEvent e) {
        if (!HIDDEN.containsKey(e.getEntity().getUUID())) return;
        var i = e.getInput();
        i.forwardImpulse = i.leftImpulse = 0;
        i.jumping = i.shiftKeyDown = false;
        i.up = i.down = i.left = i.right = false;
    }

    @SubscribeEvent
    public static void renderPlayer(RenderPlayerEvent.Pre e) {
        if (HIDDEN.containsKey(e.getEntity().getUUID())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void hand(RenderHandEvent e) {
        if (locked) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void screen(ScreenEvent.Opening e) {
        if (locked && e.getNewScreen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)
            e.setCanceled(true);
    }

    @SubscribeEvent
    public static void interaction(InputEvent.InteractionKeyMappingTriggered e) {
        if (locked) {
            e.setCanceled(true);
            e.setSwingHand(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void overlay(RenderGuiEvent.Post e) {
        if (!locked) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        var g = e.getGuiGraphics();

        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(BLACK, 0, 0, w, h, 0, 0, 1920, 1080, 1920, 1080);
        g.flush();

        var shader = ApolWingsTransitShaders.shader;
        if (shader == null) {
            RenderSystem.disableBlend();
            return;
        }

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        try {
            RenderSystem.setShader(() -> shader);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            shader.safeGetUniform("Time").set((mc.level.getGameTime() - start + e.getPartialTick()) / 20F);
            shader.safeGetUniform("Resolution").set((float) mc.getWindow().getWidth(), (float) mc.getWindow().getHeight());

            BufferBuilder b = Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            b.vertex(-1, -1, 0).uv(0, 0).endVertex();
            b.vertex(1, -1, 0).uv(1, 0).endVertex();
            b.vertex(1, 1, 0).uv(1, 1).endVertex();
            b.vertex(-1, 1, 0).uv(0, 1).endVertex();
            BufferUploader.drawWithShader(b.end());
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @SubscribeEvent
    public static void blobs(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || BLOBS.isEmpty() || world == null) return;
        Vec3 camera = e.getCamera().getPosition();
        var matrix = e.getPoseStack().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        try {
            for (Blob blob : BLOBS) {
                if (blob.pos().distanceToSqr(camera) > 96 * 96) continue;
                float age = world.getGameTime() - blob.start() + e.getPartialTick();
                float alpha = Math.min(1, age / 3F) * Math.max(0, Math.min(1, (120 - age) / 20F));
                int frame = new int[]{0, 1, 2, 1}[(int) (age / 7) % 4];
                Vec3 n = blob.normal(), axis = Math.abs(n.y) > .5 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
                Vec3 right = axis.cross(n).normalize(), up = n.cross(right).normalize();
                Vec3 center = blob.pos().add(n.scale(.012)).subtract(camera);
                double radius = .85 * (.95 + .05 * Math.sin(age * .22));
                RenderSystem.setShaderTexture(0, FRAMES[frame]);
                BufferBuilder b = Tesselator.getInstance().getBuilder();
                b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                vertex(b, matrix, center.add(right.scale(-radius)).add(up.scale(-radius)), 0, 1, alpha);
                vertex(b, matrix, center.add(right.scale(radius)).add(up.scale(-radius)), 1, 1, alpha);
                vertex(b, matrix, center.add(right.scale(radius)).add(up.scale(radius)), 1, 0, alpha);
                vertex(b, matrix, center.add(right.scale(-radius)).add(up.scale(radius)), 0, 0, alpha);
                BufferUploader.drawWithShader(b.end());
            }
        } finally {
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }

    private static void vertex(BufferBuilder b, org.joml.Matrix4f m, Vec3 p, float u, float v, float a) {
        b.vertex(m, (float) p.x, (float) p.y, (float) p.z).uv(u, v).color(1F, 1F, 1F, a).endVertex();
    }

    private ApolWingsTransitClient() {
    }
}
