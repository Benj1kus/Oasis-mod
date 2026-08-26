package com.benji.oasiso.common.block;

import com.benji.oasiso.common.block.entity.ChaosSpawnerBlockEntity;
import com.benji.oasiso.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

public class ChaosSpawnerBlock extends BaseEntityBlock {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final EnumProperty<Difficulty> DIFFICULTY =
            EnumProperty.create("difficulty", Difficulty.class);

    public ChaosSpawnerBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(ACTIVE, false)
                        .setValue(DIFFICULTY, Difficulty.NOVICE)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(ACTIVE, DIFFICULTY);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChaosSpawnerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide) {
            return null;
        }

        return createTickerHelper(
                type,
                ModBlockEntities.CHAOS_SPAWNER_BE.get(),
                ChaosSpawnerBlockEntity::serverTick
        );
    }

    public enum Difficulty implements StringRepresentable {
        NOVICE("novice"),
        EASY("easy"),
        NORMAL("normal"),
        INSECTS_HARD("insects_hard"),
        CACTUS("cactus"),
        INSECTS_BRUTAL("insects_brutal"),
        CRUSADERS_HARD("crusaders_hard"),
        CRUSADERS_BRUTAL("crusaders_brutal"),
        GOLEMS("golems");

        private final String name;

        Difficulty(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
