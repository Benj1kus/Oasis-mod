package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import com.benji.oasiso.common.dimension.BossPortalTransitionServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class BossPortalEntity extends Monster implements GeoEntity, GlowmaskEntity {

    public static final int STATE_SPAWN = 0;
    public static final int STATE_IDLE = 1;
    public static final int STATE_DESPAWN = 2;

    private static final int DESPAWN_ANIMATION_TIME = 30;

    private int despawnTicks;
    private static final String DESPAWN_TICKS_TAG = "PortalDespawnTicks";

    private static final int SPAWN_ANIMATION_TIME = 30;

    private static final EntityDataAccessor<Integer> PORTAL_PURPOSE = SynchedEntityData.defineId(BossPortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANIM_STATE = SynchedEntityData.defineId(BossPortalEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation DESPAWN_ANIMATION = RawAnimation.begin().thenPlay("despawn");
    private static final RawAnimation SPAWN_ANIMATION = RawAnimation.begin().thenPlay("spawn");
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");

    private static final String SPAWN_TICKS_TAG = "PortalSpawnTicks";
    private static final String PORTAL_PURPOSE_TAG = "PortalPurpose";
    private static final String BOSS_SPAWNED_TAG = "BossSpawned";
    private static final String ARENA_SESSION_TAG = "ArenaSessionId";
    private static final String ARENA_PREPARED_TAG = "ArenaPrepared";

    private static final String ANIM_STATE_TAG = "PortalAnimState";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int spawnTicks;

    private UUID arenaSessionId;
    private boolean arenaPrepared;

    public enum PortalPurpose {
        CHAOS, DOMINATION, CHAOS_RETURN
    }

    private boolean bossSpawned;

    public BossPortalEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.noPhysics = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1000.0D).add(Attributes.MOVEMENT_SPEED, 0.0D).add(Attributes.KNOCKBACK_RESISTANCE, 1.0D).add(Attributes.ATTACK_DAMAGE, 0.0D).add(Attributes.FOLLOW_RANGE, 30.0D);
    }

    public void startDespawning() {

        if (this.getAnimState() == STATE_DESPAWN) {
            return;
        }

        this.despawnTicks = 0;
        this.setAnimState(STATE_DESPAWN);
        this.setDeltaMovement(Vec3.ZERO);
        this.setInvulnerable(true);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();

        this.entityData.define(PORTAL_PURPOSE, PortalPurpose.DOMINATION.ordinal());
        this.entityData.define(ANIM_STATE, STATE_SPAWN);
    }

    public PortalPurpose getPortalPurpose() {
        int purpose = this.entityData.get(PORTAL_PURPOSE);
        PortalPurpose[] values = PortalPurpose.values();
        if (purpose < 0 || purpose >= values.length) {
            return PortalPurpose.DOMINATION;
        }
        return values[purpose];
    }

    private void setPortalPurpose(PortalPurpose purpose) {
        this.entityData.set(PORTAL_PURPOSE, purpose.ordinal());
    }

    public boolean isChaosPortal() {
        PortalPurpose purpose = this.getPortalPurpose();
        return purpose == PortalPurpose.CHAOS || purpose == PortalPurpose.CHAOS_RETURN;
    }

    public boolean isChaosEntryPortal() {
        return this.getPortalPurpose() == PortalPurpose.CHAOS;
    }

    public boolean isChaosReturnPortal() {
        return this.getPortalPurpose() == PortalPurpose.CHAOS_RETURN;
    }

    public void startOpening() {
        startOpening(PortalPurpose.DOMINATION, null);
    }

    @Override
    protected void registerGoals() {
    }

    public void startOpening(PortalPurpose purpose) {
        startOpening(purpose, null);
    }

    public void startOpening(PortalPurpose purpose, UUID sessionId) {
        this.spawnTicks = 0;
        this.bossSpawned = false;
        this.despawnTicks = 0;

        this.setPortalPurpose(purpose);

        if (purpose == PortalPurpose.CHAOS) {
            this.arenaSessionId = sessionId != null ? sessionId : createChaosEntrySessionId();

            this.arenaPrepared = false;
        } else {
            this.arenaSessionId = sessionId;
            this.arenaPrepared = false;
        }

        this.setAnimState(STATE_SPAWN);
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.noPhysics = true;
        this.setDeltaMovement(Vec3.ZERO);
    }


    private UUID createChaosEntrySessionId() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return this.getUUID();
        }

        BlockPos portalPos = BlockPos.containing(this.getX(), this.getY(), this.getZ());
        long openingBucket = serverLevel.getGameTime() / 20L;

        String identity = serverLevel.dimension().location().toString() + "|" + portalPos.asLong() + "|" + openingBucket;

        return UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8));
    }

    public UUID getOrCreateArenaSessionId() {
        if (this.arenaSessionId == null) {
            this.arenaSessionId = this.getUUID();
        }

        return this.arenaSessionId;
    }

    public UUID getArenaSessionId() {
        return this.arenaSessionId;
    }

    public boolean isArenaPrepared() {
        return this.arenaPrepared;
    }

    public void markArenaPrepared() {
        this.arenaPrepared = true;
    }

    // types of portals

    @Override
    public void tick() {
        super.setDeltaMovement(Vec3.ZERO);
        super.tick();

        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.noPhysics = true;

        super.setDeltaMovement(Vec3.ZERO);

        this.fallDistance = 0.0F;
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.getAnimState() == STATE_DESPAWN) {
            this.despawnTicks++;
            if (this.despawnTicks >= DESPAWN_ANIMATION_TIME) {
                this.discard();
            }
            return;
        }

        if (this.getAnimState() == STATE_IDLE) {
            PortalPurpose purpose = this.getPortalPurpose();
            if (purpose == PortalPurpose.CHAOS) {
                tickChaosEntryPortal(serverLevel);
                return;
            }
            if (purpose == PortalPurpose.CHAOS_RETURN) {
                tickChaosReturnPortal(serverLevel);
                return;
            }
            if (this.bossSpawned) {
                return;
            }
            if (spawnAzumaal(serverLevel)) {
                this.bossSpawned = true;
            }
            return;
        }
        this.spawnTicks++;
        if (this.spawnTicks < SPAWN_ANIMATION_TIME) {
            return;
        }
        this.setAnimState(STATE_IDLE);
        if (this.isChaosPortal()) {
            return;
        }
        if (spawnAzumaal(serverLevel)) {

            this.bossSpawned = true;
        }
    }

    private void tickChaosEntryPortal(ServerLevel level) {
        if (level.dimension().equals(Oasiso.CHAOS_DIMENSION)) {
            return;
        }
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, getTeleportArea(), candidate -> candidate.isAlive() && !candidate.isSpectator())) {
            BossPortalTransitionServer.beginEnter(player, this);
        }
    }

    private void tickChaosReturnPortal(ServerLevel level) {
        if (!level.dimension().equals(Oasiso.CHAOS_DIMENSION)) {
            return;
        }
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, getTeleportArea(), candidate -> candidate.isAlive() && !candidate.isSpectator())) {
            BossPortalTransitionServer.beginReturn(player, this);
        }
    }

    private AABB getTeleportArea() {
        return new AABB(this.getX() - 0.85D, this.getY() - 0.20D, this.getZ() - 0.85D, this.getX() + 0.85D, this.getY() + 2.5D, this.getZ() + 0.85D);
    }

    private boolean spawnAzumaal(ServerLevel level) {
        AzumaalEntity boss = Oasiso.AZUMAAL.get().create(level);
        if (boss == null) {
            return false;
        }
        double hoverY = this.getY() + 2.0D;
        boss.moveTo(this.getX(), hoverY, this.getZ(), this.getYRot(), 0.0F);
        boss.startSpawnSequence(hoverY);
        boss.setBossPortal(this);
        boss.setArenaSessionId(this.arenaSessionId);
        level.addFreshEntity(boss);

        return true;
    }

    public int getAnimState() {
        return this.entityData.get(ANIM_STATE);
    }

    private void setAnimState(int state) {
        this.entityData.set(ANIM_STATE, state);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public void knockback(double strength, double x, double z) {
    }

    @Override
    public void setDeltaMovement(Vec3 movement) {
        super.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(PORTAL_PURPOSE_TAG, this.getPortalPurpose().ordinal());

        tag.putInt(DESPAWN_TICKS_TAG, this.despawnTicks);
        tag.putInt(SPAWN_TICKS_TAG, this.spawnTicks);
        tag.putBoolean(BOSS_SPAWNED_TAG, this.bossSpawned);
        tag.putInt(ANIM_STATE_TAG, this.getAnimState());
        tag.putBoolean(ARENA_PREPARED_TAG, this.arenaPrepared);

        if (this.arenaSessionId != null) {
            tag.putUUID(ARENA_SESSION_TAG, this.arenaSessionId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        this.despawnTicks = tag.getInt(DESPAWN_TICKS_TAG);
        this.spawnTicks = tag.getInt(SPAWN_TICKS_TAG);
        this.bossSpawned = tag.getBoolean(BOSS_SPAWNED_TAG);
        this.setAnimState(tag.contains(ANIM_STATE_TAG) ? tag.getInt(ANIM_STATE_TAG) : STATE_SPAWN);
        this.arenaPrepared = tag.getBoolean(ARENA_PREPARED_TAG);
        this.arenaSessionId = tag.hasUUID(ARENA_SESSION_TAG) ? tag.getUUID(ARENA_SESSION_TAG) : null;

        if (tag.contains(PORTAL_PURPOSE_TAG)) {
            int purpose = tag.getInt(PORTAL_PURPOSE_TAG);
            PortalPurpose[] values = PortalPurpose.values();
            this.setPortalPurpose(purpose >= 0 && purpose < values.length ? values[purpose] : PortalPurpose.DOMINATION);

        } else {
            this.setPortalPurpose(PortalPurpose.DOMINATION);
        }
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.noPhysics = true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> switch (this.getAnimState()) {
            case STATE_DESPAWN -> state.setAndContinue(DESPAWN_ANIMATION);
            case STATE_SPAWN -> state.setAndContinue(SPAWN_ANIMATION);
            default -> state.setAndContinue(IDLE_ANIMATION);
        }));
    }

    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/" + "boss_portal_emissive.png");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}