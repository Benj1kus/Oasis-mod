#version 150

uniform float Time;
uniform float Intro;
uniform float Aspect;

in vec2 vUv;

out vec4 fragColor;

float edgeFade(vec2 uv) {
    float left = smoothstep(0.00, 0.18, uv.x);
    float right = 1.0 - smoothstep(0.82, 1.00, uv.x);
    float top = smoothstep(0.00, 0.16, uv.y);
    float bottom = 1.0 - smoothstep(0.84, 1.00, uv.y);

    return left * right * top * bottom;
}

void main() {
    vec2 uv = vUv;

    vec2 p = (uv - vec2(0.5)) * 2.0;
    p.x *= Aspect;

    float radius = length(p);
    float angle = atan(p.y, p.x);

    float phase = Time * 0.22;

    float rayPatternA = 0.5 + 0.5 * cos(angle * 6.0 - phase);
    float rayPatternB = 0.5 + 0.5 * cos(angle * 6.0 + phase * 0.52 + 1.15);

    float raysA = pow(rayPatternA, 8.0);
    float raysB = pow(rayPatternB, 13.0);

    float rayInner = smoothstep(0.12, 0.27, radius);
    float rayOuter = 1.0 - smoothstep(0.48, 1.02, radius);
    float rayMask = rayInner * rayOuter;

    float rays = (raysA * 0.72 + raysB * 0.28) * rayMask;

    float halo = 1.0 - smoothstep(0.06, 0.88, radius);
    halo = halo * halo;

    float core = 1.0 - smoothstep(0.02, 0.34, radius);
    core = core * core;

    float edge = edgeFade(uv);

    float alpha =
            (halo * 0.22
            + core * 0.12
            + rays * 0.31)
            * edge
            * Intro;

    vec3 cyan = vec3(0.050, 1.000, 0.950);
    vec3 purple = vec3(0.700, 0.300, 1.000);

    float purpleMix = 0.16
            + 0.10 * (0.5 + 0.5 * sin(angle * 3.0 + Time * 0.35));

    vec3 color = mix(cyan, purple, purpleMix);

    fragColor = vec4(color, alpha);
}
