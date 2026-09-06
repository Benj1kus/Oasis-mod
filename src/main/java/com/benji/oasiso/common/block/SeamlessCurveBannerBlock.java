package com.benji.oasiso.common.block;

import com.benji.oasiso.common.block.entity.SeamlessCurveBannerBlockEntity;
import com.benji.oasiso.registry.ModBlockEntities;
import com.benji.oasiso.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SeamlessCurveBannerBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final VoxelShape EAST_SHAPE = box(0.0D, 5.0D, 5.0D, 2.0D, 11.0D, 11.0D);
    private static final VoxelShape WEST_SHAPE = box(14.0D, 5.0D, 5.0D, 16.0D, 11.0D, 11.0D);
    private static final VoxelShape SOUTH_SHAPE = box(5.0D, 5.0D, 0.0D, 11.0D, 11.0D, 2.0D);
    private static final VoxelShape NORTH_SHAPE = box(5.0D, 5.0D, 14.0D, 11.0D, 11.0D, 16.0D);
    private static final VoxelShape UP_SHAPE = box(5.0D, 0.0D, 5.0D, 11.0D, 2.0D, 11.0D);
    private static final VoxelShape DOWN_SHAPE = box(5.0D, 14.0D, 5.0D, 11.0D, 16.0D, 11.0D);

    public SeamlessCurveBannerBlock(Properties properties) {
        super(properties);

        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case EAST -> EAST_SHAPE;
            case WEST -> WEST_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case NORTH -> NORTH_SHAPE;
            case UP -> UP_SHAPE;
            case DOWN -> DOWN_SHAPE;
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SeamlessCurveBannerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.SEEMLESS_CURVE_BANNER_BE.get(), SeamlessCurveBannerBlockEntity::serverTick);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (!level.isClientSide && blockEntity instanceof SeamlessCurveBannerBlockEntity banner && banner.isConnected()) {
            popResource(level, pos, new ItemStack(ModItems.SEEMLESS_CURVE_BANNER_ITEM.get()));
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(ModItems.SEEMLESS_CURVE_BANNER_ITEM.get());
    }
}