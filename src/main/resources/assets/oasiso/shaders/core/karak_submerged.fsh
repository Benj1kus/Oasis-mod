#version 150
in vec2 uv;
uniform float Time;
uniform float Strength;
uniform float Aspect;
out vec4 fragColor;

float wave(float p,float seed) {
    float swell=sin(p*8.0-Time*1.6+seed)*.018;
    float curl=sin(p*19.0+Time*2.1+seed+sin(p*5.0-Time*.8))*.015;
    float ripple=sin(p*43.0-Time*2.8+seed*1.7)*.006;
    return swell+curl+ripple;
}
vec2 hash2(vec2 p) {
    return fract(sin(vec2(dot(p,vec2(127.1,311.7)),dot(p,vec2(269.5,183.3))))*43758.5453);
}
float web(vec2 p) {
    vec2 q=p*31.0;
    q+=vec2(sin(p.y*17.0+Time*.9),cos(p.x*13.0-Time*.7))*.55;
    q+=vec2(Time*.30,-Time*.22);
    vec2 cell=floor(q), f=fract(q);
    float first=8.0, second=8.0;
    for(int y=-1;y<=1;y++) for(int x=-1;x<=1;x++) {
        vec2 offset=vec2(x,y);
        vec2 v=offset+.15+.7*hash2(cell+offset)-f;
        float d=dot(v,v);
        if(d<first) { second=first; first=d; }
        else second=min(second,d);
    }
    float gap=second-first;
    float aa=max(fwidth(gap),.012);
    return 1.0-smoothstep(.030-aa,.060+aa,gap);
}
float edgeDistance(vec2 p) {
    float left=p.x-wave(p.y,0.0);
    float right=Aspect-p.x-wave(p.y,2.1);
    float top=p.y-wave(p.x,4.3);
    float bottom=1.0-p.y-wave(p.x,6.2);
    return min(min(left,right),min(top,bottom));
}
void main() {
    vec2 p=vec2(uv.x*Aspect,uv.y);
    float d=edgeDistance(p);
    if(d>.19 || Strength<=.001) { fragColor=vec4(0.0); return; }
    float body=1.0-smoothstep(.015,.19,d);
    float band=smoothstep(.018,.050,d)*(1.0-smoothstep(.120,.168,d));
    float crest=1.0-smoothstep(.003,.013,abs(d-.132));
    float lace=web(p)*band;
    vec2 shadowPoint=p-vec2(.006,.010);
    float sd=edgeDistance(shadowPoint);
    float shadowBand=smoothstep(.018,.05,sd)*(1.0-smoothstep(.120,.168,sd));
    float shadow=web(shadowPoint)*shadowBand;
    float foam=max(lace*.95,crest*.62);
    float depth=.5+.5*sin(p.x*8.0+p.y*11.0-Time*.9);
    vec3 color=mix(vec3(.012,.33,.25),vec3(.025,.70,.56),depth*.45+.35);
    color=mix(color,vec3(.19,.66,.90),smoothstep(.055,.145,d)*.72);
    color=mix(color,vec3(.025,.17,.40),shadow*.74);
    color=mix(color,vec3(.91,1.0,.97),foam);
    float alpha=max(body*.75,max(foam*.93,shadow*.65))*Strength;
    fragColor=vec4(color,alpha);
}
