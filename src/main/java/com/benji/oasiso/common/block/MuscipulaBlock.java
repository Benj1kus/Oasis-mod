package com.benji.oasiso.common.block;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.player.Player;

@Mod.EventBusSubscriber(modid = Oasiso.MODID)
public class MuscipulaBlock extends Block {
    private static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final BooleanProperty CLOSED = BooleanProperty.create("closed");
    private static final int SCAN_INTERVAL = 5, COOLDOWN = 200, BLEED_TICKS = 200;
    private static final double RANGE = 2;
    private static final String BLEED_START = "oasisoMuscipulaBiteTick";

    public MuscipulaBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(CLOSED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CLOSED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = Direction.from2DDataValue(ctx.getLevel().random.nextInt(4));
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide && !old.is(this))
            level.scheduleTick(pos, this, state.getValue(CLOSED) ? COOLDOWN : SCAN_INTERVAL);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this))
            level.scheduleTick(pos, this, state.getValue(CLOSED) ? COOLDOWN : SCAN_INTERVAL);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(CLOSED)) {
            level.setBlockAndUpdate(pos, state.setValue(CLOSED, false));
            level.scheduleTick(pos, this, SCAN_INTERVAL);
            return;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        LivingEntity nearest = null;
        double distance = RANGE * RANGE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(RANGE),
                e -> e.isAlive() && !e.isSpectator() && !(e instanceof Player player && player.isCreative()))) {
            double d = e.distanceToSqr(center);
            if (d <= distance) {
                nearest = e;
                distance = d;
            }
        }
        if (nearest == null) {
            level.scheduleTick(pos, this, SCAN_INTERVAL);
            return;
        }
        level.setBlockAndUpdate(pos, state.setValue(CLOSED, true));
        level.scheduleTick(pos, this, COOLDOWN);
        level.playSound(null, pos, SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.BLOCKS, 1F, 1F);
        level.sendParticles(Oasiso.FERAL_BITE.get(), nearest.getX(), nearest.getY(.55), nearest.getZ(), 5 + random.nextInt(4), nearest.getBbWidth() * .32, nearest.getBbHeight() * .22, nearest.getBbWidth() * .32, .035);
        nearest.knockback(.35, center.x - nearest.getX(), center.z - nearest.getZ());
        nearest.hurtMarked = true;
        nearest.getPersistentData().putLong(BLEED_START, level.getGameTime());
    }

    @SubscribeEvent
    public static void bleed(LivingEvent.LivingTickEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) return;
        var data = victim.getPersistentData();
        if (!data.contains(BLEED_START)) return;
        long age = level.getGameTime() - data.getLong(BLEED_START);
        if (age > 0 && age <= BLEED_TICKS && victim.isAlive()) {
            if (age % 5 == 0)
                level.sendParticles(Oasiso.FERAL_BLOOD.get(), victim.getX(), victim.getY(.55), victim.getZ(), 2, victim.getBbWidth() * .25, victim.getBbHeight() * .18, victim.getBbWidth() * .25, .028);
            if (age % 20 == 0) victim.hurt(victim.damageSources().magic(), 1F);
        }
        if (age >= BLEED_TICKS || !victim.isAlive()) data.remove(BLEED_START);
    }
}
