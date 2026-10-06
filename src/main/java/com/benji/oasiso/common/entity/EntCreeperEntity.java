package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
// ВАЖНО: Замени ModParticles на свой класс-реестр (DeferredRegister), где зарегистрированы партиклы!
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
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
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

public class EntCreeperEntity extends Monster implements GeoEntity, GlowmaskEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Boolean> DATA_IGNITED = SynchedEntityData.defineId(EntCreeperEntity.class, EntityDataSerializers.BOOLEAN);

    private int swell = 0;
    private final int maxSwell = 20;

    public EntCreeperEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0D).add(Attributes.MOVEMENT_SPEED, 0.25D).add(Attributes.KNOCKBACK_RESISTANCE, 0.3D).add(Attributes.ATTACK_DAMAGE, 0.0D).add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IGNITED, false);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
        this.goalSelector.addGoal(2, new EntCreeperIgniteGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8D));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, event -> {
            if (this.isIgnited()) {
                return PlayState.STOP;
            }
            if (event.isMoving()) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }
            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));

        controllers.add(new AnimationController<>(this, "action", 2, event -> PlayState.STOP).triggerableAnim("hit", RawAnimation.begin().thenPlay("hit")).triggerableAnim("explode", RawAnimation.begin().thenPlay("explode")) // Добавили триггер взрыва
        );
    }

    @Override
    public void tick() {
        super.tick();

        if (this.isAlive() && this.isIgnited()) {
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);

            this.swell++;
            if (!this.level().isClientSide && this.swell >= this.maxSwell) {
                this.explode();
            }
        }
    }

    public boolean isIgnited() {
        return this.entityData.get(DATA_IGNITED);
    }

    public void ignite() {
        if (!this.isIgnited()) {
            this.entityData.set(DATA_IGNITED, true);
            if (!this.level().isClientSide) {
                this.triggerAnim("action", "explode"); // Запускаем анимацию
            }
        }
    }

    private void explode() {
        if (!this.level().isClientSide) {
            float radius = 6.0F;
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), radius, Level.ExplosionInteraction.MOB);
            ServerLevel serverLevel = (ServerLevel) this.level();

            serverLevel.sendParticles(Oasiso.CHAOS_BOMB_CENTER_SMOKE.get(), this.getX(), this.getY() + 1.0, this.getZ(), 15, 0.5, 0.5, 0.5, 0.05);
            serverLevel.sendParticles(Oasiso.CHAOS_BOMB_FIRE_SMOKE.get(), this.getX(), this.getY() + 1.0, this.getZ(), 25, 1.5, 1.5, 1.5, 0.1);
            serverLevel.sendParticles(Oasiso.CHAOS_BOMB_SPARKS.get(), this.getX(), this.getY() + 1.0, this.getZ(), 45, 2.0, 2.0, 2.0, 0.6);
            this.discard();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean wasHurt = super.hurt(source, amount);
        if (wasHurt && !this.level().isClientSide && this.isAlive() && !this.isIgnited()) {
            this.triggerAnim("action", "hit");
        }
        return wasHurt;
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/ent_creeper_emissive.png");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    class EntCreeperIgniteGoal extends Goal {
        private final EntCreeperEntity creeper;
        private LivingEntity target;

        public EntCreeperIgniteGoal(EntCreeperEntity creeper) {
            this.creeper = creeper;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity livingTarget = this.creeper.getTarget();
            return this.creeper.isIgnited() || (livingTarget != null && this.creeper.distanceToSqr(livingTarget) < 9.0D);
        }

        @Override
        public void start() {
            this.creeper.getNavigation().stop();
            this.target = this.creeper.getTarget();
            this.creeper.ignite();
        }

        @Override
        public void tick() {
            if (this.target == null) {
                this.creeper.setTarget(null);
            }
        }
    }
}