#version 150
in vec3 Position;
in vec4 Color;
in vec2 UV0;
uniform mat4 View;
uniform mat4 Projection;
uniform float Kind;
out vec3 direction;
out vec4 tint;
out vec2 uv;
void main(){
 direction=Position;tint=Color;uv=UV0;
 gl_Position=Projection*View*vec4(Position,1.0);
 if(Kind<0.5)gl_Position.z=gl_Position.w*0.99999;
}
