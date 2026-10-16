#version 150
in vec3 Position;
in vec2 UV0;
uniform mat4 FxProjection;
out vec2 texCoord;
void main(){ gl_Position=FxProjection*vec4(Position,1.0);texCoord=UV0; }
