#version 150
in vec2 uv;
in vec4 edges;
in float shoreDistance;
in float bankDistance;
in float distanceToCamera;
uniform float Time;
uniform float Range;
out vec4 fragColor;

//brightness
const float CENTER_DARKNESS = 0.50;
//distance to center
const float CENTER_START = 0.30;
//anchor
const float CENTER_CORE = 2.85;
//waving stenght
const float CENTER_WAVE = 3.00;
// waves  speed
const float CENTER_SPEED = 2.5;
//size of pixel-dithering
const float CENTER_PIXELS = 8.0;
//dithering width
const float CEL_EDGE_WIDTH = 0.100;
//colors
const vec3 CENTER_OUTER  = vec3(0.650, 0.300, 0.550);
const vec3 CENTER_MIDDLE = vec3(0.880, 0.220, 0.650);
const vec3 CENTER_DEEP   = vec3(1.000, 0.350, 0.750);

const float SHORE_WIDTH = 0.52;
const float SHORE_WAVE = 1.28;
const float SHORE_SPEED = 1.0;
const float FRAGMENT_PERIOD = 8.5;
const float FRAGMENT_TRAVEL = 1.70;
const float FRAGMENT_AMOUNT = 0.78;

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


float pixelBankDistance(vec2 pixelCenter) {
    vec2 dx=dFdx(uv),dy=dFdy(uv);
    float sx=dFdx(bankDistance),sy=dFdy(bankDistance);
    float determinant=dx.x*dy.y-dx.y*dy.x;
    if(abs(determinant)<1e-10) return bankDistance;
    vec2 gradient=vec2(sx*dy.y-sy*dx.y,dx.x*sy-dy.x*sx)/determinant;
    return max(0.0,bankDistance+dot(gradient,pixelCenter-uv));
}

float shoreWave(vec2 p,float t) {
    vec2 phase=mod(p,16.0)*TAU/16.0;
    return 0.64*sin(phase.x*4.0+phase.y*2.0-t*1.12+0.6*sin(phase.y+t*.47))
         + 0.36*sin(phase.y*4.0-phase.x*2.0+t*.86);
}

vec4 livingShore(vec2 p,float depth,float threshold) {
    float t=Time*SHORE_SPEED;
    float width=max(0.16,SHORE_WIDTH+SHORE_WAVE*shoreWave(p,t));
    float band=1.0-smoothstep(0.0,width,depth);
    band=floor(band*5.0+threshold)/5.0;
    float bright=1.0-smoothstep(0.0,0.16,depth);
    float pulse=.90+.10*sin(Time*1.8);
    vec3 color=mix(vec3(0.82, 0.28, 0.58), vec3(0.12, 0.62, 0.78), band);
    color=mix(color, vec3(0.25, 0.92, 0.78), bright*.7);
    float alpha=band*band*.82*pulse;
    vec4 result=vec4(color*alpha,alpha);

    float limit=SHORE_WIDTH+abs(SHORE_WAVE)+max(FRAGMENT_TRAVEL,0.0)+0.45;
    if(FRAGMENT_AMOUNT<=0.0 || depth>limit) return result;
    float pixel=1.0/CENTER_PIXELS;

    for(int i=0;i<3;i++) {
        float clock=t/max(FRAGMENT_PERIOD,0.5)+float(i)*0.37;
        float age=fract(clock);

        float growth=smoothstep(0.0,0.17,age);
        float dissolve=1.0-smoothstep(0.52,0.96,age);
        if(growth*dissolve<0.015) continue;
        float travel=smoothstep(0.08,0.82,age)*max(FRAGMENT_TRAVEL,0.0);
        float separation=depth-(width*.60+travel);
        if(abs(separation)>0.34+pixel) continue;

        float seed=mod(floor(clock)+float(i)*13.0,64.0);
        vec2 source=p+vec2(travel*.18,-travel*.13);
        float shape=.72*noise(source*2.0+vec2(seed*3.0,seed*7.0),512.0)
                   +.28*noise(source*4.0+vec2(seed*11.0,seed*5.0),1024.0);
        float thickness=.12+.08*shape;
        float strip=1.0-smoothstep(thickness,thickness+pixel*.85,abs(separation));
        float pieces=smoothstep(.48,.69,shape);
        float coverage=strip*pieces*growth*dissolve;

        if(coverage<=threshold) continue;
        float core=1.0-smoothstep(.02,.18,abs(separation));
        vec3 pieceColor=mix(vec3(0.85, 0.25, 0.62), vec3(0.15, 0.70, 0.85), core*.70);
        pieceColor=mix(pieceColor, vec3(0.40, 0.95, 0.82), core*growth*.18);
        float pieceAlpha=clamp(FRAGMENT_AMOUNT,0.0,1.0)*(.50+.22*core);
        result=vec4(pieceColor*pieceAlpha,pieceAlpha)+result*(1.0-pieceAlpha);
    }
    return result;
}

void main() {
    vec2 grid=floor(uv*CENTER_PIXELS+0.002);
    vec2 p=(grid+0.5)/CENTER_PIXELS;
    float pixelDepth=pixelShoreDistance(p);
    float bankDepth=pixelBankDistance(p);
    vec2 local=fract(uv);
    float d=2.0;
    if(edges.r>.5) d=min(d,local.x);
    if(edges.g>.5) d=min(d,1.0-local.x);
    if(edges.b>.5) d=min(d,local.y);
    if(edges.a>.5) d=min(d,1.0-local.y);

    float fade=1.0-smoothstep(Range-3.0,Range,distanceToCamera);
    if(fade<=0.0) discard;
    float threshold=bayer4(grid);

    vec2 pixelLocal=fract(p);
    if(edges.r>.5) bankDepth=min(bankDepth,pixelLocal.x);
    if(edges.g>.5) bankDepth=min(bankDepth,1.0-pixelLocal.x);
    if(edges.b>.5) bankDepth=min(bankDepth,pixelLocal.y);
    if(edges.a>.5) bankDepth=min(bankDepth,1.0-pixelLocal.y);
    vec4 shore=livingShore(p,bankDepth,threshold);
    float shoreAlpha=shore.a;
    vec3 shoreColor=shoreAlpha>0.0?shore.rgb/shoreAlpha:vec3(0.0);

    float centerAlpha=0.0;
    vec3 centerColor=CENTER_OUTER;
    if(CENTER_DARKNESS>0.0 && pixelDepth>0.65 && d>0.50) {
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
