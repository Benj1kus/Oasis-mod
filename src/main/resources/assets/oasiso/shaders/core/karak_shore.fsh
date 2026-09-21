#version 150
in vec2 uv;
in vec4 edges;
in float shoreDistance;
in float distanceToCamera;
uniform float Time;
uniform float Range;
out vec4 fragColor;

//brightness
const float CENTER_DARKNESS = 0.80;
//distance to center
const float CENTER_START = 1.50;
//anchor
const float CENTER_CORE = 2.85;
//waving stenght
const float CENTER_WAVE = 1.00;
// waves  speed
const float CENTER_SPEED = 1.5;
//size of pixel-dithering
const float CENTER_PIXELS = 8.0;
//dithering width
const float CEL_EDGE_WIDTH = 0.100;
//colors
const vec3 CENTER_OUTER = vec3(0.045, 0.290, 0.335);
const vec3 CENTER_MIDDLE = vec3(0.035, 0.205, 0.295);
const vec3 CENTER_DEEP = vec3(0.025, 0.115, 0.225) * 1.5;
const float TAU = 6.28318530718;

float bayer2(vec2 p) {
    p=mod(floor(p),2.0);
    return 2.0*p.x+3.0*p.y-4.0*p.x*p.y;
}
float bayer4(vec2 p) {
    return (4.0*bayer2(p)+bayer2(floor(p*0.5))+0.5)/16.0;
}
float hash(vec2 p, float period) {
    p=mod(p,period);
    vec3 q=fract(vec3(p.x,p.y,p.x)*0.1031);
    q+=dot(q,q.yzx+33.33);
    return fract((q.x+q.y)*q.z);
}
float noise(vec2 p, float period) {
    vec2 cell=floor(p), f=fract(p);
    f=f*f*(3.0-2.0*f);
    return mix(mix(hash(cell,period),hash(cell+vec2(1.0,0.0),period),f.x),
               mix(hash(cell+vec2(0.0,1.0),period),hash(cell+vec2(1.0,1.0),period),f.x),f.y);
}

float pixelShoreDistance(vec2 pixelCenter) {
    vec2 dx=dFdx(uv), dy=dFdy(uv);
    float sx=dFdx(shoreDistance), sy=dFdy(shoreDistance);
    float determinant=dx.x*dy.y-dx.y*dy.x;
    if(abs(determinant)<1e-10) return shoreDistance;
    vec2 gradient=vec2(sx*dy.y-sy*dx.y,dx.x*sy-dy.x*sx)/determinant;
    return max(0.0,shoreDistance+dot(gradient,pixelCenter-uv));
}

float celStep(float border, float depth, float threshold) {
    float width=max(CEL_EDGE_WIDTH,0.001);
    return step(threshold,smoothstep(border-width,border+width,depth));
}

void main() {
    vec2 grid=floor(uv*CENTER_PIXELS+0.002);
    vec2 p=(grid+0.5)/CENTER_PIXELS;
    float pixelDepth=pixelShoreDistance(p);
    vec2 local=fract(uv);
    float d=2.0;
    if(edges.r>.5) d=min(d,local.x);
    if(edges.g>.5) d=min(d,1.0-local.x);
    if(edges.b>.5) d=min(d,local.y);
    if(edges.a>.5) d=min(d,1.0-local.y);

    float band=1.0-smoothstep(0.0,0.50,d);
    float bright=1.0-smoothstep(0.0,0.12,d);
    float pulse=.90+.10*sin(Time*1.8);
    float fade=1.0-smoothstep(Range-3.0,Range,distanceToCamera);
    if(fade<=0.0) discard;
    vec3 shoreColor=mix(vec3(0.08,0.65,0.48),vec3(0.153,0.961,0.706),band);
    shoreColor=mix(shoreColor,vec3(0.70,1.0,0.90),bright*.7);
    float shoreAlpha=band*band*.82*pulse;

    float centerAlpha=0.0;
    vec3 centerColor=CENTER_OUTER;
    if(CENTER_DARKNESS>0.0 && pixelDepth>0.65 && d>0.50) {
        float threshold=bayer4(grid);
        float t=Time*CENTER_SPEED;
        float first=max(CENTER_START,0.95);
        float core=max(CENTER_CORE,first+0.30);

        float xPhase=mod(p.x,16.0)*TAU/16.0;
        float zPhase=mod(p.y,16.0)*TAU/16.0;
        float wave=0.68*sin(xPhase*2.0+t*0.90+0.8*sin(zPhase-t*0.43))
                  +0.32*sin(zPhase*2.0-t*0.76+0.7*sin(xPhase+t*0.37));
        float shape=noise(p/4.0+vec2(t*0.045,-t*0.034),64.0)-0.5;
        float movement=mix(1.0,0.30,smoothstep(CENTER_START,core,pixelDepth));
        float depth=pixelDepth+CENTER_WAVE*(wave+shape*0.50)*movement;
        float middle=mix(first,core,0.50);
        float outerBand=celStep(first,depth,threshold);
        float middleBand=celStep(middle,depth,threshold);
        float deepBand=celStep(core,depth,threshold);
        if(outerBand>0.5) {
            centerColor=mix(CENTER_OUTER,CENTER_MIDDLE,middleBand);
            centerColor=mix(centerColor,CENTER_DEEP,deepBand);
            float opacity=clamp(CENTER_DARKNESS,0.0,0.95);
            float layerOpacity=mix(0.42,0.72,middleBand);
            layerOpacity=mix(layerOpacity,1.0,deepBand);
            centerAlpha=opacity*layerOpacity;
        }
    }

    float alpha=shoreAlpha+centerAlpha*(1.0-shoreAlpha);
    if(alpha*fade<.003) discard;
    vec3 color=(shoreColor*shoreAlpha+centerColor*centerAlpha*(1.0-shoreAlpha))/alpha;
    fragColor=vec4(color,alpha*fade);
}
