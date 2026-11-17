package com.benji.oasiso.common.block;

import com.benji.oasiso.registry.ModKarakFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import com.benji.oasiso.ModSounds;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.event.TickEvent;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = "oasiso")
public class LilyBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape PAD = box(0, 0, 0, 16, 1, 16);
    private static final double WATER_DROP = 1.75;
    private static final VoxelShape FLOATING_PAD = PAD.move(0, -WATER_DROP / 16, 0);
    private static final BooleanProperty AFLOAT = BooleanProperty.create("afloat");
    private static final IntegerProperty VARIANT = IntegerProperty.create("variant", 1, 3);
    private static final double JUMP_Y = .80, FORWARD = .49;

    private static final int BREAK_DELAY = 20;
    private static final Map<ServerPlayer, LilyStand> STANDING = new WeakHashMap<>();

    private record LilyStand(ServerLevel level, BlockPos pos, long since) {
    }

    private static boolean hasFullArmor(Player player) {
        for (var stack : player.getArmorSlots()) {
            if (!(stack.getItem() instanceof ArmorItem)) return false;
        }
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;

        if (!player.isAlive() || !player.onGround() || player.isPassenger() || player.getAbilities().flying || !hasFullArmor(player)) {
            STANDING.remove(player);
            return;
        }

        BlockPos pos = findLilyUnderFeet(player);
        if (pos == null) {
            STANDING.remove(player);
            return;
        }

        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        LilyStand stand = STANDING.get(player);

        if (stand == null || stand.level() != level || !stand.pos().equals(pos)) {
            STANDING.put(player, new LilyStand(level, pos, now));
        } else if (now - stand.since() >= BREAK_DELAY) {
            STANDING.remove(player);
            level.destroyBlock(pos, true, player);
        }
    }

    public LilyBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(VARIANT, 1).setValue(AFLOAT, false).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT, AFLOAT, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(VARIANT, 1 + ctx.getLevel().random.nextInt(3)).setValue(AFLOAT, onWater(ctx.getLevel(), ctx.getClickedPos())).setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(AFLOAT) ? FLOATING_PAD : PAD;
    }

    private static boolean onWater(BlockGetter level, BlockPos pos) {
        var fluid = level.getFluidState(pos.below());
        return fluid.isSource() && fluid.getFluidType() == ModKarakFluids.KR_WATER_TYPE.get();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        var fluid = level.getFluidState(below);
        return level.getFluidState(pos).isEmpty() && ((fluid.isSource() && fluid.getFluidType() == ModKarakFluids.KR_WATER_TYPE.get()) || level.getBlockState(below).isFaceSturdy(level, below, Direction.UP));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (dir == Direction.DOWN) {
            if (!state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
            return state.setValue(AFLOAT, onWater(level, pos));
        }
        return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Nullable
    private static BlockPos findLilyUnderFeet(Player player) {
        var feet = player.getBoundingBox();
        int y = (int) Math.floor(feet.minY - .001);
        BlockPos from = BlockPos.containing(feet.minX + .001, y, feet.minZ + .001);
        BlockPos to = BlockPos.containing(feet.maxX - .001, y + 1, feet.maxZ - .001);
        BlockPos nearest = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            BlockState state = player.level().getBlockState(pos);
            if (!(state.getBlock() instanceof LilyBlock)) continue;

            double drop = state.getValue(AFLOAT) ? WATER_DROP : 0;
            double top = pos.getY() + (1 - drop) / 16.0;
            if (Math.abs(feet.minY - top) > .025) continue;

            double dx = player.getX() - (pos.getX() + .5);
            double dz = player.getZ() - (pos.getZ() + .5);
            double distance = dx * dx + dz * dz;
            if (distance < bestDistance) {
                nearest = pos.immutable();
                bestDistance = distance;
            }
        }
        return nearest;
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.isPassenger() || player.getAbilities().flying)
            return;

        if (player instanceof ServerPlayer serverPlayer) {
            STANDING.remove(serverPlayer);
        }

        BlockPos pos = findLilyUnderFeet(player);
        if (pos == null) return;

        double yaw = Math.toRadians(player.getYRot());
        player.setDeltaMovement(-Math.sin(yaw) * FORWARD, JUMP_Y, Math.cos(yaw) * FORWARD);

        if (player.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.POOF, player.getX(), player.getBoundingBox().minY + .05, player.getZ(), 9, .20, .035, .20, .035);
            server.playSound(null, pos, ModSounds.LILY_JUMP.get(), SoundSource.BLOCKS, .65F, .9F + server.random.nextFloat() * .25F);
        }
    }
}
