package com.benji.oasiso.common.entity;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.renderer.DasherTrailRenderer;
import com.benji.oasiso.common.entity.ai.DasherDashGoal;
import com.benji.oasiso.common.entity.ai.DasherKeepDistanceGoal;
import com.benji.oasiso.common.entity.ai.DasherMagneticGoal;
import com.benji.oasiso.common.entity.ai.DasherMeleeGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class DasherEntity extends Monster implements GeoEntity, GlowmaskEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(DasherEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_DASHING = SynchedEntityData.defineId(DasherEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_MAGNETIC = SynchedEntityData.defineId(DasherEntity.class, EntityDataSerializers.BOOLEAN);

    public int dashCooldown = 0;
    public int magneticCooldown = 0;
    private int changeTimer = 0;
    private int defenceTimer = 0;

    public boolean forceDash = false;
    private int hitCounter = 0;
    private int hitResetTimer = 0;

    public DasherEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        // 0=Idle/Walk, 1=Close Attack, 2=Dash, 3=Defence, 4=Change Phase, 5=Magnetic Shockwave
        this.entityData.define(STATE, 0);
        this.entityData.define(IS_DASHING, false);
        this.entityData.define(IS_MAGNETIC, false);
    }

    public int getAnimState() { return this.entityData.get(STATE); }
    public void setAnimState(int state) { this.entityData.set(STATE, state); }

    public boolean isDashing() { return this.entityData.get(IS_DASHING); }
    public void setDashing(boolean dashing) { this.entityData.set(IS_DASHING, dashing); }

    public boolean isMagnetic() { return this.entityData.get(IS_MAGNETIC); }
    public void setMagnetic(boolean magnetic) { this.entityData.set(IS_MAGNETIC, magnetic); }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D)
                .add(Attributes.ATTACK_DAMAGE, 40.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
        this.goalSelector.addGoal(1, new DasherDashGoal(this));
        this.goalSelector.addGoal(2, new DasherMeleeGoal(this));
        this.goalSelector.addGoal(2, new DasherMagneticGoal(this));
        this.goalSelector.addGoal(3, new DasherKeepDistanceGoal(this));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.getAnimState() == 4) return false;

        if (!this.isMagnetic() && source.getDirectEntity() instanceof Projectile) {
            if (this.getAnimState() == 0 || this.getAnimState() == 3) {
                this.setAnimState(3);
                this.defenceTimer = 20;
            }
            return false;
        }

        boolean wasHurt = super.hurt(source, amount);

        if (wasHurt && !this.level().isClientSide && this.getAnimState() != 2) {

            if (!this.isMagnetic() && this.getHealth() <= this.getMaxHealth() / 2.0F) {
                this.setAnimState(4);
                this.changeTimer = 80;
                this.setDashing(false);
                this.getNavigation().stop();
                this.hitCounter = 0;
            }
            else if (this.getAnimState() != 4 && source.getDirectEntity() instanceof Player) {
                this.hitCounter++;
                this.hitResetTimer = 60;

                if (this.hitCounter >= 3) {
                    this.forceDash = true;
                    this.hitCounter = 0;
                }
            }
        }

        return wasHurt;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            if (this.dashCooldown > 0) this.dashCooldown--;
            if (this.magneticCooldown > 0) this.magneticCooldown--;

            if (this.defenceTimer > 0) {
                this.defenceTimer--;
                if (this.defenceTimer == 0 && this.getAnimState() == 3) {
                    this.setAnimState(0);
                }
            }

            if (this.changeTimer > 0) {
                this.changeTimer--;
                this.getNavigation().stop();
                if (this.changeTimer == 0) {
                    this.setMagnetic(true);
                    this.setAnimState(0);
                }
            }

            if (this.hitResetTimer > 0) {
                this.hitResetTimer--;
                if (this.hitResetTimer == 0) {
                    this.hitCounter = 0;
                }
            }
        }

        if (this.level().isClientSide) {
            DasherTrailRenderer.updateTrail(this.getUUID(), this.position().add(0, 1.0, 0), this.isDashing());
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("IsMagnetic", this.isMagnetic());
        tag.putInt("MagneticCooldown", this.magneticCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setMagnetic(tag.getBoolean("IsMagnetic"));
        this.magneticCooldown = tag.getInt("MagneticCooldown");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, event -> {
            int state = this.getAnimState();

            if (state == 1) return event.setAndContinue(RawAnimation.begin().thenPlayAndHold("attack_close"));
            if (state == 2) return event.setAndContinue(RawAnimation.begin().thenPlayAndHold("dash"));
            if (state == 3) return event.setAndContinue(RawAnimation.begin().thenPlayAndHold("defence"));
            if (state == 4) return event.setAndContinue(RawAnimation.begin().thenPlayAndHold("change"));
            if (state == 5) return event.setAndContinue(RawAnimation.begin().thenPlayAndHold("magnetic"));

            if (event.isMoving()) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }
            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));
    }

    @Override
    public void playAmbientSound() {
        SoundEvent sound = getAmbientSound();

        if (sound != null) {
            this.playSound(sound, 0.2F, 1.0F);
        }
    }


    @Override
    protected SoundEvent getAmbientSound() {

        SoundEvent[] sounds = {
                ModSounds.DASHER1.get(),
                ModSounds.DASHER2.get(),
                ModSounds.DASHER3.get()
        };

        return sounds[this.random.nextInt(sounds.length)];
    }


    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return ModSounds.DASHER_HIT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.DASHER_DEATH.get();
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/dasher_emissive.png");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
}