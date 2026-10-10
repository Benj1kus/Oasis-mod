#version 150
in vec3 Position;
in vec4 Color;
in vec2 UV0;
uniform mat4 FountainViewProjection;
out vec2 texCoord;
out float cameraDistance;
out float vertexAlpha;
void main() {
    gl_Position=FountainViewProjection*vec4(Position,1.0);
    texCoord=UV0;
    cameraDistance=length(Position);
    vertexAlpha=Color.a;
}
