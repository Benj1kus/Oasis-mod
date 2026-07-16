package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class StatueBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int activeTicks = 0;
    private boolean isActivated = false;

    public StatueBlockEntity(BlockPos pos, BlockState state) {
        super(Oasiso.STATUE_BE.get(), pos, state);
    }

    public void startActivation() {
        if (!this.isActivated) {
            this.isActivated = true;
            this.activeTicks = 60;
            this.setChanged();
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, StatueBlockEntity entity) {

        if (!entity.isActivated) return;

        if (entity.activeTicks > 0) {
            entity.activeTicks--;

            if (level instanceof ServerLevel serverLevel) {
                double height = state.getShape(level, pos).max(net.minecraft.core.Direction.Axis.Y);

                serverLevel.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                        pos.getX() + 0.5D, pos.getY() + (height / 2.0D), pos.getZ() + 0.5D,
                        15,
                        0.4D, height / 2.0D, 0.4D,
                        0.05D
                );
            }

            if (entity.activeTicks <= 0) {
                entity.spawnGuardian(level, pos);
            }
        }
    }

    private void spawnGuardian(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            Mob guardian = null;
            Block block = this.getBlockState().getBlock();

            if (block == Oasiso.MONKI_STATUE.get()) {
                guardian = Oasiso.MONKI.get().create(serverLevel);
            } else if (block == Oasiso.DASHER_STATUE.get()) {
                guardian = Oasiso.DASHER.get().create(serverLevel);
            } else if (block == Oasiso.TITANA_STATUE.get()) {
                guardian = Oasiso.TITANA.get().create(serverLevel);
            }

            if (guardian != null) {
                guardian.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
                serverLevel.addFreshEntity(guardian);
            }

            level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.5F, 0.8F);

            level.removeBlock(pos, false);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("IsActivated", this.isActivated);
        tag.putInt("ActiveTicks", this.activeTicks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.isActivated = tag.getBoolean("IsActivated");
        this.activeTicks = tag.getInt("ActiveTicks");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}