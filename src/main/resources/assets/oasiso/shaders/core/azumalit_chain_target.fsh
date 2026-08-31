#version 150

uniform sampler2D OutlineMask;
uniform sampler2D PulseMask;

uniform vec2 TexelSize;
uniform float PulseStrength;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    float center = texture(OutlineMask, texCoord).a;

    float innerNeighbor = 0.0;
    float outerNeighbor = 0.0;

    for (int y = -4; y <= 4; y++) {
        for (int x = -4; x <= 4; x++) {
            if (x == 0 && y == 0) {
                continue;
            }

            vec2 offset = vec2(float(x), float(y));
            float distancePx = length(offset);

            if (distancePx > 4.05) {
                continue;
            }

            float sampleAlpha = texture(
                OutlineMask,
                texCoord + offset * TexelSize
            ).a;

            outerNeighbor = max(outerNeighbor, sampleAlpha);

            if (distancePx <= 2.05) {
                innerNeighbor = max(innerNeighbor, sampleAlpha);
            }
        }
    }

    float outside = 1.0 - smoothstep(0.025, 0.18, center);

    float innerEdge = outside
        * smoothstep(0.035, 0.30, innerNeighbor);

    float outerEdge = outside
        * smoothstep(0.025, 0.22, outerNeighbor)
        * (1.0 - innerEdge * 0.86);

    float innerAlpha = innerEdge * 0.96;
    float outerAlpha = outerEdge * 0.58;

    float pulseShape = smoothstep(
        0.025,
        0.20,
        texture(PulseMask, texCoord).a
    );

    float pulseAlpha = pulseShape
        * clamp(PulseStrength, 0.0, 1.0);

    vec3 outerColor = vec3(0.04, 0.72, 0.43);
    vec3 innerColor = vec3(0.10, 1.00, 0.88);
    vec3 pulseColor = vec3(0.00, 1.00, 1.00);

    float alpha = 1.0
        - (1.0 - outerAlpha)
        * (1.0 - innerAlpha)
        * (1.0 - pulseAlpha);

    if (alpha <= 0.001) {
        discard;
    }

    vec3 premultiplied =
          outerColor * outerAlpha
        + innerColor * innerAlpha * (1.0 - outerAlpha)
        + pulseColor * pulseAlpha
            * (1.0 - innerAlpha)
            * (1.0 - outerAlpha);

    fragColor = vec4(
        premultiplied / max(alpha, 0.0001),
        alpha
    );
}
