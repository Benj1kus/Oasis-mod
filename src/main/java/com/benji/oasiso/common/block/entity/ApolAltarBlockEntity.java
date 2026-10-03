package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ApolAltarBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ACTIVATE = RawAnimation.begin().thenPlay("activate");
    private static final RawAnimation LOOP = RawAnimation.begin().thenLoop("activate_loop");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private long activationTime = -1;

    public ApolAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.APOL_ALTAR_BE.get(), pos, state);
    }

    public boolean isActivated() {
        return activationTime >= 0;
    }

    public void activate() {
        if (level == null || level.isClientSide || isActivated()) return;
        activationTime = level.getGameTime();
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            long age = level == null || activationTime < 0 ? -1 : level.getGameTime() - activationTime;
            return state.setAndContinue(age < 0 ? IDLE : age < 20 ? ACTIVATE : LOOP);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("ApolActivation", activationTime);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        activationTime = tag.contains("ApolActivation") ? tag.getLong("ApolActivation") : -1;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
