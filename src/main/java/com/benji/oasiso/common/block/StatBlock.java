package com.benji.oasiso.common.block;

import com.benji.oasiso.common.block.entity.StatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class StatBlock extends GenDecorateBlock implements EntityBlock {

    public StatBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StatBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof StatBlockEntity blockEntity) {
            ItemStack heldItem = player.getItemInHand(hand);
            ItemStack storedItem = blockEntity.getStoredItem();

            // Если кликаем пустой рукой и на подставке есть предмет — забираем
            if (heldItem.isEmpty() && !storedItem.isEmpty()) {
                if (!level.isClientSide) {
                    player.setItemInHand(hand, storedItem.copy());
                    blockEntity.setStoredItem(ItemStack.EMPTY);
                    spawnEffects(level, pos);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            // Если кликаем предметом, а подставка пустая — кладем ОДИН предмет
            if (!heldItem.isEmpty() && storedItem.isEmpty()) {
                if (!level.isClientSide) {
                    ItemStack toStore = heldItem.copy();
                    toStore.setCount(1); // Берем только 1 штуку
                    blockEntity.setStoredItem(toStore);

                    if (!player.isCreative()) {
                        heldItem.shrink(1);
                    }
                    spawnEffects(level, pos);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    private void spawnEffects(Level level, BlockPos pos) {
        // Звук "поп" (ITEM_PICKUP) с высоким питчем
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.5F);

        // Песочные партиклы
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, // позиция партиклов
                    10, 0.2, 0.2, 0.2, 0.05); // количество и разлет
        }
    }

    // Если блок сломали, предмет должен выпасть
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof StatBlockEntity blockEntity) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), blockEntity.getStoredItem());
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}