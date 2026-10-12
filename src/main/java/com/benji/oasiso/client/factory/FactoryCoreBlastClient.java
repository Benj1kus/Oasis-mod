package com.benji.oasiso.client.factory;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.FactoryCoreBlockEntity;
import com.benji.oasiso.common.factory.FactoryCoreBlastRules;
import com.benji.oasiso.client.particle.GeyserSmokeParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class FactoryCoreBlastClient {
    public record Charge(BlockPos pos, long endTick) {
    }

    public static final class Blast {
        public final BlockPos pos;
        public final long tick;
        final double shakeDistance;
        int lastWave;

        Blast(BlockPos pos, long tick, double distance, int lastWave) {
            this.pos = pos;
            this.tick = tick;
            shakeDistance = distance;
            this.lastWave = lastWave;
        }
    }

    private static ClientLevel world;
    private static final Map<BlockPos, Charge> CHARGES = new HashMap<>();
    private static final Map<BlockPos, Blast> BLASTS = new HashMap<>();

    private static void reset() {
        var level = Minecraft.getInstance().level;
        if (world != level) {
            world = level;
            CHARGES.clear();
            BLASTS.clear();
        }
    }

    public static Collection<Charge> charges() {
        reset();
        return CHARGES.values();
    }

    public static Collection<Blast> blasts() {
        reset();
        return BLASTS.values();
    }

    public static boolean hasBlast(BlockPos pos, long countdownEnd) {
        reset();
        Blast b = BLASTS.get(pos);
        return b != null && b.tick >= countdownEnd;
    }

    public static void receive(ResourceLocation dimension, BlockPos pos, long tick) {
        reset();
        var mc = Minecraft.getInstance();
        if (world == null || !world.dimension().location().equals(dimension)) return;
        long elapsed = world.getGameTime() - tick;
        if (elapsed >= FactoryCoreBlastRules.EFFECT_TICKS) return;
        Blast old = BLASTS.get(pos);
        if (old != null && old.tick >= tick) return;
        double distance = mc.player == null ? 999 : mc.player.position().distanceTo(center(pos));
        int previousWave = elapsed <= 3 ? -1 : (int) Math.min(7, elapsed / 4 - 1);
        BLASTS.put(pos.immutable(), new Blast(pos.immutable(), tick, distance, previousWave));
        CHARGES.remove(pos);
        FactoryCoreWarningHud.forget(pos);
    }

    public static Vec3 center(BlockPos pos) {
        return new Vec3(pos.getX() + .5, pos.getY() + 1.4375, pos.getZ() + .5);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        reset();
        var mc = Minecraft.getInstance();
        if (world == null || mc.player == null || mc.isPaused()) return;
        long now = world.getGameTime();
        BLASTS.values().removeIf(b -> now - b.tick >= FactoryCoreBlastRules.EFFECT_TICKS);
        if (now % 5 == 0) {
            CHARGES.clear();
            int cx = mc.player.blockPosition().getX() >> 4, cz = mc.player.blockPosition().getZ() >> 4;
            for (int x = cx - 6; x <= cx + 6; x++)
                for (int z = cz - 6; z <= cz + 6; z++) {
                    var chunk = world.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
                    if (chunk == null) continue;
                    for (var be : chunk.getBlockEntities().values()) {
                        if (be instanceof FactoryCoreBlockEntity core && core.isArmed() && core.getDetonateAt() >= 0 && !hasBlast(core.getBlockPos(), core.getDetonateAt()) && center(core.getBlockPos()).distanceToSqr(mc.player.position()) <= 96 * 96)
                            CHARGES.put(core.getBlockPos().immutable(), new Charge(core.getBlockPos().immutable(), core.getDetonateAt()));
                    }
                }
        }
        int budget = 4;
        for (Blast blast : BLASTS.values()) {
            long age = now - blast.tick;
            if (age < 0 || age > 31) continue;
            int wave = (int) Math.min(7, age / 4);
            if (wave <= blast.lastWave) continue;
            blast.lastWave = wave;
            if (budget-- <= 0 || mc.player.position().distanceToSqr(center(blast.pos)) > 96 * 96) continue;
            GeyserSmokeParticle.spawnReactorWave(world, new Vec3(blast.pos.getX() + .5, blast.pos.getY(), blast.pos.getZ() + .5), FactoryCoreBlastRules.SHOCK_RADIUS * (wave + 1) / 8.0);
        }
    }

    @SubscribeEvent
    public static void shake(ViewportEvent.ComputeCameraAngles event) {
        reset();
        if (world == null) return;
        double partial = Minecraft.getInstance().isPaused() ? Minecraft.getInstance().getFrameTime() : event.getPartialTick();
        double pitch = 0, yaw = 0, roll = 0;
        for (Blast b : BLASTS.values()) {
            double age = (world.getGameTime() - b.tick + partial) / 20.0;
            double power = FactoryCoreBlastRules.shake(age, b.shakeDistance);
            double phase = (b.pos.asLong() & 1023) * .01;
            pitch += power * (.65 * Math.sin(age * 43 + phase) + .35 * Math.sin(age * 71));
            yaw += power * .7 * Math.sin(age * 51 + phase + 1.7);
            roll += power * .5 * Math.sin(age * 37 + phase + .7);
        }
        event.setPitch(event.getPitch() + (float) Math.max(-7, Math.min(7, pitch)));
        event.setYaw(event.getYaw() + (float) Math.max(-5, Math.min(5, yaw)));
        event.setRoll(event.getRoll() + (float) Math.max(-4, Math.min(4, roll)));
    }

    private FactoryCoreBlastClient() {
    }
}
