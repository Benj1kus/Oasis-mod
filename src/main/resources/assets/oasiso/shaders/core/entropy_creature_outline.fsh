#version 150
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec2 FieldSize;
uniform vec2 Origin;
uniform float WidthScale;
uniform float Time;
uniform float Opacity;
in vec2 uv;
out vec4 fragColor;

//settings
const float BASE_WIDTH = 3.0;
const float FLAME_LENGTH = 8.0;
const float FRAGMENT_TRAVEL = 23.0;
const float FLAME_SPEED = 3.5;
const float GRADIENT_SPEED = 1.80;
const int SEARCH = 20;
const vec3 CYAN = vec3(98,255,227)/255.0;
const vec3 BLUE = vec3(98,227,255)/255.0;
const vec3 PINK_LIGHT = vec3(237,186,255)/255.0;
const vec3 PINK = vec3(239,130,216)/255.0;

float hash(float p) { return fract(sin(p*127.1+311.7)*43758.5453); }
float bayer2(vec2 p) { p=mod(floor(p),2.0); return 2.0*p.x+3.0*p.y-4.0*p.x*p.y; }
float bayer4(vec2 p) { return (4.0*bayer2(p)+bayer2(floor(p*.5))+.5)/16.0; }
vec3 palette(float phase) {
    float p=mod(phase,4.0), f=fract(p); f=.5-.5*cos(f*3.14159265);
    if(p<1.0) return mix(CYAN,BLUE,f);
    if(p<2.0) return mix(BLUE,PINK_LIGHT,f);
    if(p<3.0) return mix(PINK_LIGHT,PINK,f);
    return mix(PINK,CYAN,f);
}
void main() {

    if(texture(Sampler0,uv).a > .1) discard;
    vec2 grid=floor(uv*FieldSize), p=(grid+.5)/FieldSize;
    vec4 nearest=vec4(0.0);
    float best=1e6;
    for(int i=-SEARCH;i<=SEARCH;i++) {
        vec2 at=p+vec2(0,float(i)/FieldSize.y);
        if(at.y<0.0 || at.y>=1.0) continue;
        vec4 candidate=texture(Sampler1,at);
        if(candidate.a<.5) continue;
        vec2 delta=(candidate.xy-p)*FieldSize;
        float d=dot(delta,delta);
        if(d<best) {best=d; nearest=candidate;}
    }
    if(nearest.a<.5 || best>400.0) discard;
    float scale=max(WidthScale,.1);
    vec2 outward=(p-nearest.xy)*FieldSize;
    float distance=length(outward)/scale;
    vec2 normal=outward/max(length(outward),.001);
    vec2 q=(grid-Origin)/scale;
    float t=Time*FLAME_SPEED;

    float bend=1.7*sin(q.y*.27-t*2.0)+.8*sin(q.y*.61-t*1.3);
    float strand=.5+.5*sin((q.x+bend)*.64+q.y*.17-t*.7);
    float secondary=.5+.5*sin(q.x*.29-q.y*.47+t*2.9);
    float upward=clamp(.75+normal.y*.65,.22,1.35);
    float tongue=FLAME_LENGTH*pow(strand,3.5)*(.45+.55*secondary)*upward;
    float rim=BASE_WIDTH+.45*sin(q.y*.6+q.x*.3-t*2.0);
    float body=1.0-smoothstep(rim+tongue-.8,rim+tongue+.7,distance);

    float coordinate=(q.x*.78+q.y*.36-t*1.3)/6.0;
    float lane=floor(coordinate);
    float seed=hash(lane);
    float phase=fract(t*(.30+seed*.09)+seed);
    float side=(fract(coordinate)-.5)*6.0;
    float radius=mix(1.5,.35,phase);
    float travel=mix(rim,FRAGMENT_TRAVEL,phase);
    float fragmentShape=length(vec2(side/radius,(distance-travel)/(radius*1.4)));
    float fragment=(1.0-smoothstep(.6,1.1,fragmentShape));
    fragment*=smoothstep(0.0,.10,phase)*(1.0-smoothstep(.60,1.0,phase));
    fragment*=smoothstep(-.55,.2,normal.y);
    float coverage=max(body,fragment);
    if(coverage*Opacity<=bayer4(grid)) discard;
    vec3 color=palette(Time*GRADIENT_SPEED+q.x*.016+q.y*.025-distance*.055);

    color*=mix(.78,1.0,1.0-smoothstep(0.0,14.0,distance));
    fragColor=vec4(color,1.0);
    gl_FragDepth=max(0.0,nearest.b-.000001);
}
