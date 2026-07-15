package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.common.entity.CactoEntity;
import com.benji.oasiso.common.entity.projectile.CactoProjEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class CactoAttackGoal extends Goal {
    private final CactoEntity mob;
    private LivingEntity target;
    private int attackTimer;
    private int cooldown;

    public CactoAttackGoal(CactoEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        this.target = this.mob.getTarget();
        return this.target != null && this.target.isAlive() && this.mob.hasSpawned && this.mob.distanceToSqr(this.target) <= 225.0D;
    }

    @Override
    public void tick() {
        this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);

        if (this.cooldown > 0) {
            this.cooldown--;
            return;
        }

        this.attackTimer++;

        if (this.attackTimer == 1) {
            this.mob.setAnimState(2);
        }

        if (this.attackTimer == 5) {
            shootProjectile();
        }

        if (this.attackTimer >= 10) {
            this.mob.setAnimState(1);
            this.attackTimer = 0;
            this.cooldown = 10;
        }
    }

    private void shootProjectile() {
        if (!this.mob.level().isClientSide) {
            CactoProjEntity proj = new CactoProjEntity(this.mob.level(), this.mob);

            double d0 = this.target.getY() + (double)this.target.getEyeHeight() - 1.1D;
            double d1 = this.target.getX() - this.mob.getX();
            double d2 = d0 - (this.mob.getY() + 1.0D);
            double d3 = this.target.getZ() - this.mob.getZ();


            double d4 = Math.sqrt(d1 * d1 + d3 * d3) * 0.2D;

            proj.setPos(this.mob.getX(), this.mob.getY() + 1.0D, this.mob.getZ());

            proj.shoot(d1, d2 + d4, d3, 1.6F, 0.5F);

            this.mob.level().addFreshEntity(proj);
        }
    }

    @Override
    public void stop() {
        this.mob.setAnimState(1);
        this.attackTimer = 0;
    }
}