package com.benji.oasiso.common.entity.ai;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.KrombulEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import com.benji.oasiso.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class KrombulTeleportPlayerGoal extends Goal {

    private static final double DETECTION_RADIUS = 5.0D;
    private static final double MAX_APPROACH_DISTANCE = 16.0D;

    private static final int REQUIRED_NEARBY_TIME = 60;
    private static final int TP_START_TIME = 15;
    private static final int TP_END_TIME = 15;

    private static final int TELEPORT_RADIUS = 50;
    private static final int TELEPORT_ATTEMPTS = 32;

    private final KrombulEntity krombul;

    @Nullable
    private ServerPlayer candidatePlayer;

    @Nullable
    private ServerPlayer targetPlayer;

    @Nullable
    private UUID trackedPlayerId;

    private int proximityStartTick = -1;
    private int animationTimer;


    private static final int TELEPORT_COOLDOWN = 20 * 5;

    private int nextTeleportAllowedTick;

    private Phase phase = Phase.IDLE;

    public KrombulTeleportPlayerGoal(KrombulEntity krombul) {
        this.krombul = krombul;

        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.krombul.getTarget() != null || this.krombul.isTeleporting()) {
            resetProximityTracking();
            return false;
        }

        if (this.krombul.tickCount < this.nextTeleportAllowedTick) {
            resetProximityTracking();
            return false;
        }

        ServerPlayer nearestPlayer = findNearestPlayer();

        if (nearestPlayer == null) {
            resetProximityTracking();
            return false;
        }

        UUID playerId = nearestPlayer.getUUID();

        if (!playerId.equals(this.trackedPlayerId)) {
            this.trackedPlayerId = playerId;
            this.proximityStartTick = this.krombul.tickCount;
            this.candidatePlayer = nearestPlayer;

            return false;
        }

        this.candidatePlayer = nearestPlayer;

        return this.krombul.tickCount - this.proximityStartTick >= REQUIRED_NEARBY_TIME;
    }

    @Override
    public void start() {
        this.targetPlayer = this.candidatePlayer;

        this.candidatePlayer = null;
        this.phase = Phase.APPROACH;

        resetProximityTracking();
    }

    @Override
    public boolean canContinueToUse() {
        if (this.phase == Phase.IDLE) {
            return false;
        }

        if (this.phase == Phase.TP_END) {
            return true;
        }

        return this.targetPlayer != null && this.targetPlayer.isAlive() && !this.targetPlayer.isSpectator();
    }

    @Override
    public void tick() {
        if (this.targetPlayer == null) {
            this.phase = Phase.IDLE;
            return;
        }

        switch (this.phase) {
            case APPROACH -> tickApproach();

            case TP_START -> tickTeleportStart();

            case TP_END -> tickTeleportEnd();
        }
    }

    private void tickApproach() {
        ServerPlayer player = this.targetPlayer;

        double horizontalDistanceSqr = horizontalDistanceSqr(player);

        if (horizontalDistanceSqr > MAX_APPROACH_DISTANCE * MAX_APPROACH_DISTANCE) {
            this.phase = Phase.IDLE;
            return;
        }

        this.krombul.getLookControl().setLookAt(player, 30.0F, 30.0F);

        double closeDistance = 1.25D + this.krombul.getBbWidth() * 0.5D + player.getBbWidth() * 0.5D;

        if (horizontalDistanceSqr <= closeDistance * closeDistance) {
            beginTeleportAnimation();
            return;
        }

        double wantedY = this.krombul.findHoverY(player.getX(), player.getZ());

        this.krombul.getNavigation().moveTo(player.getX(), wantedY, player.getZ(), 1.15D);
    }

    private void beginTeleportAnimation() {
        this.krombul.getNavigation().stop();
        this.krombul.setDeltaMovement(Vec3.ZERO);

        this.krombul.setAnimState(KrombulEntity.STATE_TP_START);

        this.animationTimer = TP_START_TIME;

        this.phase = Phase.TP_START;

        if (this.krombul.level() instanceof ServerLevel serverLevel) {
            playRandomTeleportSound(serverLevel, this.krombul.getX(), this.krombul.getY() + this.krombul.getBbHeight() * 0.5D, this.krombul.getZ());
        }
    }

    private void tickTeleportStart() {
        freezeKrombul();

        this.animationTimer--;

        if (this.animationTimer > 0) {
            return;
        }

        performTeleport();

        this.krombul.setAnimState(KrombulEntity.STATE_TP_END);

        this.animationTimer = TP_END_TIME;

        this.phase = Phase.TP_END;
    }

    private void tickTeleportEnd() {
        freezeKrombul();

        this.animationTimer--;

        if (this.animationTimer > 0) {
            return;
        }

        this.krombul.setAnimState(KrombulEntity.STATE_NORMAL);
        this.nextTeleportAllowedTick = this.krombul.tickCount + TELEPORT_COOLDOWN;
        this.phase = Phase.IDLE;
    }

    private void freezeKrombul() {
        this.krombul.getNavigation().stop();
        this.krombul.setDeltaMovement(Vec3.ZERO);

        if (this.targetPlayer != null) {
            this.krombul.getLookControl().setLookAt(this.targetPlayer, 30.0F, 30.0F);
        }
    }

    private void performTeleport() {
        if (this.targetPlayer == null) {
            return;
        }

        ServerPlayer player = this.targetPlayer;
        ServerLevel level = player.serverLevel();
        Vec3 destination = findSafeTeleportPosition(level, player);

        if (destination == null) {
            return;
        }


        spawnTeleportBurst(level, player.getX(), player.getY() + 1.0D, player.getZ(), 45);
        player.stopRiding();
        player.teleportTo(level, destination.x, destination.y, destination.z, player.getYRot(), player.getXRot());

        player.fallDistance = 0.0F;


        double angle = this.krombul.getRandom().nextDouble() * Math.PI * 2.0D;

        double krombulX = destination.x + Math.cos(angle) * 2.0D;
        double krombulZ = destination.z + Math.sin(angle) * 2.0D;
        double krombulY = destination.y + 1.5D;

        this.krombul.teleportTo(krombulX, krombulY, krombulZ);

        this.krombul.setDeltaMovement(Vec3.ZERO);
        this.krombul.fallDistance = 0.0F;


        spawnTeleportBurst(level, destination.x, destination.y + 1.0D, destination.z, 65);

        playRandomTeleportSound(level, destination.x, destination.y + 1.0D, destination.z);
    }

    private void playRandomTeleportSound(ServerLevel level, double x, double y, double z) {
        SoundEvent[] sounds = {ModSounds.ENTROPY1.get(), ModSounds.ENTROPY2.get(), ModSounds.ENTROPY3.get()};

        SoundEvent selectedSound = sounds[this.krombul.getRandom().nextInt(sounds.length)];

        level.playSound(null, x, y, z, selectedSound, SoundSource.NEUTRAL, 1.0F, 0.9F + this.krombul.getRandom().nextFloat() * 0.2F);
    }

    @Nullable
    private Vec3 findSafeTeleportPosition(ServerLevel level, ServerPlayer player) {
        for (int attempt = 0; attempt < TELEPORT_ATTEMPTS; attempt++) {

            double angle = this.krombul.getRandom().nextDouble() * Math.PI * 2.0D;
            double distance = 8.0D + Math.sqrt(this.krombul.getRandom().nextDouble()) * (TELEPORT_RADIUS - 8.0D);

            int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

            BlockPos feetPos = new BlockPos(x, y, z);

            if (!level.getWorldBorder().isWithinBounds(feetPos)) {
                continue;
            }

            if (!isSafeForPlayer(level, feetPos)) {
                continue;
            }

            return Vec3.atBottomCenterOf(feetPos);
        }

        return null;
    }

    private boolean isSafeForPlayer(ServerLevel level, BlockPos feetPos) {
        if (feetPos.getY() <= level.getMinBuildHeight() + 1) {
            return false;
        }

        if (feetPos.getY() >= level.getMaxBuildHeight() - 2) {
            return false;
        }

        BlockPos groundPos = feetPos.below();
        BlockPos headPos = feetPos.above();
        BlockState groundState = level.getBlockState(groundPos);
        BlockState feetState = level.getBlockState(feetPos);
        BlockState headState = level.getBlockState(headPos);

        if (!groundState.isFaceSturdy(level, groundPos, Direction.UP)) {
            return false;
        }

        if (!feetState.getCollisionShape(level, feetPos).isEmpty()) {
            return false;
        }

        if (!headState.getCollisionShape(level, headPos).isEmpty()) {
            return false;
        }

        return feetState.getFluidState().isEmpty() && headState.getFluidState().isEmpty();
    }

    private void spawnTeleportBurst(ServerLevel level, double x, double y, double z, int count) {
        level.sendParticles(Oasiso.PURPLE_STARS.get(), x, y, z, count, 0.8D, 1.0D, 0.8D, 0.12D);
    }

    @Nullable
    private ServerPlayer findNearestPlayer() {
        if (!(this.krombul.level() instanceof ServerLevel level)) {
            return null;
        }

        List<ServerPlayer> nearbyPlayers = level.getEntitiesOfClass(ServerPlayer.class, this.krombul.getBoundingBox().inflate(DETECTION_RADIUS), player -> player.isAlive() && !player.isSpectator() && player.distanceToSqr(this.krombul) <= DETECTION_RADIUS * DETECTION_RADIUS);

        ServerPlayer nearest = null;
        double nearestDistanceSqr = Double.MAX_VALUE;

        for (ServerPlayer player : nearbyPlayers) {
            double distanceSqr = player.distanceToSqr(this.krombul);

            if (distanceSqr < nearestDistanceSqr) {
                nearestDistanceSqr = distanceSqr;
                nearest = player;
            }
        }

        return nearest;
    }

    private double horizontalDistanceSqr(ServerPlayer player) {
        double x = player.getX() - this.krombul.getX();
        double z = player.getZ() - this.krombul.getZ();
        return x * x + z * z;
    }

    private void resetProximityTracking() {
        this.trackedPlayerId = null;
        this.proximityStartTick = -1;
        this.candidatePlayer = null;
    }

    @Override
    public void stop() {
        if (this.phase != Phase.IDLE) {
            this.krombul.setAnimState(KrombulEntity.STATE_NORMAL);
        }

        this.krombul.getNavigation().stop();

        this.targetPlayer = null;
        this.phase = Phase.IDLE;

        resetProximityTracking();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private enum Phase {
        IDLE, APPROACH, TP_START, TP_END
    }
}