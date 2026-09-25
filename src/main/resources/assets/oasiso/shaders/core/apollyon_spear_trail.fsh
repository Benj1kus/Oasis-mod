#version 150
uniform float Time;
in vec2 uv;
in vec4 data;
out vec4 fragColor;


const vec3 CORE=vec3(.42,.91,1.0);
const vec3 PINK=vec3(1.0,.38,.78);
const vec3 PURPLE=vec3(.36,.12,.66);
float triangle(float x) { return 1.0-abs(fract(x)*2.0-1.0); }
float bayer(vec2 p) {
    ivec2 q=ivec2(mod(p,4.0));
    int table[16]=int[16](0,8,2,10,12,4,14,6,3,11,1,9,15,7,13,5);
    return (float(table[q.x+q.y*4])+.5)/16.0;
}
void main() {

    vec2 p=floor(uv*vec2(32.0,72.0))/vec2(32.0,72.0);
    float x=(p.x-.5)*2.0, y=p.y;
    float clock=Time*1.25+data.g*7.0;
    float boundary;
    float opacity=data.a;
    if(data.r>.5) {
        boundary=max(.0,1.0-abs(y-.42)*1.85);
    } else {
        float side=x<0.0?0.0:.47;
        float notch=triangle(y*3.0-clock+side);
        boundary=(.82-.37*y)-.24*notch;
        x-=.10*(triangle(y*2.0-clock*.65)-.5);
        opacity*=1.0-smoothstep(.62,1.0,y);
    }
    float edge=boundary-abs(x);
    float coverage=clamp(edge*18.0,0.0,1.0)*opacity;
    float threshold=bayer(floor(gl_FragCoord.xy/2.0));
    if(coverage<=threshold)discard;
    float band=abs(x)/max(boundary,.001);
    vec3 color=PURPLE;
    if(band<.69)color=PINK;
    if(band<.31)color=mix(CORE,vec3(.78,.98,1.0),1.0-band/.31);
    if(abs(band-.69)<.06 && threshold>.5)color=mix(PINK,PURPLE,.65);
    if(abs(band-.31)<.04 && threshold>.5)color=mix(CORE,PINK,.55);
    fragColor=vec4(color,1.0);
}
