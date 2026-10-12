#version 150
uniform float Time;
in vec2 texCoord;
in float cameraDistance;
in vec4 flameData;
out vec4 fragColor;
float bayer2(vec2 p) { p=mod(floor(p),2.0);return 2.0*p.x+3.0*p.y-4.0*p.x*p.y; }
float bayer4(vec2 p) { return (4.0*bayer2(p)+bayer2(floor(p*.5))+.5)/16.0; }
void main() {
    vec2 cell=floor(texCoord*vec2(18.0,48.0));
    vec2 uv=(cell+.5)/vec2(18.0,48.0);
    float x=uv.x*2.0-1.0,t=uv.y;
    float phase=flameData.r*6.2831853,age=Time;
    float dither=bayer4(cell);
    float bend=(.12*sin(t*11.0-age*3.4+phase)+.07*sin(t*25.0+age*2.1+phase*2.0))*(1.0-t);
    float width=smoothstep(.015,.68,t)*sqrt(clamp((1.0-t)/.22,0.0,1.0));
    width*=.72+.10*sin(t*24.0-age*4.0+phase)+.07*sin(t*43.0+age*2.5);
    float r=abs(x-bend)/max(.02,width);
    for(int i=0;i<3;i++) {
        float k=float(i);
        float sy=.09+k*.14+.025*sin(age*1.7+phase+k);
        float sx=(i%2==0?1.0:-1.0)*(.33+.12*sin(phase+k*2.0));
        float spark=abs(x-sx)/(.075+.02*sin(age*2.0+k))+abs(t-sy)/.045;
        r=min(r,spark);
    }
    if(r>1.0) discard;
    vec3 navy=vec3(.09,.10,.22);
    vec3 purple=vec3(.37,.26,.52);
    vec3 pink=vec3(.96,.64,.79);
    vec3 cyan=vec3(.27,.91,.98);
    vec3 color=mix(navy,purple,step(dither,1.0-smoothstep(.76,.97,r)));
    color=mix(color,pink,step(dither,1.0-smoothstep(.50,.74,r)));
    color=mix(color,cyan,step(dither,1.0-smoothstep(.20,.46,r)));
    float highlight=(1.0-smoothstep(.06,.22,r))*smoothstep(.60,.87,t);
    color=mix(color,vec3(.74,.98,1.0),step(dither,highlight)*.7);
    float alpha=.97*flameData.a*smoothstep(.10,.55,cameraDistance);
    if(alpha<.005) discard;
    fragColor=vec4(color,alpha);
}
