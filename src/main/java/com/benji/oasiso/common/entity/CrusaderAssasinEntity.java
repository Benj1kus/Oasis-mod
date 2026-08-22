package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ai.CrusaderAssassinCombatGoal;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;


public class CrusaderAssasinEntity extends Monster implements GeoEntity, GlowmaskEntity {

    public static final int ACTION_NORMAL = 0;
    public static final int ACTION_ATTACK = 1;
    public static final int ACTION_INVIS = 2;

    private static final EntityDataAccessor<Integer> ACTION_STATE = SynchedEntityData.defineId(CrusaderAssasinEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK_ANIMATION = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation INVIS_ANIMATION = RawAnimation.begin().thenPlay("invis");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public CrusaderAssasinEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D)
                .add(Attributes.ATTACK_DAMAGE, 20.0D)
                .add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ACTION_STATE, ACTION_NORMAL);
    }

    public int getActionState() {
        return this.entityData.get(ACTION_STATE);
    }

    public void setActionState(int state) {
        this.entityData.set(ACTION_STATE, state);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new CrusaderAssassinCombatGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean success = super.doHurtTarget(target);
        if (success) {
            this.triggerAnim("action_controller", "attack");
        }
        return success;
    }

    public void playInvisibilityAnimation() {
        this.setActionState(ACTION_INVIS);
        this.triggerAnim("action_controller", "invis");
    }


    public void playAttackState() {
        this.setActionState(ACTION_ATTACK);
    }


    public void finishAction() {
        this.setActionState(ACTION_NORMAL);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5,

                state -> {

                    if (this.getActionState() != ACTION_NORMAL) {
                        return PlayState.STOP;
                    }
                    if (state.isMoving()) {
                        return state.setAndContinue(WALK_ANIMATION);
                    }
                    return state.setAndContinue(IDLE_ANIMATION);
                }));
        AnimationController<CrusaderAssasinEntity> actionController = new AnimationController<>(this, "action_controller", 0, state -> PlayState.STOP);
        actionController.triggerableAnim("attack", ATTACK_ANIMATION);
        actionController.triggerableAnim("invis", INVIS_ANIMATION);
        controllers.add(actionController);
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/" + "crusader_assasin_emissive.png");
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PILLAGER_AMBIENT;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return SoundEvents.PILLAGER_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PILLAGER_DEATH;
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}