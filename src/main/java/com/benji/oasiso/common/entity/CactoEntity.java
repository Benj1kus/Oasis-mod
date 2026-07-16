package com.benji.oasiso.common.entity;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ai.CactoAttackGoal;
import com.benji.oasiso.common.entity.projectile.CactoProjEntity;
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
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CactoEntity extends Monster implements GeoEntity, GlowmaskEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(CactoEntity.class, EntityDataSerializers.INT);

    public boolean hasSpawned = false;
    private int spawnTimer = 0;

    public CactoEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, 0); // 0 = Spawn, 1 = Idle, 2 = Attack
    }

    public int getAnimState() { return this.entityData.get(STATE); }
    public void setAnimState(int state) { this.entityData.set(STATE, state); }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
        this.goalSelector.addGoal(1, new CactoAttackGoal(this));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide && !this.hasSpawned) {
            this.setAnimState(0);
            this.spawnTimer++;

            if (this.spawnTimer >= 15) {
                this.hasSpawned = true;
                this.setAnimState(1);

                for (int i = 0; i < 20; i++) {
                    double angle = 2 * Math.PI * i / 20;
                    double dx = Math.cos(angle);
                    double dz = Math.sin(angle);

                    CactoProjEntity proj = new CactoProjEntity(this.level(), this);

                    proj.setPos(this.getX(), this.getY() + 1.0D, this.getZ());

                    proj.shoot(dx, 0.2D, dz, 1.0F, 0.0F);
                    this.level().addFreshEntity(proj);
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("HasSpawned", this.hasSpawned);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.hasSpawned = tag.getBoolean("HasSpawned");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, event -> {
            int state = this.getAnimState();

            if (state == 0) return event.setAndContinue(RawAnimation.begin().thenPlayAndHold("spawn"));
            if (state == 2) return event.setAndContinue(RawAnimation.begin().thenPlay("attack"));

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
                ModSounds.CACTO1.get(),
                ModSounds.CACTO2.get(),
                ModSounds.CACTO3.get()
        };

        return sounds[this.random.nextInt(sounds.length)];
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return ModSounds.CACTO_HIT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CACTO_DEATH.get();
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/cacto_emissive.png");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}