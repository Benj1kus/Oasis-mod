package com.benji.oasiso.common.glove;

import com.benji.oasiso.ModSounds;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.EntropyPhysicsBlockEntity;
import com.benji.oasiso.common.item.EntropyChestplateGloveItem;
import com.benji.oasiso.network.EntropyGrappleStateS2CPacket;
import com.benji.oasiso.network.ModMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EntropyGrappleManager {

    public static final double MAX_RANGE = 30.0D;

    private static final double STOP_DISTANCE = 1.70D;
    private static final double HARD_BREAK_DISTANCE = MAX_RANGE + 8.0D;

    private static final int MAX_PIVOTS = 8;
    private static final int PIVOT_MIN_LIFE_TICKS = 3;
    private static final int PIVOT_UNWRAP_CONFIRM_TICKS = 2;

    private static final double PIVOT_CLEARANCE = 0.075D;
    private static final double PIVOT_ENDPOINT_TOLERANCE = 0.18D;
    private static final double MIN_PIVOT_SPACING = 0.14D;

    private static final int MIN_LAUNCH_TICKS = 4;
    private static final int MAX_LAUNCH_TICKS = 11;

    private static final int MIN_RETRACT_TICKS = 5;
    private static final int MAX_RETRACT_TICKS = 10;

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private EntropyGrappleManager() {
    }

    public enum State {
        LAUNCHING, ATTACHED, RETRACTING
    }

    public static void toggleMode(ServerPlayer player, InteractionHand hand) {
        ItemStack glove = player.getItemInHand(hand);

        if (!(glove.getItem() instanceof EntropyChestplateGloveItem)) {
            return;
        }

        boolean enabled = !EntropyChestplateGloveItem.isGrappleMode(glove);

        if (enabled) {
            if (EntropyChestplateGloveItem.isFillMode(glove)) {
                EntropyGloveFillManager.disableForOtherMode(player, hand);
            }

            releaseHeldPhysicsBlock(player, glove);

            EntropyChestplateGloveItem.setGrappleMode(glove, true);
        } else {
            EntropyChestplateGloveItem.setGrappleMode(glove, false);

            Session session = SESSIONS.get(player.getUUID());
            if (session != null && session.state != State.RETRACTING) {
                beginRetract(player.serverLevel(), player, session, false);
            }
        }

        syncInventory(player);
        player.playNotifySound(ModSounds.CASER_DEFAULT.get(), SoundSource.PLAYERS, 0.72F, enabled ? 1.08F : 0.92F);
    }

    public static void use(ServerPlayer player, InteractionHand hand) {
        ItemStack glove = player.getItemInHand(hand);

        if (!(glove.getItem() instanceof EntropyChestplateGloveItem) || !EntropyChestplateGloveItem.isGrappleMode(glove)) {

            return;
        }

        Session existing = SESSIONS.get(player.getUUID());
        if (existing != null) {
            if (existing.state != State.RETRACTING) {
                beginRetract(player.serverLevel(), player, existing, true);
            }

            return;
        }

        launch(player, hand);
    }

    public static void disableForOtherMode(ServerPlayer player, InteractionHand hand) {
        ItemStack glove = player.getItemInHand(hand);

        if (!(glove.getItem() instanceof EntropyChestplateGloveItem)) {
            return;
        }

        EntropyChestplateGloveItem.setGrappleMode(glove, false);
        Session session = SESSIONS.get(player.getUUID());

        if (session != null && session.state != State.RETRACTING) {
            beginRetract(player.serverLevel(), player, session, false);
        }

        syncInventory(player);
    }

    private static void launch(ServerPlayer player, InteractionHand hand) {
        ServerLevel level = player.serverLevel();

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 traceEnd = eye.add(look.scale(MAX_RANGE));

        BlockHitResult hit = level.clip(new ClipContext(eye, traceEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        Vec3 normal = new Vec3(hit.getDirection().getStepX(), hit.getDirection().getStepY(), hit.getDirection().getStepZ());
        Vec3 anchor = hit.getLocation().add(normal.scale(0.035D));

        double distance = eye.distanceTo(anchor);

        if (distance > MAX_RANGE + 0.15D) {
            return;
        }

        int launchTicks = Mth.clamp(3 + Mth.ceil(distance * 0.38D), MIN_LAUNCH_TICKS, MAX_LAUNCH_TICKS);
        Session session = new Session(player.getUUID(), level.dimension(), hand, anchor, hit.getBlockPos().immutable(), State.LAUNCHING, launchTicks);
        SESSIONS.put(player.getUUID(), session);
        broadcastState(level, session);
    }

    private static void attach(ServerLevel level, ServerPlayer player, Session session) {
        session.state = State.ATTACHED;
        session.elapsed = 0;
        session.duration = 0;
        session.pullTicks = 0;
        session.attachDistance = player.getEyePosition().distanceTo(session.anchor);
        session.pivots.clear();
        session.unwrapClearTicks = 0;

        player.playNotifySound(ModSounds.GLOV_BOUNCE.get(), SoundSource.PLAYERS, 0.78F, 1.10F);

        broadcastState(level, session);
    }

    private static void beginRetract(ServerLevel level, ServerPlayer player, Session session, boolean playSound) {
        if (session.state == State.RETRACTING) {
            return;
        }

        double distance = player.getEyePosition().distanceTo(session.anchor);

        session.state = State.RETRACTING;
        session.elapsed = 0;
        session.duration = Mth.clamp(4 + Mth.ceil(distance * 0.23D), MIN_RETRACT_TICKS, MAX_RETRACT_TICKS);

        if (playSound) {
            player.playNotifySound(ModSounds.GLOV_BOUNCE.get(), SoundSource.PLAYERS, 0.72F, 0.92F);
        }

        broadcastState(level, session);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide || !(event.level instanceof ServerLevel level) || SESSIONS.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, Session>> iterator = SESSIONS.entrySet().iterator();

        while (iterator.hasNext()) {
            Session session = iterator.next().getValue();

            if (session.dimension != level.dimension()) {
                continue;
            }

            ServerPlayer player = level.getServer().getPlayerList().getPlayer(session.playerId);

            if (player == null || player.serverLevel() != level) {
                broadcastClear(level, session);
                iterator.remove();
                continue;
            }

            if (session.state == State.RETRACTING) {
                session.elapsed++;

                if (session.elapsed >= session.duration) {
                    broadcastClear(level, session);
                    iterator.remove();
                }

                continue;
            }

            ItemStack glove = player.getItemInHand(session.hand);

            if (!(glove.getItem() instanceof EntropyChestplateGloveItem) || !EntropyChestplateGloveItem.isGrappleMode(glove) || !player.isAlive()) {
                beginRetract(level, player, session, false);

                continue;
            }

            if (!isAnchorStillValid(level, session.anchorBlock)) {
                beginRetract(level, player, session, true);

                continue;
            }

            if (session.state == State.LAUNCHING) {
                session.elapsed++;

                if (session.elapsed >= session.duration) {
                    attach(level, player, session);
                }

                continue;
            }

            tickAttached(level, player, session);
        }
    }

    private static void tickAttached(ServerLevel level, ServerPlayer player, Session session) {
        Vec3 playerPoint = player.getEyePosition();

        updatePivots(level, playerPoint, session);

        Vec3 pullTarget = currentPullTarget(session);
        Vec3 towardTarget = pullTarget.subtract(playerPoint);

        double targetDistance = towardTarget.length();
        double directAnchorDistance = playerPoint.distanceTo(session.anchor);

        if (directAnchorDistance > HARD_BREAK_DISTANCE) {
            beginRetract(level, player, session, true);
            return;
        }

        if (targetDistance < 1.0E-5D) {
            return;
        }

        Vec3 direction = towardTarget.scale(1.0D / targetDistance);
        Vec3 velocity = player.getDeltaMovement();

        if (targetDistance <= STOP_DISTANCE) {
            double radial = velocity.dot(direction);

            if (radial < -0.12D) {
                velocity = velocity.add(direction.scale(-radial * 0.48D));

                player.setDeltaMovement(velocity);
                player.hurtMarked = true;
            }

            player.fallDistance = 0.0F;
            return;
        }

        session.pullTicks++;

        double attachFactor = Mth.clamp((session.attachDistance - 3.0D) / (MAX_RANGE - 3.0D), 0.0D, 1.0D);
        int rampTicks = 4 + Mth.floor(attachFactor * 12.0D);

        double ramp = Mth.clamp(session.pullTicks / (double) rampTicks, 0.0D, 1.0D);
        double acceleration = Mth.lerp(ramp, 0.065D, 0.285D);

        double ropePathDistance = currentRopePathDistance(playerPoint, session);
        double remainingFactor = Mth.clamp((ropePathDistance - STOP_DISTANCE) / (MAX_RANGE - STOP_DISTANCE), 0.0D, 1.0D);
        double maxRadialSpeed = Mth.lerp(remainingFactor, 0.72D, 1.95D);

        velocity = velocity.scale(0.985D).add(direction.scale(acceleration));

        double radialSpeed = velocity.dot(direction);

        if (radialSpeed > maxRadialSpeed) {
            velocity = velocity.add(direction.scale(maxRadialSpeed - radialSpeed));
        }

        player.setDeltaMovement(velocity);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
    }

    private static void updatePivots(ServerLevel level, Vec3 playerPoint, Session session) {
        for (Pivot pivot : session.pivots) {
            pivot.age++;
        }

        if (!session.pivots.isEmpty()) {
            Pivot newest = session.pivots.get(session.pivots.size() - 1);

            Vec3 previousTarget;
            BlockPos previousTargetBlock;

            if (session.pivots.size() >= 2) {
                Pivot previous = session.pivots.get(session.pivots.size() - 2);

                previousTarget = previous.position;
                previousTargetBlock = previous.blockPos;
            } else {
                previousTarget = session.anchor;
                previousTargetBlock = session.anchorBlock;
            }

            if (newest.age >= PIVOT_MIN_LIFE_TICKS && segmentIsClear(level, playerPoint, previousTarget, previousTargetBlock)) {

                session.unwrapClearTicks++;

                if (session.unwrapClearTicks >= PIVOT_UNWRAP_CONFIRM_TICKS) {
                    session.pivots.remove(session.pivots.size() - 1);
                    session.unwrapClearTicks = 0;
                }

            } else {
                session.unwrapClearTicks = 0;
            }
        }
        if (session.pivots.size() >= MAX_PIVOTS) {
            return;
        }

        Vec3 currentTarget = currentPullTarget(session);
        BlockPos currentTargetBlock = currentPullTargetBlock(session);

        BlockHitResult obstruction = firstObstruction(level, playerPoint, currentTarget, currentTargetBlock);

        if (obstruction == null) {
            return;
        }

        if (obstruction.getLocation().distanceToSqr(playerPoint) < 0.20D * 0.20D) {

            return;
        }

        Vec3 pivotPosition = findBestPivot(level, playerPoint, currentTarget, currentTargetBlock, obstruction);

        if (pivotPosition == null) {
            return;
        }

        if (!session.pivots.isEmpty()) {
            Pivot newest = session.pivots.get(session.pivots.size() - 1);

            if (newest.position.distanceToSqr(pivotPosition) < MIN_PIVOT_SPACING * MIN_PIVOT_SPACING) {

                return;
            }
        }

        session.pivots.add(new Pivot(pivotPosition, obstruction.getBlockPos().immutable()));

        session.unwrapClearTicks = 0;
    }

    private static Vec3 currentPullTarget(Session session) {
        if (session.pivots.isEmpty()) {
            return session.anchor;
        }

        return session.pivots.get(session.pivots.size() - 1).position;
    }

    private static BlockPos currentPullTargetBlock(Session session) {
        if (session.pivots.isEmpty()) {
            return session.anchorBlock;
        }

        return session.pivots.get(session.pivots.size() - 1).blockPos;
    }

    private static double currentRopePathDistance(Vec3 playerPoint, Session session) {
        double total = 0.0D;
        Vec3 cursor = playerPoint;

        for (int i = session.pivots.size() - 1; i >= 0; i--) {

            Vec3 pivot = session.pivots.get(i).position;

            total += cursor.distanceTo(pivot);
            cursor = pivot;
        }

        total += cursor.distanceTo(session.anchor);

        return total;
    }

    @Nullable
    private static BlockHitResult firstObstruction(ServerLevel level, Vec3 from, Vec3 to, @Nullable BlockPos allowedEndBlock) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();

        if (length < 0.05D) {
            return null;
        }

        Vec3 direction = delta.scale(1.0D / length);
        Vec3 rayStart = from.add(direction.scale(0.02D));
        Vec3 rayEnd = to.subtract(direction.scale(0.02D));

        BlockHitResult hit = level.clip(new ClipContext(rayStart, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));

        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        if (allowedEndBlock != null && hit.getBlockPos().equals(allowedEndBlock) && hit.getLocation().distanceToSqr(to) <= PIVOT_ENDPOINT_TOLERANCE * PIVOT_ENDPOINT_TOLERANCE) {

            return null;
        }

        if (hit.getLocation().distanceToSqr(to) <= 0.08D * 0.08D) {

            return null;
        }

        return hit;
    }

    private static boolean segmentIsClear(ServerLevel level, Vec3 from, Vec3 to, @Nullable BlockPos allowedEndBlock) {
        return firstObstruction(level, from, to, allowedEndBlock) == null;
    }

    @Nullable
    private static Vec3 findBestPivot(ServerLevel level, Vec3 from, Vec3 target, @Nullable BlockPos targetBlock, BlockHitResult obstruction) {
        BlockPos blockPos = obstruction.getBlockPos();

        VoxelShape shape = level.getBlockState(blockPos).getCollisionShape(level, blockPos);

        if (shape.isEmpty()) {
            return null;
        }

        Vec3 hit = obstruction.getLocation();

        List<Vec3> candidates = new ArrayList<>();

        for (AABB localBox : shape.toAabbs()) {
            AABB worldBox = localBox.move(blockPos.getX(), blockPos.getY(), blockPos.getZ());

            if (distanceToAabbSqr(hit, worldBox) > 0.30D * 0.30D) {

                continue;
            }

            AABB expanded = worldBox.inflate(PIVOT_CLEARANCE);
            addEdgeCandidates(candidates, expanded, hit);
        }

        if (candidates.isEmpty()) {
            for (AABB localBox : shape.toAabbs()) {
                AABB expanded = localBox.move(blockPos.getX(), blockPos.getY(), blockPos.getZ()).inflate(PIVOT_CLEARANCE);

                addEdgeCandidates(candidates, expanded, hit);
            }
        }

        Vec3 best = null;
        double bestScore = Double.POSITIVE_INFINITY;

        for (Vec3 candidate : candidates) {
            if (candidate.distanceToSqr(from) < 0.12D * 0.12D) {

                continue;
            }

            if (!segmentIsClear(level, from, candidate, blockPos)) {

                continue;
            }

            boolean clearToTarget = segmentIsClear(level, candidate, target, targetBlock);
            double score = from.distanceTo(candidate) + candidate.distanceTo(target) + hit.distanceTo(candidate) * 0.18D;

            if (!clearToTarget) {
                score += 12.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        return best;
    }

    private static void addEdgeCandidates(List<Vec3> candidates, AABB box, Vec3 near) {
        double x = Mth.clamp(near.x, box.minX, box.maxX);
        double y = Mth.clamp(near.y, box.minY, box.maxY);
        double z = Mth.clamp(near.z, box.minZ, box.maxZ);

        candidates.add(new Vec3(x, box.minY, box.minZ));
        candidates.add(new Vec3(x, box.minY, box.maxZ));
        candidates.add(new Vec3(x, box.maxY, box.minZ));
        candidates.add(new Vec3(x, box.maxY, box.maxZ));

        candidates.add(new Vec3(box.minX, y, box.minZ));
        candidates.add(new Vec3(box.minX, y, box.maxZ));
        candidates.add(new Vec3(box.maxX, y, box.minZ));
        candidates.add(new Vec3(box.maxX, y, box.maxZ));

        candidates.add(new Vec3(box.minX, box.minY, z));
        candidates.add(new Vec3(box.minX, box.maxY, z));
        candidates.add(new Vec3(box.maxX, box.minY, z));
        candidates.add(new Vec3(box.maxX, box.maxY, z));

        candidates.add(new Vec3(box.minX, box.minY, box.minZ));
        candidates.add(new Vec3(box.minX, box.minY, box.maxZ));
        candidates.add(new Vec3(box.minX, box.maxY, box.minZ));
        candidates.add(new Vec3(box.minX, box.maxY, box.maxZ));

        candidates.add(new Vec3(box.maxX, box.minY, box.minZ));
        candidates.add(new Vec3(box.maxX, box.minY, box.maxZ));
        candidates.add(new Vec3(box.maxX, box.maxY, box.minZ));
        candidates.add(new Vec3(box.maxX, box.maxY, box.maxZ));
    }

    private static double distanceToAabbSqr(Vec3 point, AABB box) {
        double dx = Math.max(Math.max(box.minX - point.x, 0.0D), point.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - point.y, 0.0D), point.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - point.z, 0.0D), point.z - box.maxZ);

        return dx * dx + dy * dy + dz * dz;
    }

    private static boolean isAnchorStillValid(ServerLevel level, BlockPos anchorBlock) {
        return !level.getBlockState(anchorBlock).isAir() && !level.getBlockState(anchorBlock).getCollisionShape(level, anchorBlock).isEmpty();
    }

    private static void releaseHeldPhysicsBlock(ServerPlayer player, ItemStack glove) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        EntropyPhysicsBlockEntity held = EntropyChestplateGloveItem.resolveHeldBlock(level, glove);

        if (held != null && held.isHeldBy(player)) {
            held.releaseHolder(player);
        }

        EntropyChestplateGloveItem.clearHeldBlock(glove);
    }

    private static void broadcastState(ServerLevel level, Session session) {
        EntropyGrappleStateS2CPacket packet = EntropyGrappleStateS2CPacket.state(session.playerId, session.hand, session.state, session.anchor, session.duration);

        broadcastNear(level, session.anchor, packet);
    }

    private static void broadcastClear(ServerLevel level, Session session) {
        broadcastNear(level, session.anchor, EntropyGrappleStateS2CPacket.clear(session.playerId));
    }

    private static void broadcastNear(ServerLevel level, Vec3 center, EntropyGrappleStateS2CPacket packet) {
        double maxDistanceSqr = 128.0D * 128.0D;

        for (ServerPlayer watcher : level.players()) {
            if (watcher.position().distanceToSqr(center) > maxDistanceSqr) {
                continue;
            }
            ModMessages.sendToPlayer(watcher, packet);
        }
    }

    private static void syncInventory(ServerPlayer player) {
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }

    private static final class Pivot {
        private final Vec3 position;
        private final BlockPos blockPos;
        private int age;

        private Pivot(Vec3 position, BlockPos blockPos) {
            this.position = position;
            this.blockPos = blockPos;
        }
    }

    private static final class Session {
        private final UUID playerId;
        private final ResourceKey<Level> dimension;
        private final InteractionHand hand;
        private final Vec3 anchor;
        private final BlockPos anchorBlock;

        private State state;
        private int elapsed;
        private int duration;

        private int pullTicks;
        private double attachDistance;

        private final List<Pivot> pivots = new ArrayList<>();
        private int unwrapClearTicks;

        private Session(UUID playerId, ResourceKey<Level> dimension, InteractionHand hand, Vec3 anchor, BlockPos anchorBlock, State state, int duration) {
            this.playerId = playerId;
            this.dimension = dimension;
            this.hand = hand;
            this.anchor = anchor;
            this.anchorBlock = anchorBlock;
            this.state = state;
            this.duration = duration;
        }
    }
}
