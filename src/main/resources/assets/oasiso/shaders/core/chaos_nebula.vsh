#version 150

in vec3 Position;
in vec4 Color;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec3 skyDirection;

void main() {
    skyDirection = Color.rgb * 2.0 - 1.0;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
