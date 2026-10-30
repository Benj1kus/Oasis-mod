#version 150
uniform float CloudKind;
in vec4 tint;
in vec2 uv;
out vec4 fragColor;
float b2(vec2 p) {
    p = mod(floor(p), 2.0);
    return 2.0*p.x + 3.0*p.y - 4.0*p.x*p.y;
}
float bayer(vec2 p) { return (4.0*b2(p) + b2(floor(p*.5)) + .5)/16.0; }
void main() {
    float alpha = tint.a;
    float edge = min(uv.x, 1.0-uv.x);
    if (CloudKind < .5) {
        alpha *= pow(max(0.0, 1.0-uv.y), 1.35);
        alpha *= smoothstep(0.0, .10, edge);
    } else {
        alpha *= smoothstep(0.0, .70, uv.y);
        alpha *= smoothstep(0.0, .045, edge);
        alpha *= 1.0-smoothstep(.94, 1.0, uv.y);
    }
    float threshold = bayer(floor(gl_FragCoord.xy / 2.0));
    if (alpha <= .005 || threshold >= clamp(alpha * 5.0, 0.0, 1.0)) discard;
    fragColor = vec4(tint.rgb, alpha);
}
