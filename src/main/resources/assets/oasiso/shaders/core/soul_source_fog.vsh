#version 150
in vec3 Position;
in vec4 Color;
in vec2 UV0;
uniform mat4 CameraView;
uniform mat4 CameraProjection;
out vec4 tint;
out vec2 uv;
void main(){
 tint=Color; uv=UV0;
 gl_Position=CameraProjection*CameraView*vec4(Position,1.0);
}
