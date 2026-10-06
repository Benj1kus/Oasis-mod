package com.benji.oasiso.common.entity;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
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
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class EntZombieEntity extends Monster implements GeoEntity, GlowmaskEntity {

    private static final net.minecraft.network.syncher.EntityDataAccessor<Long> ENTROPY_STRIKE_END =
            net.minecraft.network.syncher.SynchedEntityData.defineId(EntZombieEntity.class, net.minecraft.network.syncher.EntityDataSerializers.LONG);
    private boolean restoreNoAi;
    private boolean strikePendingRestore;

    public boolean isEntropyStriking() {
        return entityData.get(ENTROPY_STRIKE_END) > level().getGameTime();
    }
    public void beginEntropyStrike() {
        if(level().isClientSide)return;
        restoreNoAi=isNoAi();strikePendingRestore=true;
        entityData.set(ENTROPY_STRIKE_END,level().getGameTime()+10);
        getNavigation().stop();setNoAi(true);
        setDeltaMovement(0,getDeltaMovement().y,0);
    }
    private void finishEntropyStrike() {
        if(!level().isClientSide && strikePendingRestore && !isEntropyStriking()) {
            setNoAi(restoreNoAi);strikePendingRestore=false;
            entityData.set(ENTROPY_STRIKE_END,-1L);
        }
    }
    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("EntropyStrikeEnd",entityData.get(ENTROPY_STRIKE_END));
        tag.putBoolean("EntropyStrikeRestore",strikePendingRestore);
        tag.putBoolean("EntropyStrikeNoAi",restoreNoAi);
    }
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ENTROPY_STRIKE_END,tag.contains("EntropyStrikeEnd")?tag.getLong("EntropyStrikeEnd"):-1L);
        strikePendingRestore=tag.getBoolean("EntropyStrikeRestore");restoreNoAi=tag.getBoolean("EntropyStrikeNoAi");
        finishEntropyStrike();
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public EntZombieEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();entityData.define(ENTROPY_STRIKE_END,-1L);
    }
    @Override public void tick() { super.tick();finishEntropyStrike(); }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this,"entropy_strike",0,event ->
                isEntropyStriking()?event.setAndContinue(RawAnimation.begin().thenPlay("strike")):PlayState.STOP));
        controllers.add(new AnimationController<>(this, "movement", 5, event -> {
            if(isEntropyStriking())return PlayState.STOP;
            if (event.isMoving()) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }
            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));

        controllers.add(new AnimationController<>(this, "action", 2, event -> PlayState.STOP)
                .triggerableAnim("hit", RawAnimation.begin().thenPlay("hit"))
                .triggerableAnim("walk_attack", RawAnimation.begin().thenPlay("walk_attack"))
                .triggerableAnim("attack_stay", RawAnimation.begin().thenPlay("attack_stay"))
        );
    }

    @Override
    public void swing(InteractionHand hand, boolean updateSelf) {
        super.swing(hand, updateSelf);
        if (!this.level().isClientSide && !isEntropyStriking()) {
            boolean isMoving = this.getDeltaMovement().horizontalDistanceSqr() > 0.001D;

            if (isMoving) {
                this.triggerAnim("action", "walk_attack");
            } else {
                this.triggerAnim("action", "attack_stay");
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean wasHurt = super.hurt(source, amount);
        if (wasHurt && !isEntropyStriking() && !this.level().isClientSide && this.isAlive()) {
            this.triggerAnim("action", "hit");
        }
        return wasHurt;
    }

    @Override
    protected SoundEvent getAmbientSound() {

        SoundEvent[] sounds = {ModSounds.EZOMBIE_IDLE1.get(), ModSounds.EZOMBIE_IDLE2.get(), ModSounds.EZOMBIE_IDLE3.get()};

        return sounds[this.random.nextInt(sounds.length)];
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return ModSounds.EZOMBIE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.EZOMBIE_DEATH.get();
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        if(isEntropyStriking())return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID,"textures/entity/ent_zombie_strike.png");
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/ent_zombie_emissive.png");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}