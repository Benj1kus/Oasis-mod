#version 150
in vec3 Position;
in vec4 Color;
in vec2 UV0;
uniform mat4 BlastViewProjection;
out vec3 relativePosition;
out vec2 texCoord;
out vec4 data;
void main() {
    gl_Position = BlastViewProjection * vec4(Position, 1.0);
    relativePosition = Position;
    texCoord = UV0;
    data = Color;
}
