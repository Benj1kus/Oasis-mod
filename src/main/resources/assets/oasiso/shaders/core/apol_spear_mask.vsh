#version 150
in vec3 Position;
in vec2 UV0;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 ModelInverse;
uniform vec2 FadeRange;
out vec2 texCoord;
out float modelHeight;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord = UV0;
    float y = (ModelInverse * vec4(Position, 1.0)).y;
    modelHeight = (y - FadeRange.x) / max(0.001, FadeRange.y - FadeRange.x);
}
