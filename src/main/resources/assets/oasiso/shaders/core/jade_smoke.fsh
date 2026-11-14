#version 150
uniform float Time;
in vec2 uv;
in vec2 style;
out vec4 fragColor;
const int BAYER[16]=int[16](0,8,2,10,12,4,14,6,3,11,1,9,15,7,13,5);
float hash(vec2 cell){
    vec3 p=fract(vec3(cell.xyx)*vec3(.1031,.1030,.0973));
    p+=dot(p,p.yzx+33.33);
    return fract((p.x+p.y)*p.z);
}
float noise(vec2 p,float period){
    vec2 i=floor(p),f=fract(p);
    f=f*f*(3.0-2.0*f);
    return mix(mix(hash(mod(i,period)),hash(mod(i+vec2(1,0),period)),f.x),
               mix(hash(mod(i+vec2(0,1),period)),hash(mod(i+1.0,period)),f.x),f.y);
}
void main(){
    bool plume=style.x>.25 && style.x<.75;
    vec2 grid=plume ? vec2(18,32) : vec2(32,24);
    vec2 cell=floor(uv*grid);
    vec2 p=(cell+.5)/grid;
    ivec2 b=ivec2(mod(cell,4.0));
    float threshold=(float(BAYER[b.x+4*b.y])+.5)/16.0;
    float density;
    if(plume){
        float h=p.y,seed=floor(p.x);
        float bend=.12*sin(h*5.5-Time*.85+seed)+.06*sin(h*9.0-Time*.43+seed);
        float side=abs(fract(p.x)-.5-bend*h);
        float edge=(1.0-smoothstep(.03,.14+.27*h,side))*smoothstep(0.0,.14,h)*(1.0-smoothstep(.54,1.0,h));
        vec2 flow=vec2(p.x*4.0,h*3.5-Time*.65);
        density=edge*smoothstep(.22,.72,.72*noise(flow,256.0)+.28*noise(flow*2.0,512.0));
    }else{
        vec2 local=vec2(fract(p.x),p.y)*2.0-1.0;
        float seed=floor(p.x);
        vec2 flow=vec2(seed*1.71+Time*.13,seed*.43-Time*.23);
        vec2 wobble=vec2(noise(local*1.9+flow,256.0),
                         noise(local*1.9+flow+11.3,256.0))-.5;
        float edge=length(local + wobble*.26);
        float coarse=noise(local*2.2+flow+wobble*.65,256.0);
        float fine=noise(local*5.2-flow*.54,256.0);
        float mask=1.0-smoothstep(.44,.89,edge);
        density=mask*smoothstep(.19,.76,.73*coarse+.27*fine);

    }
    float band=floor(density*5.0+threshold)/5.0;
    if(band<.01) discard;
    fragColor=vec4(vec3(44,245,181)/255.0,style.y*band);
}
