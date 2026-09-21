#version 150
in vec3 Position;
in vec2 UV0;
in vec4 Color;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec2 uv;
out vec4 edges;
out float distanceToCamera;
void main() {
    vec4 p=ModelViewMat*vec4(Position,1.0);
    gl_Position=ProjMat*p;
    uv=UV0; edges=Color; distanceToCamera=length(p.xyz);
}
