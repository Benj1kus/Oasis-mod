#version 150
uniform vec4 Viewport;
uniform float Time;
uniform float Strength;
uniform float MaxWidth;
uniform float Opacity;
uniform vec3 EmotionColor;
in vec2 texCoord;
out vec4 fragColor;
const int BAYER[16]=int[16](0,8,2,10,12,4,14,6,3,11,1,9,15,7,13,5);

float jelly(vec2 p){
    vec2 bend=vec2(sin(p.y*0.72+Time*0.21),cos(p.x*0.63-Time*0.17));
    p+=bend*0.85;
    return 0.5+0.25*sin(p.x+0.65*sin(p.y))+0.25*cos(p.y+0.65*sin(p.x));
}
void over(inout vec4 layer,vec3 color,float alpha){
    alpha=clamp(alpha,0.0,1.0);
    layer=vec4(color*alpha+layer.rgb*(1.0-alpha),alpha+layer.a*(1.0-alpha));
}
float bayer(ivec2 p){
    return (float(BAYER[(p.x&3)+4*(p.y&3)])+0.5)/16.0;
}
const float HALO_WIDTH=0.16;
const float HALO_ALPHA=0.16;
const float SHADOW_ALPHA=0.62;
const float RIM_ALPHA=0.88;

void main(){
    vec2 size=Viewport.zw;
    float shortSide=min(size.x,size.y);
    vec2 pixel=texCoord*size;
    vec2 edge=min(pixel,size-pixel)/shortSide;
    float pulse=0.94+0.06*sin(Time*1.25);
    float base=MaxWidth*Strength*pulse;
    if(base<0.00001 || min(edge.x,edge.y)>=base*(1.0+HALO_WIDTH))discard;

    const float PIXEL_SIZE=3.0;
    vec2 cell=floor((gl_FragCoord.xy-Viewport.xy)/PIXEL_SIZE);
    vec2 q=floor(pixel/PIXEL_SIZE)*PIXEL_SIZE/shortSide;
    vec2 flow=q*33.0+vec2(Time*0.22,-Time*0.32);
    float field=jelly(flow);

    float breatheY=sin(q.y*13.0-Time*0.77);
    float breatheX=sin(q.x*11.0+Time*0.69);
    float side=0.76+0.12*sin(q.y*24.0-Time*0.65+0.5*breatheY)
            +0.055*sin(q.y*43.0+Time*0.43)+0.045*breatheY;
    float cap=0.76+0.12*sin(q.x*24.0+Time*0.59+0.5*breatheX)
            +0.055*sin(q.x*43.0-Time*0.39)+0.045*breatheX;
    side=clamp(side,0.50,0.98); cap=clamp(cap,0.50,0.98);
    float t=min(edge.x/(base*side),edge.y/(base*cap));
    if(t>=1.0+HALO_WIDTH)discard;
    float appear=smoothstep(0.0,0.12,Strength);
    ivec2 c=ivec2(cell);
    float threshold=bayer(c);

    float low=min(EmotionColor.r,min(EmotionColor.g,EmotionColor.b));
    float high=max(EmotionColor.r,max(EmotionColor.g,EmotionColor.b));
    vec3 saturated=clamp((EmotionColor-vec3(low*0.65))/max(high-low*0.65,0.001),0.0,1.0);

    float body=smoothstep(0.43,0.46,field);
    float core=smoothstep(0.69,0.72,field);
    vec3 color=mix(saturated,mix(saturated,vec3(1.0),0.18),body);
    color=mix(color,mix(saturated,vec3(1.0),0.34),core);

    float shifted=jelly(flow+vec2(0.33,-0.38));
    float gloss=smoothstep(0.63,0.66,shifted)*(1.0-smoothstep(0.59,0.62,field));
    color=mix(color,mix(saturated,vec3(1.0),0.62),gloss);
    vec4 layers=vec4(0.0);

    float glowPulse=0.90+0.10*sin(Time*1.25+field*2.0);
    float glowCoverage=smoothstep(0.66,0.90,t)
            *(1.0-smoothstep(0.96,1.0+HALO_WIDTH,t))*appear;
    if(glowCoverage>bayer(c+ivec2(1,2)))
        over(layers,mix(saturated,vec3(1.0),0.18),Opacity*HALO_ALPHA*glowPulse*appear);

    float bodyCoverage=(1.0-smoothstep(0.90,1.0,t))*appear;
    if(t<1.0 && bodyCoverage>threshold){
        over(layers,color,Opacity*(0.94+0.06*gloss)*appear);

        float shadowEnd=0.48+0.035*sin(q.x*19.0+q.y*17.0-Time*0.57);
        float shadowCoverage=(1.0-smoothstep(0.07,shadowEnd,t))*appear;
        if(shadowCoverage>bayer(c+ivec2(2,1)))
            over(layers,saturated*0.22,Opacity*SHADOW_ALPHA*appear);

        float rimCoverage=smoothstep(0.56,0.85,t)
                *(1.0-smoothstep(0.94,1.0,t))*appear;
        if(rimCoverage>bayer(c+ivec2(3,0))){
            vec3 rim=mix(saturated,vec3(1.0),t>0.87?0.66:0.42);
            over(layers,rim,Opacity*RIM_ALPHA*glowPulse*appear);
        }
    }
    if(layers.a<0.001)discard;
    fragColor=vec4(layers.rgb/layers.a,layers.a);
}
