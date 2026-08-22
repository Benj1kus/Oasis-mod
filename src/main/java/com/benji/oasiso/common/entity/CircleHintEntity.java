package com.benji.oasiso.common.entity;

import com.benji.oasiso.Oasiso;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class CircleHintEntity extends Monster implements GeoEntity, GlowmaskEntity {

    private static final int MODE_TRACKING = 0;
    private static final int MODE_FIXED = 1;
    private static final int WARNING_DURATION = 20 * 3;
    private static final double FLOOR_OFFSET = 0.025D;
    private static final int GRID_SIZE = 2;
    private static final double GRID_SPACING = 1.75D;
    private static final double BOMB_HEIGHT = 8.0D;

    private static final String OWNER_TAG = "CircleOwner";
    private static final String TARGET_TAG = "CircleTarget";
    private static final String MODE_TAG = "CircleMode";
    private static final String WARNING_TICKS_TAG = "CircleWarningTicks";
    private static final String REMAINING_SEQUENCE_TAG = "CircleRemainingSequence";
    private static final String FIXED_X_TAG = "CircleFixedX";
    private static final String FIXED_Z_TAG = "CircleFixedZ";

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private UUID ownerId;
    private UUID targetId;

    private int mode = MODE_TRACKING;
    private int warningTicks = WARNING_DURATION;
    private int remainingSequence = 1;

    private double fixedX;
    private double fixedZ;

    public CircleHintEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setupHintPhysics();
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1000.0D).add(Attributes.MOVEMENT_SPEED, 0.0D).add(Attributes.KNOCKBACK_RESISTANCE, 1.0D).add(Attributes.ATTACK_DAMAGE, 0.0D).add(Attributes.FOLLOW_RANGE, 128.0D);
    }


    @Override
    protected void registerGoals() {
    }
    public void startTrackingSequence(AzumaalEntity owner, ServerPlayer target, int remainingSequence) {
        this.ownerId = owner.getUUID();
        this.targetId = target.getUUID();
        this.mode = MODE_TRACKING;
        this.warningTicks = WARNING_DURATION;
        this.remainingSequence = Math.max(1, remainingSequence);

        setupHintPhysics();
        if (this.level() instanceof ServerLevel serverLevel) {
            followTargetOnGround(serverLevel, target);
        }
    }

    public void startFixed(AzumaalEntity owner, double x, double z) {
        this.ownerId = owner.getUUID();
        this.targetId = null;
        this.mode = MODE_FIXED;
        this.warningTicks = WARNING_DURATION;
        this.remainingSequence = 1;
        this.fixedX = x;
        this.fixedZ = z;

        setupHintPhysics();
        if (this.level() instanceof ServerLevel serverLevel) {
            moveToFixedGround(serverLevel);
        }
    }

    @Override
    public void tick() {
        super.tick();

        setupHintPhysics();
        this.setDeltaMovement(Vec3.ZERO);
        this.fallDistance = 0.0F;
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        AzumaalEntity owner = resolveOwner(serverLevel);
        if (owner == null) {
            this.discard();
            return;
        }
        ServerPlayer target = null;
        if (this.mode == MODE_TRACKING) {
            target = resolveTarget(serverLevel);
            if (target == null) {
                this.discard();
                return;
            }
            followTargetOnGround(serverLevel, target);
        } else {
            moveToFixedGround(serverLevel);
        }
        this.warningTicks--;
        if (this.warningTicks > 0) {
            return;
        }
        spawnBombGrid(serverLevel);
        if (this.mode == MODE_TRACKING && this.remainingSequence > 1 && target != null) {
            spawnNextTrackingCircle(serverLevel, owner, target);
        }
        this.discard();
    }

    private void followTargetOnGround(ServerLevel level, ServerPlayer target) {
        double x = target.getX();
        double z = target.getZ();

        double floorY = getGroundY(level, x, z);
        this.setPos(x, floorY + FLOOR_OFFSET, z);
    }


    private void moveToFixedGround(ServerLevel level) {
        double floorY = getGroundY(level, this.fixedX, this.fixedZ);
        this.setPos(this.fixedX, floorY + FLOOR_OFFSET, this.fixedZ);
    }

    private double getGroundY(ServerLevel level, double x, double z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
    }

    private void spawnNextTrackingCircle(ServerLevel level, AzumaalEntity owner, ServerPlayer target) {
        CircleHintEntity next = Oasiso.CIRCLE_HINT.get().create(level);
        if (next == null) {
            return;
        }
        next.moveTo(target.getX(), target.getY(), target.getZ(), 0.0F, 0.0F);
        next.startTrackingSequence(owner, target, this.remainingSequence - 1);
        level.addFreshEntity(next);
    }

    private void spawnBombGrid(ServerLevel level) {
        double halfGrid = (GRID_SIZE - 1) * GRID_SPACING * 0.5D;
        for (int gridX = 0; gridX < GRID_SIZE; gridX++) {
            for (int gridZ = 0; gridZ < GRID_SIZE; gridZ++) {

                double offsetX = gridX * GRID_SPACING - halfGrid;
                double offsetZ = gridZ * GRID_SPACING - halfGrid;

                double bombX = this.getX() + offsetX;
                double bombZ = this.getZ() + offsetZ;
                double bombY = getGroundY(level, bombX, bombZ) + BOMB_HEIGHT;

                spawnBomb(level, bombX, bombY, bombZ);
            }
        }
    }

    private void spawnBomb(ServerLevel level, double x, double y, double z) {
        ChaosBombEntity bomb = Oasiso.CHAOS_BOMB.get().create(level);
        if (bomb == null) {
            return;
        }
        AzumaalEntity owner = resolveOwner(level);
        if (owner != null) {
            bomb.setAzumaalOwner(owner);
        }
        bomb.moveTo(x, y, z, this.random.nextFloat() * 360.0F, 0.0F);
        bomb.setDeltaMovement(0.0D, -0.08D, 0.0D);
        level.addFreshEntity(bomb);
        level.sendParticles(Oasiso.PURPLE_STARS.get(), x, y + 0.35D, z, 5, 0.25D, 0.30D, 0.25D, 0.05D);
    }

    private AzumaalEntity resolveOwner(ServerLevel level) {
        if (this.ownerId == null) {
            return null;
        }
        Entity entity = level.getEntity(this.ownerId);
        if (!(entity instanceof AzumaalEntity owner) || !owner.isAlive()) {
            return null;
        }
        return owner;
    }

    private ServerPlayer resolveTarget(ServerLevel level) {
        if (this.targetId == null) {
            return null;
        }
        Entity entity = level.getEntity(this.targetId);
        if (!(entity instanceof ServerPlayer target) || !target.isAlive() || target.isSpectator() || target.isCreative()) {
            return null;
        }
        return target;
    }

    private void setupHintPhysics() {
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.noPhysics = true;
    }


    @Override
    public boolean isPushable() {
        return false;
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
        if (this.ownerId != null) {
            tag.putUUID(OWNER_TAG, this.ownerId);
        }
        if (this.targetId != null) {
            tag.putUUID(TARGET_TAG, this.targetId);
        }
        tag.putInt(MODE_TAG, this.mode);
        tag.putInt(WARNING_TICKS_TAG, this.warningTicks);
        tag.putInt(REMAINING_SEQUENCE_TAG, this.remainingSequence);

        tag.putDouble(FIXED_X_TAG, this.fixedX);
        tag.putDouble(FIXED_Z_TAG, this.fixedZ);
    }


    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        this.ownerId = tag.hasUUID(OWNER_TAG) ? tag.getUUID(OWNER_TAG) : null;
        this.targetId = tag.hasUUID(TARGET_TAG) ? tag.getUUID(TARGET_TAG) : null;

        this.mode = tag.contains(MODE_TAG) ? tag.getInt(MODE_TAG) : MODE_TRACKING;
        this.warningTicks = tag.contains(WARNING_TICKS_TAG) ? tag.getInt(WARNING_TICKS_TAG) : WARNING_DURATION;
        this.remainingSequence = tag.contains(REMAINING_SEQUENCE_TAG) ? tag.getInt(REMAINING_SEQUENCE_TAG) : 1;

        this.fixedX = tag.getDouble(FIXED_X_TAG);
        this.fixedZ = tag.getDouble(FIXED_Z_TAG);

        setupHintPhysics();
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> state.setAndContinue(IDLE_ANIMATION)));
    }


    @Override
    public ResourceLocation getGlowmaskTexture() {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/" + "circle_hint.png");
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}