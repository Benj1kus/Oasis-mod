package com.benji.oasiso.common.block.entity;
import com.benji.oasiso.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
public final class FactoryCoreBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation IDLE=RawAnimation.begin().thenLoop("idle");
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public FactoryCoreBlockEntity(BlockPos pos,BlockState state) { super(ModBlockEntities.FACTORY_CORE_BE.get(),pos,state); }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<FactoryCoreBlockEntity>(this,"idle",0,state->state.setAndContinue(IDLE)));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    public long nextLightning;
    // Include outgoing connections so turning away from the core does not cull their source.
    @Override public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX()-32,worldPosition.getY()-30,worldPosition.getZ()-32,
            worldPosition.getX()+33,worldPosition.getY()+36,worldPosition.getZ()+33);
    }
}
