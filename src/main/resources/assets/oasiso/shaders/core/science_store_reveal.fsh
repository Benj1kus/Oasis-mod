#version 150
uniform sampler2D Sampler0;
uniform float Progress;
uniform float Aspect;
in vec2 texCoord;
in vec4 vertexColor;
out vec4 fragColor;
float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1,311.7))) * 43758.5453); }
float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p); f = f*f*(3.0-2.0*f);
    return mix(mix(hash(i),hash(i+vec2(1,0)),f.x), mix(hash(i+vec2(0,1)),hash(i+vec2(1,1)),f.x),f.y);
}
void main() {
    vec2 pixel = floor(texCoord * vec2(192.0,108.0)) / vec2(192.0,108.0);
    vec2 p = (pixel - .5) * vec2(Aspect,1.0);
    float cloud = noise(pixel * 11.0) * .10 + noise(pixel * 31.0) * .045;
    float radius = Progress * (length(vec2(Aspect,1.0)) * .5 + .30);
    float mask = (1.0 - smoothstep(radius - .18, radius + .03, length(p) + cloud)) * smoothstep(0.0,.08,Progress);
    mask = mix(mask,1.0,smoothstep(.94,1.0,Progress));
    vec4 color = texture(Sampler0,texCoord) * vertexColor;
    fragColor = vec4(color.rgb, color.a * mask);
}
