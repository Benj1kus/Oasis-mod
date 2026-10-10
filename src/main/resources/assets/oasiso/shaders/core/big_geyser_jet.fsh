#version 150
uniform float Age;
uniform float Seed;
uniform float DistanceFade;
uniform float ShellViewAngle;
in vec2 texCoord;
in float layer;
in float cameraDistance;
out vec4 fragColor;
float hash(vec2 p) { return fract(sin(dot(p,vec2(127.1,311.7))+Seed)*43758.5453); }
float bayer2(vec2 p) {
    p=mod(floor(p),2.0);
    return 2.0*p.x+3.0*p.y-4.0*p.x*p.y;
}
float bayer4(vec2 p) { return (4.0*bayer2(p)+bayer2(floor(p*.5))+.5)/16.0; }
float triangle(float x) { return 1.0-4.0*abs(fract(x)-.5); }
vec3 band(vec3 a,vec3 b,float value,float edge,float dither) {
    return mix(a,b,step(dither,clamp((value-edge)*6.0+.5,0.0,1.0)));
}
void main() {
    bool shard=layer>.06 && layer<.14;
    vec2 cell=floor(texCoord*vec2(36.0,72.0));
    float dither=bayer4(cell);
    float beat=floor(Age*24.0)/24.0;
    float u=cell.x/36.0,y=cell.y/72.0;
    float zig=triangle(y*3.6-beat*2.4+Seed*.021);
    float angular=abs(sin(u*6.2831853-ShellViewAngle));
    float edgeWave=triangle(y*5.0-beat*3.1+u*3.0+Seed*.03);
    float threshold=layer<.17?0.0:(layer<.5?.36:(layer<.84?.68:.81));
    float coverage=clamp((angular-threshold+edgeWave*.065)*22.0+.5,0.0,1.0);
    if(!shard && layer>.17 && coverage<dither) discard;

    float fragment=hash(vec2(mod(cell.x,36.0),floor((cell.y-beat*38.0)/4.0)));
    float front=clamp((Age-.55)/1.35,0.0,1.0)*1.2-.15;
    float dissolve=clamp((texCoord.y-front+(fragment-.5)*.17)*12.0+.35,0.0,1.0);
    if(dissolve<dither) discard;
    float fade=(1.0-smoothstep(1.65,2.0,Age))*smoothstep(0.0,.045,Age);
    float alpha=(shard?.98:.97)*fade*DistanceFade*smoothstep(.12,.65,cameraDistance);
    if(alpha<.008) discard;

    vec3 white = vec3(255.0, 253.0, 255.0) / 255.0;
    vec3 mint = vec3(91.0, 255.0, 207.0) / 255.0;
    vec3 cyan = vec3(98.0, 227.0, 255.0) / 255.0;
    vec3 outerTop = vec3(167.0, 111.0, 255.0) / 255.0;
    vec3 outerBottom = vec3(245.0, 118.0, 204.0) / 255.0;
    vec3 deepCore = vec3(16.0, 7.0, 25.0) / 255.0;

    float stripe=triangle(u*5.0+y*1.7-beat*1.6+Seed*.013)+zig*.24;
    vec3 color;
    if(shard) {
        color=band(cyan,white,fragment,.4,dither);
    } else if(layer<.17) {
        color=band(cyan,white,stripe,.02,dither);
        color=band(mint,color,1.0-angular,.12,dither);
    } else if(layer<.5) {
        color=band(outerBottom,outerTop,stripe,.48,dither);
        color=band(cyan,color,angular,.47+zig*.04,dither);
    } else if(layer<.84) {
        color=band(outerTop,outerBottom,stripe,.27,dither);
    } else {
        color=band(deepCore,outerTop,stripe,.08,dither);
    }
    if(dissolve<.85 && layer<.5 && !shard) color=band(color,white,fragment,.56,dither);
    fragColor=vec4(color,alpha);
}