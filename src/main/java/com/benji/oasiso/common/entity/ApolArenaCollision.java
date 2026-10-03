package com.benji.oasiso.common.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

public final class ApolArenaCollision extends Entity {
    public static EntityType<ApolArenaCollision> TYPE;
    private static final EntityDataAccessor<Integer> PARENT = SynchedEntityData.defineId(ApolArenaCollision.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ROW = SynchedEntityData.defineId(ApolArenaCollision.class, EntityDataSerializers.INT);
    private ApolSummoningEntity owner;

    public ApolArenaCollision(EntityType<? extends ApolArenaCollision> type, Level level) {
        super(type, level);
        setNoGravity(true);
        blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(PARENT, -1);
        entityData.define(ROW, 0);
    }

    public void attach(ApolSummoningEntity parent, int row) {
        owner = parent;
        entityData.set(PARENT, parent.getId());
        entityData.set(ROW, row);
        sync();
    }

    public void sync() {
        if (owner == null || owner.isRemoved()) {
            Entity e = level().getEntity(entityData.get(PARENT));
            owner = e instanceof ApolSummoningEntity ritual ? ritual : null;
        }
        if (owner == null) return;
        int row = entityData.get(ROW), first = ApolArenaShape.first(row), last = ApolArenaShape.last(row);
        if (first > last) {
            setPos(owner.getX(), owner.getY(), owner.getZ());
            setBoundingBox(new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()));
            return;
        }
        double x0 = owner.origin().getX() - 3 + first, z0 = owner.origin().getZ() - 3 + row, y = owner.getY();
        setPos(x0 + (last - first + 1) * .5, y, z0 + .5);
        setBoundingBox(new AABB(x0, y, z0, x0 + last - first + 1, y + 8, z0 + 1));
    }

    @Override
    public void tick() {
        super.tick();
        sync();
        if (!level().isClientSide && (owner == null || owner.isRemoved() || owner.settled())) discard();
    }

    @Override
    public boolean canBeCollidedWith() {
        int row = entityData.get(ROW);
        return !isRemoved() && owner != null && !owner.settled() && row >= 0 && row < ApolArenaShape.SIZE;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return false;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {
        sync();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }
}
