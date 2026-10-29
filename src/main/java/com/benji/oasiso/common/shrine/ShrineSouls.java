package com.benji.oasiso.common.shrine;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.*;
import com.benji.oasiso.registry.ModBlocks;
import com.benji.oasiso.registry.ModEntities;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.*;

public final class ShrineSouls {
    public static final int RADIUS = 10;

    public static void lookAtPlayer(Mob mob) {
        mob.setTarget(null);
        mob.getNavigation().stop();
        mob.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        Player p = mob.level().getNearestPlayer(mob, 16);
        if (p == null || p.isSpectator()) return;
        float target = (float) Math.toDegrees(Math.atan2(p.getZ() - mob.getZ(), p.getX() - mob.getX())) - 90;
        mob.setYRot(net.minecraft.util.Mth.approachDegrees(mob.getYRot(), target, 5));
        mob.yBodyRot = mob.getYRot();
        mob.yHeadRot = mob.getYRot();
        mob.getLookControl().setLookAt(p, 30, 30);
    }

    public static void awaken(ServerPlayer player) {
        awaken(player, null);
    }

    public static void awaken(ServerPlayer player, UUID group) {
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        List<BlockPos> cores = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-RADIUS, -RADIUS, -RADIUS), center.offset(RADIUS, RADIUS, RADIUS))) {
            if (!level.hasChunkAt(p) || p.getCenter().distanceToSqr(player.position()) > RADIUS * RADIUS || !level.getBlockState(p).is(ModBlocks.SOUL_PCORE.get()))
                continue;
            boolean frame = false;
            for (Direction d : Direction.values())
                if (level.hasChunkAt(p.relative(d)) && level.getBlockState(p.relative(d)).is(ModBlocks.SOUL_FRAME.get())) {
                    frame = true;
                    break;
                }
            if (frame) cores.add(p.immutable());
        }
        cores.sort(Comparator.<BlockPos>comparingDouble(p -> p.getCenter().distanceToSqr(player.position())).thenComparingLong(BlockPos::asLong));
        int kind = 0;
        for (BlockPos p : cores) {
            if (!level.getEntitiesOfClass(Mob.class, new AABB(p).inflate(.1, 3, .1), m -> m instanceof AngerEntity a && a.isShrineSoul() || m instanceof HappinessEntity h && h.isShrineSoul() || m instanceof SadnessEntity s && s.isShrineSoul()).isEmpty())
                continue;
            Mob mob = switch (kind % 3) {
                case 0 -> ModEntities.ANGER.get().create(level);
                case 1 -> ModEntities.SADNESS.get().create(level);
                default -> ModEntities.HAPPINESS.get().create(level);
            };
            if (mob == null) continue;
            var shape = level.getBlockState(p).getShape(level, p);
            double top = shape.isEmpty() ? 1 : shape.max(Direction.Axis.Y);
            mob.moveTo(p.getX() + .5, p.getY() + top + .01, p.getZ() + .5, 0, 0);
            if (!level.noCollision(mob)) continue;
            if (mob instanceof AngerEntity a) a.setShrineSoul();
            if (mob instanceof SadnessEntity s) s.setShrineSoul();
            if (mob instanceof HappinessEntity h) h.setShrineSoul();
            if (group != null) mob.getPersistentData().putUUID(WarCoreRitual.GROUP, group);
            lookAtPlayer(mob);
            if (!level.addFreshEntity(mob)) continue;
            var particle = kind % 3 == 0 ? Oasiso.ANGER_PART.get() : kind % 3 == 1 ? Oasiso.SAD_BOP.get() : Oasiso.HAPPY_FLARE.get();
            level.sendParticles(particle, mob.getX(), mob.getY() + mob.getBbHeight() * .5, mob.getZ(), 32, .35, .4, .35, .035);
            kind++;
        }
    }

    private ShrineSouls() {
    }
}
