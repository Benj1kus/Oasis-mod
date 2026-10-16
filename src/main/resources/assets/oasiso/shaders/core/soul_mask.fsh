#version 150
uniform sampler2D Sampler0;
in vec2 texCoord;
out vec4 fragColor;
void main(){ float a=texture(Sampler0,texCoord).a;if(a<0.1)discard;fragColor=vec4(1.0,1.0,1.0,a); }
