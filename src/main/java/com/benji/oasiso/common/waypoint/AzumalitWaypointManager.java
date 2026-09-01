package com.benji.oasiso.common.waypoint;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.MouthPointBlockEntity;
import com.benji.oasiso.common.event.AzumalitArmorHandler;
import com.benji.oasiso.common.item.AzumalitArmorItem;
import com.benji.oasiso.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import com.benji.oasiso.ModSounds;
import net.minecraft.sounds.SoundSource;
import com.benji.oasiso.common.chain.AzumalitChainManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AzumalitWaypointManager {

    private static final int WAYPOINT_RADIUS = 5;
    private static final int WAYPOINT_KEY_TICK = 21;
    private static final int WAYPOINT_ANIMATION_TICKS = AzumalitArmorItem.WAYPOINT_ANIMATION_TICKS;

    private static final String DATA_CAST_SPAWN_AT = "OasisoAzumalitWaypointSpawnAt";
    private static final String DATA_CAST_END_AT = "OasisoAzumalitWaypointEndAt";
    private static final String DATA_CAST_SPAWN_DONE = "OasisoAzumalitWaypointSpawnDone";

    private static final String DATA_FIRST_DIM = "OasisoAzumalitWaypointFirstDim";
    private static final String DATA_FIRST_POS = "OasisoAzumalitWaypointFirstPos";
    private static final String DATA_SECOND_DIM = "OasisoAzumalitWaypointSecondDim";
    private static final String DATA_SECOND_POS = "OasisoAzumalitWaypointSecondPos";

    private AzumalitWaypointManager() {
    }

    public static boolean requestCast(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.isSpectator() || !player.isShiftKeyDown() || !AzumalitArmorItem.isWearingFullSet(player)) {
            return false;
        }

        long gameTime = player.serverLevel().getGameTime();

        if (isCasting(player) || AzumalitArmorItem.isGuardAnimationActive(player) || AzumalitArmorItem.isWaypointAnimationActive(player) || AzumalitChainManager.isCasting(player) || AzumalitArmorItem.isChainAnimationActive(player)) {
            return false;
        }

        PairState pair = sanitizePair(player);

        if (pair.first() != null && pair.second() != null) {
            spawnFailureAtPlayer(player);
            return false;
        }
        AzumalitArmorHandler.cancelActiveArmAttack(player);

        CompoundTag data = player.getPersistentData();
        data.putLong(DATA_CAST_SPAWN_AT, gameTime + WAYPOINT_KEY_TICK);
        data.putLong(DATA_CAST_END_AT, gameTime + WAYPOINT_ANIMATION_TICKS);
        data.putBoolean(DATA_CAST_SPAWN_DONE, false);

        player.serverLevel().playSound(null, player.getX(), player.getY() + 1.0D, player.getZ(), ModSounds.SUMMON_CAST.get(), SoundSource.PLAYERS, 1.15F, 1.0F);

        AzumalitArmorItem.triggerChestAnimation(player, AzumalitArmorItem.TRIGGER_WAYPOINT);
        return true;
    }

    public static boolean isCasting(ServerPlayer player) {
        if (player == null) {
            return false;
        }

        return player.getPersistentData().getLong(DATA_CAST_END_AT) > player.serverLevel().getGameTime();
    }

    public static void cancelCast(ServerPlayer player) {
        if (player == null) {
            return;
        }

        CompoundTag data = player.getPersistentData();
        data.remove(DATA_CAST_SPAWN_AT);
        data.remove(DATA_CAST_END_AT);
        data.remove(DATA_CAST_SPAWN_DONE);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        CompoundTag data = player.getPersistentData();
        long endAt = data.getLong(DATA_CAST_END_AT);

        if (endAt <= 0L) {
            return;
        }

        long gameTime = player.serverLevel().getGameTime();

        if (!player.isAlive() || !AzumalitArmorItem.isWearingFullSet(player)) {
            cancelCast(player);
            return;
        }

        if (!data.getBoolean(DATA_CAST_SPAWN_DONE) && gameTime >= data.getLong(DATA_CAST_SPAWN_AT)) {
            data.putBoolean(DATA_CAST_SPAWN_DONE, true);
            placeWaypoint(player);
        }

        if (gameTime >= endAt) {
            cancelCast(player);
        }
    }

    private static void placeWaypoint(ServerPlayer player) {
        PairState pair = sanitizePair(player);

        if (pair.first() != null && pair.second() != null) {
            spawnFailureAtPlayer(player);
            return;
        }

        ServerLevel level = player.serverLevel();
        BlockPos placementPos = findPlacementPos(level, player);

        if (placementPos == null) {
            spawnFailureAtPlayer(player);
            return;
        }

        if (!level.setBlock(placementPos, ModBlocks.MOUTH_POINT.get().defaultBlockState(), 3)) {
            spawnFailureAtPlayer(player);
            return;
        }

        clearReplaceableAbove(level, placementPos.above());
        clearReplaceableAbove(level, placementPos.above(2));

        BlockEntity rawBlockEntity = level.getBlockEntity(placementPos);

        if (!(rawBlockEntity instanceof MouthPointBlockEntity mouthPoint)) {
            level.removeBlock(placementPos, false);
            spawnFailureAtPlayer(player);
            return;
        }

        WaypointRef newRef = new WaypointRef(level.dimension(), placementPos.immutable());
        UUID ownerId = player.getUUID();

        if (pair.first() == null) {
            mouthPoint.configure(ownerId, 1);
            mouthPoint.startSpawnAnimation();
            writeFirst(player, newRef);
            clearSecond(player);
            return;
        }

        MouthPointBlockEntity first = resolve(player.getServer(), pair.first(), ownerId);

        if (first == null) {
            mouthPoint.configure(ownerId, 1);
            mouthPoint.startSpawnAnimation();
            writeFirst(player, newRef);
            clearSecond(player);
            return;
        }

        mouthPoint.configure(ownerId, 2);
        mouthPoint.setPartner(pair.first());
        mouthPoint.startSpawnAnimation();

        first.setVariant(1);
        first.setPartner(newRef);

        writeFirst(player, pair.first());
        writeSecond(player, newRef);
    }


    private static void clearReplaceableAbove(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (!state.isAir() && state.canBeReplaced() && state.getFluidState().isEmpty()) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @Nullable
    private static BlockPos findPlacementPos(ServerLevel level, ServerPlayer player) {
        BlockPos origin = player.blockPosition();

        Vec3 look = player.getLookAngle();
        Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);

        if (horizontalLook.lengthSqr() < 0.0001D) {
            horizontalLook = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horizontalLook = horizontalLook.normalize();
        }

        Vec3 desired = player.position().add(horizontalLook.scale(3.25D));

        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;

        for (int dx = -WAYPOINT_RADIUS; dx <= WAYPOINT_RADIUS; dx++) {
            for (int dz = -WAYPOINT_RADIUS; dz <= WAYPOINT_RADIUS; dz++) {
                for (int dy = 3; dy >= -4; dy--) {
                    BlockPos candidate = origin.offset(dx, dy, dz);
                    Vec3 candidateCenter = Vec3.atBottomCenterOf(candidate);

                    if (candidateCenter.distanceToSqr(player.position()) > WAYPOINT_RADIUS * WAYPOINT_RADIUS) {
                        continue;
                    }

                    if (!canPlaceAt(level, player, candidate)) {
                        continue;
                    }

                    double horizontalDesired = square(candidateCenter.x - desired.x) + square(candidateCenter.z - desired.z);
                    double verticalPenalty = square(candidateCenter.y - player.getY()) * 0.65D;
                    double score = horizontalDesired + verticalPenalty;

                    if (score < bestScore) {
                        bestScore = score;
                        best = candidate.immutable();
                    }
                }
            }
        }

        return best;
    }

    private static boolean canPlaceAt(ServerLevel level, ServerPlayer player, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || level.isOutsideBuildHeight(pos.above(2)) || !level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        BlockState aboveOne = level.getBlockState(pos.above());
        BlockState aboveTwo = level.getBlockState(pos.above(2));
        BlockPos supportPos = pos.below();
        BlockState support = level.getBlockState(supportPos);

        if (!state.canBeReplaced() || !aboveOne.canBeReplaced() || !aboveTwo.canBeReplaced() || !state.getFluidState().isEmpty() || !aboveOne.getFluidState().isEmpty() || !aboveTwo.getFluidState().isEmpty() || !support.isFaceSturdy(level, supportPos, Direction.UP)) {
            return false;
        }

        AABB occupied = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D, pos.getY() + (45.0D / 16.0D), pos.getZ() + 1.0D);

        if (player.getBoundingBox().intersects(occupied)) {
            return false;
        }

        return level.getEntitiesOfClass(LivingEntity.class, occupied, entity -> entity.isAlive() && !entity.isSpectator()).isEmpty();
    }

    public static PairState sanitizePair(ServerPlayer player) {
        MinecraftServer server = player.getServer();

        if (server == null) {
            clearAll(player);
            return new PairState(null, null);
        }

        UUID ownerId = player.getUUID();
        WaypointRef firstRef = readRef(player, DATA_FIRST_DIM, DATA_FIRST_POS);
        WaypointRef secondRef = readRef(player, DATA_SECOND_DIM, DATA_SECOND_POS);

        MouthPointBlockEntity first = resolve(server, firstRef, ownerId);
        MouthPointBlockEntity second = resolve(server, secondRef, ownerId);

        if (first == null && second == null) {
            clearAll(player);
            return new PairState(null, null);
        }

        if (first == null) {
            WaypointRef promotedRef = secondRef;
            second.setVariant(1);
            second.clearPartner();
            writeFirst(player, promotedRef);
            clearSecond(player);
            return new PairState(promotedRef, null);
        }

        if (second == null) {
            first.setVariant(1);
            first.clearPartner();
            writeFirst(player, firstRef);
            clearSecond(player);
            return new PairState(firstRef, null);
        }

        first.setVariant(1);
        second.setVariant(2);
        first.setPartner(secondRef);
        second.setPartner(firstRef);
        writeFirst(player, firstRef);
        writeSecond(player, secondRef);

        return new PairState(firstRef, secondRef);
    }

    public static void onWaypointRemoved(ServerLevel level, MouthPointBlockEntity removed) {
        UUID ownerId = removed.getOwnerId();

        if (ownerId == null) {
            return;
        }

        MinecraftServer server = level.getServer();
        WaypointRef partnerRef = removed.getPartnerRef();
        MouthPointBlockEntity partner = resolve(server, partnerRef, ownerId);

        if (partner != null) {
            partner.setVariant(1);
            partner.clearPartner();
        }

        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);

        if (owner != null) {
            if (partner != null && partnerRef != null) {
                writeFirst(owner, partnerRef);
                clearSecond(owner);
            } else {
                clearAll(owner);
            }
        }
    }

    public static void removePairForTeleport(MouthPointBlockEntity source, MouthPointBlockEntity target) {
        UUID ownerId = source.getOwnerId();

        if (!(source.getLevel() instanceof ServerLevel sourceLevel) || !(target.getLevel() instanceof ServerLevel targetLevel)) {
            return;
        }

        source.suppressRemovalCallback();
        target.suppressRemovalCallback();

        BlockPos sourcePos = source.getBlockPos().immutable();
        BlockPos targetPos = target.getBlockPos().immutable();

        sourceLevel.removeBlock(sourcePos, false);

        if (sourceLevel != targetLevel || !sourcePos.equals(targetPos)) {
            targetLevel.removeBlock(targetPos, false);
        }

        if (ownerId != null) {
            ServerPlayer owner = sourceLevel.getServer().getPlayerList().getPlayer(ownerId);

            if (owner != null) {
                clearAll(owner);
            }
        }
    }

    @Nullable
    public static MouthPointBlockEntity resolve(MinecraftServer server, @Nullable WaypointRef ref, UUID ownerId) {
        if (server == null || ref == null) {
            return null;
        }

        ServerLevel level = server.getLevel(ref.dimension());

        if (level == null || level.isOutsideBuildHeight(ref.pos())) {
            return null;
        }
        level.getChunkAt(ref.pos());
        BlockEntity rawBlockEntity = level.getBlockEntity(ref.pos());

        if (rawBlockEntity instanceof MouthPointBlockEntity mouthPoint && mouthPoint.isOwnedBy(ownerId) && level.getBlockState(ref.pos()).is(ModBlocks.MOUTH_POINT.get())) {
            return mouthPoint;
        }

        return null;
    }

    public static WaypointRef refOf(MouthPointBlockEntity mouthPoint) {
        if (!(mouthPoint.getLevel() instanceof ServerLevel level)) {
            throw new IllegalStateException("Mouth point is not attached to a ServerLevel");
        }

        return new WaypointRef(level.dimension(), mouthPoint.getBlockPos().immutable());
    }

    @Nullable
    public static WaypointRef createRef(String dimensionId, BlockPos pos) {
        ResourceLocation id = ResourceLocation.tryParse(dimensionId);

        if (id == null) {
            return null;
        }

        return new WaypointRef(ResourceKey.create(Registries.DIMENSION, id), pos.immutable());
    }

    private static void spawnFailureAtPlayer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        double x = player.getX();
        double y = player.getY() + 1.0D;
        double z = player.getZ();

        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 10, 0.35D, 0.45D, 0.35D, 0.025D);
        level.sendParticles(ParticleTypes.SMOKE, x, y, z, 14, 0.30D, 0.42D, 0.30D, 0.02D);
    }

    private static double square(double value) {
        return value * value;
    }

    @Nullable
    private static WaypointRef readRef(ServerPlayer player, String dimKey, String posKey) {
        CompoundTag data = player.getPersistentData();

        if (!data.contains(dimKey) || !data.contains(posKey)) {
            return null;
        }

        return createRef(data.getString(dimKey), BlockPos.of(data.getLong(posKey)));
    }

    private static void writeFirst(ServerPlayer player, WaypointRef ref) {
        writeRef(player, DATA_FIRST_DIM, DATA_FIRST_POS, ref);
    }

    private static void writeSecond(ServerPlayer player, WaypointRef ref) {
        writeRef(player, DATA_SECOND_DIM, DATA_SECOND_POS, ref);
    }

    private static void writeRef(ServerPlayer player, String dimKey, String posKey, WaypointRef ref) {
        CompoundTag data = player.getPersistentData();
        data.putString(dimKey, ref.dimension().location().toString());
        data.putLong(posKey, ref.pos().asLong());
    }

    private static void clearSecond(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        data.remove(DATA_SECOND_DIM);
        data.remove(DATA_SECOND_POS);
    }

    private static void clearAll(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        data.remove(DATA_FIRST_DIM);
        data.remove(DATA_FIRST_POS);
        data.remove(DATA_SECOND_DIM);
        data.remove(DATA_SECOND_POS);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cancelCast(player);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cancelCast(player);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer oldPlayer) || !(event.getEntity() instanceof ServerPlayer newPlayer)) {
            return;
        }

        CompoundTag oldData = oldPlayer.getPersistentData();
        CompoundTag newData = newPlayer.getPersistentData();

        copyIfPresent(oldData, newData, DATA_FIRST_DIM, true);
        copyIfPresent(oldData, newData, DATA_FIRST_POS, false);
        copyIfPresent(oldData, newData, DATA_SECOND_DIM, true);
        copyIfPresent(oldData, newData, DATA_SECOND_POS, false);

        cancelCast(newPlayer);
    }

    private static void copyIfPresent(CompoundTag from, CompoundTag to, String key, boolean stringValue) {
        if (!from.contains(key)) {
            return;
        }

        if (stringValue) {
            to.putString(key, from.getString(key));
        } else {
            to.putLong(key, from.getLong(key));
        }
    }

    public record WaypointRef(ResourceKey<Level> dimension, BlockPos pos) {
    }

    public record PairState(@Nullable WaypointRef first, @Nullable WaypointRef second) {
    }
}
