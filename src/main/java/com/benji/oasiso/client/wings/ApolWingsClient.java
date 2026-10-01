package com.benji.oasiso.client.wings;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.wings.ApolWingsMechanics;
import com.benji.oasiso.common.wings.ApolWingsNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.benji.oasiso.ModSounds;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ApolWingsClient {
    public record Wave(Vec3 origin, long start, int seed) {
    }

    public static final List<Wave> WAVES = new ArrayList<>();
    private static final ResourceLocation[] SCALE = {texture("empty"), texture("slow"), texture("normal"), texture("full")};
    private static ClientLevel world;
    private static boolean downBefore, charging;
    private static long localStart, confirmedStart, windupUntil;

    private static ResourceLocation texture(String frame) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/wings_scale_" + frame + ".png");
    }

    private static void send(int action) {
        ApolWingsNetwork.CHANNEL.sendToServer(new ApolWingsNetwork.Input(action));
    }

    @SubscribeEvent
    public static void input(MovementInputUpdateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || event.getEntity() != p || mc.level == null) return;
        boolean down = mc.options.keyJump.isDown();
        if (mc.screen != null || !ApolWingsMechanics.canCharge(p)) {
            if (charging || windupUntil > 0) {
                send(2);
                charging = false;
                windupUntil = 0;
            }
            downBefore = down;
            return;
        }
        long now = mc.level.getGameTime();
        if (windupUntil > now) {
            event.getInput().jumping = false;
            downBefore = down;
            return;
        }
        windupUntil = 0;
        if (down && !downBefore && !charging) {
            charging = true;
            localStart = confirmedStart = now;
            send(0);
        }
        if (charging) {
            event.getInput().jumping = false;
            if (!down) {
                long held = now - localStart;
                charging = false;
                if (held < ApolWingsMechanics.HOLD_THRESHOLD) {
                    send(2);
                    event.getInput().jumping = true;
                } else {
                    send(1);
                    windupUntil = now + ApolWingsMechanics.WINDUP_TICKS + 20;
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
            charging = downBefore = false;
            windupUntil = 0;
        }
        if (world == null) return;
        WAVES.removeIf(w -> world.getGameTime() - w.start() > com.benji.oasiso.common.entity.ai.ApollyonShockwaveAttack.WAVE_TICKS);
        if (mc.player == null) return;
        if ((charging || windupUntil > 0) && (mc.screen != null || !ApolWingsMechanics.canCharge(mc.player))) {
            send(2);
            charging = false;
            windupUntil = 0;
        }
    }

    public static void receive(ApolWingsNetwork.Fx packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        var entity = mc.level.getEntity(packet.id());
        if (entity == null || !entity.getUUID().equals(packet.uuid())) return;
        boolean self = entity == mc.player;
        if (packet.kind() == 0) {
            if (self && charging) confirmedStart = packet.time();
        } else if (packet.kind() == 1) {
            entity.getPersistentData().putLong(ApolWingsMechanics.JUMP_UNTIL, packet.time() + ApolWingsMechanics.WINDUP_TICKS);
            if (self) windupUntil = packet.time() + ApolWingsMechanics.WINDUP_TICKS;
        } else if (packet.kind() == 2) {
            entity.getPersistentData().remove(ApolWingsMechanics.JUMP_UNTIL);
            if (self) {
                windupUntil = 0;
                Vec3 motion = entity.getDeltaMovement();
                entity.setDeltaMovement(motion.x, packet.velocity(), motion.z);
                entity.setOnGround(false);
                entity.fallDistance = 0;
            }
            if (WAVES.size() >= 32) WAVES.remove(0);
            WAVES.add(new Wave(packet.pos().add(0, .06, 0), packet.time(), packet.id()));
            mc.level.playLocalSound(packet.pos().x, packet.pos().y, packet.pos().z, ModSounds.PALADIN_SHOCK.get(), SoundSource.PLAYERS, 1.0F, 1.0F, false);
        } else if (packet.kind() == 3) {
            entity.getPersistentData().remove(ApolWingsMechanics.JUMP_UNTIL);
            if (self) {
                charging = false;
                windupUntil = 0;
            }
        }
    }

    @SubscribeEvent
    public static void hud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!charging || mc.level == null || mc.player == null || mc.options.hideGui || mc.screen != null) return;
        long held = Math.max(0, mc.level.getGameTime() - confirmedStart);
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
