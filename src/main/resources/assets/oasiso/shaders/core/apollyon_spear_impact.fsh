#version 150
uniform float Time;
in vec2 uv;
in vec4 data;
out vec4 fragColor;
float bayer(vec2 pixel){
    ivec2 p=ivec2(mod(pixel,4.0));
    int b[16]=int[16](0,8,2,10,12,4,14,6,3,11,1,9,15,7,13,5);
    return (float(b[p.x+p.y*4])+.5)/16.0;
}
void main(){
    vec2 q=(floor(uv*32.0)+.5)/32.0;
    float coverage;
    vec3 color;
    if(data.r<.5){
        vec2 p=q*2.0-1.0;
        // Обратное вращение координат = вращение изображения по часовой стрелке.
        float angle=Time*7.5;
        float c=cos(angle),s=sin(angle);
        p=vec2(c*p.x-s*p.y,s*p.x+c*p.y);
        vec2 a=abs(p);
        float d=max(a.x,a.y)+3.6*min(a.x,a.y);
        coverage=clamp((1.0-d)*22.0,0.0,1.0);
        color=d<.64?vec3(.80,.98,1.0):(d<.84?vec3(.19,.85,1.0):vec3(.53,.30,.97));
    }else{
        float x=abs(q.x-.5)*2.0;
        float width=(1.0-q.y)*.90+.03;
        float notch=step(.38,q.y)*step(q.y,.56)*.18;
        float edge=width-notch-x;
        coverage=clamp(edge*18.0,0.0,1.0);
        color=mix(vec3(.60,.24,.95),vec3(1.0,.39,.81),step(.5,data.g));
        if(x<width*.25)color=mix(color,vec3(.95,.80,1.0),.32);
    }
    if(coverage*data.a<=bayer(floor(gl_FragCoord.xy/2.0)))discard;
    fragColor=vec4(color,1.0);
}
