#version 150
in vec3 Position;
in vec4 Color;
in vec2 UV0;
uniform mat4 ClipMatrix;
out vec4 pieceColor;
out vec2 texCoord;
void main() {
    gl_Position=ClipMatrix*vec4(Position,1.0);
    pieceColor=Color;
    texCoord=UV0;
}
