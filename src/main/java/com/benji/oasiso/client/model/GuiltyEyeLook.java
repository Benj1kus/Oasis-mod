package com.benji.oasiso.client.model;

public final class GuiltyEyeLook {
    public static final double MAX_HEAD_PITCH = Math.toRadians(45);
    public static final double MAX_PUPIL_OFFSET = 2.4;
    public static final double HEAD_RESPONSE_TICKS = 10;
    public static final double PUPIL_RESPONSE_TICKS = 3;
    private static final double HEAD_Y = 7.96429, HEAD_Z = -.02857;
    private static final double[] EYE_X = {0, 0, -6.25, 6.25};
    private static final double[] EYE_Z = {-6.3, 6.2, -.05, -.05};
    private static final double[] PUPIL_Z = {-6.4, 6.1, -.15, -.15};
    private static final double[] EYE_YAW = {0, -Math.PI, Math.PI / 2, -Math.PI / 2};
    public double yaw, pitch;
    public final double[] pupilX = new double[4], pupilY = new double[4];

    public void update(double x, double y, double z, boolean tracking, double elapsedTicks) {
        double dt = Math.max(0, Math.min(2, elapsedTicks));
        double headBlend = -Math.expm1(-dt / HEAD_RESPONSE_TICKS);
        double pupilBlend = -Math.expm1(-dt / PUPIL_RESPONSE_TICKS);
        double targetYaw = 0, targetPitch = 0;

        if (tracking) {
            double horizontal = Math.hypot(x, z);
            if (horizontal > 1e-6) {
                targetYaw = Math.atan2(-x, -z);
            }
            targetPitch = clamp(Math.atan2(y - 14.0 / 16, Math.max(1e-6, horizontal)), MAX_HEAD_PITCH);
        }

        double yawDiff = targetYaw - yaw;
        yawDiff = Math.atan2(Math.sin(yawDiff), Math.cos(yawDiff));
        yaw += yawDiff * headBlend;
        yaw = Math.atan2(Math.sin(yaw), Math.cos(yaw));

        pitch += (targetPitch - pitch) * headBlend;

        double px = x * 16, py = y * 16 - HEAD_Y, pz = z * 16 - HEAD_Z;
        double c = Math.cos(yaw), s = Math.sin(yaw);
        double hx = c * px - s * pz, hz = s * px + c * pz;
        c = Math.cos(pitch);
        s = Math.sin(pitch);
        double hy = c * py + s * hz + HEAD_Y;
        hz = -s * py + c * hz + HEAD_Z;
        for (int i = 0; i < 4; i++) {
            double targetX = 0, targetY = 0;
            if (tracking) {
                double dx = hx - EYE_X[i], dz = hz - EYE_Z[i];
                c = Math.cos(EYE_YAW[i]);
                s = Math.sin(EYE_YAW[i]);
                double ex = c * dx - s * dz;
                double ey = hy - 14;
                double ez = s * dx + c * dz + EYE_Z[i] - PUPIL_Z[i];
                double length = Math.sqrt(ex * ex + ey * ey + ez * ez);
                if (length > 1e-6) {
                    targetX = -MAX_PUPIL_OFFSET * ex / length;
                    targetY = MAX_PUPIL_OFFSET * ey / length;
                }
            }
            pupilX[i] += (targetX - pupilX[i]) * pupilBlend;
            pupilY[i] += (targetY - pupilY[i]) * pupilBlend;
        }
    }

    private static double clamp(double value, double limit) {
        return Math.max(-limit, Math.min(limit, value));
    }
}