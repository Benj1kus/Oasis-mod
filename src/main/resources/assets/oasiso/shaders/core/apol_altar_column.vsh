#version 150
in vec3 Position;
in vec4 Color;
in vec2 UV0;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec2 texCoord;
out float radialLayer;
void main() {
    gl_Position=ProjMat*ModelViewMat*vec4(Position,1.0);
    texCoord=UV0;
    radialLayer=Color.r*4.0;
}
