package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ai.CrusaderTankCombatGoal;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;


public class CrusaderTankEntity extends Monster implements GeoEntity, GlowmaskEntity {

    public static final int STATE_IDLE = 0;
    public static final int STATE_ATTACK_MELEE = 1;
    public static final int STATE_ATTACK_LONG = 2;

    private static final EntityDataAccessor<Integer> ANIM_STATE = SynchedEntityData.defineId(CrusaderTankEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK_MELEE_ANIMATION = RawAnimation.begin().thenPlay("attack_melee");
    private static final RawAnimation ATTACK_LONG_ANIMATION = RawAnimation.begin().thenPlay("attack_long");

    private static final double CANNON_SIDE_OFFSET = 0.875D;
    private static final double CANNON_FORWARD_OFFSET = 0.15D;
    private static final double CANNON_HEIGHT_FACTOR = 0.46D;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CrusaderTankEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 20.0D)
                .add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIM_STATE, STATE_IDLE);
    }
    public int getAnimState() {
        return this.entityData.get(ANIM_STATE);
    }
    public void setAnimState(int state) {
        this.entityData.set(ANIM_STATE, state);
    }
    public boolean isAttacking() {
        return this.getAnimState() != STATE_IDLE;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new CrusaderTankCombatGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
    }

    public Vec3 getCannonMuzzlePosition() {
        double yawRadians = Math.toRadians(this.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
        Vec3 cannonSide = new Vec3(-forward.z, 0.0D, forward.x);
        return new Vec3(this.getX(), this.getY() + this.getBbHeight() * CANNON_HEIGHT_FACTOR, this.getZ()).add(cannonSide.scale(CANNON_SIDE_OFFSET)).add(forward.scale(CANNON_FORWARD_OFFSET));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0,
                state -> {
                    switch (this.getAnimState()) {

                        case STATE_ATTACK_MELEE -> {
                            return state.setAndContinue(ATTACK_MELEE_ANIMATION);
                        }
                        case STATE_ATTACK_LONG -> {
                            return state.setAndContinue(ATTACK_LONG_ANIMATION);
                        }
                        default -> {
                            if (state.isMoving()) {
                                return state.setAndContinue(WALK_ANIMATION);
                            }
                            return state.setAndContinue(IDLE_ANIMATION);
                        }
                    }
                }));
    }
    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/" + "crusader_tank_emissive.png");
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}