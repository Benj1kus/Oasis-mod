#version 150
uniform float Age;
uniform float Seed;
in vec2 texCoord;
in float radialLayer;
out vec4 fragColor;
float hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7))+Seed*13.73)*43758.5453);}
float bayer(vec2 p){
    ivec2 q=ivec2(mod(floor(p),4.0));
    int v[16]=int[16](0,8,2,10,12,4,14,6,3,11,1,9,15,7,13,5);
    return (float(v[q.x+q.y*4])+.5)/16.0;
}
void main(){
    if(Age>=6.0)discard;
    float layer=floor(radialLayer+.5);
    vec2 cell=floor(texCoord*vec2(192.0,80.0));
    vec2 uv=cell/vec2(192.0,80.0);
    float clock=floor(Age*16.0)/16.0;
    float stripe=hash(vec2(floor(uv.x*48.0),layer));
    float tip=.75+stripe*.25+.04*sin(uv.x*100.53+clock*4.0+layer);
    float edge=1.0-smoothstep(tip-.055,tip,uv.y);
    if(layer<.5)edge=1.0-smoothstep(.94,1.0,uv.y);
    // Tall, broken ribbons travel upward on a fixed pixel grid.
    float bands=hash(vec2(floor(uv.x*96.0),floor((uv.y-clock*.48)*12.0)));
    float streak=step(.57,bands);
    float pulse=.9+.1*sin(clock*8.0+uv.y*12.0);
    float dissolve=smoothstep(4.4,6.0,Age);
    vec2 fragment=floor(cell/vec2(3.0,5.0));
    float life=hash(fragment+vec2(layer*19.0,0));
    float coverage=clamp((life-dissolve)*8.0,0.0,1.0);
    if(dissolve>0.0 && coverage<bayer(cell))discard;
    vec3 color;float alpha;
    if(layer<.5){color=vec3(.58,1.0,1.0);alpha=.98;}
    else if(layer<1.5){color=vec3(.07,.73,1.0);alpha=.38;}
    else if(layer<2.5){color=vec3(1.0,.12,.64);alpha=.40;}
    else if(layer<3.5){color=vec3(.53,.12,.91);alpha=.28;}
    else{color=vec3(.13,.035,.24);alpha=.30;}
    color*=pulse*(.8+.3*streak);
    color=mix(color,vec3(.75,1.0,1.0),streak*.12*(1.0-layer/5.0));
    color=floor(color*16.0)/16.0;
    float burst=step(3.0,Age)*(1.0-smoothstep(3.0,3.35,Age));
    color=mix(color,vec3(.8,1.0,1.0),burst*.65);
    alpha*=edge*(1.0-dissolve*.7)*smoothstep(0.0,.16,Age);
    if(alpha<.02 || edge<bayer(cell)*.45)discard;
    fragColor=vec4(color,alpha);
}
