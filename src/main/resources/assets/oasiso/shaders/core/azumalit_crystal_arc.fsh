#version 150
in vec2 texCoord;
in vec4 arcData;
out vec4 fragColor;

const vec3 PURPLE=vec3(0.56,0.25,1.0);
const vec3 PINK=vec3(1.0,0.32,0.77);
const vec3 BLUE=vec3(0.14,0.73,1.0);
const vec3 WHITE=vec3(1.0);
const float IMPACT=4.0;
const float LIFE=26.0;

float bayer2(vec2 p) {
    p=mod(floor(p),2.0);
    return 2.0*p.x+3.0*p.y-4.0*p.x*p.y;
}
float bayer4(vec2 p) {
    return (4.0*bayer2(p)+bayer2(floor(p*.5))+.5)/16.0;
}
float hash(vec2 p) {
    return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);
}

void main() {
    float age=arcData.r*LIFE;
    float after=age-IMPACT;
    float kind=arcData.g*255.0;
    float dither=bayer4(floor(gl_FragCoord.xy/2.0));
    vec3 halo=mix(PURPLE,PINK,smoothstep(0.3,IMPACT,age));
    if(after>=0.0) halo=mix(BLUE,mix(BLUE,PINK,.26),smoothstep(7.0,20.0,after));
    float white=0.0,alpha=0.0;

    if(kind>220.0) {
        vec2 p=(floor(texCoord*40.0)+.5)/40.0*2.0-1.0;
        float angle=atan(p.y,p.x);
        float radius=length(p);
        float spikes=.70+.17*cos(angle*6.0)+.075*cos(angle*3.0+1.1);
        float edge=radius/max(.25,spikes);
        float ring=1.0-smoothstep(.065,.20,abs(edge-.73));
        float center=(1.0-smoothstep(.05,.60,edge))*(1.0-smoothstep(0.2,3.5,after));
        float fade=1.0-smoothstep(2.0,9.0,after);
        white=max(center,ring*.75)*fade;
        alpha=max(center,ring*.92)*fade;
        alpha=max(alpha,exp(-edge*edge*3.8)*.38*fade);
        if(after<0.0)discard;
    } else if(kind>120.0) {
        vec2 p=(floor(texCoord*vec2(12,20))+.5)/vec2(12,20);
        float width=(1.0-p.y)*.46+.025;
        float edge=abs(p.x-.5)/width;
        alpha=(1.0-smoothstep(.65,1.15,edge));
        white=1.0-smoothstep(.22,.60,edge);
    } else {
        float x=abs(((floor(texCoord.x*24.0)+.5)/24.0-.5)*2.0);
        white=1.0-smoothstep(.13,.24,x);
        float glow=exp(-x*x*4.6)*.62;
        alpha=max(white,glow);
        float fade=1.0-smoothstep(3.0,9.0,after);
        if(kind>30.0)fade*=.65;

        vec2 cell=floor(vec2(texCoord.x*9.0,texCoord.y*64.0));
        float erosion=smoothstep(1.5,9.0,after);
        if(hash(cell+arcData.b*79.0)<erosion*.92)discard;
        alpha*=fade;
    }

    alpha*=arcData.a;
    if(alpha<.005)discard;
    float coverage=smoothstep(0.0,.24,alpha);
    if(coverage<=dither)discard;
    vec3 color=mix(halo,WHITE,clamp(white,0.0,1.0));
    fragColor=vec4(color,clamp(alpha,0.0,1.0));
}
