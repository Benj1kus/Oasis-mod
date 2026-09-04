package com.benji.oasiso.client.event;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.network.EntropyGrappleStateS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EntropyGrappleClientState {

    private static final int SEGMENTS = 36;
    private static final int CONSTRAINT_ITERATIONS = 10;

    private static final double ROPE_COLLISION_RADIUS = 0.055D;
    private static final double COLLISION_EPSILON = 0.0025D;
    private static final double SURFACE_FRICTION = 0.82D;

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private EntropyGrappleClientState() {
    }

    public static void apply(UUID playerId, InteractionHand hand, EntropyGrappleStateS2CPacket.VisualState state, Vec3 anchor, int duration) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        if (state == EntropyGrappleStateS2CPacket.VisualState.CLEAR) {
            SESSIONS.remove(playerId);
            return;
        }

        Player player = findPlayer(minecraft.level, playerId);

        if (player == null) {
            return;
        }

        long now = minecraft.level.getGameTime();

        Session session = SESSIONS.get(playerId);

        if (session == null) {
            Vec3 start = getGloveOrigin(player, hand, 1.0F);

            session = new Session(playerId, hand, state, anchor, Math.max(1, duration), now, start);
            session.rope.seed(start, state == EntropyGrappleStateS2CPacket.VisualState.ATTACHED ? anchor : start);

            SESSIONS.put(playerId, session);

            return;
        }

        if (state == EntropyGrappleStateS2CPacket.VisualState.RETRACTING) {
            session.retractFrom = session.currentEnd;
        }

        session.hand = hand;
        session.state = state;
        session.anchor = anchor;
        session.duration = Math.max(1, duration);
        session.phaseStartTick = now;

        if (state == EntropyGrappleStateS2CPacket.VisualState.LAUNCHING) {
            session.launchOrigin = getGloveOrigin(player, hand, 1.0F);

            session.retractFrom = session.launchOrigin;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;

        if (level == null) {
            SESSIONS.clear();
            return;
        }

        long now = level.getGameTime();
        Iterator<Session> iterator = SESSIONS.values().iterator();

        while (iterator.hasNext()) {
            Session session = iterator.next();

            Player player = findPlayer(level, session.playerId);

            if (player == null) {
                iterator.remove();
                continue;
            }

            Vec3 start = getGloveOrigin(player, session.hand, 1.0F);
            Vec3 end = getVisualEnd(session, start, now);

            session.currentEnd = end;
            session.rope.step(level, start, end, session.state, now);

            if (session.state == EntropyGrappleStateS2CPacket.VisualState.RETRACTING && now - session.phaseStartTick > session.duration + 2L) {

                iterator.remove();
            }
        }
    }

    private static Vec3 getVisualEnd(Session session, Vec3 currentStart, long now) {
        double rawProgress = Mth.clamp((now - session.phaseStartTick) / (double) Math.max(1, session.duration), 0.0D, 1.0D);

        return switch (session.state) {
            case ATTACHED -> session.anchor;

            case LAUNCHING -> {
                double t = 1.0D - Math.pow(1.0D - rawProgress, 3.0D);

                Vec3 base = session.launchOrigin.lerp(session.anchor, t);

                double distance = session.launchOrigin.distanceTo(session.anchor);
                double arc = Math.sin(rawProgress * Math.PI) * Math.min(0.72D, 0.11D + distance * 0.028D);

                Vec3 direction = session.anchor.subtract(session.launchOrigin);
                Vec3 lateral = lateral(direction);

                double sideWhip = Math.sin(rawProgress * Math.PI * 2.0D) * Math.sin(rawProgress * Math.PI) * Math.min(0.22D, distance * 0.012D);
                yield base.add(0.0D, arc, 0.0D).add(lateral.scale(sideWhip));
            }

            case RETRACTING -> {

                double c1 = 1.70158D;
                double c3 = c1 + 1.0D;
                double x = rawProgress - 1.0D;

                double t = 1.0D + c3 * x * x * x + c1 * x * x;

                Vec3 base = session.retractFrom.lerp(currentStart, t);
                Vec3 direction = currentStart.subtract(session.retractFrom);
                Vec3 lateral = lateral(direction);

                double whip = Math.sin(rawProgress * Math.PI * 4.0D) * (1.0D - rawProgress) * 0.18D;

                yield base.add(lateral.scale(whip));
            }

            case CLEAR -> currentStart;
        };
    }

    public static Collection<Session> sessions() {
        return SESSIONS.values();
    }

    public static Player findPlayer(ClientLevel level, UUID uuid) {
        for (Player player : level.players()) {
            if (uuid.equals(player.getUUID())) {
                return player;
            }
        }

        return null;
    }

    public static Vec3 getGloveOrigin(Player player, InteractionHand hand, float partialTick) {
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick).normalize();

        Vec3 horizontalForward = new Vec3(look.x, 0.0D, look.z);

        if (horizontalForward.lengthSqr() < 1.0E-5D) {
            double yaw = Math.toRadians(player.getYRot());

            horizontalForward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        } else {
            horizontalForward = horizontalForward.normalize();
        }

        Vec3 right = new Vec3(-horizontalForward.z, 0.0D, horizontalForward.x);
        net.minecraft.world.entity.HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? net.minecraft.world.entity.HumanoidArm.LEFT : net.minecraft.world.entity.HumanoidArm.RIGHT);

        double side = arm == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1.0D : -1.0D;
        return eye.add(look.scale(0.28D)).add(right.scale(0.30D * side)).add(0.0D, -0.34D, 0.0D);
    }

    private static Vec3 lateral(Vec3 direction) {
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);

        if (horizontal.lengthSqr() < 1.0E-6D) {
            return new Vec3(1.0D, 0.0D, 0.0D);
        }

        horizontal = horizontal.normalize();

        return new Vec3(-horizontal.z, 0.0D, horizontal.x);
    }

    public static final class Session {
        private final UUID playerId;
        private InteractionHand hand;
        private EntropyGrappleStateS2CPacket.VisualState state;
        private Vec3 anchor;
        private int duration;
        private long phaseStartTick;

        private Vec3 launchOrigin;
        private Vec3 retractFrom;
        private Vec3 currentEnd;

        private final Rope rope = new Rope();

        private Session(UUID playerId, InteractionHand hand, EntropyGrappleStateS2CPacket.VisualState state, Vec3 anchor, int duration, long phaseStartTick, Vec3 start) {
            this.playerId = playerId;
            this.hand = hand;
            this.state = state;
            this.anchor = anchor;
            this.duration = duration;
            this.phaseStartTick = phaseStartTick;

            this.launchOrigin = start;
            this.retractFrom = start;
            this.currentEnd = start;
        }

        public UUID playerId() {
            return playerId;
        }

        public InteractionHand hand() {
            return hand;
        }

        public EntropyGrappleStateS2CPacket.VisualState state() {
            return state;
        }

        public Vec3 currentEnd() {
            return currentEnd;
        }

        public Rope rope() {
            return rope;
        }
    }

    public static final class Rope {
        private final Vec3[] points = new Vec3[SEGMENTS + 1];

        private final Vec3[] previous = new Vec3[SEGMENTS + 1];

        private final Vec3[] renderPrevious = new Vec3[SEGMENTS + 1];

        private boolean initialized;

        private void seed(Vec3 start, Vec3 end) {
            double distance = start.distanceTo(end);

            for (int i = 0; i <= SEGMENTS; i++) {
                double t = i / (double) SEGMENTS;

                double sag = Math.sin(Math.PI * t) * Math.min(0.45D, distance * 0.06D);

                Vec3 point = start.lerp(end, t).add(0.0D, -sag, 0.0D);

                points[i] = point;
                previous[i] = point;
                renderPrevious[i] = point;
            }

            initialized = true;
        }

        private void step(ClientLevel level, Vec3 start, Vec3 end, EntropyGrappleStateS2CPacket.VisualState state, long gameTime) {
            if (!initialized || points[0] == null || points[SEGMENTS] == null || points[0].distanceToSqr(start) > 256.0D || points[SEGMENTS].distanceToSqr(end) > 256.0D) {

                seed(start, end);
            }

            for (int i = 0; i <= SEGMENTS; i++) {
                renderPrevious[i] = points[i];
            }

            double damping = state == EntropyGrappleStateS2CPacket.VisualState.ATTACHED ? 0.955D : 0.972D;
            double gravity = state == EntropyGrappleStateS2CPacket.VisualState.ATTACHED ? 0.018D : 0.011D;

            for (int i = 1; i < SEGMENTS; i++) {
                Vec3 current = points[i];
                Vec3 old = previous[i];

                Vec3 velocity = current.subtract(old).scale(damping);

                previous[i] = current;

                double envelope = Math.sin(Math.PI * i / SEGMENTS);
                double microWobble = Math.sin(gameTime * 0.38D + i * 1.73D) * 0.0028D * envelope;

                points[i] = current.add(velocity).add(microWobble, -gravity, -microWobble * 0.55D);
            }


            collideRopeWithWorld(level);

            double distance = Math.max(0.01D, start.distanceTo(end));

            double slack = switch (state) {
                case LAUNCHING -> 1.13D;
                case ATTACHED -> 1.045D;
                case RETRACTING -> 1.16D;
                case CLEAR -> 1.0D;
            };

            double segmentLength = Math.max(0.045D, distance * slack / SEGMENTS);

            for (int iteration = 0; iteration < CONSTRAINT_ITERATIONS; iteration++) {

                points[0] = start;
                points[SEGMENTS] = end;

                for (int i = 0; i < SEGMENTS; i++) {
                    Vec3 first = points[i];
                    Vec3 second = points[i + 1];

                    Vec3 delta = second.subtract(first);
                    double length = delta.length();

                    if (length < 1.0E-7D) {
                        continue;
                    }

                    double error = (length - segmentLength) / length;
                    Vec3 correction = delta.scale(error * 0.5D);

                    if (i != 0) {
                        points[i] = points[i].add(correction);
                    }

                    if (i + 1 != SEGMENTS) {
                        points[i + 1] = points[i + 1].subtract(correction);
                    }
                }

                if ((iteration & 1) == 1) {
                    collideRopeWithWorld(level);
                }
            }

            collideRopeWithWorld(level);

            points[0] = start;
            points[SEGMENTS] = end;
        }

        private void collideRopeWithWorld(ClientLevel level) {
            for (int i = 1; i < SEGMENTS; i++) {
                Vec3 original = points[i];

                CollisionResult collision = resolvePointCollision(level, original);

                if (!collision.collided()) {
                    continue;
                }

                Vec3 resolved = collision.point();
                Vec3 normal = collision.normal();

                Vec3 velocity = original.subtract(previous[i]);

                double normalVelocity = velocity.dot(normal);

                if (normalVelocity < 0.0D) {
                    velocity = velocity.subtract(normal.scale(normalVelocity));
                }

                velocity = velocity.scale(SURFACE_FRICTION);

                points[i] = resolved;
                previous[i] = resolved.subtract(velocity);
            }
        }

        private static CollisionResult resolvePointCollision(ClientLevel level, Vec3 source) {
            Vec3 point = source;
            Vec3 lastNormal = Vec3.ZERO;
            boolean collided = false;

            for (int pass = 0; pass < 3; pass++) {
                CollisionResult best = findDeepestCollision(level, point);

                if (!best.collided()) {
                    break;
                }

                collided = true;
                point = best.point();
                lastNormal = best.normal();
            }

            return new CollisionResult(point, lastNormal, collided);
        }

        private static CollisionResult findDeepestCollision(ClientLevel level, Vec3 point) {
            int minX = Mth.floor(point.x - ROPE_COLLISION_RADIUS);
            int maxX = Mth.floor(point.x + ROPE_COLLISION_RADIUS);

            int minY = Mth.floor(point.y - ROPE_COLLISION_RADIUS);
            int maxY = Mth.floor(point.y + ROPE_COLLISION_RADIUS);

            int minZ = Mth.floor(point.z - ROPE_COLLISION_RADIUS);
            int maxZ = Mth.floor(point.z + ROPE_COLLISION_RADIUS);

            net.minecraft.core.BlockPos.MutableBlockPos mutable = new net.minecraft.core.BlockPos.MutableBlockPos();

            CollisionResult best = CollisionResult.NONE;
            double bestDepth = Double.POSITIVE_INFINITY;

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {

                        mutable.set(x, y, z);

                        BlockState state = level.getBlockState(mutable);

                        if (state.isAir()) {
                            continue;
                        }

                        VoxelShape shape = state.getCollisionShape(level, mutable);

                        if (shape.isEmpty()) {
                            continue;
                        }

                        for (AABB localBox : shape.toAabbs()) {
                            AABB box = localBox.move(x, y, z).inflate(ROPE_COLLISION_RADIUS);

                            if (!box.contains(point)) {
                                continue;
                            }

                            double toMinX = point.x - box.minX;
                            double toMaxX = box.maxX - point.x;

                            double toMinY = point.y - box.minY;
                            double toMaxY = box.maxY - point.y;

                            double toMinZ = point.z - box.minZ;
                            double toMaxZ = box.maxZ - point.z;

                            double depth = toMinX;

                            Vec3 resolved = new Vec3(box.minX - COLLISION_EPSILON, point.y, point.z);

                            Vec3 normal = new Vec3(-1.0D, 0.0D, 0.0D);

                            if (toMaxX < depth) {
                                depth = toMaxX;

                                resolved = new Vec3(box.maxX + COLLISION_EPSILON, point.y, point.z);

                                normal = new Vec3(1.0D, 0.0D, 0.0D);
                            }

                            if (toMinY < depth) {
                                depth = toMinY;

                                resolved = new Vec3(point.x, box.minY - COLLISION_EPSILON, point.z);

                                normal = new Vec3(0.0D, -1.0D, 0.0D);
                            }

                            if (toMaxY < depth) {
                                depth = toMaxY;

                                resolved = new Vec3(point.x, box.maxY + COLLISION_EPSILON, point.z);

                                normal = new Vec3(0.0D, 1.0D, 0.0D);
                            }

                            if (toMinZ < depth) {
                                depth = toMinZ;

                                resolved = new Vec3(point.x, point.y, box.minZ - COLLISION_EPSILON);

                                normal = new Vec3(0.0D, 0.0D, -1.0D);
                            }

                            if (toMaxZ < depth) {
                                depth = toMaxZ;

                                resolved = new Vec3(point.x, point.y, box.maxZ + COLLISION_EPSILON);

                                normal = new Vec3(0.0D, 0.0D, 1.0D);
                            }

                            if (depth < bestDepth) {
                                bestDepth = depth;

                                best = new CollisionResult(resolved, normal, true);
                            }
                        }
                    }
                }
            }

            return best;
        }

        public Vec3 getPoint(int index, float partialTick) {
            index = Mth.clamp(index, 0, SEGMENTS);

            Vec3 previousPoint = renderPrevious[index] != null ? renderPrevious[index] : points[index];

            Vec3 point = points[index];

            if (point == null) {
                return Vec3.ZERO;
            }

            return previousPoint == null ? point : previousPoint.lerp(point, partialTick);
        }

        public int size() {
            return SEGMENTS + 1;
        }

        private record CollisionResult(Vec3 point, Vec3 normal, boolean collided) {
            private static final CollisionResult NONE = new CollisionResult(Vec3.ZERO, Vec3.ZERO, false);
        }
    }

}
