#version 150
uniform float Time;
uniform vec2 Resolution;
in vec2 uv;
out vec4 fragColor;
float hash(float n){return fract(sin(n*127.1+311.7)*43758.5453);}
float bayer(vec2 p){
    ivec2 q=ivec2(mod(p,4.0));
    int m[16]=int[16](0,8,2,10,12,4,14,6,3,11,1,9,15,7,13,5);
    return (float(m[q.y*4+q.x])+.5)/16.0;
}
void main(){
    float pixelSize=max(3.0,floor(Resolution.y/180.0+.5));
    vec2 cells=max(vec2(1),floor(Resolution/pixelSize));
    vec2 p=(floor(uv*cells)+.5)/cells;
    float aspect=Resolution.x/max(1.0,Resolution.y);
    vec3 color=vec3(0);float cover=0;
    for(int edge=0;edge<4;edge++){
        vec2 q=edge==0?p:edge==1?vec2(1.0-p.x,p.y):edge==2?p.yx:vec2(1.0-p.y,p.x);
        float alongScale=edge<2?1.0:aspect;
        float depthScale=edge<2?aspect:1.0;

        for(int j=0;j<9;j++){
            float seed=float(j+edge*13);
            float h=hash(seed);
            float base=(float(j)+.35+.3*h)/9.0;

            // Чем ближе к углам, тем массивнее и короче щупальце.
            float corner=1.0-smoothstep(.08,.24,min(base,1.0-base));

            float reach=mix(.15+.20*hash(seed+8.0),
                            .105+.055*hash(seed+21.0),corner);
            reach*=1.0+.10*sin(Time*(.85+.45*h)+seed);
            float t=q.x*depthScale/reach;

            // Независимая скорость и усиленные изгибы в углах.
            float bend=(.018+.018*h+.055*corner)/alongScale;
            float speed=1.05+.85*hash(seed+4.0)+.7*corner;
            float sway=bend*(sin(t*5.6-Time*speed+seed)
            +.38*sin(t*10.5+Time*speed*.67+seed*1.7))
            *sin(clamp(t,0.0,1.0)*2.5);
            float center=base+sway;

            float thickness=(.008+.026*hash(seed+12.0)
            +.044*corner)/alongScale;
            float width=thickness*pow(max(0.0,1.0-t),.72);
            width*=.92+.08*sin(t*17.0-Time*1.3+seed);

            float dist=abs(q.y-center);
            float aa=1.0/(edge<2?cells.y:cells.x);
            float ink=(1.0-smoothstep(width,width+aa,dist))
            *(1.0-smoothstep(.97,1.0,t));

            float section=dist/max(width,.0001);
            float rim=smoothstep(.62,.97,section);

            vec3 body=mix(vec3(23,22,53)/255.0,
                          vec3(.025,.045,.16),
                          .5+.5*sin(t*4.0+Time*.7+seed));
            vec3 accent=mix(vec3(.17,.72,.9),
                            vec3(.74,.19,.57),
                            .5+.5*sin(seed+t*5.0-Time*.6));

            // Ступенчатые тени и прерывистые цветные блики.
            float bands=floor(clamp(section,0.0,1.0)*3.0)/3.0;
            body*=.78+.32*bands;
            float glint=pow(.5+.5*sin(t*14.0-Time*1.5+seed),3.0);
            vec3 c=mix(body,accent,rim*(.28+.28*glint));
            float dither=bayer(floor(uv*cells));
            ink=step(dither,ink);
            if(ink>0.0){color=c;cover=1.0;}
        }
    }
    color=floor(color*23.0+bayer(floor(uv*cells)))/23.0;
    fragColor=vec4(color,cover*smoothstep(0.0,.25,Time));
}
