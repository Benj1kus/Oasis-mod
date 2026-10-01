#version 150
in vec4 vertexColor;
in vec2 uv;
out vec4 fragColor;
float bayer(vec2 pixel) {
    ivec2 p=ivec2(mod(pixel,4.0));
    int table[16]=int[16](0,8,2,10,12,4,14,6,3,11,1,9,15,7,13,5);
    return (float(table[p.x+p.y*4])+.5)/16.0;
}
void main() {
    float across=abs(uv.y*2.0-1.0);
    float core=1.0-smoothstep(.16,.40,across);
    float edge=1.0-smoothstep(.38,1.0,across);
    float coverage=edge*vertexColor.a;
    if(coverage<=bayer(floor(gl_FragCoord.xy/2.0)))discard;
    vec3 color=mix(vertexColor.rgb,vec3(.80,.98,1.0),core*.40*(1.0-uv.x));
    fragColor=vec4(color,1.0);
}
