#version 150
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec2 FieldSize;
uniform vec2 Origin;
uniform float WidthScale;
uniform float Time;
uniform float Opacity;
uniform vec3 EmotionColor;
in vec2 texCoord;
out vec4 fragColor;
const int BAYER[16]=int[16](0,8,2,10,12,4,14,6,3,11,1,9,15,7,13,5);
void main(){
    if(texture(Sampler0,texCoord).a>0.1)discard;
    float best=64.0,depth=1.0;
    for(int y=-16;y<=16;y++){
        vec2 uv=texCoord+vec2(0.0,float(y))/FieldSize;
        if(any(lessThan(uv,vec2(0.0)))||any(greaterThan(uv,vec2(1.0))))continue;
        vec2 field=texture(Sampler1,uv).rg;
        float d=length(vec2(field.x,float(y)));
        if(d<best){best=d;depth=field.y;}
    }
    vec2 p=floor(texCoord*FieldSize)-Origin;
    float wave=sin(p.y*0.15-Time*2.6)+0.45*sin(p.x*0.22+p.y*0.09+Time*1.8);
    float width=(3.0+0.8*wave)*WidthScale;
    if(best>width+1.5 || depth>=1.0)discard;
    float coverage=(1.0-smoothstep(width-0.6,width+1.5,best))*Opacity;
    ivec2 cell=ivec2(floor(gl_FragCoord.xy/2.0));
    float threshold=(float(BAYER[(cell.x&3)+4*(cell.y&3)])+0.5)/16.0;
    if(coverage<threshold)discard;
    float edge=clamp(best/max(width,0.1),0.0,1.0);
    vec3 bright=mix(EmotionColor,vec3(1.0),0.28);
    vec3 color=mix(bright,EmotionColor,smoothstep(0.1,0.5,edge));
    color*=0.92+0.08*sin(p.y*0.16-Time*2.6);
    fragColor=vec4(color,0.92*Opacity);
    gl_FragDepth=max(0.0,depth-0.000002);
}
