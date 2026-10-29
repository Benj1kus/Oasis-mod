package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.registry.ModItems;
import com.benji.oasiso.common.shrine.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SoulGuardianEntity extends Monster implements GeoEntity, GlowmaskEntity {
    public static final double SCENE_RADIUS = 4;
    public static final int DISSOLVE_TICKS = 60;
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(SoulGuardianEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DISSOLVE = SynchedEntityData.defineId(SoulGuardianEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle"), SCENE = RawAnimation.begin().thenPlayAndHold("cutscene"), HOLD = RawAnimation.begin().thenLoop("shrine_hold"), AFTER = RawAnimation.begin().thenLoop("after_idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int retryDelay = 20, visualAge, deathAge;

    public SoulGuardianEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 500).add(Attributes.MOVEMENT_SPEED, 0).add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.ATTACK_DAMAGE, 0).add(Attributes.FOLLOW_RANGE, 30);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(PHASE, 0);
        entityData.define(DISSOLVE, 0);
    }

    public int getShrinePhase() {
        return entityData.get(PHASE);
    }

    public void setShrinePhase(int phase) {
        entityData.set(PHASE, phase);
    }

    public void endScene(boolean completed) {
        if (getShrinePhase() == 1) {
            setShrinePhase(completed ? 2 : 0);
            retryDelay = 100;
        }
    }

    public boolean isDissolving() {
        return getShrinePhase() == 3 || isDeadOrDying();
    }

    public int getDissolveAge() {
        return isDeadOrDying() ? deathAge : Math.max(visualAge, entityData.get(DISSOLVE));
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return false;
    }

    @Override
    public void travel(Vec3 input) {
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return getShrinePhase() != 1 && getShrinePhase() != 2 && getShrinePhase() != 3 && super.hurt(source, amount);
    }

    public void face(Player player) {
        float yaw = (float) Math.toDegrees(Math.atan2(player.getZ() - getZ(), player.getX() - getX())) - 90;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    @Override
    public void tick() {
        super.tick();
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        if (!isAlive()) return;
        if (level().isClientSide) {
            if (getShrinePhase() == 3) visualAge = Math.max(visualAge + 1, entityData.get(DISSOLVE));
            return;
        }
        if (getShrinePhase() == 3) {
            int age = entityData.get(DISSOLVE) + 1;
            if (age >= DISSOLVE_TICKS) {
                discard();
                return;
            }
            entityData.set(DISSOLVE, age);
            return;
        }
        if (getShrinePhase() != 0) return;
        if (retryDelay > 0) retryDelay--;
        ServerPlayer nearest = level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(16), p -> GuardianScenes.eligible(p) && hasLineOfSight(p)).stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (nearest == null) return;
        face(nearest);
        if (retryDelay == 0 && distanceToSqr(nearest) <= SCENE_RADIUS * SCENE_RADIUS)
            GuardianScenes.begin(this, nearest);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (getShrinePhase() != 2 || !isAlive() || player.isSpectator()) return super.mobInteract(player, hand);
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer server)) return InteractionResult.FAIL;
        setShrinePhase(3);
        entityData.set(DISSOLVE, 0);
        ItemStack reward = new ItemStack(ModItems.WAR_CORE.get());
        if (!player.getInventory().add(reward)) player.drop(reward, false);
        ((ServerLevel) level()).sendParticles(Oasiso.ENT_SOUL.get(), getX(), getY() + getBbHeight() * .5, getZ(), 45, getBbWidth() * .4, getBbHeight() * .4, getBbWidth() * .4, .04);
        ShrineSouls.awaken(server);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void tickDeath() {
        deathAge++;
        if (deathAge >= DISSOLVE_TICKS) {
            deathTime = 19;
            super.tickDeath();
        } else deathTime = Math.min(19, deathAge);
    }

    @Override
    public void handleEntityEvent(byte event) {
        if (event != 60) super.handleEntityEvent(event);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ShrinePhase", getShrinePhase());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int p = tag.getInt("ShrinePhase");
        setShrinePhase(p == 2 ? 2 : p == 3 ? 3 : 0);
        if (p == 3) entityData.set(DISSOLVE, DISSOLVE_TICKS - 1);
        setPersistenceRequired();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, event -> {
            int p = getShrinePhase();
            event.getController().transitionLength(p == 3 ? 8 : p == 2 ? 0 : 5);
            return event.setAndContinue(p == 3 ? AFTER : p == 2 ? HOLD : p == 1 ? SCENE : IDLE);
        }));
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/soul_guardian.png");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
