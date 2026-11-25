#version 150
uniform float Closure;
uniform float HasTexture;
uniform sampler2D Sampler0;
in vec2 uv;
out vec4 fragColor;
void main() {
    vec2 eye = (uv * 2.0 - 1.0) * vec2(0.82, 1.16);
    float radius = mix(1.45, -0.28, Closure);
    float alpha = smoothstep(radius, radius + 0.26, length(eye));
    vec3 ink = HasTexture > 0.5 ? texture(Sampler0, uv).rgb : vec3(0.0);
    fragColor = vec4(ink, alpha);
}
