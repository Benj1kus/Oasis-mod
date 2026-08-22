package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.benji.oasiso.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CrusaderWizardEntity extends Monster implements GeoEntity, GlowmaskEntity {

    public static final int STATE_IDLE = 0;
    public static final int STATE_CAST = 1;

    private static final EntityDataAccessor<Integer> ANIM_STATE = SynchedEntityData.defineId(CrusaderWizardEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation CAST_ANIMATION = RawAnimation.begin().thenPlay("cast");

    private static final int CAST_DURATION_TICKS = 50;
    private static final int CAST_PILLARS_TICK = 10;

    private static final int MIN_CAST_COOLDOWN = 90;
    private static final int MAX_CAST_COOLDOWN = 150;

    private static final double CAST_TRIGGER_RANGE = 18.0D;
    private static final int PILLAR_COUNT = 3;
    private static final int PILLAR_RADIUS = 10;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int castTick = -1;
    private int castCooldown = 40;
    private UUID focusTargetId;

    public CrusaderWizardEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIM_STATE, STATE_IDLE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    public int getAnimState() {
        return this.entityData.get(ANIM_STATE);
    }
    public void setAnimState(int state) {
        this.entityData.set(ANIM_STATE, state);
    }
    public boolean isCasting() {
        return this.getAnimState() == STATE_CAST;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) this.level();
        if (this.isCasting()) {
            tickCasting(serverLevel);
            return;
        }
        if (this.castCooldown > 0) {
            this.castCooldown--;
        }
        Player target = findNearestValidPlayer(serverLevel);
        if (target != null) {
            this.getLookControl().setLookAt(target, 25.0F, 25.0F);
        }
        if (target != null && this.castCooldown <= 0 && this.distanceToSqr(target) <= CAST_TRIGGER_RANGE * CAST_TRIGGER_RANGE && this.hasLineOfSight(target)) {
            startCasting(target);
        }
    }

    private void startCasting(Player target) {
        this.setAnimState(STATE_CAST);
        this.castTick = 0;
        this.focusTargetId = target.getUUID();
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.CAST.get(), SoundSource.HOSTILE, 1.15F, 1.0F);
    }

    private void tickCasting(ServerLevel level) {
        this.getNavigation().stop();
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        Player target = resolveFocusTarget(level);
        if (target != null) {
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        this.castTick++;
        if (this.castTick == CAST_PILLARS_TICK) {
            summonPillars(level);
        }
        if (this.castTick >= CAST_DURATION_TICKS) {
            finishCasting();
        }
    }

    private void finishCasting() {
        this.setAnimState(STATE_IDLE);
        this.castTick = -1;
        this.focusTargetId = null;
        this.castCooldown = randomBetween(MIN_CAST_COOLDOWN, MAX_CAST_COOLDOWN);
    }

    private void summonPillars(ServerLevel level) {
        List<BlockPos> positions = findPillarBasePositions(level, PILLAR_COUNT, PILLAR_RADIUS);
        for (BlockPos pos : positions) {
            WizardPillarEntity pillar = new WizardPillarEntity(Oasiso.WIZARD_PILLAR_ENTITY.get(), level);
            pillar.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
            pillar.setOwnerUuid(this.getUUID());
            level.addFreshEntity(pillar);
        }
    }

    private List<BlockPos> findPillarBasePositions(ServerLevel level, int count, int radius) {
        List<BlockPos> result = new ArrayList<>();
        BlockPos center = this.blockPosition();
        for (int i = 0; i < count; i++) {
            BlockPos chosen = null;
            for (int attempt = 0; attempt < 32; attempt++) {
                int dx = Mth.nextInt(this.random, -radius, radius);
                int dz = Mth.nextInt(this.random, -radius, radius);
                BlockPos base = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.offset(dx, 0, dz));
                if (!canPlacePillar(level, base)) {
                    continue;
                }
                boolean tooClose = false;
                for (BlockPos existing : result) {
                    if (existing.distSqr(base) < 9.0D) {
                        tooClose = true;
                        break;
                    }
                }
                if (tooClose) {
                    continue;
                }
                chosen = base;
                break;
            }
            if (chosen != null) {
                result.add(chosen);
            }
        }
        return result;
    }

    private boolean canPlacePillar(ServerLevel level, BlockPos base) {
        if (!level.getBlockState(base.below()).isFaceSturdy(level, base.below(), Direction.UP)) {
            return false;
        }
        for (int i = 0; i < 5; i++) {
            BlockPos pos = base.above(i);
            if (!level.getBlockState(pos).canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    private Player findNearestValidPlayer(ServerLevel level) {
        Player player = level.getNearestPlayer(this, CAST_TRIGGER_RANGE);
        if (player == null) {
            return null;
        }
        if (player.isSpectator() || player.isCreative()) {
            return null;
        }
        return player;
    }

    private Player resolveFocusTarget(ServerLevel level) {
        if (this.focusTargetId == null) {
            return null;
        }
        Player player = level.getPlayerByUUID(this.focusTargetId);
        if (player == null || !player.isAlive() || player.isSpectator() || player.isCreative()) {
            return null;
        }
        return player;
    }

    private int randomBetween(int min, int max) {
        return min + this.random.nextInt(max - min + 1);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (this.getAnimState() == STATE_CAST) {
                return state.setAndContinue(CAST_ANIMATION);
            }
            if (state.isMoving()) {
                return state.setAndContinue(WALK_ANIMATION);
            }
            return state.setAndContinue(IDLE_ANIMATION);
        }));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("AnimState", this.getAnimState());
        tag.putInt("CastTick", this.castTick);
        tag.putInt("CastCooldown", this.castCooldown);
        if (this.focusTargetId != null) {
            tag.putUUID("FocusTarget", this.focusTargetId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setAnimState(tag.getInt("AnimState"));
        this.castTick = tag.getInt("CastTick");
        this.castCooldown = tag.getInt("CastCooldown");

        if (tag.hasUUID("FocusTarget")) {
            this.focusTargetId = tag.getUUID("FocusTarget");
        } else {
            this.focusTargetId = null;
        }
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PILLAGER_AMBIENT;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return SoundEvents.PILLAGER_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PILLAGER_DEATH;
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/crusader_wizard_emissive.png");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}