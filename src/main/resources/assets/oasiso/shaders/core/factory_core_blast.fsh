#version 150
uniform float Age;
in vec3 relativePosition;
in vec2 texCoord;
in vec4 data;
out vec4 fragColor;
float b2(vec2 p) { p=mod(floor(p),2.0); return 2.0*p.x+3.0*p.y-4.0*p.x*p.y; }
float bayer(vec2 p) { return (4.0*b2(p)+b2(floor(p*.5))+.5)/16.0; }
float hash(vec2 p) { return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453); }
void main() {
    float phase=data.r*6.2831853;
    vec3 dark=vec3(.12,.055,.22), purple=vec3(.36,.16,.48);
    vec3 pink=vec3(.98,.58,.79), cyan=vec3(.22,.92,1.0), bright=vec3(.76,.995,1.0);
    float kind=data.g*2.0;
    vec3 color;
    vec2 cell;
    if(kind>.5 && kind<1.5) {
        cell=floor(texCoord*vec2(18.0,48.0));
        vec2 uv=(cell+.5)/vec2(18.0,48.0);
        float x=uv.x*2.0-1.0,t=uv.y;
        float bend=(.12*sin(t*11.0-Age*3.4+phase)+.07*sin(t*25.0+Age*2.1+phase*2.0))*(1.0-t);
        float width=smoothstep(.015,.68,t)*sqrt(clamp((1.0-t)/.22,0.0,1.0));
        width*=.72+.10*sin(t*24.0-Age*4.0+phase)+.07*sin(t*43.0+Age*2.5);
        float r=abs(x-bend)/max(.02,width);
        for(int i=0;i<3;i++) {
            float k=float(i), sy=.09+k*.14+.025*sin(Age*1.7+phase+k);
            float sx=(i%2==0?1.0:-1.0)*(.33+.12*sin(phase+k*2.0));
            r=min(r,abs(x-sx)/(.075+.02*sin(Age*2.0+k))+abs(t-sy)/.045);
        }
        if(r>1.0) discard;
        float d=bayer(cell);
        color=mix(dark,purple,step(d,1.0-smoothstep(.76,.97,r)));
        color=mix(color,pink,step(d,1.0-smoothstep(.50,.74,r)));
        color=mix(color,cyan,step(d,1.0-smoothstep(.20,.46,r)));
        color=mix(color,bright,step(d,(1.0-smoothstep(.06,.22,r))*smoothstep(.60,.87,t))*.7);
    } else if(kind>1.5) {
        cell=floor(texCoord*vec2(8.0,256.0));
        float r=abs(texCoord.x*2.0-1.0);
        if(sin(texCoord.y*125.6637+Age*4.0+phase)>.91) discard;
        color=mix(pink,bright,step(bayer(cell),1.0-smoothstep(.2,.85,r)));
    } else {
        cell=floor(texCoord*vec2(96.0,48.0));
        vec2 uv=(cell+.5)/vec2(96.0,48.0);
        vec2 tile=uv*vec2(3.0,2.0);
        vec2 p=fract(tile)-.5;
        float swirl=sin(length(p)*13.0-atan(p.y,p.x)*1.5-Age*1.2+phase+hash(floor(tile))*6.0);
        float rolls=sin(uv.x*14.0+sin(uv.y*12.0-Age*.8+phase)*1.5);
        vec3 normal=normalize(cross(dFdx(relativePosition),dFdy(relativePosition)));
        float facing=abs(dot(normal,normalize(-relativePosition)));
        float curl=smoothstep(.68,.91,swirl)*smoothstep(-.35,.3,rolls);
        float energy=.58*data.b+.30*facing+.11*cos(uv.y*3.141593)-.16*curl;
        float d=bayer(cell);
        color=mix(dark,purple,step(d,smoothstep(.24,.28,energy)));
        color=mix(color,pink,step(d,smoothstep(.43,.47,energy)));
        color=mix(color,cyan,step(d,smoothstep(.65,.69,energy)));
        color=mix(color,bright,step(d,smoothstep(.82,.86,energy)));
        float contour=(1.0-smoothstep(.025,.065,abs(swirl+.30)))*smoothstep(.45,.7,rolls);
        color=mix(color,purple,step(d,contour)*.7);
    }
    float breakup=hash(floor(cell/3.0)+floor(data.r*91.0));
    float coverage=smoothstep(breakup*.82,breakup*.82+.18,data.a);
    coverage*=smoothstep(.12,.55,length(relativePosition));
    if(coverage<bayer(gl_FragCoord.xy)) discard;
    fragColor=vec4(color,1.0);
}
