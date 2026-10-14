#version 150
uniform float Time;
uniform float Seed;
uniform float Layer;
uniform float Amount;
uniform vec3 Radii;
in vec3 localPosition;
in vec3 worldNormal;
in vec3 viewDirection;
in float rippleLight;
out vec4 fragColor;

float hash(vec3 p) {
    return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453);
}
vec3 hash3(vec3 p) {
    return vec3(hash(p), hash(p + 19.17), hash(p + 43.71));
}
float bayer(vec2 cell) {
    ivec2 q = ivec2(mod(cell, 4.0));
    int table[16] = int[16](0,8,2,10, 12,4,14,6, 3,11,1,9, 15,7,13,5);
    return (float(table[q.y * 4 + q.x]) + .5) / 16.0;
}

void mosaic(vec3 p, out vec3 site, out vec3 id, out float seam) {
    vec3 base = floor(p);
    float first = 100.0, second = 100.0;
    for (int z = -1; z <= 1; z++)
    for (int y = -1; y <= 1; y++)
    for (int x = -1; x <= 1; x++) {
        vec3 cell = base + vec3(x,y,z);
        vec3 candidate = cell + .5 + (hash3(cell + Seed * .037) - .5) * .78;
        vec3 delta = p - candidate;
        float distanceSquared = dot(delta, delta);
        if (distanceSquared < first) {
            second = first; first = distanceSquared; site = candidate; id = cell;
        } else second = min(second, distanceSquared);
    }
    seam = sqrt(second) - sqrt(first);
}
vec3 jewel(float id) {
    if (id < .17) return vec3(.30, .018, .62);
    if (id < .43) return vec3(.58, .025, .97);
    if (id < .70) return vec3(.92, .26, .88);
    if (id < .90) return vec3(1.0, .38, .78);
    return vec3(.80, .30, 1.0);
}
void main() {
    if (Layer > 1.5) discard;
    vec3 direction = normalize(localPosition);
    vec3 grid = vec3(48.0, 24.0, 48.0);
    vec3 cell = floor(direction * grid);
    vec3 p = localPosition / (1.0 - Layer * .135);
    vec3 a = abs(direction);
    vec2 plane = a.y > max(a.x, a.z) ? cell.xz : (a.x > a.z ? cell.zy : cell.xy);
    float dither = bayer(plane);
    vec3 mosaicScale = vec3(3.2, 2.0, 3.2);
    float t = Time + Seed * .17;
    vec3 drift = Layer * vec3(sin(t * .24), cos(t * .19), sin(t * .21 + 2.0)) * .10;
    vec3 site, id;
    float seam;
    mosaic(p * mosaicScale + drift, site, id, seam);
    vec3 random = hash3(id + Seed * .037 + 8.13);
    vec3 cutNormal = normalize(hash3(id + 71.3) - .5);
    float cut = step(0.0, dot(p * mosaicScale + drift - site, cutNormal));
    float tint = fract(random.x + cut * .23);
    vec3 color = jewel(tint);
    vec3 n = normalize(worldNormal), v = normalize(viewDirection);
    vec3 facet = normalize(site / mosaicScale / Radii + (random - .5) * .32 + cutNormal * (cut - .5) * .18);
    vec3 light = normalize(vec3(-.55, .8, .45));
    float lit = max(dot(facet, light), 0.0);
    float shade = floor(clamp(lit * 2.4, 0.0, 2.0) + dither * .7) / 2.0;
    color *= .84 + shade * .16;
    float splitShade = mix(.90, 1.0, cut);
    color *= splitShade;
    float sweep = pow(max(0.0, sin(t * .85 + random.y * 6.283)), 6.0);
    float cyan = step(.66, random.z) * sweep;
    color = mix(color, vec3(.18, .93, 1.0), cyan * .90);
    float reflection = dot(facet, normalize(light + v));
    float white = step(.855 + dither * .075, reflection) * step(.40, random.y);
    color = mix(color, vec3(.30, .95, 1.0), white * .97);
    float join = (1.0 - step(.024, seam)) * step(.60, random.x);
    color = mix(color, vec3(.37, .025, .62), join * .22);
    float facing = abs(dot(n, v));
    float rim = 1.0 - smoothstep(.10, .27, facing);
    float alpha;
    if (Layer > .5) {
        float fragments = step(.78, random.x) * step(.24, seam) * step(.25, cut);
        alpha = fragments * .12;
        color = mix(color, vec3(.61, .19, .97), .25);
    } else {
        alpha = .51 + .15 * step(.70, tint) + cyan * .20 + white * .28;
        color = mix(color, vec3(1.0, .38, .78), rim);
        alpha = mix(alpha, .94, rim);
    }
    if (!gl_FrontFacing) alpha *= .035;
    float wave = floor(clamp(abs(rippleLight) * 2.2, 0.0, 1.0) * 3.0 + dither) / 3.0;
    color = mix(color, vec3(.42, .91, 1.0), wave * .75);
    alpha = clamp((alpha + wave * .18) * Amount, 0.0, .96);
    if (alpha < .003) discard;
    fragColor = vec4(color, alpha);
}
