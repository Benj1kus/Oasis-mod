package com.benji.oasiso.client.cloud;

public final class CloudMotion {
    public record Point(double x, double y, double z) {
    }

    public static float smooth(float x) {
        x = Math.max(0, Math.min(1, x));
        return x * x * (3 - 2 * x);
    }

    public static Point sample(int seed, double progress, double height, boolean soul) {
        double t = Math.max(0, Math.min(1, progress));
        double phase = (seed & 65535) * .017;
        double amplitude = soul ? .65 : .32;
        double frequency = soul ? 10.0 : 5.0;
        double envelope = Math.sin(Math.min(1, t * 3) * Math.PI * .5);
        double x = (Math.sin(t * frequency + phase) - Math.sin(phase)) * amplitude * envelope;
        double z = (Math.cos(t * frequency * .79 + phase * 1.37) - Math.cos(phase * 1.37)) * amplitude * envelope;
        x += Math.sin(phase * 2.1) * t * .7;
        z += Math.cos(phase * 1.9) * t * .7;
        return new Point(x, height * (.8 * t + .2 * t * t), z);
    }

    public static float opacity(float age, float life) {
        return smooth(age / 8F) * (1 - smooth((age / life - .65F) / .35F));
    }

    private CloudMotion() {
    }
}
