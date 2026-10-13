package com.benji.oasiso.client.wings;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.ModSounds;
import com.benji.oasiso.common.entity.ai.ApollyonShockwaveAttack;
import com.benji.oasiso.common.wings.ApolWingsMechanics;
import com.benji.oasiso.common.wings.ApolWingsNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ApolWingsClient {
    public record Wave(Vec3 origin, long start, int seed, Vec3 normal, double radius, boolean air) {
        public Wave(Vec3 origin, long start, int seed) {
            this(origin, start, seed, new Vec3(0, 1, 0), ApollyonShockwaveAttack.WAVE_RADIUS, false);
        }
    }

    public static final List<Wave> WAVES = new ArrayList<>();
    private static final ResourceLocation[] SCALE = {texture("empty"), texture("slow"), texture("normal"), texture("full")};

    private static final double[] AIR_WAVE_RADIUS = {0, 2.0, 3.5, 5.0};
    private static ClientLevel world;
    private static boolean downBefore, charging, airCharge;
    private static long localStart, windupUntil, airReplyUntil;
    private static int requestId;

    private static ResourceLocation texture(String frame) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/wings_scale_" + frame + ".png");
    }

    private static void send(int action) {
        ApolWingsNetwork.CHANNEL.sendToServer(new ApolWingsNetwork.Input(action, 0, requestId));
    }

    private static void resetCharge() {
        charging = airCharge = false;
        windupUntil = airReplyUntil = 0;
    }

    private static boolean activeModeValid() {
        var p = Minecraft.getInstance().player;
        return p != null && (airCharge ? ApolWingsMechanics.canAirCharge(p) : ApolWingsMechanics.canCharge(p));
    }

    @SubscribeEvent
    public static void input(MovementInputUpdateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || event.getEntity() != p || mc.level == null) return;
        boolean down = mc.options.keyJump.isDown();
        boolean active = charging || windupUntil > 0 || airReplyUntil > 0;
        boolean eligible = active ? activeModeValid() : ApolWingsMechanics.canCharge(p) || ApolWingsMechanics.canAirCharge(p);
        if (mc.screen != null || !eligible) {
            if (active) {
                send(2);
                resetCharge();
            }
            downBefore = down;
            return;
        }
        long now = mc.level.getGameTime();
        if (windupUntil > now || airReplyUntil > now) {
            event.getInput().jumping = false;
            downBefore = down;
            return;
        }
        if (windupUntil > 0 || airReplyUntil > 0) {
            send(2);
            resetCharge();
            downBefore = down;
            return;
        }
        if (down && !downBefore && !charging) {
            requestId++;
            charging = true;
            airCharge = ApolWingsMechanics.canAirCharge(p);
            localStart = now;
            send(airCharge ? 3 : 0);
        }
        if (charging) {
            event.getInput().jumping = false;
            if (!down) {
                long held = Math.max(0, now - localStart);
                charging = false;
                if (held < ApolWingsMechanics.HOLD_THRESHOLD) {
                    send(2);
                    event.getInput().jumping = !airCharge;
                    resetCharge();
                } else {
                    ApolWingsNetwork.CHANNEL.sendToServer(new ApolWingsNetwork.Input(1, held, requestId));
                    if (airCharge) airReplyUntil = now + 100;
                    else windupUntil = now + ApolWingsMechanics.WINDUP_TICKS + 100;
                }
            }
        }
        downBefore = down;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (world != mc.level) {
            world = mc.level;
            WAVES.clear();
            resetCharge();
            downBefore = false;
        }
        if (world == null) return;
        WAVES.removeIf(w -> world.getGameTime() - w.start() > ApollyonShockwaveAttack.WAVE_TICKS);
        if (mc.player == null) return;
        if ((charging || windupUntil > 0 || airReplyUntil > 0) && (mc.screen != null || !activeModeValid())) {
            send(2);
            resetCharge();
        }
    }

    public static void receive(ApolWingsNetwork.Fx packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        var entity = mc.level.getEntity(packet.id());
        if (entity == null || !entity.getUUID().equals(packet.uuid())) return;
        boolean self = entity == mc.player;
        if (self && packet.requestId() != requestId) return;
        if (packet.kind() == 0) {
            return;
        } else if (packet.kind() == 1) {
            entity.getPersistentData().putLong(ApolWingsMechanics.JUMP_UNTIL, packet.time() + ApolWingsMechanics.WINDUP_TICKS);
            if (self)
                windupUntil = Math.max(mc.level.getGameTime(), packet.time()) + ApolWingsMechanics.WINDUP_TICKS + 100;
        } else if (packet.kind() == 2) {
            entity.getPersistentData().remove(ApolWingsMechanics.JUMP_UNTIL);
            if (self) {
                resetCharge();
                Vec3 motion = entity.getDeltaMovement();
                entity.setDeltaMovement(motion.x, packet.velocity(), motion.z);
                entity.setOnGround(false);
                entity.fallDistance = 0;
            }
            addWave(new Wave(packet.pos().add(0, .06, 0), mc.level.getGameTime(), packet.id()));
            playShock(packet.pos(), 1.0F);
        } else if (packet.kind() == 3) {
            entity.getPersistentData().remove(ApolWingsMechanics.JUMP_UNTIL);
            if (self) resetCharge();
        } else if (packet.kind() == 4) {
            if (packet.stage() < 1 || packet.stage() > 3) return;
            entity.getPersistentData().remove(ApolWingsMechanics.JUMP_UNTIL);
            if (self) {
                resetCharge();
                if (ApolWingsMechanics.canAirCharge(mc.player)) {
                    entity.setDeltaMovement(packet.motion());
                    entity.fallDistance = 0;
                    entity.hasImpulse = true;
                }
            }
            Vec3 normal = packet.direction().normalize();
            if (normal.lengthSqr() < .5) normal = new Vec3(0, 0, 1);
            addWave(new Wave(packet.pos(), mc.level.getGameTime(), packet.id(), normal, AIR_WAVE_RADIUS[packet.stage()], true));
            playShock(packet.pos(), 1.25F - .1F * packet.stage());
        }
    }

    private static void addWave(Wave wave) {
        if (WAVES.size() >= 32) WAVES.remove(0);
        WAVES.add(wave);
    }

    private static void playShock(Vec3 pos, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null)
            mc.level.playLocalSound(pos.x, pos.y, pos.z, ModSounds.PALADIN_SHOCK.get(), SoundSource.PLAYERS, 1.0F, pitch, false);
    }

    @SubscribeEvent
    public static void hud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!charging || mc.level == null || mc.player == null || mc.options.hideGui || mc.screen != null) return;
        long held = Math.max(0, mc.level.getGameTime() - localStart);
        if (held < ApolWingsMechanics.HOLD_THRESHOLD) return;
        int current = ApolWingsMechanics.frame(held);
        int previous = ApolWingsMechanics.frame(Math.max(0, held - ApolWingsMechanics.FRAME_TICKS));
        float phase = (held - ApolWingsMechanics.HOLD_THRESHOLD) % ApolWingsMechanics.FRAME_TICKS + event.getPartialTick();
        float fade = Math.min(1, phase / 3F);
        int x = mc.getWindow().getGuiScaledWidth() / 2 - 8, y = mc.getWindow().getGuiScaledHeight() - 69;
        var g = event.getGuiGraphics();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            g.setColor(1, 1, 1, 1 - fade);
            g.blit(SCALE[previous], x, y, 0, 0, 16, 17, 16, 17);
            g.flush();
            g.setColor(1, 1, 1, fade);
            g.blit(SCALE[current], x, y, 0, 0, 16, 17, 16, 17);
            g.flush();
        } finally {
            g.setColor(1, 1, 1, 1);
            RenderSystem.disableBlend();
        }
    }

    private ApolWingsClient() {
    }
}
