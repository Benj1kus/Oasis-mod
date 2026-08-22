package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import com.benji.oasiso.common.util.DamageNumberSpawner;
import com.benji.oasiso.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

public class WizardPillarEntity extends Entity {

    private static final EntityDataAccessor<Integer> VISIBLE_HEIGHT = SynchedEntityData.defineId(WizardPillarEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> COLLAPSING = SynchedEntityData.defineId(WizardPillarEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(WizardPillarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FLASH_LEVEL = SynchedEntityData.defineId(WizardPillarEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> FLASH_MODE = SynchedEntityData.defineId(WizardPillarEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> FLASH_SERIAL = SynchedEntityData.defineId(WizardPillarEntity.class, EntityDataSerializers.INT);

    private static final int MAX_HEIGHT = 5;
    private static final int STEP_INTERVAL = 4;
    private static final int LIFETIME_AFTER_BUILD = 300;
    private static final int HEAL_INTERVAL = 20;
    private static final double HEAL_RANGE = 16.0D;

    private UUID ownerUuid;

    private int stepTimer;
    private int activeTicks;
    private int healTimer;

    public WizardPillarEntity(EntityType<? extends WizardPillarEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(VISIBLE_HEIGHT, 0);
        this.entityData.define(COLLAPSING, false);
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(FLASH_LEVEL, -1);
        this.entityData.define(FLASH_MODE, 0);
        this.entityData.define(FLASH_SERIAL, 0);
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    public int getVisibleHeight() {
        return this.entityData.get(VISIBLE_HEIGHT);
    }

    private void setVisibleHeight(int height) {
        this.entityData.set(VISIBLE_HEIGHT, height);
    }

    public boolean isCollapsing() {
        return this.entityData.get(COLLAPSING);
    }

    private void setCollapsing(boolean collapsing) {
        this.entityData.set(COLLAPSING, collapsing);
    }

    public int getHealTargetId() {
        return this.entityData.get(TARGET_ID);
    }

    private void setHealTargetId(int id) {
        this.entityData.set(TARGET_ID, id);
    }

    public int getFlashLevel() {
        return this.entityData.get(FLASH_LEVEL);
    }

    public int getFlashMode() {
        return this.entityData.get(FLASH_MODE);
    }

    public int getFlashSerial() {
        return this.entityData.get(FLASH_SERIAL);
    }

    private void triggerFlash(int level, int mode) {
        this.entityData.set(FLASH_LEVEL, level);
        this.entityData.set(FLASH_MODE, mode);
        this.entityData.set(FLASH_SERIAL, this.entityData.get(FLASH_SERIAL) + 1);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        if (!this.isCollapsing() && shouldCollapseBecauseOwnerIsGone(level)) {
            startCollapse();
        }
        if (!this.isCollapsing()) {
            tickBuildOrActive(level);
        } else {
            tickCollapse(level);
        }
    }

    private void tickBuildOrActive(ServerLevel level) {
        if (this.getVisibleHeight() == 0) {
            placeNextSegment(level, false);
            return;
        }
        if (this.getVisibleHeight() < MAX_HEIGHT) {
            this.stepTimer++;
            if (this.stepTimer >= STEP_INTERVAL) {
                this.stepTimer = 0;
                placeNextSegment(level, true);
            }
            return;
        }
        this.activeTicks++;
        if (this.activeTicks >= LIFETIME_AFTER_BUILD) {
            startCollapse();
            return;
        }
        tickHealing(level);
    }

    private void tickCollapse(ServerLevel level) {
        this.stepTimer++;

        if (this.stepTimer < STEP_INTERVAL) {
            return;
        }
        this.stepTimer = 0;
        if (this.getVisibleHeight() <= 0) {
            this.discard();
            return;
        }
        removeTopSegment(level, true);
        if (this.getVisibleHeight() <= 0) {
            this.discard();
        }
    }

    private void placeNextSegment(ServerLevel level, boolean flashy) {
        int newHeight = this.getVisibleHeight() + 1;
        BlockPos pos = basePos().above(newHeight - 1);
        BlockState state = newHeight == MAX_HEIGHT ? Oasiso.WIZARD_EYE.get().defaultBlockState()

                : Oasiso.WIZARD_COLUMN.get().defaultBlockState();
        boolean placed = level.setBlock(pos, state, 3);

        if (!placed) {
            return;
        }
        this.setVisibleHeight(newHeight);


        //build sound
        level.playSound(null, pos, ModSounds.TOWER_PLACE.get(), SoundSource.BLOCKS, 0.9F,

                0.94F + this.random.nextFloat() * 0.12F);


        if (flashy) {

            spawnAssemblyParticles(
                    level,
                    pos,
                    false
            );

            triggerFlash(
                    newHeight,
                    1
            );
        }
    }

    private void removeTopSegment(
            ServerLevel level,
            boolean flashy
    ) {
        int currentHeight =
                this.getVisibleHeight();

        if (currentHeight <= 0) {
            return;
        }

        BlockPos pos =
                basePos().above(currentHeight - 1);

        if (flashy) {
            spawnAssemblyParticles(level, pos, true);
            triggerFlash(currentHeight, 2);
        }

        if (isWizardBlock(level.getBlockState(pos))) {
            level.removeBlock(pos, false);
        }

        this.setVisibleHeight(currentHeight - 1);

        if (this.getVisibleHeight() < MAX_HEIGHT) {
            this.setHealTargetId(-1);
        }
    }

    private void tickHealing(ServerLevel level) {
        LivingEntity target = findNearestHealTarget(level);
        if (target == null) {
            this.setHealTargetId(-1);
            return;
        }
        this.setHealTargetId(target.getId());
        this.healTimer++;
        if (this.healTimer >= HEAL_INTERVAL) {
            this.healTimer = 0;
            float healthBefore = target.getHealth();
            target.heal(2.0F);
            float healedAmount = target.getHealth() - healthBefore;
            //damagenummber
            if (healedAmount > 0.0F) {
                DamageNumberSpawner.spawn(level, target, healedAmount);
            }
            if (healedAmount > 0.0F) {
                level.sendParticles(Oasiso.PURPLE_STARS.get(),

                        target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ(),

                        6,

                        0.35D, 0.4D, 0.35D,

                        0.01D);

                level.sendParticles(Oasiso.WIZARD_PIXELS.get(),

                        target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ(),

                        10,

                        0.45D, 0.55D, 0.45D,

                        0.01D);
            }
        }
    }

    private LivingEntity findNearestHealTarget(ServerLevel level) {
        AABB box = this.getBoundingBox().inflate(HEAL_RANGE, 8.0D, HEAL_RANGE);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box, this::canHeal);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;

        for (LivingEntity entity : entities) {
            double distance = this.distanceToSqr(entity);

            if (distance < bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }
        return best;
    }

    private boolean canHeal(LivingEntity entity) {
        if (!entity.isAlive()) {
            return false;
        }
        if (entity instanceof Player) {
            return false;
        }
        if (!(entity instanceof Mob)) {
            return false;
        }
        if (this.ownerUuid != null && entity.getUUID().equals(this.ownerUuid)) {

            return false;
        }
        return true;
    }


    private boolean shouldCollapseBecauseOwnerIsGone(ServerLevel level) {
        if (this.ownerUuid == null) {
            return true;
        }
        Entity owner = level.getEntity(this.ownerUuid);
        return owner == null || !owner.isAlive();
    }

    private void startCollapse() {
        if (this.isCollapsing()) {
            return;
        }
        this.setCollapsing(true);
        this.stepTimer = 0;
        this.setHealTargetId(-1);
    }

    private void spawnAssemblyParticles(ServerLevel level, BlockPos pos, boolean reverse) {
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        level.sendParticles(Oasiso.PURPLE_STARS.get(), centerX, centerY, centerZ, 12, 0.35D, 0.35D, 0.35D, 0.02D);
        level.sendParticles(Oasiso.WIZARD_PIXELS.get(), centerX, centerY, centerZ, 18, 0.35D, 0.35D, 0.35D, reverse ? 0.04D : 0.02D);
    }
    private boolean isWizardBlock(BlockState state) {
        return state.is(Oasiso.WIZARD_COLUMN.get()) || state.is(Oasiso.WIZARD_EYE.get());
    }

    private BlockPos basePos() {
        return this.blockPosition();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide) {
            clearRemainingBlocks();
        }

        super.remove(reason);
    }

    private void clearRemainingBlocks() {
        for (int i = 0; i < MAX_HEIGHT; i++) {
            BlockPos pos = basePos().above(i);
            BlockState state = this.level().getBlockState(pos);

            if (isWizardBlock(state)) {
                this.level().removeBlock(pos, false);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;

        this.setVisibleHeight(tag.getInt("VisibleHeight"));
        this.setCollapsing(tag.getBoolean("Collapsing"));
        this.activeTicks = tag.getInt("ActiveTicks");
        this.stepTimer = tag.getInt("StepTimer");
        this.healTimer = tag.getInt("HealTimer");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }

        tag.putInt("VisibleHeight", this.getVisibleHeight());
        tag.putBoolean("Collapsing", this.isCollapsing());
        tag.putInt("ActiveTicks", this.activeTicks);
        tag.putInt("StepTimer", this.stepTimer);
        tag.putInt("HealTimer", this.healTimer);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }
}