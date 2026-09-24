package com.benji.oasiso.client.model;

import com.benji.oasiso.common.entity.EntropySpiderEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;

import java.util.Map;
import java.util.WeakHashMap;

public final class EntropySpiderIK {

    private static final float BODY_LIFT = 2.5F;
    private static final float FOOT_SPREAD = 1.65F;
    private static final float FRONT_BACK_SPREAD = 1.35F;
    private static final float STEP_DISTANCE = .50F;
    private static final float STEP_HEIGHT = .32F;
    private static final float STEP_TICKS = 4F;
    private static final String[] SUFFIX = {"right_front", "right_mid", "right_back", "left_front", "left_mid", "left_back"};
    private static final String[] MARKER = {"right_front_stepmarker", "right_front_mid_stepmarker", "right_front_back_stepmarker", "left_front_stepmarker", "left_front_mid_stepmarker", "left_front_back_stepmarker"};
    private static final String[] PREFIX = {"leg_", "p_", "pp_", "ppp_", "pppp_"};
    private final Map<EntropySpiderEntity, State> states = new WeakHashMap<>();

    public void apply(EntropySpiderModel model, EntropySpiderEntity entity, float partial) {
        CoreGeoBone body = model.getAnimationProcessor().getBone("body_center_mass");
        if (body == null) return;
        Rig[] rigs = new Rig[6];
        for (int i = 0; i < 6; i++) {
            rigs[i] = rig(model, i);
            if (rigs[i] == null) return;
        }
        Vec3 base = new Vec3(Mth.lerp(partial, entity.xo, entity.getX()), Mth.lerp(partial, entity.yo, entity.getY()) + .01, Mth.lerp(partial, entity.zo, entity.getZ()));
        float yaw = Mth.rotLerp(partial, entity.yBodyRotO, entity.yBodyRot);
        double angle = Math.toRadians(180 - yaw);
        State state = states.computeIfAbsent(entity, e -> new State());
        float now = entity.tickCount + partial;
        boolean reset = state.lastBase == null || state.lastBase.distanceToSqr(base) > 16 || now < state.lastTime;
        float dt = reset ? 0 : Mth.clamp(now - state.lastTime, 0, 2);
        Vec3 speed = reset || dt < .0001 ? Vec3.ZERO : base.subtract(state.lastBase).scale(1.0 / dt);

        if (speed.lengthSqr() > 9.0) {
            speed = speed.normalize().scale(3.0);
        }

        Vector3f center = pivot(body);

        if (reset || state.probeTick != entity.tickCount) {
            state.probeTick = entity.tickCount;
            for (int i = 0; i < 6; i++) {
                Rig rig = rigs[i];
                float side = Math.signum(rig.hip.x - center.x);
                int row = i % 3;
                Vector3f nominal = new Vector3f(rig.hip.x + side * FOOT_SPREAD, 0, center.z + (row - 1) * FRONT_BACK_SPREAD);
                Vec3 desired = toWorld(nominal, base, angle).add(speed.scale(7));
                Vec3 hipWorld = toWorld(rig.hip, base, angle);
                Vec3 ground = null;

                for (double reach : new double[]{1, .82, .64, .46}) {
                    Vec3 spot = hipWorld.lerp(desired, reach);
                    ground = ground(entity, spot.x, spot.z, base.y);
                    if (ground != null && ground.distanceToSqr(hipWorld.add(0, BODY_LIFT, 0)) < rig.total * rig.total * .94)
                        break;
                    ground = null;
                }
                Leg leg = state.legs[i];
                leg.support = ground != null;
                leg.desired = ground != null ? ground : new Vec3(desired.x, base.y - .35, desired.z);
                if (reset || leg.foot == null) {
                    leg.foot = leg.desired;
                    leg.from = leg.to = leg.foot;
                    leg.phase = 1;
                }
            }
        }

        float targetLift = BODY_LIFT;
        double sumY = 0, sumXX = 0, sumZZ = 0, sumXY = 0, sumZY = 0;
        int supportCount = 0;
        for (Leg leg : state.legs)
            if (leg.support) {
                Vector3f p = toLocal(leg.desired, base, angle);
                double x = p.x - center.x, z = p.z - center.z;
                sumY += p.y;
                sumXX += x * x;
                sumZZ += z * z;
                sumXY += x * p.y;
                sumZY += z * p.y;
                supportCount++;
            }
        if (supportCount > 0) targetLift += (float) (sumY / supportCount);
        targetLift = Mth.clamp(targetLift, .45F, 1.55F);
        float targetRoll = supportCount >= 3 ? (float) Math.atan2(sumXY, Math.max(.1, sumXX)) : 0;
        float targetPitch = supportCount >= 3 ? -(float) Math.atan2(sumZY, Math.max(.1, sumZZ)) : 0;
        float blend = reset ? 1 : 1 - (float) Math.exp(-dt * .16);
        state.lift = Mth.lerp(blend, state.lift, targetLift);
        state.roll = Mth.lerp(blend, state.roll, Mth.clamp(targetRoll, -.28F, .28F));
        state.pitch = Mth.lerp(blend, state.pitch, Mth.clamp(targetPitch, -.28F, .28F));
        body.setPosY(state.lift * 16);
        body.setRotX(state.pitch);
        body.setRotY(0);
        body.setRotZ(state.roll);
        Quaternionf bodyRotation = new Quaternionf().rotationZ(state.roll).rotateX(state.pitch);
        Quaternionf inverseBody = new Quaternionf(bodyRotation).conjugate();
        Vector3f up = inverseBody.transform(new Vector3f(0, 1, 0));

        boolean stepping = false;
        for (Leg leg : state.legs) if (leg.phase < 1) stepping = true;
        if (!reset && !stepping && dt > 0) {
            float[] error = new float[2];
            for (int i = 0; i < 6; i++) {
                Leg leg = state.legs[i];
                float displacement = (float) leg.foot.distanceTo(leg.desired);
                float heightError = (float) Math.abs(leg.foot.y - leg.desired.y) * STEP_DISTANCE / .12F;
                error[i % 2] = Math.max(error[i % 2], Math.max(displacement, heightError));
            }
            int group = error[0] >= error[1] ? 0 : 1;
            if (error[1 - state.lastGroup] > .50) group = 1 - state.lastGroup;
            if (error[group] > STEP_DISTANCE) {
                state.lastGroup = group;
                for (int i = group; i < 6; i += 2) {
                    Leg leg = state.legs[i];
                    leg.from = leg.foot;
                    leg.to = leg.desired;
                    leg.phase = 0;
                }
            }
        }
        for (int i = 0; i < 6; i++) {
            Leg leg = state.legs[i];
            Rig rig = rigs[i];
            if (leg.phase < 1) {
                leg.phase = Math.min(1, leg.phase + dt / STEP_TICKS);
                double f = leg.phase * leg.phase * (3 - 2 * leg.phase);
                double lift = Math.sin(Math.PI * leg.phase) * STEP_HEIGHT;
                leg.foot = leg.from.lerp(leg.to, f).add(0, lift, 0);
            }

            Vector3f target = toLocal(leg.foot, base, angle).sub(center).add(0, -state.lift, 0);
            inverseBody.transform(target);
            target.add(center);
            Vector3f[] solved = EntropySpiderLegSolver.solve(rig.hip, target, rig.lengths, up);
            Quaternionf parent = new Quaternionf();
            for (int j = 0; j < 5; j++) {
                Vector3f rest = new Vector3f(rig.points[j + 1]).sub(rig.points[j]).normalize();
                Vector3f direction = new Vector3f(solved[j + 1]).sub(solved[j]).normalize();
                Quaternionf desired = new Quaternionf().rotationTo(rest, direction);
                Quaternionf local = new Quaternionf(parent).conjugate().mul(desired).normalize();
                rotate(rig.bones[j], local);
                parent.set(desired);
            }
        }
        state.lastBase = base;
        state.lastTime = now;
    }

    private static Vec3 ground(EntropySpiderEntity entity, double x, double z, double y) {

        Vec3 from = new Vec3(x, y + 2.0, z), to = new Vec3(x, y - 2.8, z);
        for (int iy = Mth.floor(to.y); iy <= Mth.floor(from.y); iy++)
            if (!entity.level().hasChunkAt(new BlockPos(Mth.floor(x), iy, Mth.floor(z)))) return null;
        var hit = entity.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        if (hit.getType() == HitResult.Type.MISS || hit.getDirection() != net.minecraft.core.Direction.UP) return null;
        return hit.getLocation().add(0, .075, 0);
    }

    private static Rig rig(EntropySpiderModel model, int index) {
        CoreGeoBone[] bones = new CoreGeoBone[5];
        Vector3f[] points = new Vector3f[6];
        float[] lengths = new float[5];
        for (int i = 0; i < 5; i++) {
            bones[i] = model.getAnimationProcessor().getBone(PREFIX[i] + SUFFIX[index]);
            if (bones[i] == null) return null;
            points[i] = pivot(bones[i]);
        }
        CoreGeoBone marker = model.getAnimationProcessor().getBone(MARKER[index]);
        if (marker == null) return null;
        points[5] = pivot(marker);
        float total = 0;
        for (int i = 0; i < 5; i++) {
            lengths[i] = points[i].distance(points[i + 1]);
            total += lengths[i];
        }
        return new Rig(bones, points, lengths, total);
    }

    private static Vector3f pivot(CoreGeoBone bone) {
        return new Vector3f(bone.getPivotX() / 16F, bone.getPivotY() / 16F, bone.getPivotZ() / 16F);
    }

    private static Vec3 toWorld(Vector3f p, Vec3 base, double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return base.add(c * p.x + s * p.z, p.y, -s * p.x + c * p.z);
    }

    private static Vector3f toLocal(Vec3 p, Vec3 base, double a) {
        Vec3 d = p.subtract(base);
        double c = Math.cos(a), s = Math.sin(a);
        return new Vector3f((float) (c * d.x - s * d.z), (float) d.y, (float) (s * d.x + c * d.z));
    }

    private static void rotate(CoreGeoBone bone, Quaternionf q) {
        float x = q.x, y = q.y, z = q.z, w = q.w;
        bone.setRotX((float) Math.atan2(2 * (w * x + y * z), 1 - 2 * (x * x + y * y)));
        bone.setRotY((float) Math.asin(Mth.clamp(2 * (w * y - z * x), -1F, 1F)));
        bone.setRotZ((float) Math.atan2(2 * (w * z + x * y), 1 - 2 * (y * y + z * z)));
    }

    private static final class Rig {
        final CoreGeoBone[] bones;
        final Vector3f[] points;
        final float[] lengths;
        final Vector3f hip;
        final float total;

        Rig(CoreGeoBone[] b, Vector3f[] p, float[] l, float t) {
            bones = b;
            points = p;
            lengths = l;
            hip = p[0];
            total = t;
        }
    }

    private static final class Leg {
        Vec3 foot, desired, from, to;
        float phase = 1;
        boolean support;
    }

    private static final class State {
        final Leg[] legs = {new Leg(), new Leg(), new Leg(), new Leg(), new Leg(), new Leg()};
        Vec3 lastBase;
        float lastTime, lift = BODY_LIFT, pitch, roll;
        int probeTick = -1, lastGroup = 1;
    }
}
