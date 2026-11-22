package com.benji.oasiso.common.block;

import com.benji.oasiso.common.block.entity.FluwirusBlockEntity;
import com.benji.oasiso.registry.ModBlockEntities;
import com.benji.oasiso.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.function.BiFunction;

@Mod.EventBusSubscriber(modid = "oasiso")
public class FluwirusBlock extends BaseEntityBlock {
    public static final BooleanProperty CUTTING = BooleanProperty.create("cutting");
    public static final BooleanProperty AWAKE = BooleanProperty.create("awake");
    private static final int CUT_TICKS = 65;
    private final BiFunction<BlockPos, BlockState, BlockEntity> blockEntityProvider;

    public FluwirusBlock(Properties properties, BiFunction<BlockPos, BlockState, BlockEntity> provider) {
        super(properties);
        blockEntityProvider = provider;
        registerDefaultState(defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(CUTTING, false)
                .setValue(AWAKE, false));
    }

    public FluwirusBlock(Properties properties) {
        this(properties, FluwirusBlockEntity::new);
    }

    @SubscribeEvent
    public static void onShiftRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().isShiftKeyDown()) return;
        if (event.getUseBlock() == Event.Result.DENY) return;

        if (event.getLevel().getBlockState(event.getPos()).getBlock() instanceof FluwirusBlock) {
            event.setUseBlock(Event.Result.ALLOW);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING, CUTTING, AWAKE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(HorizontalDirectionalBlock.FACING, rotation.rotate(state.getValue(HorizontalDirectionalBlock.FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(HorizontalDirectionalBlock.FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level instanceof ServerLevel server && !state.getValue(CUTTING) && level.getBlockEntity(pos) instanceof FluwirusBlockEntity flower) {
            flower.setCollector(player.getUUID());
            server.setBlock(pos, state.setValue(CUTTING, true).setValue(AWAKE, false), Block.UPDATE_CLIENTS);
            server.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
            server.scheduleTick(pos, this, CUT_TICKS);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(CUTTING) || !(level.getBlockEntity(pos) instanceof FluwirusBlockEntity flower)) return;
        Player player = flower.getCollector() == null ? null : level.getServer().getPlayerList().getPlayer(flower.getCollector());
        if (!level.removeBlock(pos, false)) return;
        ItemStack reward = new ItemStack(ModItems.FLUWIRUS_HEAD.get());
        if (player == null || !player.isAlive()) {
            popResource(level, pos, reward);
        } else if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean harvest, FluidState fluid) {
        if (state.getValue(CUTTING)) return false;
        boolean removed = super.onDestroyedByPlayer(state, level, pos, player, harvest, fluid);
        if (removed && level instanceof ServerLevel server) FeralDrying.start(server, pos);
        return removed;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FLUWIRUS_BE.get(), FluwirusBlockEntity::lookTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return blockEntityProvider.apply(pos, state);
    }
}
