package com.benji.oasiso.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class DamageNumberEntity extends Entity {

    //life
    private static final int HOLD_TICKS = 8;
// ~1.6 sec
    private static final int LIFETIME = 32;

    private static final int FADE_START_TICK = 14;
    private static final double MIN_RISE_SPEED = 0.014D;
    private static final double MAX_RISE_SPEED = 0.042D;

    private static final EntityDataAccessor<Integer> DAMAGE_VALUE = SynchedEntityData.defineId(DamageNumberEntity.class, EntityDataSerializers.INT);

    public DamageNumberEntity(EntityType<? extends DamageNumberEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DAMAGE_VALUE, 0);
    }

    public void setDamageValue(int damage) {
        this.entityData.set(DAMAGE_VALUE, Math.max(0, damage));
    }

    public int getDamageValue() {
        return this.entityData.get(DAMAGE_VALUE);
    }

    @Override
    public void tick() {
        super.tick();
        this.noPhysics = true;
        this.setNoGravity(true);

        if (this.tickCount > HOLD_TICKS) {
            float riseProgress = Mth.clamp((this.tickCount - HOLD_TICKS) / (float) (LIFETIME - HOLD_TICKS), 0.0F, 1.0F);

            double riseSpeed = Mth.lerp(riseProgress, MIN_RISE_SPEED, MAX_RISE_SPEED);
            this.setPos(this.getX(), this.getY() + riseSpeed, this.getZ());
        }
        if (!this.level().isClientSide && this.tickCount >= LIFETIME) {
            this.discard();
        }
    }

    public float getRenderAlpha(float partialTick) {
        float age = this.tickCount + partialTick;
        if (age < 3.0F) {
            return Mth.clamp(age / 3.0F, 0.0F, 1.0F);
        }
        if (age <= FADE_START_TICK) {
            return 1.0F;
        }
        return Mth.clamp(1.0F - (age - FADE_START_TICK) / (LIFETIME - FADE_START_TICK), 0.0F, 1.0F);
    }


    public float getRenderScale(float partialTick) {
        float age = this.tickCount + partialTick;
        if (age < 4.0F) {
            return Mth.lerp(age / 4.0F, 0.70F, 1.0F);
        }
        return 1.0F;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        final double renderDistance = 128.0D;
        return distance < renderDistance * renderDistance;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("DamageValue", this.getDamageValue());
        tag.putInt("DamageNumberAge", this.tickCount);
    }


    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setDamageValue(tag.getInt("DamageValue"));
        this.tickCount = tag.getInt("DamageNumberAge");
        this.noPhysics = true;
        this.setNoGravity(true);
    }
}