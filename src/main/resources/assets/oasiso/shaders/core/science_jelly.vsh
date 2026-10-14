#version 150
in vec3 Position;
in vec4 Color;
in vec2 UV0;
uniform mat4 ShieldView;
uniform mat4 ShieldProjection;
uniform vec3 Center;
uniform vec3 Radii;
uniform float Time;
uniform float Seed;
uniform float Layer;
uniform vec4 Hit0;
uniform vec4 Hit1;
uniform vec4 Hit2;
uniform vec4 Hit3;
out vec3 localPosition;
out vec3 worldNormal;
out vec3 viewDirection;
out float rippleLight;

float ripple(vec3 n, vec4 hit) {
    if (hit.w < 0.0 || hit.w > 1.3) return 0.0;
    float arc = acos(clamp(dot(n, hit.xyz), -1.0, 1.0));
    float front = hit.w * 2.65;
    float offset = arc - front;
    return sin(offset * 22.0) * exp(-offset * offset * 18.0)
        * exp(-hit.w * 2.3) * smoothstep(0.0, .05, hit.w);
}
void main() {
    vec3 n = normalize(Position);
    float time = Time + Seed * .17;
    float wobble = sin(n.x * 5.0 + time * .9) * sin(n.z * 4.0 - time * .7)
                 + .55 * sin(n.y * 6.0 + n.z * 3.0 + time * 1.1);
    float hitWave = ripple(n, Hit0) + ripple(n, Hit1) + ripple(n, Hit2) + ripple(n, Hit3);
    float scale = 1.0 - Layer * .135;
    vec3 local = n * (scale + wobble * .026 + hitWave * .09);
    local.x += Layer * .018 * sin(time * .6 + n.y * 3.0);
    local.z += Layer * .018 * cos(time * .5 + n.y * 3.0);
    vec3 p = Center + local * Radii;
    localPosition = local;
    worldNormal = normalize(n / Radii);
    viewDirection = inverse(ShieldView)[3].xyz - p;
    rippleLight = hitWave;
    gl_Position = ShieldProjection * ShieldView * vec4(p, 1.0);
}
