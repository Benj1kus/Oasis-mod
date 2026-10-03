#version 150
uniform float Age;
uniform float Seed;
in vec2 texCoord;
in vec4 material;
in float cameraDistance;
out vec4 fragColor;

float hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7))+Seed*13.73)*43758.5453);}
float bayer(vec2 p){
    ivec2 q=ivec2(mod(floor(p),4.0));
    int v[16]=int[16](0,8,2,10,12,4,14,6,3,11,1,9,15,7,13,5);
    return (float(v[q.x+q.y*4])+.5)/16.0;
}

vec3 palette(float t) {
    vec3 cyan   = vec3(0.2, 0.85, 1.0);
    vec3 violet = vec3(0.7, 0.2,  1.0);
    vec3 pink   = vec3(1.0, 0.25, 0.85);

    t = fract(t) * 3.0;
    if(t < 1.0) return mix(cyan, violet, t);
    if(t < 2.0) return mix(violet, pink, t - 1.0);
    return mix(pink, cyan, t - 2.0);
}

void main() {
    if(Age <= 0.0 || Age >= 6.0) discard;

    float speed = 2.0;
    float kind = floor(material.r * 4.0 + .5);
    float clock = floor(Age * 24.0 * speed) / 24.0;

    vec2 grid = kind < .5 ? vec2(160, 120) : kind < 1.5 ? vec2(8, 100) : kind < 2.5 ? vec2(200, 6) : vec2(8, 16);
    vec2 cell = floor(texCoord * grid);
    vec2 uv = (cell + .5) / grid;
    float threshold = bayer(cell);

    float dissolve = smoothstep(4.4, 6.0, Age);
    float life = hash(floor(cell / (kind < .5 ? vec2(4, 6) : vec2(2, 5))) + vec2(kind * 39.0, material.g * 17.0));
    if(dissolve > 0.0 && life < dissolve + threshold * .045) discard;

    vec3 color = palette(material.g + uv.x * .32);
    float alpha = material.a;
    float edge = 1.0;

    if(kind < .5) {
        float stripe = hash(vec2(floor(uv.x * 80.0), floor((uv.y - clock * .9) * 9.0)));
        float streak = step(.53, stripe);

        vec3 whiteHot = vec3(0.92, 0.97, 1.0);
        vec3 edgeGlow = palette(uv.x + material.g * .25);

        color = mix(edgeGlow * 1.2, whiteHot * 1.5, (1.0 - material.g) * 0.7 + streak * 0.3);
        alpha *= .85 + .15 * streak;
        edge = 1.0 - smoothstep(.85, 1.0, uv.y);

    } else if(kind < 1.5) {
        float moving = fract(uv.y * 2.8 - clock * (.85 + material.g * .4) + material.g * 5.0);
        float pulse = sin(moving * 3.14159);
        edge = step(.14 + threshold * .14, pulse) * (1.0 - smoothstep(.9, 1.0, uv.y));
        edge *= 1.0 - smoothstep(.25, .51, abs(uv.x - .5));

        vec3 glowColor = mix(color, vec3(1.2, 0.5, 1.0), pow(pulse, 4.0));
        color = glowColor * 1.3;

    } else if(kind < 2.5) {
        float center = 1.0 - abs(uv.y * 2.0 - 1.0);
        color = mix(color * 1.2, vec3(1.1, 0.4, 1.2), step(.53, center) * .88);
        edge = .6 + .4 * center;

    } else {
        vec2 q = abs(uv * 2.0 - 1.0);
        float shape = kind < 3.5 ? q.x + q.y : min(q.x * 4.0 + q.y, q.x + q.y * 4.0);
        edge = 1.0 - step(1.0, shape);
        color = mix(color, vec3(1.3, 0.8, 1.0), .8) * 1.4;
    }

    alpha *= edge * smoothstep(.45, 1.8, cameraDistance) * smoothstep(0.0, .18, Age) * (1.0 - dissolve * .65);
    if(alpha < .015) discard;

    color = floor(clamp(color, 0.0, 2.0) * 16.0 + threshold) / 16.0;
    fragColor = vec4(color, alpha);
}