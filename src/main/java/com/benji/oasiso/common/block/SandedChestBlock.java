package com.benji.oasiso.common.block;

import com.benji.oasiso.common.block.entity.SandedChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SandedChestBlock extends BaseEntityBlock {
    // Хитбокс точно как у обычного сундука
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);

    public SandedChestBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED; // Указываем, что блок рендерится через Geckolib
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SandedChestBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SandedChestBlockEntity chestBe)) return InteractionResult.PASS;

        if (chestBe.isBrushing()) return InteractionResult.PASS;

        // Очистка кистью
        if (player.getItemInHand(hand).is(Items.BRUSH)) {
            if (!level.isClientSide) {
                chestBe.startBrushing();
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // Попытка открыть
        if (!level.isClientSide) {
            level.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.blockEvent(pos, this, 1, 0);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // =========================================================
    // НОВЫЙ МЕТОД: ФОНОВЫЕ ПАРТИКЛЫ (Только на стороне клиента)
    // =========================================================
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Спавним частицы с вероятностью ~33% каждый тик, чтобы не засыпать весь экран
        if (random.nextInt(3) == 0) {
            // Выбираем случайную точку на плоскости блока
            double x = (double) pos.getX() + random.nextDouble();
            // Высота: 0.9D это чуть выше крышки сундука (ее высота 14/16 = 0.875)
            double y = (double) pos.getY() + 0.9D;
            double z = (double) pos.getZ() + random.nextDouble();

            // Создаем частицу падающей пыли с текстурой песка
            level.addParticle(
                    new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
                    x, y, z,
                    0.0D, 0.0D, 0.0D // Вектор скорости (частица сама будет падать вниз под гравитацией)
            );
        }
    }

    // Подключаем тикер для таймера очистки
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide ? createTickerHelper(type, com.benji.oasiso.Oasiso.SANDED_CHEST_BE.get(), SandedChestBlockEntity::tick) : null;
    }
}