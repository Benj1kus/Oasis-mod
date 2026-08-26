package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.config.OsirisRealmConfig;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.AzumaalEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class AzumaalCloneManager {



    private static final double CLONE_SEARCH_RANGE = 64.0D;

    private final AzumaalEntity boss;

    private Vec3 formationTarget;

    private int formationTicksRemaining;

    public AzumaalCloneManager(AzumaalEntity boss) {
        this.boss = boss;
    }

    public boolean beginFormation(ServerLevel level) {
        if (hasActiveClones(level)) {
            return false;
        }

        int cloneCount = OsirisRealmConfig.AZUMAAL_CLONE_COUNT.get();
        int totalMembers = cloneCount + 1;
        int formationTicks = OsirisRealmConfig.AZUMAAL_CLONE_FORMATION_TICKS.get();
        double formationRadius = OsirisRealmConfig.AZUMAAL_CLONE_FORMATION_RADIUS.get();

        Vec3 center = boss.position();
        float baseYaw = boss.getYRot();
        Vec3 originalVertex = radialOffset(baseYaw, formationRadius);

        this.formationTarget = center.add(originalVertex);
        this.formationTicksRemaining = formationTicks;

        for (int i = 1; i <= cloneCount; i++) {
            float cloneYaw = baseYaw + i * (360.0F / totalMembers);

            Vec3 cloneVertex = radialOffset(cloneYaw, formationRadius);
            Vec3 relativeOffset = cloneVertex.subtract(originalVertex);
            AzumaalEntity clone = Oasiso.AZUMAAL.get().create(level);

            if (clone == null) {
                continue;
            }
            clone.moveTo(center.x, boss.getY(), center.z, boss.getYRot(), 0.0F);
            clone.initializeClone(boss, i, relativeOffset, formationTicks);

            level.addFreshEntity(clone);

            level.sendParticles(Oasiso.PURPLE_STARS.get(), center.x, boss.getY() + boss.getBbHeight() * 0.5D, center.z, 14, 0.65D, 1.7D, 0.65D, 0.08D);
        }
        return true;
    }

    public void tickFormation() {
        if (this.formationTicksRemaining <= 0 || this.formationTarget == null) {
            return;
        }

        double fraction = 1.0D / this.formationTicksRemaining;
        double newX = boss.getX() + (this.formationTarget.x - boss.getX()) * fraction;
        double newZ = boss.getZ() + (this.formationTarget.z - boss.getZ()) * fraction;

        boss.setPos(newX, boss.getY(), newZ);
        boss.setDeltaMovement(Vec3.ZERO);

        this.formationTicksRemaining--;
    }

    public boolean isTargetInAttackRange(ServerLevel level, ServerPlayer target, double range) {
        double rangeSqr = range * range;

        if (boss.distanceToSqr(target) <= rangeSqr) {
            return true;
        }

        for (AzumaalEntity clone : getActiveClones(level)) {
            if (clone.distanceToSqr(target) <= rangeSqr) {
                return true;
            }
        }

        return false;
    }

    public Vec3 getClosestMemberPosition(ServerLevel level, ServerPlayer target) {
        Vec3 closestPosition = boss.position();

        double closestDistance = boss.distanceToSqr(target);

        for (AzumaalEntity clone : getActiveClones(level)) {
            double distance = clone.distanceToSqr(target);
            if (distance >= closestDistance) {
                continue;
            }

            closestDistance = distance;
            closestPosition = clone.position();
        }
        return closestPosition;
    }

    public boolean hasActiveClones(ServerLevel level) {
        return !getActiveClones(level).isEmpty();
    }

    public List<AzumaalEntity> getActiveClones(ServerLevel level) {
        return level.getEntitiesOfClass(AzumaalEntity.class, boss.getBoundingBox().inflate(CLONE_SEARCH_RANGE), entity -> entity.isCloneOf(boss));
    }

    private Vec3 radialOffset(float yaw, double radius) {
        double radians = Math.toRadians(yaw);
        return new Vec3(-Math.sin(radians) * radius, 0.0D, Math.cos(radians) * radius);
    }

    public void save(CompoundTag tag) {
        tag.putInt("CloneFormationTicks", this.formationTicksRemaining);

        if (this.formationTarget != null) {

            tag.putBoolean("HasCloneFormationTarget", true);

            tag.putDouble("CloneFormationX", this.formationTarget.x);
            tag.putDouble("CloneFormationY", this.formationTarget.y);
            tag.putDouble("CloneFormationZ", this.formationTarget.z);
        }
    }

    public void load(CompoundTag tag) {
        this.formationTicksRemaining = tag.getInt("CloneFormationTicks");

        if (tag.getBoolean("HasCloneFormationTarget")) {
            this.formationTarget = new Vec3(tag.getDouble("CloneFormationX"),
                    tag.getDouble("CloneFormationY"),
                    tag.getDouble("CloneFormationZ"));

        } else {
            this.formationTarget = null;
        }
    }


    public void resetFormation() {
        this.formationTarget = null;
        this.formationTicksRemaining = 0;
    }
}