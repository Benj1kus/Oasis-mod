#version 150
uniform float FogTime;
uniform float Opacity;
uniform float Layer;
uniform float DebugMode;
in vec4 tint;
in vec2 uv;
out vec4 fragColor;
float hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
float noise(vec2 p){
 vec2 i=floor(p),f=fract(p); f=f*f*(3.0-2.0*f);
 return mix(mix(hash(i),hash(i+vec2(1,0)),f.x),mix(hash(i+vec2(0,1)),hash(i+vec2(1,1)),f.x),f.y);
}
float bayer(){
 ivec2 p=ivec2(gl_FragCoord.xy);
 int x=p.x&3,y=p.y&3;
 int k=((x&1)^(y&1))*8+(y&1)*4+(((x>>1)&1)^((y>>1)&1))*2+((y>>1)&1);
 return (float(k)+.5)/16.0;
}
void main(){
 if(DebugMode>.5){fragColor=vec4(0.0,1.0,.7,.32*tint.a);return;}
 vec2 p=floor(uv*6.0)/6.0;
 vec2 flow=vec2(FogTime*.13,-FogTime*.08);
 vec2 bend=vec2(noise(p*.12+flow*.3),noise(p*.12-flow*.25+19.0))-.5;
 float n=noise(p*.30+flow+bend*.9+Layer*.10);
 float fine=noise(p*.73-flow*.7+Layer*.12);
 float shape=n*.78+fine*.22;
 float density=smoothstep(.47,.72,shape);
 density=floor(density*5.0+bayer())/5.0;
 float edge=smoothstep(0.0,.24,density);
 if(edge<=bayer()*.65) discard;
 float layerFade=1.0/(1.0+Layer*.7);
 fragColor=vec4(tint.rgb,Opacity*tint.a*density*layerFade);
}
