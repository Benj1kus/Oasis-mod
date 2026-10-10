package com.benji.oasiso.client.geyser;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.GeyserBigBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class BigGeyserClient {
    public static final int RANGE = 32, INTERVAL = 100, DURATION = 40, MAX_JETS = 12;
    private static ClientLevel world;
    private static final Map<BlockPos, Emitter> EMITTERS = new LinkedHashMap<>();

    private static final class Emitter {
        long bubble, next, start = -1;
        float height = 5;

        Emitter(long now, long offset) {
            bubble = now;
            next = now + offset;
        }
    }

    public record Jet(BlockPos pos, Vec3 base, long start, float height, int seed) {
    }

    static void checkWorld() {
        ClientLevel level = Minecraft.getInstance().level;
        if (world != level) {
            EMITTERS.clear();
            world = level;
        }
    }

    public static void observe(Level level, BlockPos pos) {
        checkWorld();
        var player = Minecraft.getInstance().player;
        if (world == null || world != level || player == null || player.distanceToSqr(base(pos)) > RANGE * RANGE)
            return;
        if (EMITTERS.size() < 256)
            EMITTERS.computeIfAbsent(pos.immutable(), p -> new Emitter(world.getGameTime(), 1 + Math.floorMod(p.asLong() ^ (p.asLong() >>> 17), INTERVAL)));
    }

    private static Vec3 base(BlockPos pos) {
        return new Vec3(pos.getX() + .5, pos.getY() + 26.0 / 16.0 + .02, pos.getZ() + .5);
    }

    private static float ceiling(BlockPos pos) {
        Vec3 base = base(pos);
        var hit = world.clip(new ClipContext(base, base.add(0, 5, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Minecraft.getInstance().player));
        return hit.getType() == HitResult.Type.MISS ? 5F : (float) Math.max(0, hit.getLocation().y - base.y - .03);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        checkWorld();
        var mc = Minecraft.getInstance();
        if (world == null || mc.player == null || mc.isPaused()) return;
        long now = world.getGameTime();
        int active = 0, bubbles = 0;
        for (Emitter emitter : EMITTERS.values())
            if (emitter.start >= 0 && now - emitter.start < DURATION && now >= emitter.start) active++;
        var it = EMITTERS.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            BlockPos pos = entry.getKey();
            Emitter emitter = entry.getValue();
            Vec3 base = base(pos);
            if (!world.hasChunkAt(pos) || !(world.getBlockState(pos).getBlock() instanceof GeyserBigBlock) || mc.player.distanceToSqr(base) > RANGE * RANGE) {
                it.remove();
                continue;
            }
            if (!GeyserBigBlock.open(world, pos)) {
                emitter.start = -1;
                emitter.next = now + INTERVAL;
                continue;
            }
            if (emitter.bubble > now + 20) emitter.bubble = now;
            if (emitter.next > now + INTERVAL || emitter.start > now) {
                emitter.next = now + INTERVAL;
                emitter.start = -1;
            }
            if (now >= emitter.bubble && bubbles < 12) {
                bubbles++;
                emitter.bubble = now + 5 + world.random.nextInt(6);
                world.addParticle(Oasiso.ENTROPY_BULB.get(), base.x + (world.random.nextDouble() - .5) * .45, base.y, base.z + (world.random.nextDouble() - .5) * .45, 0, 0, 0);
            }
            if (now >= emitter.next && active < MAX_JETS) {
                emitter.next = now + INTERVAL;
                emitter.height = ceiling(pos);
                if (emitter.height < .15F) continue;
                emitter.start = now;
                active++;
                world.playLocalSound(base.x, base.y, base.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, .6F, .65F + world.random.nextFloat() * .15F, false);
            }
            if (emitter.start >= 0 && now - emitter.start < DURATION && now % 5 == 0)
                emitter.height = Math.min(emitter.height, ceiling(pos));
        }
    }

    static List<Jet> jets() {
        checkWorld();
        List<Jet> result = new ArrayList<>();
        if (world == null) return result;
        long now = world.getGameTime();
        for (var entry : EMITTERS.entrySet()) {
            Emitter e = entry.getValue();
            BlockPos p = entry.getKey();
            if (e.start >= 0 && now >= e.start && now - e.start < DURATION && world.hasChunkAt(p) && world.getBlockState(p).getBlock() instanceof GeyserBigBlock) {
                long key = p.asLong();
                result.add(new Jet(p, base(p), e.start, e.height, (int) ((key ^ (key >>> 32)) & 1023)));
            }
        }
        return result;
    }

    private BigGeyserClient() {
    }
}
