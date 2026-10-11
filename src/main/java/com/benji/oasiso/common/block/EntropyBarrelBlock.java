package com.benji.oasiso.common.block;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class EntropyBarrelBlock extends Block {

    public EntropyBarrelBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;

        if (!level.isClientSide) {
            float radius = 4.0F;
            float maxDamage = 12.0F;

            AABB area = new AABB(pos).inflate(radius);
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area);

            for (LivingEntity entity : entities) {
                double distanceSqr = entity.distanceToSqr(x, y, z);
                if (distanceSqr <= radius * radius) {
                    double distance = Math.sqrt(distanceSqr);
                    double impact = 1.0D - (distance / radius);
                    float damage = (float) (impact * maxDamage);
                    entity.hurt(level.damageSources().explosion(player, player), Math.max(1.0F, damage));
                    Vec3 dir = new Vec3(entity.getX() - x, entity.getY() - y, entity.getZ() - z).normalize();
                    entity.setDeltaMovement(entity.getDeltaMovement().add(dir.scale(impact * 0.8D)));
                    entity.hasImpulse = true;
                }
            }
            level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0F, 0.8F + level.random.nextFloat() * 0.2F);
        } else {
            spawnExplosionParticles(level, pos);
        }

        super.playerWillDestroy(level, pos, state, player);
    }

    private void spawnExplosionParticles(Level level, BlockPos pos) {
        RandomSource random = level.getRandom();
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;

        for (int i = 0; i < 8; i++) {
            double vx = (random.nextDouble() - 0.5D) * 0.12D;
            double vy = random.nextDouble() * 0.15D + 0.05D;
            double vz = (random.nextDouble() - 0.5D) * 0.12D;
            level.addParticle(Oasiso.CHAOS_BOMB_CENTER_SMOKE.get(), x, y, z, vx, vy, vz);
        }

        for (int i = 0; i < 18; i++) {
            double vx = (random.nextDouble() - 0.5D) * 0.25D;
            double vy = random.nextDouble() * 0.25D + 0.05D;
            double vz = (random.nextDouble() - 0.5D) * 0.25D;
            level.addParticle(Oasiso.CHAOS_BOMB_FIRE_SMOKE.get(), x, y, z, vx, vy, vz);
        }

        for (int i = 0; i < 25; i++) {
            double vx = (random.nextDouble() - 0.5D) * 0.5D;
            double vy = random.nextDouble() * 0.4D + 0.1D;
            double vz = (random.nextDouble() - 0.5D) * 0.5D;
            level.addParticle(Oasiso.CHAOS_BOMB_SPARKS.get(), x, y, z, vx, vy, vz);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(8) == 0) {
            double x = pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.6D;
            double y = pos.getY() + 1.0D + 0.02D;
            double z = pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.6D;

            level.addParticle(Oasiso.ENTROPY_BULB.get(), x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }
}