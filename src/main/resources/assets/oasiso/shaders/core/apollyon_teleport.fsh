#version 150
uniform sampler2D Sampler0;
uniform float Strength;
in vec4 pieceColor;
in vec2 texCoord;
out vec4 fragColor;
float bayer(vec2 pixel) {
    ivec2 q=ivec2(mod(pixel,4.0));
    int table[16]=int[16](0,8,2,10,12,4,14,6,3,11,1,9,15,7,13,5);
    return (float(table[q.x+q.y*4])+.5)/16.0;
}
void main() {
    vec4 texel=texture(Sampler0,texCoord);
    if(texel.a<.1)discard;
    if(pieceColor.a*texel.a<=bayer(floor(gl_FragCoord.xy/2.0)))discard;
    float luminance=dot(texel.rgb,vec3(.21,.72,.07));
    vec3 colored=pieceColor.rgb*(.68+.32*luminance);
    float amount=smoothstep(.0,.46,Strength);
    vec3 color=mix(texel.rgb,colored,amount);
    float hot=step(.88,luminance)*amount*.35;
    fragColor=vec4(mix(color,vec3(.78,.97,1.0),hot),1.0);
}
