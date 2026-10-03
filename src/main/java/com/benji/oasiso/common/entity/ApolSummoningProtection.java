package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = Oasiso.MODID)
public final class ApolSummoningProtection {
    private static boolean reserved(Level level, BlockPos pos) {
        for (ApolSummoningEntity ritual : level.getEntitiesOfClass(ApolSummoningEntity.class, new AABB(pos).inflate(40))) {
            if (ritual.isTerrainChange()) continue;
            if (!ritual.settled() && ritual.reserves(pos)) return true;
            if (ritual.age() < ApolSummoningEntity.SPAWN_AT && pos.equals(ritual.raisedAltar())) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void place(BlockEvent.EntityPlaceEvent e) {
        if (e.getLevel() instanceof Level level && !level.isClientSide) {
            if (e instanceof BlockEvent.EntityMultiPlaceEvent multi) {
                for (var snapshot : multi.getReplacedBlockSnapshots())
                    if (reserved(level, snapshot.getPos())) {
                        e.setCanceled(true);
                        return;
                    }
            } else if (reserved(level, e.getPos())) e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void breakBlock(BlockEvent.BreakEvent e) {
        if (e.getLevel() instanceof Level level && reserved(level, e.getPos())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void explosion(ExplosionEvent.Detonate e) {
        e.getAffectedBlocks().removeIf(pos -> reserved(e.getLevel(), pos));
    }

    private ApolSummoningProtection() {
    }
}
