package com.benji.oasiso.client.fountain;

import com.benji.oasiso.Oasiso;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class EntropyFountainClient {
    public static final int VIEW_DISTANCE = 32, MAX_VISIBLE = 8;
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "entropy_fountain");
    private static ClientLevel world;
    private static Block block;
    private static final Set<BlockPos> KNOWN = new HashSet<>();
    private static List<EntropyFountainLayout.Fountain> fountains = List.of();
    private static int scanIndex, ticks;

    static void checkWorld() {
        ClientLevel current = Minecraft.getInstance().level;
        if (world != current) {
            world = current;
            KNOWN.clear();
            fountains = List.of();
            scanIndex = ticks = 0;
            block = ForgeRegistries.BLOCKS.containsKey(ID) ? ForgeRegistries.BLOCKS.getValue(ID) : null;
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        checkWorld();
        var mc = Minecraft.getInstance();
        if (world == null || block == null || mc.player == null || mc.isPaused()) return;
        BlockPos camera = BlockPos.containing(mc.gameRenderer.getMainCamera().getPosition());

        for (int n = 0; n < 6; n++) {
            int index = scanIndex;
            scanIndex = (scanIndex + 1) % 343;
            int sx = (camera.getX() >> 4) + index % 7 - 3;
            int sz = (camera.getZ() >> 4) + (index / 7) % 7 - 3;
            int sy = (camera.getY() >> 4) + index / 49 - 3;
            scanSection(sx, sy, sz);
        }
        if (++ticks % 10 != 0) return;
        KNOWN.removeIf(p -> Math.abs((p.getX() >> 4) - (camera.getX() >> 4)) > 3 || Math.abs((p.getZ() >> 4) - (camera.getZ() >> 4)) > 3 || Math.abs((p.getY() >> 4) - (camera.getY() >> 4)) > 3 || !world.hasChunkAt(p) || world.getBlockState(p).getBlock() != block);
        List<EntropyFountainLayout.Cell> cells = new ArrayList<>();
        for (BlockPos start : KNOWN)
            for (var axis : EntropyFountainLayout.Axis.values()) {
                int end = findEnd(start, axis);
                if (end != Integer.MIN_VALUE) cells.add(axis.cell(start.getX(), start.getY(), start.getZ(), end));
            }
        fountains = EntropyFountainLayout.merge(cells);
    }

    private static void scanSection(int sx, int sy, int sz) {
        if (sy * 16 < world.getMinBuildHeight() || sy * 16 >= world.getMaxBuildHeight()) return;
        LevelChunk chunk = world.getChunkSource().getChunk(sx, sz, ChunkStatus.FULL, false);
        if (chunk == null) return;
        var section = chunk.getSection(world.getSectionIndex(sy * 16));
        if (section.hasOnlyAir() || !section.maybeHas(state -> state.getBlock() == block)) return;
        for (int y = 0; y < 16; y++)
            for (int z = 0; z < 16; z++)
                for (int x = 0; x < 16; x++)
                    if (KNOWN.size() < 4096 && section.getBlockState(x, y, z).getBlock() == block)
                        KNOWN.add(new BlockPos(sx * 16 + x, sy * 16 + y, sz * 16 + z));
    }

    private static int findEnd(BlockPos start, EntropyFountainLayout.Axis axis) {
        BlockPos.MutableBlockPos sample = new BlockPos.MutableBlockPos();
        int dx = axis == EntropyFountainLayout.Axis.X ? 1 : 0;
        int dy = axis == EntropyFountainLayout.Axis.Y ? 1 : 0;
        int dz = axis == EntropyFountainLayout.Axis.Z ? 1 : 0;

        for (int distance = 1; distance <= EntropyFountainLayout.MAX_GAP + 1; distance++) {
            sample.set(start.getX() + dx * distance, start.getY() + dy * distance, start.getZ() + dz * distance);
            if (sample.getY() < world.getMinBuildHeight() || sample.getY() >= world.getMaxBuildHeight() || !world.hasChunkAt(sample))
                return Integer.MIN_VALUE;
            var state = world.getBlockState(sample);
            if (state.getBlock() == block)
                return distance >= 2 ? axis.coordinate(sample.getX(), sample.getY(), sample.getZ()) : Integer.MIN_VALUE;
            if (!state.getFluidState().isEmpty() || !state.getCollisionShape(world, sample).isEmpty())
                return Integer.MIN_VALUE;
        }
        return Integer.MIN_VALUE;
    }

    static List<EntropyFountainLayout.Fountain> fountains() {
        checkWorld();
        return fountains;
    }

    private EntropyFountainClient() {
    }
}
