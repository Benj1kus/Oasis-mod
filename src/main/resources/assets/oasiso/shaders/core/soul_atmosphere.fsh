#version 150
uniform float Time;
uniform float Fade;
uniform float Kind;
in vec3 direction;
in vec4 tint;
in vec2 uv;
out vec4 fragColor;
float hash(vec3 p){return fract(sin(dot(p,vec3(127.1,311.7,74.7)))*43758.5453);}
float noise(vec3 p){
 vec3 i=floor(p),f=fract(p);
 return mix(mix(mix(hash(i),hash(i+vec3(1,0,0)),f.x),mix(hash(i+vec3(0,1,0)),hash(i+vec3(1,1,0)),f.x),f.y),mix(mix(hash(i+vec3(0,0,1)),hash(i+vec3(1,0,1)),f.x),mix(hash(i+vec3(0,1,1)),hash(i+vec3(1,1,1)),f.x),f.y),f.z);
}
float bayer(){
 ivec2 p=ivec2(floor(gl_FragCoord.xy/2.0));
 int x=p.x&3,y=p.y&3;
 int index=((x&1)^ (y&1))*8+(y&1)*4+(((x>>1)&1)^((y>>1)&1))*2+((y>>1)&1);
 return (float(index)+0.5)/16.0;
}
void main(){
 float dither=bayer();
 if(Kind>1.5){fragColor=vec4(tint.rgb,tint.a*Fade);return;}
 if(Kind>0.5){
  vec2 p=floor(uv*5.0)/5.0;
  float n=noise(vec3(p*.32+vec2(Time*.10,-Time*.07),2.0));
  n+=noise(vec3(p*.67+vec2(-Time*.06,Time*.04),8.0))*.35;
  float edge=clamp((n-.62)*2.2,0.0,1.0);
  if(edge<dither)discard;
  fragColor=vec4(tint.rgb*(.65+floor(n*4.0)/8.0),tint.a*Fade);return;
 }
 vec3 d=normalize(direction);
 vec3 pixel= floor(d*150.0)/150.0;
 vec3 color=vec3(.006,.014,.045);
 float above=smoothstep(.02,.25,d.y);
 for(int i=0;i<3;i++){
  float fi=float(i),signum=(i==1?-1.0:1.0);
  vec3 p=pixel*(4.0+fi*3.0)+vec3(Time*.012*signum*(1.0+fi),0,Time*.008*signum);
  float n=noise(p)+.35*noise(p*2.0);
  float mask=clamp((n-.65)*3.5,0.0,1.0)*above;
  mask=floor(mask*3.0+dither*.22)/3.0;
  vec3 cloud=(i==0?vec3(.015,.09,.18):(i==1?vec3(.015,.20,.21):vec3(.035,.12,.24)));
  color=mix(color,cloud,mask*.48);
 }
 float wave=noise(vec3(pixel.x*12.0,Time*.15,pixel.z*12.0));
 float fine=noise(vec3(pixel.x*25.0-Time*.08,Time*.22,pixel.z*25.0));
 float height=.035+pow(wave,2.0)*.48+fine*.055;
 float flame=clamp((height-d.y)*13.0,0.0,1.0)*smoothstep(-.13,-.015,d.y);
 if(flame>dither)color+=mix(vec3(.0,.20,.17),vec3(.02,.65,.42),clamp(1.0-d.y/height,0.0,1.0))*.65;
 for(int i=0;i<7;i++){
  float fi=float(i);float az=fi*2.39996;
  vec3 star=normalize(vec3(cos(az),.25+hash(vec3(fi,8,3))*1.4,sin(az)));
  vec3 right=normalize(cross(star,vec3(0,1,0))),up=cross(right,star);
  vec2 q=vec2(dot(d,right),dot(d,up));
  float facing=step(.6,dot(d,star));
  float phase=mod(Time+fi*5.31,23.0);
  float starAlpha=smoothstep(0.0,.6,phase)*(1.0-smoothstep(6.0,9.0,phase))*facing;
  float radius=length(q);
  float spike=max(0.0,max(1.0-abs(q.x)/.004-abs(q.y)/.034,1.0-abs(q.y)/.004-abs(q.x)/.034));
  color+=vec3(.15,1.0,.68)*spike*starAlpha;
  for(int ring=0;ring<3;ring++){
   float age=phase-float(ring)*.7;
   float rr=age*.035;
   float line=(1.0-smoothstep(.0015,.005,abs(radius-rr)))*step(0.0,age)*(1.0-smoothstep(2.0,7.0,age));
   if(line*starAlpha>dither)color+=vec3(.01,.19,.15);
  }
 }
 fragColor=vec4(color,Fade);
}
