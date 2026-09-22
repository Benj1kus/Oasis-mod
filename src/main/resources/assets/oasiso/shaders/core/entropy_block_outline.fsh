#version 150
uniform sampler2D Sampler0;
uniform vec2 FieldSize;
uniform float Time;
uniform float WidthScale;
in vec2 uv;
out vec4 fragColor;

const float INNER_WIDTH=2.6;
const float OUTER_WIDTH=4.8;
const float WAVE_AMOUNT=3.1;
const float GRADIENT_SPEED=.60;

const vec3 CYAN=vec3(98,255,227)/255.0;
const vec3 BLUE=vec3(98,227,255)/255.0;
const vec3 PINK_LIGHT=vec3(237,186,255)/255.0;
const vec3 PINK=vec3(239,130,216)/255.0;
const int RADIUS=12;
const float MAX_D2=144.0;
float bayer2(vec2 p) {p=mod(floor(p),2.0);return 2.0*p.x+3.0*p.y-4.0*p.x*p.y;}
float bayer4(vec2 p) {return (4.0*bayer2(p)+bayer2(floor(p*.5))+.5)/16.0;}
vec4 fieldAt(vec2 p) {
    if(any(lessThan(p,vec2(0))) || any(greaterThanEqual(p,vec2(1)))) return vec4(1,0,0,1);
    return texture(Sampler0,p);
}
vec3 palette(float phase) {
    float p=mod(phase,4.0),f=fract(p);f=.5-.5*cos(f*3.14159265);
    if(p<1) return mix(CYAN,BLUE,f);
    if(p<2) return mix(BLUE,PINK_LIGHT,f);
    if(p<3) return mix(PINK_LIGHT,PINK,f);
    return mix(PINK,CYAN,f);
}
void main() {
    vec2 grid=floor(uv*FieldSize),p=(grid+.5)/FieldSize;
    vec2 best=vec2(MAX_D2);
    for(int i=-RADIUS;i<=RADIUS;i++) {
        vec2 horizontal=fieldAt(p+vec2(0,float(i)/FieldSize.y)).rg*MAX_D2;
        best=min(best,horizontal+float(i*i));
    }
    bool inside=fieldAt(p).b>.5;
    float distance=sqrt(inside?best.y:best.x);

    vec2 q=grid/max(WidthScale,.01);
    float wave=.66*sin(q.x*.060+q.y*.035-Time*2.2)
              +.34*sin(q.y*.090-q.x*.025+Time*1.6);
    float width=(inside?INNER_WIDTH:OUTER_WIDTH)*WidthScale;
    width+=WAVE_AMOUNT*WidthScale*(inside?-wave*.65:wave);
    width=clamp(width,1.1,10.3);
    float coverage=1.0-smoothstep(width-1.15,width+.45,distance-.5);
    if(coverage<=bayer4(grid)) discard;
    vec3 color=inside?vec3(1.0):palette(Time*GRADIENT_SPEED+q.x*.007+q.y*.010);
    fragColor=vec4(color,1.0);
}
