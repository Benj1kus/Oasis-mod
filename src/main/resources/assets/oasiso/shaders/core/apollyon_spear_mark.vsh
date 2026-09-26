#version 150
in vec3 Position;
in vec4 Color;
in vec2 UV0;
uniform mat4 WaveProjection;
out vec2 uv;
out vec4 vertexColor;
void main() {
    gl_Position = WaveProjection * vec4(Position, 1.0);
    uv = UV0;
    vertexColor = Color;
}
