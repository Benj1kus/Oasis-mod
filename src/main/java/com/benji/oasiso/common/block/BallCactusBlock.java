package com.benji.oasiso.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BallCactusBlock extends Block {
    public static final IntegerProperty STATE = IntegerProperty.create("cactus_state", 0, 4);
    private static final VoxelShape VASE_SHAPE = box(2, 0, 2, 14, 17, 14);

    public BallCactusBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(STATE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STATE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VASE_SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState blockBelow = level.getBlockState(pos.below());
        return blockBelow.is(BlockTags.SAND);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (state.getValue(STATE) == 0) {
            entity.hurt(level.damageSources().cactus(), 1.0F);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        int currentState = state.getValue(STATE);

        if (stack.isEmpty() && (currentState == 0 || currentState == 1)) {
            if (!level.isClientSide) {
                player.getFoodData().eat(1, 0.1f);

                level.setBlock(pos, state.setValue(STATE, currentState + 1), 3);

                level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, 1.0f, 1.0f + level.random.nextFloat() * 0.2f);

                ((ServerLevel) level).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CACTUS.defaultBlockState()),
                        pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 10, 0.2, 0.2, 0.2, 0.05);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (currentState == 2 && stack.is(Items.WATER_BUCKET)) {
            if (!level.isClientSide) {
                if (!player.isCreative()) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                level.setBlock(pos, state.setValue(STATE, 3), 3);
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
                ((ServerLevel) level).sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 10, 0.2, 0.1, 0.2, 0.1);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (currentState == 3 && stack.is(Items.BUCKET)) {
            if (!level.isClientSide) {
                stack.shrink(1);
                if (stack.isEmpty()) player.setItemInHand(hand, new ItemStack(Items.WATER_BUCKET));
                else if (!player.getInventory().add(new ItemStack(Items.WATER_BUCKET))) player.drop(new ItemStack(Items.WATER_BUCKET), false);

                level.setBlock(pos, state.setValue(STATE, 2), 3);
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (currentState == 2 && stack.is(Items.MILK_BUCKET)) {
            if (!level.isClientSide) {
                if (!player.isCreative()) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                level.setBlock(pos, state.setValue(STATE, 4), 3);
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (currentState == 4 && stack.is(Items.BUCKET)) {
            if (!level.isClientSide) {
                stack.shrink(1);
                if (stack.isEmpty()) player.setItemInHand(hand, new ItemStack(Items.MILK_BUCKET));
                else if (!player.getInventory().add(new ItemStack(Items.MILK_BUCKET))) player.drop(new ItemStack(Items.MILK_BUCKET), false);

                level.setBlock(pos, state.setValue(STATE, 2), 3);
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(STATE) == 3;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(STATE) == 3) {
            if (random.nextInt(4) == 0) {
                if (random.nextBoolean()) {
                    level.setBlock(pos, state.setValue(STATE, 4), 3);
                    level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0f, 1.0f);
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 5, 0.2, 0.1, 0.2, 0.05);
                } else {
                    level.setBlock(pos, state.setValue(STATE, 2), 3);
                    level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 2.6f + (random.nextFloat() - random.nextFloat()) * 0.8f);
                    level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 8, 0.2, 0.1, 0.2, 0.05);
                }
            }
        }
    }
}