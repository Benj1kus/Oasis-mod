package com.benji.oasiso.common.entity.projectile;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class DesertBallEntity extends ThrowableProjectile implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DesertBallEntity(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
    }

    public DesertBallEntity(Level level, LivingEntity shooter) {
        super((EntityType<? extends ThrowableProjectile>) Oasiso.DESERT_BALL.get(), shooter, level);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        // particles
        if (this.level().isClientSide) {
            this.level().addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
                    this.getX(), this.getY() + 0.1, this.getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide) {
            // damage
            result.getEntity().hurt(this.damageSources().mobProjectile(this, (LivingEntity) this.getOwner()), 5.0F);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            for (int i = 0; i < 8; ++i) {
                this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                        this.getX(), this.getY(), this.getZ(),
                        (this.random.nextDouble() - 0.5D) * 0.2D, this.random.nextDouble() * 0.2D, (this.random.nextDouble() - 0.5D) * 0.2D);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event ->
                event.setAndContinue(RawAnimation.begin().thenLoop("idle"))
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}