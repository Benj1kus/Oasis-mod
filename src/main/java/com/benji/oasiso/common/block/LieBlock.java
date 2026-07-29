package com.benji.oasiso.common.block;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.LieBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LieBlock extends BaseEntityBlock {
    public static final BooleanProperty MIMICKING = BooleanProperty.create("mimicking");

    public LieBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(MIMICKING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MIMICKING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LieBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(MIMICKING) ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.MODEL;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        if (state.getValue(MIMICKING) && level.getBlockEntity(currentPos) instanceof LieBlockEntity lieBE) {
            BlockState mimicState = lieBE.getMimicState();
            if (mimicState != null) {
                BlockState newMimicState = mimicState.updateShape(direction, neighborState, level, currentPos, neighborPos);
                if (mimicState != newMimicState) {
                    lieBE.setMimicState(newMimicState);
                }
            }
        }
        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof LieBlockEntity lieBE)) return InteractionResult.PASS;

        ItemStack stack = player.getItemInHand(hand);

// hand
        if (stack.isEmpty()) {
            if (lieBE.getMimicState() != null) {

                if (lieBE.isPhasing() && !hasFullSuperGoldArmor(player)) {
                    return InteractionResult.PASS;
                }

                Block.popResource(level, pos, new ItemStack(lieBE.getMimicState().getBlock()));
                if (lieBE.isPhasing()) {
                    Block.popResource(level, pos, new ItemStack(Oasiso.NEPHRITIS_CORE.get()));
                }

                lieBE.setMimicState(null);
                lieBE.setPhasing(false);
                level.setBlock(pos, state.setValue(MIMICKING, false), 3);
                level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        if (stack.is(Oasiso.NEPHRITIS_CORE.get())) {
            if (lieBE.getMimicState() != null && !lieBE.isPhasing()) {
                lieBE.setPhasing(true);
                if (!player.isCreative()) stack.shrink(1);
                level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 2.0f);
                return InteractionResult.SUCCESS;
            }
        }

        if (stack.getItem() instanceof BlockItem blockItem) {
            if (lieBE.getMimicState() == null) {
                BlockPlaceContext ctx = new BlockPlaceContext(player, hand, stack, hit);
                BlockState placedState = blockItem.getBlock().getStateForPlacement(ctx);
                if (placedState == null) placedState = blockItem.getBlock().defaultBlockState();

                for (Direction dir : Direction.values()) {
                    placedState = placedState.updateShape(dir, level.getBlockState(pos.relative(dir)), level, pos, pos.relative(dir));
                }

                lieBE.setMimicState(placedState);
                level.setBlock(pos, state.setValue(MIMICKING, true), 3);
                if (!player.isCreative()) stack.shrink(1);
                level.playSound(null, pos, placedState.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0f, 1.0f);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    private boolean hasFullSuperGoldArmor(Player player) {
        int superGoldPieces = 0;
        for (ItemStack armorSlot : player.getArmorSlots()) {
            if (armorSlot.is(Oasiso.SUPER_GOLD_HELMET.get()) ||
                    armorSlot.is(Oasiso.SUPER_GOLD_CHESTPLATE.get()) ||
                    armorSlot.is(Oasiso.SUPER_GOLD_LEGGINGS.get()) ||
                    armorSlot.is(Oasiso.SUPER_GOLD_BOOTS.get())) {
                superGoldPieces++;
            }
        }
        return superGoldPieces == 4;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(MIMICKING) && level.getBlockEntity(pos) instanceof LieBlockEntity lieBE) {

            if (lieBE.isPhasing() && context instanceof EntityCollisionContext ecc && ecc.getEntity() != null) {

                if (ecc.getEntity() instanceof Player player) {
                    if (hasFullSuperGoldArmor(player)) {
                        return Shapes.block();
                    }
                }

                double distSqr = ecc.getEntity().distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                if (distSqr < 12.25) {
                    return Shapes.empty();
                }
            }
        }
        return Shapes.block();
    }
}