package com.benji.oasiso.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import com.benji.oasiso.common.dimension.ChaosPocketTransitionServer;

import javax.annotation.Nullable;
import java.util.UUID;

public class ChaosPortalEntity extends Entity {

    public static final int OPEN_TIME_TICKS = 44;
    public static final int IDLE_TIMEOUT_TICKS = 20 * 20;
    public static final int DESPAWN_TIME_TICKS = 50;

    private PortalRole role = PortalRole.ENTRANCE;
    private boolean keepAlive;
    private boolean acceptingPlayers = true;

    private static final EntityDataAccessor<Float> PORTAL_YAW = SynchedEntityData.defineId(ChaosPortalEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SHADER_SEED = SynchedEntityData.defineId(ChaosPortalEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> OPENED = SynchedEntityData.defineId(ChaosPortalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DESPAWNING = SynchedEntityData.defineId(ChaosPortalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Long> DESPAWN_START_TIME = SynchedEntityData.defineId(ChaosPortalEntity.class, EntityDataSerializers.LONG);

    @Nullable
    private UUID ownerId;
    @Nullable
    private UUID linkedPortalId;

    private long lastActivityGameTime = Long.MIN_VALUE;


    public enum PortalRole {
        ENTRANCE, RETURN
    }

    public ChaosPortalEntity(EntityType<? extends ChaosPortalEntity> type, Level level) {
        super(type, level);

        this.setNoGravity(true);
        this.noPhysics = true;
        this.setInvulnerable(true);
    }

    public void initializePortal(Player owner, float portalYaw, float shaderSeed) {
        this.ownerId = owner.getUUID();
        this.role = PortalRole.ENTRANCE;
        this.keepAlive = false;
        this.acceptingPlayers = true;
        this.linkedPortalId = null;

        this.setPortalYaw(portalYaw);
        this.setShaderSeed(shaderSeed);
        this.setOpened(false);
        this.setDespawning(false);
        this.setDespawnStartTime(-1L);

        this.lastActivityGameTime = this.level().getGameTime();
        this.setDeltaMovement(Vec3.ZERO);
    }

    public void initializeReturnPortal(Player owner, float portalYaw, float shaderSeed, UUID entrancePortal) {
        this.ownerId = owner.getUUID();
        this.role = PortalRole.RETURN;
        this.keepAlive = true;
        this.acceptingPlayers = true;

        this.linkedPortalId = entrancePortal;

        this.setPortalYaw(portalYaw);
        this.setShaderSeed(shaderSeed);
        this.setOpened(false);
        this.setDespawning(false);
        this.setDespawnStartTime(-1L);

        this.lastActivityGameTime = this.level().getGameTime();
        this.setDeltaMovement(Vec3.ZERO);
    }

    public PortalRole getPortalRole() {
        return this.role;
    }


    public boolean isEntrancePortal() {
        return this.role == PortalRole.ENTRANCE;
    }


    public boolean isReturnPortal() {
        return this.role == PortalRole.RETURN;
    }


    @Nullable
    public UUID getLinkedPortalId() {
        return this.linkedPortalId;
    }


    public void setLinkedPortalId(@Nullable UUID linkedPortalId) {
        this.linkedPortalId = linkedPortalId;
    }


    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;

        if (keepAlive) {
            this.markActivity();
        }
    }


    public void releaseAfterUse() {
        this.keepAlive = false;
        this.acceptingPlayers = false;

        this.markActivity();
    }


    public boolean canAcceptPlayer(ServerPlayer player) {
        return this.acceptingPlayers && this.canEnter() && this.belongsTo(player.getUUID());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(PORTAL_YAW, 0.0F);
        this.entityData.define(SHADER_SEED, 0.0F);
        this.entityData.define(OPENED, false);
        this.entityData.define(DESPAWNING, false);
        this.entityData.define(DESPAWN_START_TIME, -1L);
    }


    public float getPortalYaw() {
        return this.entityData.get(PORTAL_YAW);
    }


    public void setPortalYaw(float yaw) {
        yaw = Mth.wrapDegrees(yaw);
        this.entityData.set(PORTAL_YAW, yaw);

        this.setYRot(yaw);
        this.setXRot(0.0F);
    }

    public float getShaderSeed() {
        return this.entityData.get(SHADER_SEED);
    }

    public void setShaderSeed(float seed) {
        this.entityData.set(SHADER_SEED, seed);
    }

    public boolean isOpened() {
        return this.entityData.get(OPENED);
    }

    private void setOpened(boolean opened) {
        this.entityData.set(OPENED, opened);
    }

    public boolean isDespawning() {
        return this.entityData.get(DESPAWNING);
    }

    private void setDespawning(boolean despawning) {
        this.entityData.set(DESPAWNING, despawning);
    }

    private long getDespawnStartTime() {
        return this.entityData.get(DESPAWN_START_TIME);
    }

    private void setDespawnStartTime(long time) {
        this.entityData.set(DESPAWN_START_TIME, time);
    }

    @Nullable
    public UUID getOwnerId() {
        return this.ownerId;
    }

    public boolean belongsTo(UUID playerId) {
        return this.ownerId != null && this.ownerId.equals(playerId);
    }

    public void markActivity() {
        if (this.level().isClientSide || this.isDespawning()) {
            return;
        }
        this.lastActivityGameTime = this.level().getGameTime();
    }

    public boolean canEnter() {
        return this.isOpened() && !this.isDespawning();
    }

    public float getOpenProgress(float partialTick) {
        if (this.isOpened()) {
            return 1.0F;
        }
        return Mth.clamp((this.tickCount + partialTick) / (float) OPEN_TIME_TICKS, 0.0F, 1.0F);
    }

    public float getDespawnProgress(float partialTick) {
        if (!this.isDespawning()) {
            return 0.0F;
        }
        long start = this.getDespawnStartTime();
        if (start < 0L) {
            return 0.0F;
        }

        double current = this.level().getGameTime() + partialTick;
        return Mth.clamp((float) ((current - start) / DESPAWN_TIME_TICKS), 0.0F, 1.0F);
    }

    private void beginDespawning() {
        if (this.level().isClientSide || this.isDespawning()) {
            return;
        }
        this.setDespawning(true);
        this.setDespawnStartTime(this.level().getGameTime());
    }

    private void checkPlayerCollision() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox(), player -> player.isAlive() && !player.isSpectator() && this.belongsTo(player.getUUID()))) {
            if (!this.canAcceptPlayer(player)) {
                continue;
            }
            if (ChaosPocketTransitionServer.begin(player, this)) {
                this.markActivity();
                break;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        this.noPhysics = true;
        this.setNoGravity(true);

        this.setDeltaMovement(Vec3.ZERO);
        this.setYRot(this.getPortalYaw());
        this.setXRot(0.0F);


        if (this.level().isClientSide) {
            return;
        }

        long now = this.level().getGameTime();

        if (!this.isOpened()) {

            if (this.tickCount >= OPEN_TIME_TICKS) {

                this.setOpened(true);
                this.lastActivityGameTime = now;
            }
            return;
        }

        if (this.isOpened()
                && !this.isDespawning()
                && this.acceptingPlayers) {

            this.checkPlayerCollision();
        }

        if (this.isDespawning()) {
            long despawnStart = this.getDespawnStartTime();
            if (despawnStart >= 0L && now - despawnStart >= DESPAWN_TIME_TICKS) {
                this.discard();
            }

            return;
        }

        if (this.lastActivityGameTime == Long.MIN_VALUE) {
            this.lastActivityGameTime = now;
        }

        if (this.keepAlive) {
            return;
        }

        if (now - this.lastActivityGameTime >= IDLE_TIMEOUT_TICKS) {
            this.beginDespawning();
        }
    }

    public static void discardOwnedReturnPortals(MinecraftServer server, UUID playerId) {
        java.util.List<ChaosPortalEntity> toRemove = new java.util.ArrayList<>();

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {

                if (!(entity instanceof ChaosPortalEntity portal)) {
                    continue;
                }

                if (!portal.isAlive()) {
                    continue;
                }

                if (!portal.isReturnPortal()) {
                    continue;
                }

                if (!portal.belongsTo(playerId)) {
                    continue;
                }

                toRemove.add(portal);
            }
        }


        for (ChaosPortalEntity portal : toRemove) {

            portal.discard();
        }
    }

    @Nullable
    public static ChaosPortalEntity findOwnedEntrancePortal(MinecraftServer server, UUID playerId) {
        for (ServerLevel level : server.getAllLevels()) {

            for (Entity entity : level.getAllEntities()) {

                if (!(entity instanceof ChaosPortalEntity portal)) {

                    continue;
                }

                if (!portal.isAlive()) {
                    continue;
                }

                if (!portal.isEntrancePortal()) {
                    continue;
                }

                if (!portal.belongsTo(playerId)) {
                    continue;
                }
                return portal;
            }
        }
        return null;
    }


    public static boolean hasOwnedEntrancePortal(MinecraftServer server, UUID playerId) {
        return findOwnedEntrancePortal(server, playerId) != null;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("PortalYaw", this.getPortalYaw());
        tag.putFloat("ShaderSeed", this.getShaderSeed());

        tag.putString("PortalRole", this.role.name());
        tag.putBoolean("KeepAlive", this.keepAlive);
        tag.putBoolean("AcceptingPlayers", this.acceptingPlayers);

        if (this.linkedPortalId != null) {
            tag.putUUID("LinkedPortal", this.linkedPortalId);
        }

        tag.putBoolean("Opened", this.isOpened());
        tag.putBoolean("Despawning", this.isDespawning());

        tag.putLong("DespawnStartTime", this.getDespawnStartTime());
        tag.putLong("LastActivityGameTime", this.lastActivityGameTime);

        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setPortalYaw(tag.getFloat("PortalYaw"));
        this.setShaderSeed(tag.getFloat("ShaderSeed"));
        this.setOpened(tag.getBoolean("Opened"));
        this.setDespawning(tag.getBoolean("Despawning"));
        this.setDespawnStartTime(tag.contains("DespawnStartTime") ? tag.getLong("DespawnStartTime") : -1L);

        try {
            this.role = PortalRole.valueOf(tag.getString("PortalRole"));
        } catch (Exception ignored) {
            this.role = PortalRole.ENTRANCE;
        }


        this.keepAlive = tag.getBoolean("KeepAlive");


        this.acceptingPlayers = !tag.contains("AcceptingPlayers") || tag.getBoolean("AcceptingPlayers");


        this.linkedPortalId = tag.hasUUID("LinkedPortal") ? tag.getUUID("LinkedPortal") : null;

        this.lastActivityGameTime = tag.contains("LastActivityGameTime") ? tag.getLong("LastActivityGameTime") : this.level().getGameTime();
        this.ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        this.noPhysics = true;

        this.setNoGravity(true);
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}