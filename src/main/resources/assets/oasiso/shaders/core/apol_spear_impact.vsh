#version 150
in vec3 Position;
in vec2 UV0;
uniform mat4 FlashProjection;
out vec2 uv;
void main(){gl_Position=FlashProjection*vec4(Position,1.0);uv=UV0;}
