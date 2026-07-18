package com.benji.oasiso.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SandHandEntity extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int lifeTicks = 0;
    
    private LivingEntity owner;

    public SandHandEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setOwner(LivingEntity owner) {
        this.owner = owner;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D);
    }

    @Override
    public void tick() {
        super.tick();
        this.lifeTicks++;

        if (this.lifeTicks == 5 && !this.level().isClientSide) {
            for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(1.5D))) {

                if (entity == this || entity == this.owner) continue;

                if (this.owner == null && !(entity instanceof Player)) continue;

                if (entity instanceof Player player && player.isCreative()) continue;

                boolean hit = entity.hurt(this.damageSources().mobAttack(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));

                if (hit) {
                    entity.setDeltaMovement(entity.getDeltaMovement().add(0, 0.5D, 0));
                    entity.hurtMarked = true;
                    entity.hasImpulse = true;
                }
            }
        }

        if (this.lifeTicks >= 30 && !this.level().isClientSide) {
            this.discard();
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event ->
                event.setAndContinue(RawAnimation.begin().thenPlayAndHold("spawn"))
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}