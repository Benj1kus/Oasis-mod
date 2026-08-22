package com.benji.oasiso.common.dimension;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChaosEyeAmbientHandler {


    private static final int MIN_SPAWN_RADIUS = 5;

    private static final int MAX_SPAWN_RADIUS = 20;


    private static final int VERTICAL_RADIUS = 15;


    private static final int MIN_SPAWN_DELAY = 10;
    private static final int MAX_SPAWN_DELAY = 30;


    private static final int MIN_LIFETIME = 20;
    private static final int MAX_LIFETIME = 100;


    private static final int MAX_CLUSTER_SIZE = 4;

    private static final int POSITION_ATTEMPTS = 20;
    private static final int CLUSTER_ATTEMPTS = 16;

    private static final Direction[] DIRECTIONS = Direction.values();

    private static final Map<UUID, Integer> SPAWN_TIMERS = new HashMap<>();

    private ChaosEyeAmbientHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        UUID playerId = player.getUUID();

        if (!player.level().dimension().equals(Oasiso.CHAOS_DIMENSION)) {

            SPAWN_TIMERS.remove(playerId);
            return;
        }

        ServerLevel level = player.serverLevel();
        RandomSource random = player.getRandom();

        int timer = SPAWN_TIMERS.getOrDefault(playerId, 20 + random.nextInt(41));

        if (timer > 0) {
            SPAWN_TIMERS.put(playerId, timer - 1);

            return;
        }

        trySpawnCluster(level, player, random);
        SPAWN_TIMERS.put(playerId, randomBetween(random, MIN_SPAWN_DELAY, MAX_SPAWN_DELAY));
    }

    private static void trySpawnCluster(ServerLevel level, ServerPlayer player, RandomSource random) {
        BlockPos origin = findSpawnPosition(level, player, random);

        if (origin == null) {
            return;
        }

        int clusterSize = 1 + random.nextInt(MAX_CLUSTER_SIZE);
        int lifetime = randomBetween(random, MIN_LIFETIME, MAX_LIFETIME);

        List<BlockPos> clusterPositions = new ArrayList<>();

        clusterPositions.add(origin);

        for (int attempt = 0; attempt < CLUSTER_ATTEMPTS && clusterPositions.size() < clusterSize; attempt++) {

            BlockPos basePos = clusterPositions.get(random.nextInt(clusterPositions.size()));
            Direction direction = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
            BlockPos candidate = basePos.relative(direction);

            if (clusterPositions.contains(candidate)) {
                continue;
            }

            if (!isValidSpawnPosition(level, candidate)) {
                continue;
            }

            clusterPositions.add(candidate.immutable());
        }

        for (BlockPos pos : clusterPositions) {
            level.setBlock(pos, Oasiso.CHAOS_EYE.get().defaultBlockState(), 3);

            level.scheduleTick(pos, Oasiso.CHAOS_EYE.get(), lifetime);
        }
    }

    private static BlockPos findSpawnPosition(ServerLevel level, ServerPlayer player, RandomSource random) {
        BlockPos playerPos = player.blockPosition();

        int minDistanceSqr = MIN_SPAWN_RADIUS * MIN_SPAWN_RADIUS;
        int maxDistanceSqr = MAX_SPAWN_RADIUS * MAX_SPAWN_RADIUS;

        for (int attempt = 0; attempt < POSITION_ATTEMPTS; attempt++) {

            int offsetX = randomBetween(random, -MAX_SPAWN_RADIUS, MAX_SPAWN_RADIUS);
            int offsetY = randomBetween(random, -VERTICAL_RADIUS, VERTICAL_RADIUS);
            int offsetZ = randomBetween(random, -MAX_SPAWN_RADIUS, MAX_SPAWN_RADIUS);

            int distanceSqr = offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ;
            if (distanceSqr < minDistanceSqr || distanceSqr > maxDistanceSqr) {
                continue;
            }

            BlockPos candidate = playerPos.offset(offsetX, offsetY, offsetZ);

            if (isValidSpawnPosition(level, candidate)) {
                return candidate.immutable();
            }
        }

        return null;
    }

    private static boolean isValidSpawnPosition(ServerLevel level, BlockPos pos) {
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
            return false;
        }
        if (!level.hasChunkAt(pos)) {
            return false;
        }
        return level.getBlockState(pos).isAir();
    }

    private static int randomBetween(RandomSource random, int minimum, int maximum) {
        return minimum + random.nextInt(maximum - minimum + 1);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SPAWN_TIMERS.remove(event.getEntity().getUUID());
    }
}