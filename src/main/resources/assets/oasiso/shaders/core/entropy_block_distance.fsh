#version 150
uniform sampler2D Sampler0;
uniform vec2 FieldSize;
in vec2 uv;
out vec4 fragColor;
const int RADIUS=12;
const float MAX_D2=144.0;
float maskAt(vec2 p) {
    if(any(lessThan(p,vec2(0))) || any(greaterThanEqual(p,vec2(1)))) return 0.0;
    return step(.5,texture(Sampler0,p).a);
}
void main() {
    vec2 p=(floor(uv*FieldSize)+.5)/FieldSize;
    float inside=maskAt(p);
    float toInside=MAX_D2,toOutside=MAX_D2;
    for(int i=-RADIUS;i<=RADIUS;i++) {
        float covered=maskAt(p+vec2(float(i)/FieldSize.x,0));
        float d2=float(i*i);
        if(covered>.5) toInside=min(toInside,d2);
        else toOutside=min(toOutside,d2);
    }
    fragColor=vec4(toInside/MAX_D2,toOutside/MAX_D2,inside,1.0);
}
