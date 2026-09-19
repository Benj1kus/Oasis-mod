#version 150
uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform vec2 BlurDir;
uniform float Spread;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec2 d = BlurDir * Spread / InSize;
    vec3 c = texture(DiffuseSampler, texCoord).rgb * 0.227027;
    c += texture(DiffuseSampler, texCoord + d * 1.384615).rgb * 0.316216;
    c += texture(DiffuseSampler, texCoord - d * 1.384615).rgb * 0.316216;
    c += texture(DiffuseSampler, texCoord + d * 3.230769).rgb * 0.070270;
    c += texture(DiffuseSampler, texCoord - d * 3.230769).rgb * 0.070270;
    fragColor = vec4(c, 1.0);
}
