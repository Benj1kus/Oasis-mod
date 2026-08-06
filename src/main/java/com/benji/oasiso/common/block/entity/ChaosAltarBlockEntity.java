package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ChaosAltarBlockEntity
        extends BlockEntity
        implements GeoBlockEntity {

    private static final String CONTROLLER_NAME =
            "controller";

    private static final String CHAOS_TRIGGER =
            "chaos_sphere";

    private static final String DOMINANCE_TRIGGER =
            "dominance_sphere";


    private static final int ANIMATION_DURATION =
            65;


    private static final int SAND_PARTICLE_TICK =
            30;

    private static final RawAnimation IDLE_ANIMATION =
            RawAnimation.begin()
                    .thenLoop("idle");

    private static final RawAnimation CHAOS_ANIMATION =
            RawAnimation.begin()
                    .thenPlay("chaos_sphere");

    private static final RawAnimation DOMINANCE_ANIMATION =
            RawAnimation.begin()
                    .thenPlay("dominance_sphere");

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);


    private int animationTick = -1;

    private ActivationType currentActivation;

    public ChaosAltarBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                Oasiso.CHAOS_ALTAR_BE.get(),
                pos,
                state
        );
    }

    public boolean activate(
            ActivationType activationType
    ) {
        if (this.level == null
                || this.level.isClientSide
                || this.animationTick >= 0
                || !(this.level
                instanceof ServerLevel serverLevel)) {
            return false;
        }

        this.currentActivation =
                activationType;

        this.animationTick = 0;


        this.triggerAnim(
                CONTROLLER_NAME,
                activationType.triggerName
        );

        spawnActivationEffects(
                serverLevel,
                activationType
        );

        return true;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            ChaosAltarBlockEntity altar
    ) {
        if (!(level
                instanceof ServerLevel serverLevel)
                || altar.animationTick < 0) {
            return;
        }

        altar.animationTick++;

        if (altar.animationTick
                == SAND_PARTICLE_TICK) {

            altar.spawnSandParticles(
                    serverLevel
            );
        }

        if (altar.animationTick
                >= ANIMATION_DURATION) {

            altar.animationTick = -1;
            altar.currentActivation = null;


            if (serverLevel.getBlockState(pos)
                    .is(Oasiso.CHAOS_ALTAR.get())) {

                serverLevel.removeBlock(
                        pos,
                        false
                );
            }
        }
    }

    private void spawnActivationEffects(
            ServerLevel level,
            ActivationType activationType
    ) {
        double centerX =
                this.worldPosition.getX() + 0.5D;

        double centerY =
                this.worldPosition.getY() + 0.75D;

        double centerZ =
                this.worldPosition.getZ() + 0.5D;

        ParticleOptions particle =
                activationType == ActivationType.CHAOS
                        ? Oasiso.PURPLE_STARS.get()
                        : Oasiso.GOLDEN_STARS.get();

        level.sendParticles(
                particle,
                centerX,
                centerY,
                centerZ,
                45,
                0.5D,
                0.55D,
                0.5D,
                0.07D
        );

        level.playSound(
                null,
                this.worldPosition,
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS,
                1.25F,
                activationType == ActivationType.CHAOS
                        ? 0.75F
                        : 1.15F
        );

        level.playSound(
                null,
                this.worldPosition,
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.BLOCKS,
                0.8F,
                activationType == ActivationType.CHAOS
                        ? 0.65F
                        : 1.3F
        );
    }

    private void spawnSandParticles(
            ServerLevel level
    ) {
        BlockParticleOption sandParticle =
                new BlockParticleOption(
                        ParticleTypes.BLOCK,
                        Blocks.SAND.defaultBlockState()
                );

        double centerX =
                this.worldPosition.getX() + 0.5D;

        double centerY =
                this.worldPosition.getY() + 0.12D;

        double centerZ =
                this.worldPosition.getZ() + 0.5D;

        level.sendParticles(
                sandParticle,
                centerX,
                centerY,
                centerZ,
                65,
                0.72D,
                0.12D,
                0.72D,
                0.12D
        );

        level.playSound(
                null,
                this.worldPosition,
                SoundEvents.SAND_BREAK,
                SoundSource.BLOCKS,
                1.15F,
                0.7F
                        + level.random.nextFloat()
                        * 0.15F
        );
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers
    ) {
        AnimationController<ChaosAltarBlockEntity>
                controller =
                new AnimationController<>(
                        this,
                        CONTROLLER_NAME,
                        0,
                        state ->
                                state.setAndContinue(
                                        IDLE_ANIMATION
                                )
                );

        controller.triggerableAnim(
                CHAOS_TRIGGER,
                CHAOS_ANIMATION
        );

        controller.triggerableAnim(
                DOMINANCE_TRIGGER,
                DOMINANCE_ANIMATION
        );

        controllers.add(controller);
    }

    @Override
    public AnimatableInstanceCache
    getAnimatableInstanceCache() {
        return this.cache;
    }

    public enum ActivationType {

        CHAOS(CHAOS_TRIGGER),
        DOMINANCE(DOMINANCE_TRIGGER);

        private final String triggerName;

        ActivationType(
                String triggerName
        ) {
            this.triggerName = triggerName;
        }
    }
}