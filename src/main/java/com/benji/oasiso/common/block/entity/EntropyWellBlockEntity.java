package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.registry.EntropyWellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class EntropyWellBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation ON = RawAnimation.begin().thenLoop("on");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public boolean punchPending;
    public long lastPunch = Long.MIN_VALUE, nextBubble;

    public EntropyWellBlockEntity(BlockPos pos, BlockState state) {
        super(EntropyWellRegistry.WELL.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<EntropyWellBlockEntity>(this, "well", 0, s -> s.setAndContinue(ON)).setCustomInstructionKeyframeHandler(e -> {
            if (level != null && level.isClientSide && e.getKeyframeData().getInstructions().contains("ent_punch"))
                punchPending = true;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - .5, worldPosition.getY() - 1, worldPosition.getZ() - .5, worldPosition.getX() + 1.5, worldPosition.getY() + 7, worldPosition.getZ() + 1.5);
    }
}
