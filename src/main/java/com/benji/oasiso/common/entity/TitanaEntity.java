package com.benji.oasiso.common.entity;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ai.TitanaMeleeGoal;
import com.benji.oasiso.common.entity.ai.TitanaSandHandGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TitanaEntity extends Monster implements GeoEntity, GlowmaskEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // States
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(TitanaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MODEL_STATE = SynchedEntityData.defineId(TitanaEntity.class, EntityDataSerializers.INT);

    public int handCooldown = 0;
    public int handStrikesLeft = 0;
    public int nextStrikeTimer = 0;
    public int returnHandTimer = 0;

    public TitanaEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, 0);       // 0=Idle/Walk, 1=Hammer1, 2=Hammer2, 3=SandCast
        this.entityData.define(MODEL_STATE, 0);
    }

    public int getAnimState() { return this.entityData.get(STATE); }
    public void setAnimState(int state) { this.entityData.set(STATE, state); }

    public int getModelState() { return this.entityData.get(MODEL_STATE); }
    public void setModelState(int state) { this.entityData.set(MODEL_STATE, state); }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 30.0D)
                .add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
        this.goalSelector.addGoal(1, new TitanaSandHandGoal(this));
        this.goalSelector.addGoal(2, new TitanaMeleeGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            if (this.handCooldown > 0) this.handCooldown--;

            if (this.handStrikesLeft > 0) {
                if (this.nextStrikeTimer > 0) {
                    this.nextStrikeTimer--;
                } else {
                    if (this.getTarget() != null) {
                        SandHandEntity hand = Oasiso.SAND_HAND.get().create(this.level());
                        if (hand != null) {
                            hand.moveTo(this.getTarget().getX(), this.getTarget().getY(), this.getTarget().getZ(), this.getRandom().nextFloat() * 360F, 0);
                            this.level().addFreshEntity(hand);
                        }
                    }
                    this.handStrikesLeft--;

                    if (this.handStrikesLeft > 0) {
                        this.nextStrikeTimer = 40 + this.getRandom().nextInt(160);
                    } else {
                        this.returnHandTimer = 40;
                    }
                }
            }

            if (this.returnHandTimer > 0) {
                this.returnHandTimer--;
                spawnLeftParticles();

                if (this.returnHandTimer == 0) {
                    this.setModelState(0);
                }
            }
        }
    }

    public void spawnLeftParticles() {
        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 forward = Vec3.directionFromRotation(0, this.yBodyRot);
            Vec3 left = forward.yRot((float) Math.PI / 2); // left

            double px = this.getX() + left.x * 0.8;
            double pz = this.getZ() + left.z * 0.8;

            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
                    px, this.getY() + 1.0D, pz, 10, 0.2, 0.5, 0.2, 0.05);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, event -> {
            int state = this.getAnimState();

            if (state == 1) return event.setAndContinue(RawAnimation.begin().thenPlayAndHold("hammer"));
            if (state == 2) return event.setAndContinue(RawAnimation.begin().thenPlayAndHold("hammer2"));
            if (state == 3) return event.setAndContinue(RawAnimation.begin().thenPlayAndHold("sand_attack"));

            if (event.isMoving()) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }
            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));
    }

    @Override
    protected SoundEvent getAmbientSound() {

        SoundEvent[] sounds = {
                ModSounds.TITANA1.get(),
                ModSounds.TITANA2.get(),
                ModSounds.TITANA3.get()
        };

        return sounds[this.random.nextInt(sounds.length)];
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return ModSounds.TITANA_HIT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.TITANA_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        this.playSound(ModSounds.TITANA_STEP.get(), 1.0F, 1.0F);
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/titana_emissive.png");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
}