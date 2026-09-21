package com.benji.oasiso.client.block;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.registry.ModBlocks;
import com.benji.oasiso.registry.ModKarakFluids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class KarakGrassColors {
    private static final int DRY = 0xE1AC5D;
    private static final int MIDDLE = 0xBA5139;
    private static final int WET = 0x32B27C;
    private static final int RADIUS = 6;
    private static final int VERTICAL_RADIUS = 2;
    private static final int MAX_CACHE = 2048;
    private static final int REFRESH_PER_TICK = 32;
    private static final Object LOCK = new Object();
    private static final Map<BlockPos, Integer> COLORS = new HashMap<>();
    private static final ArrayDeque<BlockPos> QUEUE = new ArrayDeque<>();
    private static ClientLevel cachedLevel;

    @SubscribeEvent
    public static void registerBlocks(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> {
            if (tint != 0) return 0xFFFFFF;
            if (level == null || pos == null) return DRY;
            BlockPos anchor = pos;
            if (state.is(ModBlocks.KR_BIGGRASS.get())) {
                anchor = pos.below(state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER ? 2 : 1);
            }
            return color(level, anchor);
        }, ModBlocks.KR_GRASS.get(), ModBlocks.KR_BIGGRASS.get(), ModBlocks.KR_SMALLGRASS.get());
    }

    private static int color(BlockAndTintGetter level, BlockPos pos) {
        synchronized (LOCK) {
            Integer value = COLORS.get(pos);
            if (value != null) return value;
        }
        int result = sample(level, pos);
        synchronized (LOCK) {
            if (!COLORS.containsKey(pos)) {
                if (COLORS.size() >= MAX_CACHE) COLORS.remove(QUEUE.removeFirst());
                BlockPos key = pos.immutable();
                COLORS.put(key, result);
                QUEUE.addLast(key);
            }
            return COLORS.get(pos);
        }
    }

    @SubscribeEvent
    public static void registerItems(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint == 0 ? DRY : 0xFFFFFF, ModBlocks.KR_GRASS.get());
    }
    private static int sample(BlockAndTintGetter level, BlockPos center) {
        double nearestSand = RADIUS, nearestWater = RADIUS;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -VERTICAL_RADIUS; dy <= VERTICAL_RADIUS; dy++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                    double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (distance > RADIUS) continue;
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState neighbor = level.getBlockState(cursor);
                    if (neighbor.is(ModBlocks.KR_SAND.get())) nearestSand = Math.min(nearestSand, distance);
                    if (neighbor.getFluidState().getType().isSame(ModKarakFluids.KR_WATER.get()))
                        nearestWater = Math.min(nearestWater, distance);
                }
            }
        }
        double wet = influence(nearestWater);
        double dry = Math.max(1.0 - wet, influence(nearestSand));
        double t = wet / (wet + dry);
        return t <= 0.5 ? blend(DRY, MIDDLE, t * 2) : blend(MIDDLE, WET, t * 2 - 1);
    }

    private static double influence(double distance) {
        double t = Math.max(0, Math.min(1, (RADIUS - distance) / (RADIUS - 1.0)));
        return t * t * (3 - 2 * t);
    }

    private static int blend(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int blue = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | g << 8 | blue;
    }

    @Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Refresh {
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            synchronized (LOCK) {
                if (level != cachedLevel) {
                    COLORS.clear();
                    QUEUE.clear();
                    cachedLevel = level;
                }
            }
            if (level == null || mc.isPaused()) return;
            int count;
            synchronized (LOCK) {
                count = Math.min(REFRESH_PER_TICK, QUEUE.size());
            }
            for (int i = 0; i < count; i++) {
                BlockPos pos;
                synchronized (LOCK) {
                    pos = QUEUE.pollFirst();
                    if (pos == null) break;
                    QUEUE.addLast(pos);
                }
                if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(ModBlocks.KR_GRASS.get())) {
                    synchronized (LOCK) {
                        COLORS.remove(pos);
                        QUEUE.remove(pos);
                    }
                    continue;
                }
                int next = sample(level, pos);
                boolean changed;
                synchronized (LOCK) {
                    Integer old = COLORS.get(pos);
                    changed = old != null && old != next;
                    if (old != null) COLORS.put(pos, next);
                }
                if (changed) {
                    mc.levelRenderer.setBlocksDirty(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY() + 2, pos.getZ());
                }
            }
        }
    }

    private KarakGrassColors() {
    }
}
