package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.common.block.CrusaderSkelBlock;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import com.benji.oasiso.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class CrusaderSkelBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation[] POSES = {RawAnimation.begin().thenLoop("pile1"), RawAnimation.begin().thenLoop("pile2"), RawAnimation.begin().thenLoop("pile3"), RawAnimation.begin().thenLoop("pile4")};
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CrusaderSkelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUSADER_SKEL_BE.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "pile", 0, event -> event.setAndContinue(POSES[getBlockState().getValue(CrusaderSkelBlock.PILE) - 1])));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(2.5);
    }
}