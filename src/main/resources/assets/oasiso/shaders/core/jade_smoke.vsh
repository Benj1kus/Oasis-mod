#version 150
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
in vec3 Position;
in vec2 UV0;
in vec4 Color;
out vec2 uv;
out vec2 style;
void main(){
    gl_Position = ProjMat * ModelViewMat * vec4(Position,1.0);
    uv = UV0;
    style = Color.ra;
}
