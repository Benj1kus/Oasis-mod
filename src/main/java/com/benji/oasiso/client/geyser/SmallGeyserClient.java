package com.benji.oasiso.client.geyser;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.GeyserSmallBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class SmallGeyserClient {
    public static final int VISUAL_RANGE = 32, ERUPTION_INTERVAL = 100;
    private static final Map<BlockPos, Emitter> EMITTERS = new LinkedHashMap<>();
    private static ClientLevel world;

    private static final class Emitter {
        long bubble, burst;

        Emitter(long now, long offset) {
            bubble = now;
            burst = now + offset;
        }
    }

    public static void observe(Level level, BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (level != mc.level || mc.player == null || !(level instanceof ClientLevel client)) return;
        resetWorld(client);
        if (mc.player.distanceToSqr(pos.getX() + .5, pos.getY() + .813, pos.getZ() + .5) > VISUAL_RANGE * VISUAL_RANGE)
            return;
        if (EMITTERS.size() < 256)
            EMITTERS.computeIfAbsent(pos.immutable(), p -> new Emitter(client.getGameTime(), 1 + Math.floorMod(p.asLong(), ERUPTION_INTERVAL)));
    }

    private static void resetWorld(ClientLevel level) {
        if (world != level) {
            EMITTERS.clear();
            world = level;
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        resetWorld(mc.level);
        if (world == null || mc.player == null || mc.isPaused()) return;
        long now = world.getGameTime();
        int bubbles = 0, bursts = 0;
        var iterator = EMITTERS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            BlockPos p = entry.getKey();
            double x = p.getX() + .5, y = p.getY() + 13.0 / 16.0 + .025, z = p.getZ() + .5;
            if (!world.hasChunkAt(p) || mc.player.distanceToSqr(x, y, z) > VISUAL_RANGE * VISUAL_RANGE || !(world.getBlockState(p).getBlock() instanceof GeyserSmallBlock)) {
                iterator.remove();
                continue;
            }
            Emitter emitter = entry.getValue();
            if (!GeyserSmallBlock.open(world, p)) {
                emitter.burst = now + ERUPTION_INTERVAL;
                continue;
            }
            if (emitter.bubble > now + 20) emitter.bubble = now;
            if (emitter.burst > now + ERUPTION_INTERVAL) emitter.burst = now + ERUPTION_INTERVAL;
            if (now >= emitter.bubble && bubbles < 12) {
                emitter.bubble = now + 6 + world.random.nextInt(7);
                bubbles++;
                world.addParticle(Oasiso.ENTROPY_BULB.get(), x + (world.random.nextDouble() - .5) * .48, y, z + (world.random.nextDouble() - .5) * .48, 0, 0, 0);
            }
            if (now >= emitter.burst && bursts < 2) {
                emitter.burst = now + ERUPTION_INTERVAL;
                bursts++;
                for (int i = 0; i < 20; i++)
                    world.addParticle(Oasiso.GEYSER_SMOKE.get(), x + (world.random.nextDouble() - .5) * .32, y, z + (world.random.nextDouble() - .5) * .32, 0, 0, 0);
                world.playLocalSound(x, y, z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, .35F, .8F + world.random.nextFloat() * .25F, false);
            }
        }
    }

    private SmallGeyserClient() {
    }
}
