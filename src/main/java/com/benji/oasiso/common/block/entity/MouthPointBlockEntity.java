package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.common.waypoint.AzumalitWaypointManager;
import com.benji.oasiso.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class MouthPointBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final String CONTROLLER_NAME = "controller";
    private static final String SPAWN_TRIGGER = "spawn";

    private static final String TAG_OWNER = "Owner";
    private static final String TAG_VARIANT = "Variant";
    private static final String TAG_PARTNER_DIMENSION = "PartnerDimension";
    private static final String TAG_PARTNER_POS = "PartnerPos";
    private static final String TAG_SPAWN_UNTIL = "SpawnUntil";

    private static final int SPAWN_ANIMATION_TICKS = 49;

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SPAWN_ANIMATION = RawAnimation.begin().thenPlay("spawn");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private UUID ownerId;

    private int variant = 1;

    @Nullable
    private String partnerDimension;

    private long partnerPos;
    private long spawnUntilGameTime;

    private boolean suppressRemovalCallback;
    private long lastSmokeEmissionTick = Long.MIN_VALUE;

    public MouthPointBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOUTH_POINT_BE.get(), pos, state);
    }

    public void configure(UUID ownerId, int variant) {
        this.ownerId = ownerId;
        this.variant = variant == 2 ? 2 : 1;
        this.clearPartnerInternal();
        this.sync();
    }

    public void startSpawnAnimation() {
        if (this.level == null) {
            return;
        }

        this.spawnUntilGameTime = this.level.getGameTime() + SPAWN_ANIMATION_TICKS;
        this.sync();

        if (!this.level.isClientSide) {
            this.triggerAnim(CONTROLLER_NAME, SPAWN_TRIGGER);
        }
    }

    public boolean isSpawnAnimationActive() {
        return this.level != null && this.level.getGameTime() < this.spawnUntilGameTime;
    }

    public int getVariant() {
        return this.variant;
    }

    public void setVariant(int variant) {
        int normalized = variant == 2 ? 2 : 1;

        if (this.variant == normalized) {
            return;
        }

        this.variant = normalized;
        this.sync();
    }

    @Nullable
    public UUID getOwnerId() {
        return this.ownerId;
    }

    public boolean isOwnedBy(UUID playerId) {
        return this.ownerId != null && this.ownerId.equals(playerId);
    }

    public boolean hasPartner() {
        return this.partnerDimension != null && !this.partnerDimension.isBlank();
    }

    public void setPartner(AzumalitWaypointManager.WaypointRef partner) {
        this.partnerDimension = partner.dimension().location().toString();
        this.partnerPos = partner.pos().asLong();
        this.sync();
    }

    public void clearPartner() {
        if (!this.hasPartner()) {
            return;
        }

        this.clearPartnerInternal();
        this.sync();
    }

    private void clearPartnerInternal() {
        this.partnerDimension = null;
        this.partnerPos = 0L;
    }

    @Nullable
    public AzumalitWaypointManager.WaypointRef getPartnerRef() {
        if (!this.hasPartner()) {
            return null;
        }

        return AzumalitWaypointManager.createRef(this.partnerDimension, BlockPos.of(this.partnerPos));
    }

    public void spawnBlockedClickEffects(ServerLevel level) {
        double x = this.worldPosition.getX() + 0.5D;
        double y = this.worldPosition.getY() + 1.55D;
        double z = this.worldPosition.getZ() + 0.5D;

        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 14, 0.22D, 0.28D, 0.22D, 0.02D);
        level.sendParticles(ParticleTypes.SMOKE, x, y, z, 18, 0.18D, 0.30D, 0.18D, 0.015D);
    }

    public boolean tryMarkSmokeEmission(long gameTime) {
        if (gameTime == this.lastSmokeEmissionTick || gameTime % 2L != 0L) {
            return false;
        }

        this.lastSmokeEmissionTick = gameTime;
        return true;
    }

    public void suppressRemovalCallback() {
        this.suppressRemovalCallback = true;
    }

    public boolean isRemovalCallbackSuppressed() {
        return this.suppressRemovalCallback;
    }

    private void sync() {
        this.setChanged();

        if (this.level != null && !this.level.isClientSide) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        if (this.ownerId != null) {
            tag.putUUID(TAG_OWNER, this.ownerId);
        }

        tag.putInt(TAG_VARIANT, this.variant);
        tag.putLong(TAG_SPAWN_UNTIL, this.spawnUntilGameTime);

        if (this.hasPartner()) {
            tag.putString(TAG_PARTNER_DIMENSION, this.partnerDimension);
            tag.putLong(TAG_PARTNER_POS, this.partnerPos);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        this.ownerId = tag.hasUUID(TAG_OWNER) ? tag.getUUID(TAG_OWNER) : null;
        this.variant = tag.getInt(TAG_VARIANT) == 2 ? 2 : 1;
        this.spawnUntilGameTime = tag.getLong(TAG_SPAWN_UNTIL);

        if (tag.contains(TAG_PARTNER_DIMENSION) && tag.contains(TAG_PARTNER_POS)) {
            this.partnerDimension = tag.getString(TAG_PARTNER_DIMENSION);
            this.partnerPos = tag.getLong(TAG_PARTNER_POS);
        } else {
            this.clearPartnerInternal();
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<MouthPointBlockEntity> controller = new AnimationController<>(this, CONTROLLER_NAME, 0, state -> state.setAndContinue(this.isSpawnAnimationActive() ? SPAWN_ANIMATION : IDLE_ANIMATION));

        controller.triggerableAnim(SPAWN_TRIGGER, SPAWN_ANIMATION);
        controllers.add(controller);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
