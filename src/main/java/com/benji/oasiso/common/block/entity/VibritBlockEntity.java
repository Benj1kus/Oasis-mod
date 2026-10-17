package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class VibritBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation VIBR = RawAnimation.begin().thenPlay("vibr");
    private int vibrationDelay = -1;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public VibritBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VIBRIT_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VibritBlockEntity entity) {
        if (entity.vibrationDelay < 0) entity.vibrationDelay = 200 + level.random.nextInt(301);
        if (--entity.vibrationDelay <= 0) {
            entity.triggerAnim("idle", "vibr");

            SoundEvent randomSpringSound = switch (level.random.nextInt(3)) {
                case 1 -> ModSounds.SPRING2.get();
                case 2 -> ModSounds.SPRING3.get();
                default -> ModSounds.SPRING1.get();
            };

            level.playSound(null, pos, randomSpringSound, SoundSource.BLOCKS, 1.0F, 0.8F + level.random.nextFloat() * 0.4F // Небольшой рандом тональности
            );

            if (level instanceof net.minecraft.server.level.ServerLevel server) {
                com.benji.oasiso.common.soul.VibritSoulLogic.release(server, pos, state);
            }
            entity.vibrationDelay = 200 + level.random.nextInt(301);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<VibritBlockEntity>(this, "idle", 0, state -> state.setAndContinue(IDLE)).triggerableAnim("vibr", VIBR));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), worldPosition.getX() + 1, worldPosition.getY() + 6.875, worldPosition.getZ() + 1);
    }
}
