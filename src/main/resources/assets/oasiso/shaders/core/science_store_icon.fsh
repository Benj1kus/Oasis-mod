#version 150
uniform sampler2D Sampler0;
uniform vec2 TexelSize;
uniform float OutlineWidth;
uniform float Flash;
in vec2 texCoord;
in vec4 vertexColor;
out vec4 fragColor;
void main() {
    vec4 base = texture(Sampler0, texCoord);
    float edge = 0.0;
    for (int y = -4; y <= 4; y++) {
        for (int x = -4; x <= 4; x++) {
            vec2 offset = vec2(x,y);
            if (length(offset) <= OutlineWidth)
                edge = max(edge, texture(Sampler0, texCoord + offset * TexelSize).a);
        }
    }
    float alpha = max(base.a, edge);
    if (alpha < .005) discard;
    vec3 color = mix(vec3(.19,.025,.30), base.rgb, step(.05,base.a));
    color = mix(color,vec3(1.0),Flash);
    fragColor = vec4(color,alpha) * vertexColor;
}
