package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.SandedChestBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SandedChestBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private boolean isBrushing = false;
    private int brushingTicks = 0;

    public SandedChestBlockEntity(BlockPos pos, BlockState state) {
        super(Oasiso.SANDED_CHEST_BE.get(), pos, state);
    }

    public void startBrushing() {
        this.isBrushing = true;
        this.brushingTicks = 0;
    }

    public boolean isBrushing() {
        return isBrushing;
    }

    // Этот метод вызывается каждый тик сервером
    public static void tick(Level level, BlockPos pos, BlockState state, SandedChestBlockEntity entity) {
        if (!entity.isBrushing) return;

        entity.brushingTicks++;

        if (level instanceof ServerLevel serverLevel) {
            // Партиклы песка каждый тик
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                    pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                    3, 0.3, 0.2, 0.3, 0.05);

            // Звук очистки кистью каждые 10 тиков (полсекунды)
            if (entity.brushingTicks % 10 == 0) {
                level.playSound(null, pos, SoundEvents.BRUSH_SAND, SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            // На 40-й тик (2 секунды) превращаем в сундук
            if (entity.brushingTicks >= 40) {
                Direction facing = state.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);

                // Ставим ванильный сундук с тем же направлением
                level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 3);

                // Присваиваем лут-таблицу новому сундуку
                BlockEntity newBe = level.getBlockEntity(pos);
                if (newBe instanceof ChestBlockEntity chestBe) {
                    chestBe.setLootTable(ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "chests/sanded_chest_loot"), level.random.nextLong());
                }

                // Звук завершения
                level.playSound(null, pos, SoundEvents.BRUSH_SAND_COMPLETED, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        // Ловим сигнал от блока на клиенте и запускаем анимацию
        if (id == 1) {
            this.triggerAnim("controller", "try");
            return true;
        }
        return super.triggerEvent(id, type);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> PlayState.STOP)
                // Регистрируем анимацию, которая будет запускаться по триггеру
                .triggerableAnim("try", RawAnimation.begin().thenPlay("try"))
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}