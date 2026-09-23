#version 150
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec2 TexelSize;
uniform float Radius;
uniform float Opacity;
in vec2 texCoord;
out vec4 fragColor;
void main() {

    if (texture(Sampler0, texCoord).a > 0.5) discard;
    float best = 1000.0;
    vec2 nearest = texCoord;
    float height = 0.0;
    for (int y = -6; y <= 6; ++y) {
        for (int x = -6; x <= 6; ++x) {
            vec2 delta = vec2(float(x), float(y));
            float d = length(delta);
            if (d > Radius + 0.5 || d >= best) continue;
            vec2 uv = texCoord + delta * TexelSize;
            if (any(lessThan(uv, vec2(0.0))) || any(greaterThan(uv, vec2(1.0)))) continue;
            vec4 m = texture(Sampler0, uv);
            if (m.a > 0.5) { best = d; nearest = uv; height = m.r; }
        }
    }
    if (best > Radius + 0.5) discard;
    float fade = pow(smoothstep(0.0, 1.0, height), 1.25);
    float edge = 1.0 - smoothstep(Radius - 0.6, Radius + 0.5, best);
    float alpha = Opacity * fade * edge;
    if (alpha < 0.003) discard;

    vec3 lower = vec3(0.04, 0.57, 0.34);
    vec3 middle = vec3(0.12, 0.95, 0.69);
    vec3 upper = vec3(0.54, 1.00, 0.85);
    vec3 color = height < 0.55
        ? mix(lower, middle, smoothstep(0.0, 0.55, height))
        : mix(middle, upper, smoothstep(0.55, 1.0, height));
    fragColor = vec4(color, alpha);

    gl_FragDepth = max(0.0, texture(Sampler1, nearest).r - 0.000001);
}
