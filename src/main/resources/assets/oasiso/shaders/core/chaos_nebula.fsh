#version 150

uniform float Time;
uniform float Brightness;
in vec3 skyDirection;
out vec4 fragColor;

const float SKY_PIXEL_DENSITY = 128.0;
const float ANIMATION_SPEED = 3.0;

float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f*f*(3.0-2.0*f);
    return mix(mix(mix(hash31(i), hash31(i+vec3(1,0,0)), f.x),
                   mix(hash31(i+vec3(0,1,0)), hash31(i+vec3(1,1,0)), f.x), f.y),
               mix(mix(hash31(i+vec3(0,0,1)), hash31(i+vec3(1,0,1)), f.x),
                   mix(hash31(i+vec3(0,1,1)), hash31(i+vec3(1,1,1)), f.x), f.y), f.z);
}

float clouds(vec3 p) {
    return 0.70*noise3(p) + 0.30*noise3(p*2.03+17.7);
}

mat2 rotate2(float a) {
    float c=cos(a), s=sin(a);
    return mat2(c,-s,s,c);
}

vec3 swirl(vec3 d, vec3 axis, float speed, float offset) {
    float pole = dot(d, axis);
    float influence = smoothstep(0.82, 0.995, pole);
    float angle = influence*(1.85 + 0.55*sin(Time*ANIMATION_SPEED*speed+offset));
    float c=cos(angle), s=sin(angle);
    return d*c + cross(axis,d)*s + axis*pole*(1.0-c);
}

void main() {
    vec3 d = normalize(skyDirection);
    vec3 cube = d / max(max(abs(d.x),abs(d.y)),abs(d.z));
    vec3 p = normalize(floor(cube*SKY_PIXEL_DENSITY+0.5)/SKY_PIXEL_DENSITY);
    float t = Time*ANIMATION_SPEED;
    p.xz = rotate2(t*0.0008)*p.xz;
    p = swirl(p, normalize(vec3(0.78,0.44,-0.44)), 0.032, 0.0);
    p = swirl(p, normalize(vec3(-0.50,0.65,0.57)), 0.025, 2.4);

    vec3 drift = vec3(t*0.012,t*0.005,-t*0.009);
    float broad = noise3(p*2.8 + drift*0.22 + 4.6);
    vec3 flow = vec3(sin(p.y*4.2+t*0.024),
                     sin(p.z*4.8-t*0.020), sin(p.x*4.0+t*0.018));
    float gas = clouds(p*4.6 + flow*0.72 + drift);

    float bank = smoothstep(0.27,0.68,broad);
    float density = smoothstep(0.30,0.69,gas) * bank;
    float voids = smoothstep(0.42,0.78,noise3(p*3.7-drift*0.35-12.0));
    density *= 1.0 - 0.84*voids;

    vec3 deep = vec3(0.008,0.029,0.043);
    float jade = smoothstep(0.38,0.70,noise3(p*2.2+8.2));
    vec3 color = deep;
    color = mix(color, mix(vec3(0.020,0.080,0.125),vec3(0.015,0.100,0.105),jade),
                smoothstep(0.070,0.082,density));
    color = mix(color, mix(vec3(0.022,0.145,0.220),vec3(0.015,0.190,0.170),jade),
                smoothstep(0.160,0.172,density));
    color = mix(color, mix(vec3(0.020,0.255,0.330),vec3(0.018,0.305,0.240),jade),
                smoothstep(0.285,0.297,density));
    color = mix(color, mix(vec3(0.025,0.390,0.450),vec3(0.025,0.440,0.315),jade),
                smoothstep(0.435,0.447,density));
    color = mix(color, mix(vec3(0.060,0.555,0.585),vec3(0.090,0.610,0.430),jade),
                smoothstep(0.590,0.602,density));
    color = mix(color, mix(vec3(0.220,0.720,0.710),vec3(0.260,0.770,0.540),jade),
                smoothstep(0.765,0.777,density));
    color = mix(deep*0.35,color,smoothstep(-0.6,0.2,d.y));
    fragColor = vec4(color*Brightness,1.0);
}
