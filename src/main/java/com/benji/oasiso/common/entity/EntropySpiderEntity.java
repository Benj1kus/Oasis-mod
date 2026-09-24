package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import software.bernie.geckolib.util.GeckoLibUtil;

public class EntropySpiderEntity extends Monster implements GeoEntity, GlowmaskEntity  {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public EntropySpiderEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false) {
            private long nextHit;

            @Override
            protected void checkAndPerformAttack(LivingEntity target, double distanceSqr) {
                long now = EntropySpiderEntity.this.level().getGameTime();
                if (now >= nextHit && distanceSqr <= getAttackReachSqr(target) && EntropySpiderEntity.this.getSensing().hasLineOfSight(target)) {
                    nextHit = now + 40;
                    EntropySpiderEntity.this.swing(InteractionHand.MAIN_HAND);
                    EntropySpiderEntity.this.doHurtTarget(target);
                }
            }
        });
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "procedural", 0, event -> PlayState.STOP));
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(
                Oasiso.MODID,
                "textures/entity/emissive/entropy_spider_emissive.png"
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
