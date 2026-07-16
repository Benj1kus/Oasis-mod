package com.benji.oasiso.common.entity;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ai.MonkiBigTornadoGoal;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MonkiBigEntity extends Monster implements GeoEntity, GlowmaskEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(MonkiBigEntity.class, EntityDataSerializers.INT);

    private float hp1 = 10.0F, hp2 = 10.0F, hp3 = 10.0F;
    private int attackCount = 0;

    public MonkiBigEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, 0);
    }

    public int getAnimState() { return this.entityData.get(STATE); }
    public void setAnimState(int state) { this.entityData.set(STATE, state); }

    public void setStoredHealths(float h1, float h2, float h3) {
        this.hp1 = h1;
        this.hp2 = h2;
        this.hp3 = h3;
    }

    public void incrementAttackCount() { this.attackCount++; }
    public int getAttackCount() { return this.attackCount; }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
        this.goalSelector.addGoal(1, new MonkiBigTornadoGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide && this.getAnimState() == 1) {
            for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.5D))) {
                if (entity instanceof Player player && !player.isCreative()) {
                    player.hurt(this.damageSources().mobAttack(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));

                    Vec3 knockback = player.position().subtract(this.position()).normalize().scale(1.2D);
                    player.push(knockback.x, 0.1D, knockback.z);
                    player.hurtMarked = true;
                }
            }
        }
    }

    public void splitBack() {
        if (!this.level().isClientSide) {
            float[] hps = {this.hp1, this.hp2, this.hp3};
            for (int i = 0; i < 3; i++) {
                MonkiEntity monki = Oasiso.MONKI.get().create(this.level());
                if (monki != null) {
                    double ox = (this.random.nextDouble() - 0.5) * 3.0D;
                    double oz = (this.random.nextDouble() - 0.5) * 3.0D;
                    monki.moveTo(this.getX() + ox, this.getY(), this.getZ() + oz, this.getYRot(), this.getXRot());
                    monki.setHealth(Math.max(1.0F, hps[i]));

                    monki.mergeCooldown = 600;

                    this.level().addFreshEntity(monki);
                }
            }

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                        this.getX(), this.getY() + 1.5, this.getZ(),
                        200, 1.5, 2.0, 1.5, 0.2);
            }
            this.level().playSound(null, this.blockPosition(), SoundEvents.SAND_BREAK, SoundSource.HOSTILE, 2.0F, 1.0F);

            this.discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("HP1", this.hp1);
        tag.putFloat("HP2", this.hp2);
        tag.putFloat("HP3", this.hp3);
        tag.putInt("AttackCount", this.attackCount);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.hp1 = tag.getFloat("HP1");
        this.hp2 = tag.getFloat("HP2");
        this.hp3 = tag.getFloat("HP3");
        this.attackCount = tag.getInt("AttackCount");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, event -> {
            if (this.getAnimState() == 1) {
                return event.setAndContinue(RawAnimation.begin().thenPlayAndHold("tornado"));
            }
            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));
    }

    @Override
    protected SoundEvent getAmbientSound() {

        SoundEvent[] sounds = {
                ModSounds.MONKI1.get(),
                ModSounds.MONKI2.get(),
                ModSounds.MONKI3.get()
        };

        return sounds[this.random.nextInt(sounds.length)];
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return ModSounds.MONKI_HIT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.MONKI_DEATH.get();
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {

        return ResourceLocation.fromNamespaceAndPath(
                Oasiso.MODID,
                "textures/entity/emissive/monki_big_emissive.png"
        );
    }
}