#version 150
uniform sampler2D Sampler0;
in vec2 texCoord;
in vec4 data;
out vec4 fragColor;
float b2(vec2 p){p=mod(floor(p),2.0);return 2.0*p.x+3.0*p.y-4.0*p.x*p.y;}
float bayer(vec2 p){return (4.0*b2(p)+b2(floor(p*.5))+.5)/16.0;}
float hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
const vec3 SOUL_CYAN = vec3(7.0, 240.0, 157.0) / 255.0;
void main(){
 float progress=data.r;
 float dissolve=smoothstep(.84,1.0,progress);
 float dither=bayer(floor(gl_FragCoord.xy/2.0));
 vec3 color;
 float alpha;
 if(data.g>.75){
  vec2 uv=(floor(clamp(texCoord,vec2(0),vec2(.9999))*16.0)+.5)/16.0;
  vec4 face=texture(Sampler0,uv);
  if(face.a<.05)discard;
  color=face.rgb*vec3(.65,1.0,1.0);
  alpha=face.a;
  if(hash(floor(uv*16.0)+data.b*71.0)<dissolve)discard;
 }else if(data.g>.25){
  vec2 tile=floor(clamp(texCoord,vec2(0),vec2(.9999))*4.0);
  float fleck=step(.73,hash(tile+data.b*19.0));
  color=mix(SOUL_CYAN * .70, SOUL_CYAN, fleck);
  color*=.55+.45*data.b;
  alpha=1.0;
  if(hash(tile+data.b*43.0)<dissolve)discard;
 }else{
  vec2 cell=floor(clamp(texCoord,vec2(0),vec2(.9999))*vec2(8,12));
  vec2 uv=(cell+.5)/vec2(8,12);
  float noise=hash(cell+data.b*43.0);
  float edge=abs(uv.x-.5)*2.0;
  float width=.48+.45*uv.y;
  width+=.12*step(.55,hash(vec2(cell.y,data.b*71.0)));
  if(edge>width)discard;
  if(uv.y<.65 && noise>smoothstep(.15,.65,uv.y))discard;
  float bright=floor(smoothstep(.25,1.0,uv.y)*3.0+.5)/3.0;
  color=mix(SOUL_CYAN * .36, SOUL_CYAN, bright);
  color*=.79+.21*step(.38,noise);
  alpha=smoothstep(.12,.9,uv.y);
  if(noise<dissolve)discard;
 }
 alpha*=data.a*(1.0-dissolve);
 if(alpha<.01 || dither>=smoothstep(0.0,.65,alpha))discard;
 fragColor=vec4(color,alpha);
}
