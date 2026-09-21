#version 150
in vec3 Position;
in vec2 UV0;
in vec4 Color;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec2 uv;
out vec4 edges;
out float shoreDistance;
out float distanceToCamera;
void main() {
    vec4 p=ModelViewMat*vec4(Position,1.0);
    gl_Position=ProjMat*p;
    uv=UV0;
    float mask=floor(Color.r*255.0+0.5);
    edges=mod(floor(mask/vec4(1.0,2.0,4.0,8.0)),2.0);
    shoreDistance=Color.g*8.0;
    distanceToCamera=length(p.xyz);
}
