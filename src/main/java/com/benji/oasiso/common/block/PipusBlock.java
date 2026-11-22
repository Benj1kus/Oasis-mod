package com.benji.oasiso.common.block;

import net.minecraft.world.level.block.state.StateDefinition;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.registry.ModKarakFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PipusBlock extends Block {
    public PipusBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FeralDrying.DRY, 0));
    }

    private static boolean isKarak(FluidState fluid) {
        return fluid.getFluidType() == ModKarakFluids.KR_WATER_TYPE.get();
    }

    private void wake(LevelAccessor level, BlockPos pos) {
        if (!level.isClientSide() && isKarak(level.getFluidState(pos.above())) && !level.getBlockTicks().hasScheduledTick(pos, this))
            level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FeralDrying.DRY);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!old.is(this)) wake(level, pos);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos other) {
        if (direction == Direction.UP) wake(level, pos);
        return super.updateShape(state, direction, neighbor, level, pos, other);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        wake(level, pos);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos.MutableBlockPos cursor = pos.above().mutable();
        FluidState fluid = level.getFluidState(cursor);
        if (!isKarak(fluid)) return;
        double bottom = cursor.getY();
        double surface;
        do {
            surface = cursor.getY() + fluid.getHeight(level, cursor);
            cursor.move(Direction.UP);
            fluid = level.getFluidState(cursor);
        } while (isKarak(fluid));

        double x = pos.getX(), z = pos.getZ();
        boolean open = level.getBlockState(cursor).isAir();
        if (open) {
            if ((level.getGameTime() + pos.asLong()) % 4 == 0) {
                for (int i = 0; i < 2; i++)
                    level.sendParticles(Oasiso.KARAK_BUBBLES.get(), x + .08 + random.nextDouble() * .84, surface + .025, z + .08 + random.nextDouble() * .84, 0, 0, .10 + random.nextDouble() * .20, 0, 1);
            }
            AABB waterline = new AABB(x, surface - .18, z, x + 1, surface + .12, z + 1);
            for (Boat boat : level.getEntitiesOfClass(Boat.class, waterline)) {
                if (boat.getY() <= surface + .12 && boat.getY() >= surface - .55) boat.onAboveBubbleCol(true);
            }
        }
        AABB current = new AABB(x, bottom, z, x + 1, surface + .04, z + 1);
        for (Player player : level.getEntitiesOfClass(Player.class, current)) {
            if (player.isSpectator() || player.getAbilities().flying || player.isPassenger() || Mth.floor(player.getX()) != pos.getX() || Mth.floor(player.getZ()) != pos.getZ())
                continue;
            Vec3 v = player.getDeltaMovement();
            double dx = x + .5 - player.getX(), dz = z + .5 - player.getZ();
            player.setDeltaMovement(v.x * .92 + dx * .045, Mth.clamp(v.y - .035, -.30, .35), v.z * .92 + dz * .045);
            player.hurtMarked = true;
        }
        level.scheduleTick(pos, this, 1);
    }
}
