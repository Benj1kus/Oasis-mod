package com.benji.oasiso.common.soul;

import com.benji.oasiso.common.block.GraveBlock;
import com.benji.oasiso.common.block.VibritBlock;
import com.benji.oasiso.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

public final class VibritSoulLogic {
    public static final int GRAVE_RADIUS = 10;

    private VibritSoulLogic() {
    }

    public static void release(ServerLevel level, BlockPos vibrit, BlockState vibritState) {
        BlockPos grave = null;
        int seen = 0;
        for (BlockPos p : BlockPos.betweenClosed(vibrit.offset(-GRAVE_RADIUS, -GRAVE_RADIUS, -GRAVE_RADIUS), vibrit.offset(GRAVE_RADIUS, GRAVE_RADIUS, GRAVE_RADIUS))) {
            if (p.distSqr(vibrit) > GRAVE_RADIUS * GRAVE_RADIUS || !level.hasChunkAt(p) || !(level.getBlockState(p).getBlock() instanceof GraveBlock))
                continue;
            if (level.random.nextInt(++seen) == 0) grave = p.immutable();
        }
        if (grave == null) return;
        BlockState old = level.getBlockState(grave);
        if (!(old.getBlock() instanceof GraveBlock)) return;

        Mob mob = ModEntities.ENT_ZOMBIE.get().create(level);
        if (mob == null) return;
        mob.moveTo(grave.getX() + 0.5, grave.getY() + 0.05, grave.getZ() + 0.5, level.random.nextFloat() * 360F, 0);
        ForgeEventFactory.onFinalizeSpawn(mob, level, level.getCurrentDifficultyAt(grave), MobSpawnType.MOB_SUMMONED, null, null);
        if (mob.isSpawnCancelled()) return;
        level.playSound(null, grave, SoundEvents.SOUL_ESCAPE, SoundSource.BLOCKS, 2.0F, 0.8F + level.random.nextFloat() * 0.4F);

        if (!level.destroyBlock(grave, false)) return;
        if (!level.addFreshEntity(mob)) {
            if (level.getBlockState(grave).isAir()) level.setBlock(grave, old, 3);
            return;
        }
        Vec3 start = new Vec3(grave.getX() + 0.5, grave.getY() + 0.15, grave.getZ() + 0.5);
        Vec3 end = new Vec3(vibrit.getX() + 0.5, vibrit.getY() + top(vibritState), vibrit.getZ() + 0.5);
        VibritSoulNetwork.send(level, vibrit, start, end, level.random.nextInt());
    }

    private static double top(BlockState state) {
        return switch (state.getValue(VibritBlock.TYPE)) {
            case NORMAL -> 25.0 / 16.0;
            case MID -> 46.0 / 16.0;
            case SMALL -> 17.0 / 16.0;
        };
    }
}
