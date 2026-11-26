package com.benji.oasiso.common.entity;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CowritEntity extends PathfinderMob implements GeoEntity, GlowmaskEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation HIT = RawAnimation.begin().thenPlayAndHold("hit");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("death");
    private static final int BLEND = 3;
    private static final EntityDataAccessor<Long> HIT_AT = SynchedEntityData.defineId(CowritEntity.class, EntityDataSerializers.LONG);
    private long lastAnimatedHit = -1;

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(HIT_AT, -1L);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && isAlive() && !level().isClientSide) entityData.set(HIT_AT, level().getGameTime());
        return hurt;
    }

    @Override
    protected void tickDeath() {
        ++deathTime;
        if (deathTime >= 40 + BLEND + 1 && !level().isClientSide && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!isAlive() || !held.is(Items.BUCKET)) return super.mobInteract(player, hand);
        if (!level().isClientSide) {
            playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(Items.MILK_BUCKET)));
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CowritEntity(EntityType<? extends Mob> type, Level level) {
        super((EntityType<? extends PathfinderMob>) type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 15.0D).add(Attributes.MOVEMENT_SPEED, 0.2D).add(Attributes.KNOCKBACK_RESISTANCE, 0.3D).add(Attributes.ATTACK_DAMAGE, 0.0D).add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.4D));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", BLEND, event -> {
            if (isDeadOrDying()) return event.setAndContinue(DEATH);
            long hit = entityData.get(HIT_AT);
            if (hit >= 0 && level().getGameTime() - hit < 18 + BLEND) {
                if (lastAnimatedHit != hit) {
                    event.getController().forceAnimationReset();
                    lastAnimatedHit = hit;
                }
                return event.setAndContinue(HIT);
            }
            return event.setAndContinue(event.isMoving() ? WALK : IDLE);
        }));
    }

    @Override
    protected SoundEvent getAmbientSound() {

        SoundEvent[] sounds = {ModSounds.COWR_IDLE1.get(), ModSounds.COWR_IDLE2.get(), ModSounds.COWR_IDLE3.get()};

        return sounds[this.random.nextInt(sounds.length)];
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return ModSounds.COWR_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.COWR_DEATH.get();
    }


    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/cowrit_emissive.png");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
