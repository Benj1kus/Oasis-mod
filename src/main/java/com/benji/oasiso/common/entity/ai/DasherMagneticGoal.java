package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.common.entity.DasherEntity;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

import java.util.EnumSet;

public class DasherMagneticGoal extends Goal {
    private final DasherEntity mob;
    private LivingEntity target;
    private int attackTimer;

    public DasherMagneticGoal(DasherEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        this.target = this.mob.getTarget();
        return this.target != null && this.mob.isMagnetic() && this.mob.magneticCooldown <= 0
                && this.mob.distanceToSqr(this.target) <= 25.0D && this.mob.getAnimState() == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.attackTimer < 50 && this.mob.getAnimState() == 5;
    }

    @Override
    public void start() {
        this.attackTimer = 0;
        this.mob.setAnimState(5);
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.attackTimer++;
        if (this.target != null) {
            this.mob.getLookControl().setLookAt(this.target, 30F, 30F);
        }

        if (this.attackTimer == 10) {
            this.mob.playSound(com.benji.oasiso.ModSounds.DASHER_ATTACK.get(), 1.0F, 1.0F);
            createShockwave();
        }
    }

    private void createShockwave() {
        if (this.mob.level() instanceof ServerLevel sl) {
            this.mob.playSound(SoundEvents.ANVIL_LAND, 1.0F, 1.8F);
            double px = this.mob.getX();
            double py = this.mob.getY() + 0.1;
            double pz = this.mob.getZ();

            for (int i = 0; i < 360; i += 10) {
                double rad = Math.toRadians(i);
                double dx = Math.cos(rad) * 3.5;
                double dz = Math.sin(rad) * 3.5;
                sl.sendParticles(
                        new BlockParticleOption(
                                ParticleTypes.FALLING_DUST,
                                Blocks.SAND.defaultBlockState()
                        ),
                        px + dx,
                        py,
                        pz + dz,
                        2,
                        dx * 0.1,
                        0,
                        dz * 0.1,
                        0.05
                );
            }

            for (LivingEntity e : this.mob.level().getEntitiesOfClass(LivingEntity.class, this.mob.getBoundingBox().inflate(6.0D))) {
                if (e != this.mob && e instanceof Player player && !player.isCreative()) {
                    player.hurt(this.mob.damageSources().mobAttack(this.mob), (float) this.mob.getAttributeValue(Attributes.ATTACK_DAMAGE));

                    player.setDeltaMovement(player.getDeltaMovement().add(0, 1.0D, 0));
                    player.hasImpulse = true;
                }
            }
        }
    }

    @Override
    public void stop() {
        if (this.mob.getAnimState() == 5) {
            this.mob.setAnimState(0);
        }
        this.mob.magneticCooldown = 100;
    }
}