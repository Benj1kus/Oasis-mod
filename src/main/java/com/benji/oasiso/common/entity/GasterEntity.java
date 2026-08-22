package com.benji.oasiso.common.entity;

import com.benji.oasiso.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GasterEntity extends Monster implements GeoEntity {

    private static final EntityDataAccessor<Boolean> SMILING = SynchedEntityData.defineId(GasterEntity.class, EntityDataSerializers.BOOLEAN);


    private static final double EGG_RADIUS = 5.0D;

    private static final double TRIGGER_RADIUS = 1.75D;
    private static final double LOOK_RANGE = 64.0D;

    private static final int MIN_DISAPPEAR_TIME = 20;
    private static final int MAX_DISAPPEAR_TIME = 40;

    private static final String REWARDED_PLAYERS_TAG = "RewardedPlayers";
    private static final String DISAPPEAR_TICKS_TAG = "DisappearTicks";
    private static final String TRIGGER_PLAYER_TAG = "TriggerPlayer";


    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Set<UUID> rewardedPlayers = new HashSet<>();

    private int disappearTicks = -1;
    private UUID triggerPlayerId;

    public GasterEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1000.0D).add(Attributes.MOVEMENT_SPEED, 0.0D).add(Attributes.KNOCKBACK_RESISTANCE, 1.0D).add(Attributes.ATTACK_DAMAGE, 0.0D).add(Attributes.FOLLOW_RANGE, 300.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SMILING, false);
    }


    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 movement = this.getDeltaMovement();
        if (movement.x != 0.0D || movement.z != 0.0D) {
            this.setDeltaMovement(0.0D, movement.y, 0.0D);
        }
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.disappearTicks >= 0) {
            tickDisappearance(serverLevel);
            return;
        }
        lookAtNearestPlayer(serverLevel);
        giveEggsToNearbyPlayers(serverLevel);
        checkCloseTrigger(serverLevel);
    }

    private void lookAtNearestPlayer(ServerLevel level) {
        Player nearest = level.getNearestPlayer(this, LOOK_RANGE);
        if (nearest == null || nearest.isSpectator() || nearest.isCreative()) {
            return;
        }
        double deltaX = nearest.getX() - this.getX();
        double deltaZ = nearest.getZ() - this.getZ();

        float targetYaw = (float) (Mth.atan2(deltaZ, deltaX) * (180.0D / Math.PI)) - 90.0F;
        float yawDifference = Mth.wrapDegrees(targetYaw - this.getYRot());
        float newYaw = this.getYRot() + yawDifference * 0.18F;

        this.setYRot(newYaw);
        this.setYHeadRot(newYaw);

        this.yBodyRot = newYaw;
        this.yBodyRotO = newYaw;

        this.getLookControl().setLookAt(nearest, 30.0F, 30.0F);
    }

    private void giveEggsToNearbyPlayers(ServerLevel level) {
        double radiusSqr = EGG_RADIUS * EGG_RADIUS;
        for (ServerPlayer player : level.getPlayers(candidate -> candidate.isAlive() && !candidate.isSpectator() && !candidate.isCreative() && candidate.distanceToSqr(this) <= radiusSqr)) {

            UUID playerId = player.getUUID();
            if (!this.rewardedPlayers.add(playerId)) {
                continue;
            }
            ItemStack egg = new ItemStack(Items.EGG);
            boolean added = player.getInventory().add(egg);
            if (!added && !egg.isEmpty()) {
                player.drop(egg, false);
            }
            player.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8F, 1.45F);
        }
    }

    private void checkCloseTrigger(ServerLevel level) {
        double radiusSqr = TRIGGER_RADIUS * TRIGGER_RADIUS;
        ServerPlayer triggerPlayer = null;
        double nearestDistance = Double.MAX_VALUE;
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator() || player.isCreative()) {
                continue;
            }
            double distance = player.distanceToSqr(this);
            if (distance > radiusSqr || distance >= nearestDistance) {
                continue;
            }
            nearestDistance = distance;
            triggerPlayer = player;
        }
        if (triggerPlayer == null) {
            return;
        }
        beginDisappearance(level, triggerPlayer);
    }

    private void beginDisappearance(ServerLevel level, ServerPlayer player) {
        this.triggerPlayerId = player.getUUID();
        this.disappearTicks = MIN_DISAPPEAR_TIME + this.random.nextInt(MAX_DISAPPEAR_TIME - MIN_DISAPPEAR_TIME + 1);

        this.setSmiling(true);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.GASTER.get(), SoundSource.HOSTILE, 1.25F, 1.0F);

        spawnSmokeBurst(level, 35);
    }

    private void tickDisappearance(ServerLevel level) {
        if (this.tickCount % 3 == 0) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                    this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(),
                    5,
                    0.35D, 0.75D, 0.35D,
                    0.025D);
        }
        this.disappearTicks--;
        if (this.disappearTicks > 0) {
            return;
        }

        spawnSmokeBurst(level, 55);
        UUID playerId = this.triggerPlayerId;

        this.discard();
        if (playerId != null) {
            teleportPlayerToSpawn(level.getServer(), playerId);
        }
    }

    private void spawnSmokeBurst(ServerLevel level, int count) {
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(),
                count,
                0.55D, 1.05D, 0.55D,
                0.045D);
    }

    private static void teleportPlayerToSpawn(MinecraftServer server, UUID playerId) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            return;
        }
        ServerLevel targetLevel = server.getLevel(player.getRespawnDimension());
        BlockPos spawnPos = player.getRespawnPosition();
        float yaw = player.getRespawnAngle();
        if (targetLevel == null || spawnPos == null) {
            targetLevel = server.overworld();
            spawnPos = targetLevel.getSharedSpawnPos();
            yaw = 0.0F;
        }

        player.stopRiding();
        player.teleportTo(targetLevel,
                spawnPos.getX() + 0.5D, spawnPos.getY() + 1.0D, spawnPos.getZ() + 0.5D,
                yaw, 0.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
    }

    public boolean isSmiling() {
        return this.entityData.get(SMILING);
    }

    private void setSmiling(boolean smiling) {
        this.entityData.set(SMILING, smiling);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }


    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        ListTag rewardedList = new ListTag();
        for (UUID uuid : this.rewardedPlayers) {
            rewardedList.add(StringTag.valueOf(uuid.toString()));
        }
        tag.put(REWARDED_PLAYERS_TAG, rewardedList);
        tag.putInt(DISAPPEAR_TICKS_TAG, this.disappearTicks);
        if (this.triggerPlayerId != null) {
            tag.putString(TRIGGER_PLAYER_TAG, this.triggerPlayerId.toString());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.rewardedPlayers.clear();

        ListTag rewardedList = tag.getList(REWARDED_PLAYERS_TAG, Tag.TAG_STRING);

        for (int i = 0; i < rewardedList.size(); i++) {
            try {
                this.rewardedPlayers.add(UUID.fromString(rewardedList.getString(i)));
            } catch (IllegalArgumentException ignored) {
            }
        }

        this.disappearTicks = tag.contains(DISAPPEAR_TICKS_TAG, Tag.TAG_INT) ? tag.getInt(DISAPPEAR_TICKS_TAG) : -1;
        if (tag.contains(TRIGGER_PLAYER_TAG, Tag.TAG_STRING)) {
            try {
                this.triggerPlayerId = UUID.fromString(tag.getString(TRIGGER_PLAYER_TAG));
            } catch (IllegalArgumentException ignored) {
                this.triggerPlayerId = null;
            }
        }
        this.setSmiling(this.disappearTicks >= 0);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, event -> event.setAndContinue(RawAnimation.begin().thenLoop("idle"))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}