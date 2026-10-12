package com.benji.oasiso.client.factory;

public final class FactoryCoreStreamPattern {
    public static final double EMISSION_INTERVAL = 3.05;
    public static final double FLIGHT_TIME_SCALE = 1.5;
    public static final double TRAIL_SECONDS = .95;
    public static final int TRAIL_SEGMENTS = 16;

    public record Point(double x, double y, double z) {
        public Point add(Point p) {
            return new Point(x + p.x, y + p.y, z + p.z);
        }

        public Point sub(Point p) {
            return new Point(x - p.x, y - p.y, z - p.z);
        }

        public Point mul(double n) {
            return new Point(x * n, y * n, z * n);
        }

        public double dot(Point p) {
            return x * p.x + y * p.y + z * p.z;
        }

        public Point cross(Point p) {
            return new Point(y * p.z - z * p.y, z * p.x - x * p.z, x * p.y - y * p.x);
        }

        public double length() {
            return Math.sqrt(dot(this));
        }

        public Point unit() {
            double n = length();
            return n < 1e-10 ? new Point(0, 1, 0) : mul(1 / n);
        }
    }

    public static final class Flight {
        public final Point start, end;
        public final double birth, duration, halfWidth, phase;
        private final Point along, side, other;
        private final double distance, radius, turns, bend, arc;
        private final int direction;

        public Flight(Point start, Point end, double birth, long seed) {
            this.start = start;
            this.end = end;
            this.birth = birth;
            Point delta = end.sub(start);
            distance = delta.length();
            along = delta.unit();
            Point reference = Math.abs(along.y) < .9 ? new Point(0, 1, 0) : new Point(1, 0, 0);
            side = along.cross(reference).unit();
            other = along.cross(side).unit();
            duration = (2.4 + distance * .18) * (.90 + hash(seed, 1) * .20) * FLIGHT_TIME_SCALE;
            halfWidth = .24 + hash(seed, 2) * .11;
            phase = hash(seed, 3) * Math.PI * 2;
            radius = Math.min(distance * .21, .65 + hash(seed, 4) * .75);
            turns = .8 + hash(seed, 5) * .85;
            direction = hash(seed, 6) < .5 ? -1 : 1;
            bend = (hash(seed, 7) - .5) * .5;
            arc = Math.min(distance * .14, .35 + hash(seed, 8) * .60);
        }

        public double progress(double now) {
            return clamp((now - birth) / duration);
        }

        public Point at(double progress) {
            double p = clamp(progress);
            if (p == 0) return start;
            if (p == 1) return end;
            double advance = .20 * p + .80 * p * p * (2 - p);
            double envelope = Math.pow(Math.sin(Math.PI * p), 1.15);
            double angle = phase + direction * Math.PI * 2 * turns * p + .25 * Math.sin(Math.PI * 2 * p);
            double swirlRadius = radius * envelope;
            double a = Math.cos(angle) * swirlRadius + bend * Math.sin(Math.PI * 3 * p) * envelope;
            double b = Math.sin(angle) * swirlRadius + .17 * Math.sin(Math.PI * 4 * p + phase) * envelope;
            double forward = distance * advance;
            return new Point(start.x + along.x * forward + side.x * a + other.x * b, start.y + along.y * forward + side.y * a + other.y * b + arc * Math.sin(Math.PI * p), start.z + along.z * forward + side.z * a + other.z * b);
        }

        public double opacity(double now) {
            double age = now - birth;
            return smooth(age / .20) * smooth((duration - age) / .28);
        }
    }

    public static double interval(long seed) {
        return EMISSION_INTERVAL * (.85 + .30 * hash(seed, 9));
    }

    public static double hash(long seed, int salt) {
        long x = seed + salt * 0x9E3779B97F4A7C15L;
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return ((x ^ (x >>> 31)) >>> 11) * 0x1.0p-53;
    }

    private static double clamp(double x) {
        return Math.max(0, Math.min(1, x));
    }

    private static double smooth(double x) {
        x = clamp(x);
        return x * x * (3 - 2 * x);
    }

    private FactoryCoreStreamPattern() {
    }
}
