#version 150
in vec3 Position;
in vec4 Color;
in vec2 UV0;
uniform mat4 ArcProjection;
out vec2 texCoord;
out vec4 arcData;
void main() {
    gl_Position=ArcProjection*vec4(Position,1.0);
    texCoord=UV0;
    arcData=Color;
}
