package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ai.MonkiMergeGoal;
import com.benji.oasiso.common.entity.ai.MonkiSpecialAttackGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MonkiEntity extends Monster implements GeoEntity, GlowmaskEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(MonkiEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(MonkiEntity.class, EntityDataSerializers.INT);

    public int hitCount = 0;
    public boolean isMerging = false;
    public int mergeCooldown = 0;

    public MonkiEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, 0); // def = 0
        this.entityData.define(STATE, 0);
    }

    public int getVariant() { return this.entityData.get(VARIANT); }
    public void setVariant(int variant) { this.entityData.set(VARIANT, variant); }

    public int getAnimState() { return this.entityData.get(STATE); }
    public void setAnimState(int state) { this.entityData.set(STATE, state); }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new MonkiMergeGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
        this.goalSelector.addGoal(1, new MonkiSpecialAttackGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.mergeCooldown > 0) {
            this.mergeCooldown--;
        }
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
// random texture
        this.setVariant(this.random.nextInt(4));
        return super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.getAnimState() == 1) {
            return false;
        }

        boolean wasHurt = super.hurt(source, amount);

        if (wasHurt && !this.level().isClientSide() && source.getEntity() instanceof Player && this.getAnimState() == 0) {
            this.hitCount++;
        }

        return wasHurt;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.getVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(tag.getInt("Variant"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, event -> {
            int state = this.getAnimState();

            if (state == 1) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("pose"));
            } else if (state == 2) {
                return event.setAndContinue(RawAnimation.begin().thenPlayAndHold("ball"));
            }

            if (event.isMoving()) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }
            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {

        int variant = getVariant();

        String textureName = variant == 0
                ? "monki_emissive"
                : "monki" + (variant + 1) + "_emissive";


        return ResourceLocation.fromNamespaceAndPath(
                Oasiso.MODID,
                "textures/entity/emissive/" + textureName + ".png"
        );
    }
}