#version 150
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec2 FieldSize;
in vec2 texCoord;
out vec4 fragColor;
void main(){
    float best=64.0,depth=1.0;
    for(int x=-16;x<=16;x++){
        vec2 uv=texCoord+vec2(float(x),0.0)/FieldSize;
        if(any(lessThan(uv,vec2(0.0)))||any(greaterThan(uv,vec2(1.0))))continue;
        float d=abs(float(x));
        if(texture(Sampler0,uv).a>0.1 && d<best){best=d;depth=texture(Sampler1,uv).r;}
    }
    fragColor=vec4(best,depth,0.0,1.0);
}
