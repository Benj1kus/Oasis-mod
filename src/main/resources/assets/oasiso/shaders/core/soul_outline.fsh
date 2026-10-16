#version 150
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec2 ScreenSize;
uniform float Radius;
uniform float Time;
uniform float Seed;
uniform float Mode;
uniform float Amount;
in vec2 texCoord;
out vec4 fragColor;
const vec3 SOUL=vec3(0.0,1.0,150.0/255.0);
float b2(vec2 p){p=mod(floor(p),2.0);return 2.0*p.x+3.0*p.y-4.0*p.x*p.y;}
float bayer(vec2 p){return (4.0*b2(p)+b2(floor(p*.5))+.5)/16.0;}
float sampleMask(vec2 p){
 if(any(lessThan(p,vec2(0)))||any(greaterThan(p,vec2(1))))return 0.0;
 return texture(Sampler0,p).a;
}
void main(){
 vec2 pixel=gl_FragCoord.xy;
 float dither=bayer(floor(pixel/2.0));
 float inside=sampleMask(texCoord);
 if(Mode<.5 && inside>.1)discard;
 float outer=0.0,inner=0.0,solid=0.0,z=1.0;
 if(inside>.1)z=texture(Sampler1,texCoord).r;
 vec2 cell=floor(pixel/3.0)*3.0;
 float wave=1.0+.20*sin(cell.y*.075+Time*2.1+Seed)+.12*sin(cell.x*.10-Time*1.7);
 float innerWave=.48+.12*sin(cell.x*.13+Time*3.0+Seed*.37)+.07*cos(cell.y*.11-Time*2.3);
 for(int i=0;i<20;i++){
  float a=6.2831853*float(i)/20.0;
  vec2 direction=vec2(cos(a),sin(a));
  float spike=Mode>.5 ? (.55+1.35*pow(max(0.0,sin(cell.x*.17+cell.y*.13+Seed)),6.0)) : wave;
  for(int j=1;j<=3;j++){
   vec2 uv=texCoord+direction*(Radius*spike*float(j)/3.0)/ScreenSize;
   float m=sampleMask(uv);
   outer=max(outer,m);
   if(m>.1)z=min(z,texture(Sampler1,uv).r);
  }
  vec2 uv=texCoord+direction*(Radius*innerWave)/ScreenSize;
  float m=sampleMask(uv);
  inner=max(inner,m);
  solid=max(solid,sampleMask(texCoord+direction*(Radius*innerWave*.75)/ScreenSize));
  if(m>.1)z=min(z,texture(Sampler1,uv).r);
 }
 float coverage=max(inside,outer);
 if(coverage<.1 || z>=1.0)discard;
 vec3 color=SOUL;
 float alpha=Amount;
 if(Mode<.5){
  float boundary=clamp(inner*.84+.12*sin(cell.y*.21+Time*1.9),0.0,1.0);
  color=solid>.1 || boundary>dither?vec3(1.0):SOUL;
  alpha=.93;
  float ragged=.82+.18*sin(cell.x*.34+cell.y*.27-Time*2.0+Seed);
  if(inner<.1 && dither>ragged)discard;
 }else{
  alpha=Amount*(inside>.1?.94:.90);
 }
 if(dither>=clamp(alpha*coverage,0.0,1.0))discard;
 gl_FragDepth=max(0.0,z-.000001);
 fragColor=vec4(color,Mode>.5?clamp(alpha,0.0,1.0):.95);
}
