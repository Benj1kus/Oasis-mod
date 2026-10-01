package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ApollyonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;

public final class ApollyonDeathReward {
    private static final ResourceLocation LOOT = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "chests/apollyon_barrel");

    public static void finish(ServerLevel level, ApollyonEntity boss) {
        spawnDeathExplosion(level, boss);
        BlockPos pos = findPlace(level, boss);
        if (pos == null) {
            // Пустота: ищем свободное место рядом с точкой смерти, не затираем постройки.
            BlockPos center = boss.blockPosition();
            for (int y = 0; y <= 3 && pos == null; y++)
                for (int x = -2; x <= 2 && pos == null; x++)
                    for (int z = -2; z <= 2 && pos == null; z++) {
                        BlockPos p = center.offset(x, y, z);
                        if (free(level, p)) pos = p;
                    }
        }
        if (pos == null || !level.setBlock(pos, Blocks.BARREL.defaultBlockState(), 3)) {
            com.mojang.logging.LogUtils.getLogger().warn("[Apollyon] No free position for reward barrel near {}", boss.blockPosition());
            return;
        }
        if (level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
            barrel.setLootTable(LOOT, level.random.nextLong());
            barrel.setChanged();
        }
    }

    private static boolean free(ServerLevel level, BlockPos p) {
        return !level.isOutsideBuildHeight(p) && level.hasChunkAt(p)
                && level.getWorldBorder().isWithinBounds(p)
                && level.getBlockEntity(p) == null && level.getBlockState(p).canBeReplaced();
    }

    private static BlockPos findPlace(ServerLevel level, ApollyonEntity boss) {
        int top = Math.min(level.getMaxBuildHeight() - 2, Mth.floor(boss.getY()));
        int bx = Mth.floor(boss.getX()), bz = Mth.floor(boss.getZ());
        // Сначала непосредственно под боссом, затем ближайшие соседние столбцы.
        for (int radius = 0; radius <= 4; radius++)
            for (int dx = -radius; dx <= radius; dx++)
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    if (!level.hasChunkAt(new BlockPos(bx + dx, top, bz + dz))) continue;
                    for (int y = top; y >= level.getMinBuildHeight(); y--) {
                        BlockPos floor = new BlockPos(bx + dx, y, bz + dz);
                        if (level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                                && free(level, floor.above())) return floor.above();
                    }
                }
        return null;
    }

    private static void spawnDeathExplosion(ServerLevel level, ApollyonEntity boss) {
        double x = boss.getX();
        double y = boss.getY() + boss.getBbHeight() * 0.48D;
        double z = boss.getZ();

        level.sendParticles(Oasiso.CHAOS_BOMB_CENTER_SMOKE.get(), x, y, z, 10, 0.45D, 0.65D, 0.45D, 0.04D);
        level.sendParticles(Oasiso.CHAOS_BOMB_FIRE_SMOKE.get(), x, y, z, 38, 1.35D, 1.75D, 1.35D, 0.13D);
        level.sendParticles(Oasiso.CHAOS_BOMB_SPARKS.get(), x, y, z, 80, 1.15D, 1.55D, 1.15D, 0.27D);

        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0F, 0.72F);
    }
    private ApollyonDeathReward() {}
}
