package com.benji.oasiso.client.soulcast;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.*;

final class SoulStrikeGround {
    record Surface(BlockPos pos, BlockState state, double y) {
    }

    private record Top(AABB box, Surface surface) {
    }

    private final ClientLevel level;
    private final double feet;
    private final Map<Long, List<Top>> columns = new HashMap<>();

    SoulStrikeGround(ClientLevel level, double feet) {
        this.level = level;
        this.feet = feet;
    }

    Surface at(double x, double z) {
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
        long key = ((long) bx << 32) ^ (bz & 0xffffffffL);
        var tops = columns.computeIfAbsent(key, k -> load(bx, bz));
        double lx = x - bx, lz = z - bz;
        for (Top t : tops) {
            var b = t.box;
            if (lx >= b.minX && lx < b.maxX && lz >= b.minZ && lz < b.maxZ) return t.surface;
        }
        return null;
    }

    private List<Top> load(int bx, int bz) {
        List<Top> tops = new ArrayList<>();
        int high = (int) Math.floor(feet + 1.05), low = (int) Math.floor(feet - 2.51);
        if (!level.hasChunkAt(new BlockPos(bx, high, bz))) return tops;
        for (int y = high; y >= low; y--) {
            BlockPos pos = new BlockPos(bx, y, bz);
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getRenderShape() == RenderShape.INVISIBLE) continue;
            for (AABB box : state.getCollisionShape(level, pos).toAabbs()) {
                double top = y + box.maxY;
                if (top >= feet - 2.5 && top <= feet + 1.05) tops.add(new Top(box, new Surface(pos, state, top)));
            }
        }
        tops.sort(Comparator.comparingDouble((Top top) -> top.surface.y()).reversed());
        return tops;
    }
}
