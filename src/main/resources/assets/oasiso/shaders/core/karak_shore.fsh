#version 150
in vec2 uv;
in vec4 edges;
in float distanceToCamera;
uniform float Time;
uniform float Range;
out vec4 fragColor;
void main() {
    float d=2.0;
    if(edges.r>.5) d=min(d,uv.x);
    if(edges.g>.5) d=min(d,1.0-uv.x);
    if(edges.b>.5) d=min(d,uv.y);
    if(edges.a>.5) d=min(d,1.0-uv.y);
    float band=1.0-smoothstep(0.0,0.50,d);
    float bright=1.0-smoothstep(0.0,0.12,d);
    float pulse=.90+.10*sin(Time*1.8);
    float fade=1.0-smoothstep(Range-3.0,Range,distanceToCamera);
    vec3 color=mix(vec3(0.08, 0.65, 0.48), vec3(0.153, 0.961, 0.706), band);
    color=mix(color, vec3(0.70, 1.0, 0.90), bright*.7);
    float alpha=band*band*.82*pulse*fade;
    if(alpha<.003) discard;
    fragColor=vec4(color,alpha);
}
