package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.MonkiBigEntity;
import com.benji.oasiso.common.entity.MonkiEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public class MonkiMergeGoal extends Goal {
    private final MonkiEntity mob;
    private MonkiEntity ally1;
    private MonkiEntity ally2;
    private Vec3 mergePos;
    private int mergeTimer;

    public MonkiMergeGoal(MonkiEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.mob.isMerging || this.mob.getHealth() > this.mob.getMaxHealth() / 2.0F || this.mob.mergeCooldown > 0) return false;

        List<MonkiEntity> list = this.mob.level().getEntitiesOfClass(MonkiEntity.class, this.mob.getBoundingBox().inflate(12.0D),
                e -> e != this.mob && !e.isMerging && e.mergeCooldown <= 0 && e.getHealth() <= e.getMaxHealth() / 2.0F);

        if (list.size() >= 2) {
            this.ally1 = list.get(0);
            this.ally2 = list.get(1);
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return this.ally1 != null && this.ally1.isAlive() &&
                this.ally2 != null && this.ally2.isAlive() &&
                this.mergeTimer < 40;
    }

    @Override
    public void start() {
        this.mob.isMerging = true;
        this.ally1.isMerging = true;
        this.ally2.isMerging = true;
        this.mergeTimer = 0;

        double cx = (this.mob.getX() + this.ally1.getX() + this.ally2.getX()) / 3.0D;
        double cy = (this.mob.getY() + this.ally1.getY() + this.ally2.getY()) / 3.0D;
        double cz = (this.mob.getZ() + this.ally1.getZ() + this.ally2.getZ()) / 3.0D;
        this.mergePos = new Vec3(cx, cy, cz);
    }

    @Override
    public void stop() {
        this.mob.isMerging = false;
        if (this.ally1 != null) this.ally1.isMerging = false;
        if (this.ally2 != null) this.ally2.isMerging = false;
    }

    @Override
    public void tick() {
        this.mergeTimer++;

        this.mob.getNavigation().moveTo(this.mergePos.x, this.mergePos.y, this.mergePos.z, 1.2D);
        this.ally1.getNavigation().moveTo(this.mergePos.x, this.mergePos.y, this.mergePos.z, 1.2D);
        this.ally2.getNavigation().moveTo(this.mergePos.x, this.mergePos.y, this.mergePos.z, 1.2D);

        if (this.mergeTimer >= 40 && !this.mob.level().isClientSide) {
            MonkiBigEntity big = Oasiso.MONKI_BIG.get().create(this.mob.level());
            if (big != null) {
                big.moveTo(this.mergePos.x, this.mergePos.y, this.mergePos.z, this.mob.getYRot(), this.mob.getXRot());
                big.setStoredHealths(this.mob.getHealth(), this.ally1.getHealth(), this.ally2.getHealth());
                this.mob.level().addFreshEntity(big);

                if (this.mob.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                            this.mergePos.x, this.mergePos.y + 1.0, this.mergePos.z,
                            150, 1.0, 1.5, 1.0, 0.15);
                }
                this.mob.level().playSound(null, BlockPos.containing(this.mergePos), SoundEvents.SAND_BREAK, SoundSource.HOSTILE, 2.0F, 1.0F);

                this.mob.discard();
                this.ally1.discard();
                this.ally2.discard();
            }
        }
    }
}