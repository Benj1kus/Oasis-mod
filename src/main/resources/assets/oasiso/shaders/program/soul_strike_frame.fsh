#version 150
uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
in vec2 texCoord;
out vec4 fragColor;

float brightness(vec2 uv) {
    return dot(texture(DiffuseSampler, clamp(uv,vec2(0.0),vec2(1.0))).rgb, vec3(.299,.587,.114));
}

void main() {
    float pixel = max(1.0, floor(InSize.y / 360.0));
    vec2 stepUV = pixel / InSize, cell = floor(texCoord / stepUV);
    vec2 uv = (cell + .5) * stepUV;
    float lum = brightness(uv);
    float dx = abs(brightness(uv + vec2(stepUV.x,0)) - brightness(uv - vec2(stepUV.x,0)));
    float dy = abs(brightness(uv + vec2(0,stepUV.y)) - brightness(uv - vec2(0,stepUV.y)));
    float hatch = mod(cell.x + 2.0*cell.y, 5.0) < 1.0 ? .09 : 0.0;
    vec2 radial = (uv - vec2(.5,.44)) * vec2(InSize.x/InSize.y,1);
    float angle = atan(radial.y, radial.x + 1.e-6);
    float ray = sin(angle*57.0 + 2.0*sin(angle*13.0));
    bool streak = length(radial) > .49 + .16*sin(angle*9.0) && ray > .965;
    bool lit = lum > .34 + hatch || dx+dy > .125 || streak;
    fragColor = vec4(lit ? vec3(39,245,194)/255.0 : vec3(0), 1.0);
}
