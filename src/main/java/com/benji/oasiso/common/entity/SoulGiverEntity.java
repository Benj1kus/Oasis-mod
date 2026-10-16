package com.benji.oasiso.common.entity;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SoulGiverEntity extends Monster implements GeoEntity, GlowmaskEntity  {
    private int retaliationCooldown;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public SoulGiverEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 45.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && isAlive()) com.benji.oasiso.common.soul.SoulGraveLogic.tick(this);
        if (!level().isClientSide && retaliationCooldown > 0) retaliationCooldown--;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean damaged = super.hurt(source, amount);
        if (damaged && amount > 0 && level() instanceof ServerLevel server
                && source.getEntity() instanceof Player attacker && retaliationCooldown == 0) {
            retaliationCooldown = 100;
            attacker.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 300, 0));
            attacker.addEffect(new MobEffectInstance(MobEffects.POISON, 300, 0));
            for (int i = 0; i < 36; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double vertical = random.nextDouble() * 2.0 - 1.0;
                double horizontal = Math.sqrt(1.0 - vertical * vertical);
                double speed = 0.07 + random.nextDouble() * 0.08;
                server.sendParticles(Oasiso.SOUL_SMOKE.get(),
                        getX(), getY() + getBbHeight() * (0.2 + random.nextDouble() * 0.6), getZ(),
                        0, Math.cos(angle) * horizontal * speed, vertical * speed,
                        Math.sin(angle) * horizontal * speed, 1.0);
            }
        }
        return damaged;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, event -> {
            if (event.isMoving()) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }
            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));
    }

    @Override
    protected SoundEvent getAmbientSound() {

        SoundEvent[] sounds = {ModSounds.GIVER_IDLE1.get(), ModSounds.GIVER_IDLE2.get(), ModSounds.GIVER_IDLE3.get()};

        return sounds[this.random.nextInt(sounds.length)];
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return ModSounds.GIVER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.GIVER_DEATH.get();
    }


    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(
                Oasiso.MODID,
                "textures/entity/emissive/soul_giver_emissive.png"
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
