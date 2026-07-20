package com.benji.oasiso.common.entity;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ai.SandGolemAttackGoal;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class SandGolemEntity extends Monster implements GeoEntity, GlowmaskEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public static final EntityDataAccessor<Boolean> PLAYER_CREATED = SynchedEntityData.defineId(SandGolemEntity.class, EntityDataSerializers.BOOLEAN);

    private int decayTimer = 0;
    private int lastStage = -1;

    public SandGolemEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PLAYER_CREATED, false);
    }

    public boolean isPlayerCreated() {
        return this.entityData.get(PLAYER_CREATED);
    }

    public void setPlayerCreated(boolean created) {
        this.entityData.set(PLAYER_CREATED, created);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("PlayerCreated", this.isPlayerCreated());
        compound.putInt("DecayTimer", this.decayTimer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.setPlayerCreated(compound.getBoolean("PlayerCreated"));
        this.decayTimer = compound.getInt("DecayTimer");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 25.0D)
                .add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this, Monster.class, false, false));
        this.goalSelector.addGoal(2, new SandGolemAttackGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
    }

    public int getStage() {
        float ratio = this.getHealth() / this.getMaxHealth();
        if (ratio > 0.90F) return 0;
        if (ratio > 0.70F) return 1;
        if (ratio > 0.50F) return 2;
        if (ratio > 0.30F) return 3;
        return 4;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            this.decayTimer++;
            if (this.decayTimer >= 3600) {
                this.decayTimer = 0;
                this.hurt(this.damageSources().generic(), 40.0F);
            }

            int currentStage = getStage();
            if (this.lastStage != -1 && currentStage > this.lastStage) {
                ((ServerLevel) this.level()).sendParticles(
                        new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
                        this.getX(), this.getY() + 1.5D, this.getZ(),
                        60, 0.6D, 1.0D, 0.6D, 0.1D
                );
                this.playSound(SoundEvents.SAND_BREAK, 1.0F, 0.8F);
            }
            this.lastStage = currentStage;
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (stack.is(Items.SANDSTONE) || stack.is(Items.CHISELED_SANDSTONE) || stack.is(Items.CUT_SANDSTONE) || stack.is(Items.SMOOTH_SANDSTONE)) {
            if (this.getHealth() < this.getMaxHealth()) {
                this.heal(40.0F);
                this.playSound(SoundEvents.SAND_PLACE, 1.0F, 1.0F);

                if (!this.level().isClientSide) {
                    ((ServerLevel) this.level()).sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.5D, this.getZ(), 15, 0.6D, 1.0D, 0.6D, 0.1D);
                }

                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }
        return super.mobInteract(player, hand);
    }

    public void performAoEAttack() {
        if (this.getStage() == 4) return;

        this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.5F, 0.5F);

        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(5.0D));

        for (LivingEntity target : targets) {
            if (target == this || !target.isAlive()) continue;
            if (target instanceof Player p && p.isCreative()) continue;

            if (this.isPlayerCreated() && target instanceof Player) continue;

            if (this.distanceToSqr(target) <= 49.0D) {
                boolean hit = target.hurt(this.damageSources().mobAttack(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
                if (hit) {
                    target.setDeltaMovement(target.getDeltaMovement().add(0.0D, 1.2D, 0.0D));
                    target.hasImpulse = true;
                    target.hurtMarked = true;
                }
            }
        }

        if (!this.level().isClientSide) {
            ((ServerLevel) this.level()).sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                    this.getX(), this.getY() + 0.1D, this.getZ(),
                    150, 3.0D, 0.2D, 3.0D, 0.15D
            );
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 5, event -> {
            if (event.isMoving()) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }
            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));

        controllers.add(new AnimationController<>(this, "action_controller", 0, event -> PlayState.CONTINUE)
                .triggerableAnim("attack", RawAnimation.begin().thenPlay("attack"))
        );
    }


    @Override
    protected SoundEvent getAmbientSound() {

        SoundEvent[] sounds = {
                ModSounds.SANDGOLEM1.get(),
                ModSounds.SANDGOLEM2.get(),
                ModSounds.SANDGOLEM_STEP.get()
        };

        return sounds[this.random.nextInt(sounds.length)];
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return ModSounds.SANDGOLEM_HIT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SANDGOLEM3.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        this.playSound(ModSounds.TITANA_STEP.get(), 1.0F, 1.0F);
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/sand_golem_emissive.png");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}