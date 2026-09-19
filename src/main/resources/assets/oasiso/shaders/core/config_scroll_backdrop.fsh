#version 150

uniform float Time;
uniform float Entrance;
uniform vec2 Size;
uniform vec3 Accent;
in vec2 uv;
out vec4 fragColor;

float bayer(vec2 p) {
    ivec2 q = ivec2(mod(p, 4.0));
    int i = q.x + q.y * 4;
    float values[16] = float[16](0., 8., 2., 10., 12., 4., 14., 6., 3., 11., 1., 9., 15., 7., 13., 5.);
    return (values[i] + 0.5) / 16.0;
}
void main() {
    float intro = smoothstep(0.0, 1.0, Entrance);
    float right = smoothstep(0.30, 0.44, uv.x);
    float wave = sin(uv.y * 9.0 + Time * 0.50) * 0.018
               + sin(uv.y * 19.0 - Time * 0.31) * 0.007;
    // The cyan bloom moves up from below the screen during the first stage.
    vec2 glowCenter = vec2(0.80, 0.47 + (1.0 - intro) * 0.95);
    vec2 delta = (uv - glowCenter) * vec2(1.75, 1.0);
    float bloom = exp(-dot(delta, delta) * 4.8) * right * intro;
    // A dark veil is densest beside the ring and fades towards the right edge.
    // Its reveal advances horizontally, with ordered dithering at the moving edge.
    float reveal = smoothstep(0.12, 0.84, Entrance);
    float edge = mix(0.30, 1.12, reveal) + wave;
    float coverage = 1.0 - smoothstep(edge - 0.10, edge + 0.02, uv.x);
    float shade = (0.86 - 0.31 * smoothstep(0.43 + wave, 1.0, uv.x)) * right * intro;
    float ordered = bayer(floor(uv * Size / 1.35));
    float dither = step(ordered, coverage) * shade;
    float alpha = 0.18 * intro + bloom * 0.27 + dither * 0.74;
    vec3 tint = mix(vec3(0.010, 0.018, 0.028), Accent * 0.33, bloom * (1.0 - dither * 0.42));
    // Small dither variation remains visible in the settled, slowly waving veil.
    alpha += (ordered - 0.5) * 0.024 * right * intro;
    fragColor = vec4(tint, clamp(alpha, 0.0, 0.92));
}
