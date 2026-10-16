package com.benji.oasiso.common.soul;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.GraveBlock;
import com.benji.oasiso.common.entity.SoulGiverEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class SoulGraveLogic {
    public static final int CHECK_TICKS = 100;
    public static final float CHANCE = 0.30F;
    public static final int RADIUS = 8;

    private SoulGraveLogic() {
    }

    public static void tick(SoulGiverEntity mob) {
        if (!(mob.level() instanceof ServerLevel level) || Math.floorMod(mob.tickCount + mob.getId() * 31, CHECK_TICKS) != 0 || mob.getRandom().nextFloat() >= CHANCE)
            return;
        BlockPos origin = mob.blockPosition();
        BlockPos chosen = null;
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-RADIUS, -RADIUS, -RADIUS), origin.offset(RADIUS, RADIUS, RADIUS))) {
            if (Vec3.atCenterOf(pos).distanceToSqr(mob.position()) > RADIUS * RADIUS || !level.hasChunkAt(pos) || !(level.getBlockState(pos).getBlock() instanceof GraveBlock))
                continue;
            if (mob.getRandom().nextInt(++count) == 0) chosen = pos.immutable();
        }
        if (chosen == null) return;
        level.playSound(null, chosen, ModSounds.ENTROPY_LIGHTNING.get(), SoundSource.BLOCKS, 2.0F, 0.9F + level.random.nextFloat() * 0.2F);
        SoulGraveNetwork.send(mob, chosen, mob.getRandom().nextInt());
        for (int i = 0; i < 5; i++)
            level.sendParticles(Oasiso.ENT_SOUL.get(), chosen.getX() + 0.2 + mob.getRandom().nextDouble() * 0.6, chosen.getY() + 0.1, chosen.getZ() + 0.2 + mob.getRandom().nextDouble() * 0.6, 0, (mob.getRandom().nextDouble() - 0.5) * 0.014, 0.025 + mob.getRandom().nextDouble() * 0.014, (mob.getRandom().nextDouble() - 0.5) * 0.014, 1.0);
    }
}
