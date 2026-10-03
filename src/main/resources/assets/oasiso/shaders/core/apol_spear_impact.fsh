#version 150
uniform float Progress;
uniform float Seed;
in vec2 uv;
out vec4 fragColor;
float bayer(vec2 p){ivec2 q=ivec2(mod(p,4.0));int m[16]=int[16](0,8,2,10,12,4,14,6,3,11,1,9,15,7,13,5);return (float(m[q.x+q.y*4])+.5)/16.0;}
void main(){
    vec2 cell=floor(uv*80.0),p=((cell+.5)/80.0-.5)*2.0;
    float r=length(p),a=atan(p.y,p.x)+Seed*.27;
    float teeth=1.0-abs(fract(a/6.2831853*8.0)-.5)*2.0;
    float longRay=pow(teeth,5.0);
    float shape=.19+.61*longRay*(.7+.3*cos(a*3.0+Seed));
    shape+=.075*pow(max(0.0,cos(a*4.0)),16.0);
    float inside=1.0-smoothstep(shape-.015,shape+.015,r);
    float life=1.0-smoothstep(.18,1.0,Progress);
    if(inside*life<bayer(cell))discard;
    float rim=clamp(r/max(shape,.001),0.0,1.0);
    vec3 color=mix(vec3(.28,.91,1.0),vec3(1.0,.24,.7),smoothstep(.38,.92,rim));
    float bevel=step(.67,rim)*step(rim,.83);
    color=mix(color,vec3(.14,.26,.48),bevel*.55);
    color=mix(color,vec3(.91,1.0,1.0),(.5+.5*cos(a*2.0))*step(rim,.35)*.7);
    color=floor(color*23.0+bayer(cell))/23.0;
    fragColor=vec4(color,1.0);
}
