package com.benji.oasiso.common.entity;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import com.benji.oasiso.registry.ModEntities;
import com.benji.oasiso.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BabyCowritEntity extends AgeableMob implements GeoEntity, GlowmaskEntity {
    private static final int GROWTH_TICKS = 20 * 60 * 20;
    private UUID motherId;
    private int cactosRemaining = 3 + random.nextInt(3);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public BabyCowritEntity(EntityType<? extends BabyCowritEntity> type, Level level) {
        super(type, level);
        setAge(-GROWTH_TICKS);
    }

    void setMother(UUID id) {
        motherId = id;
    }

    @Override
    public float getScale() {
        return 1.0F;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && isAlive() && !isRemoved() && getAge() >= 0) {
            convertTo(ModEntities.COWRIT.get(), true);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!stack.is(ModItems.CACTOS_ITEM.get()) || !isAlive()) return super.mobInteract(player, hand);
        if (level() instanceof ServerLevel server && getAge() < 0) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY(0.65), getZ(),
                    7, getBbWidth() * 0.35, getBbHeight() * 0.25, getBbWidth() * 0.35, 0.02);
            if (--cactosRemaining == 0) setAge(0);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (motherId != null) tag.putUUID("CowritMother", motherId);
        tag.putInt("CactosRemaining", cactosRemaining);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (!tag.contains("Age")) setAge(-GROWTH_TICKS);
        motherId = tag.hasUUID("CowritMother") ? tag.getUUID("CowritMother") : null;
        if (tag.contains("CactosRemaining")) cactosRemaining = tag.getInt("CactosRemaining");
    }

    private final class FollowCowritGoal extends Goal {
        private CowritEntity adult;
        private int repath;

        FollowCowritGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!(level() instanceof ServerLevel server)) return false;
            if (motherId != null && server.getEntity(motherId) instanceof CowritEntity mother
                    && mother.isAlive() && distanceToSqr(mother) <= 32 * 32) {
                adult = mother;
            } else {
                adult = null;
                double nearest = 16 * 16;
                for (CowritEntity cow : server.getEntitiesOfClass(CowritEntity.class,
                        getBoundingBox().inflate(16), cow -> cow.isAlive() && !cow.isBaby())) {
                    double distance = distanceToSqr(cow);
                    if (distance < nearest) {
                        nearest = distance;
                        adult = cow;
                    }
                }
            }
            return adult != null && distanceToSqr(adult) >= 4 * 4;
        }

        @Override
        public boolean canContinueToUse() {
            if (adult == null || !adult.isAlive()) return false;
            double distance = distanceToSqr(adult);
            return distance > 2.5 * 2.5 && distance <= 32 * 32;
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void tick() {
            if (--repath <= 0) {
                repath = adjustedTickDelay(10);
                getNavigation().moveTo(adult, 1.15);
            }
        }

        @Override
        public void stop() {
            adult = null;
            getNavigation().stop();
        }
    }

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final int BLEND = 3;

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.4D));
        this.goalSelector.addGoal(3, new FollowCowritGoal());
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8D));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", BLEND,
                event -> event.setAndContinue(event.isMoving() ? WALK : IDLE)));
    }

    @Override
    protected SoundEvent getAmbientSound() {

        SoundEvent[] sounds = {ModSounds.BCOWR_IDLE1.get(), ModSounds.BCOWR_IDLE2.get(), ModSounds.BCOWR_IDLE3.get()};

        return sounds[this.random.nextInt(sounds.length)];
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return ModSounds.BCOWR_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.BCOWR_DEATH.get();
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(
                Oasiso.MODID,
                "textures/entity/emissive/baby_cowrit_emissive.png"
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
