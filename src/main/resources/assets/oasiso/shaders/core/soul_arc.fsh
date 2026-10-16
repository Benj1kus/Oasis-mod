#version 150
in vec2 texCoord;
in vec4 arcData;
out vec4 fragColor;
const vec3 SOUL=vec3(0.0,1.0,150.0/255.0);
float b2(vec2 p){p=mod(floor(p),2.0);return 2.0*p.x+3.0*p.y-4.0*p.x*p.y;}
float bayer(vec2 p){return (4.0*b2(p)+b2(floor(p*.5))+.5)/16.0;}
float hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
void main(){
 float age=arcData.r*24.0, kind=arcData.g*255.0;
 float phase=arcData.r;
 float dither=bayer(floor(gl_FragCoord.xy/2.0));
 vec3 color=SOUL;
 float alpha=0.0;
 if(kind>220.0){
  vec2 p=(floor(texCoord*40.0)+.5)/40.0*2.0-1.0;
  float a=atan(p.y,p.x),r=length(p);
  float edge=r/(.56+.23*pow(abs(cos(a*3.0)),10.0));
  float flare=1.0-smoothstep(.15,1.0,edge);
  float ring=(1.0-smoothstep(.02,.12,abs(edge-.72)))*.7;
  alpha=max(flare,ring)*(1.0-smoothstep(0.0,9.0,age));
  color=mix(SOUL,vec3(1),flare*.85);
 }else if(kind>120.0){
  vec2 p=(floor(texCoord*20.0)+.5)/20.0*2.0-1.0;
  float a=atan(p.y,p.x);
  float wave=.74+.055*sin(a*3.0+phase*9.0+arcData.b*20.0);
  float pop=smoothstep(.78,1.0,phase);
  float r=length(p)/(wave+pop*.16);
  float blob=1.0-smoothstep(.82,1.0,r);
  float ring=1.0-smoothstep(.035,.14,abs(r-.84));
  alpha=mix(blob,ring,pop)*smoothstep(0.0,.12,phase)*(1.0-smoothstep(.86,1.0,phase));
  color=kind<170.0?vec3(1):SOUL;
 }else{
  float x=abs(((floor(texCoord.x*24.0)+.5)/24.0-.5)*2.0);
  float core=1.0-smoothstep(.12,.27,x);
  float edge=1.0-smoothstep(.35,.95,x);
  float fade=1.0-smoothstep(3.0,11.0,age);
  vec2 cell=floor(texCoord*vec2(12,32));
  if(hash(cell+arcData.b*91.0)<smoothstep(4.0,11.0,age)*.9)discard;
  alpha=max(core,edge*.85)*fade*(kind>30.0?.72:1.0);
  color=mix(SOUL,vec3(1),core);
 }
 alpha*=arcData.a;
 if(alpha<.01 || dither>=smoothstep(0.0,.5,alpha))discard;
 fragColor=vec4(color,alpha);
}
