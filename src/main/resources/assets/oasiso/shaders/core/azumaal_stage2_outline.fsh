#version 150

uniform sampler2D Mask;
uniform vec2 TexelSize;
uniform float Pulse;
uniform float Time;

in vec2 texCoord;
out vec4 fragColor;

float readMask(vec2 offsetPixels) {
    return texture(
        Mask,
        texCoord + offsetPixels * TexelSize
    ).a;
}

float sample8(float radius) {
    float result = 0.0;

    result = max(result, readMask(vec2( radius,  0.0)));
    result = max(result, readMask(vec2(-radius,  0.0)));
    result = max(result, readMask(vec2( 0.0,  radius)));
    result = max(result, readMask(vec2( 0.0, -radius)));

    float diagonal = radius * 0.70710678;

    result = max(result, readMask(vec2( diagonal,  diagonal)));
    result = max(result, readMask(vec2(-diagonal,  diagonal)));
    result = max(result, readMask(vec2( diagonal, -diagonal)));
    result = max(result, readMask(vec2(-diagonal, -diagonal)));

    return result;
}

float cooldownFade(float cyclePos, float fadeEnd) {
    float fade = 1.0 - smoothstep(0.0, fadeEnd, cyclePos);
    return clamp(fade, 0.0, 1.0);
}

void main() {
    float center =
    texture(
        Mask,
        texCoord
    ).a;

    float waveA =
    0.5 + 0.5 *
    sin(
        gl_FragCoord.x * 0.18
        + gl_FragCoord.y * 0.11
        + Time * 3.8
    );

    float waveB =
    0.5 + 0.5 *
    sin(
        gl_FragCoord.x * -0.10
        + gl_FragCoord.y * 0.24
        - Time * 2.6
    );

    float waveC =
    0.5 + 0.5 *
    sin(
        gl_FragCoord.x * 0.33
        - gl_FragCoord.y * 0.16
        + Time * 4.7
    );

    float magma =
    smoothstep(
        0.26,
        0.82,
        waveA * 0.46
        + waveB * 0.34
        + waveC * 0.20
    );

    float micro =
    0.5 + 0.5 *
    sin(
        gl_FragCoord.x * 0.47
        - gl_FragCoord.y * 0.41
        + Time * 6.0
    );


    float innerJitter =
    magma * 0.42
    + micro * 0.12;

    float outerJitter =
    magma * 3.33
    + micro * 0.22;

    float innerMask = center;

    innerMask = max(innerMask, sample8(0.50 + innerJitter * 0.08));
    innerMask = max(innerMask, sample8(1.10 + innerJitter * 0.16));
    innerMask = max(innerMask, sample8(2.85 + innerJitter * 0.34));

    float pulseWidth = Pulse * 1.45;

    float outerMask = innerMask;

    outerMask = max(
        outerMask,
        sample8(2.05 + outerJitter * 0.26 + pulseWidth * 0.20)
    );

    outerMask = max(
        outerMask,
        sample8(3.50 + outerJitter * 0.55 + pulseWidth * 0.55)
    );

    outerMask = max(
        outerMask,
        sample8(4.30 + outerJitter * 0.92 + pulseWidth)
    );

    float innerEdge =
    clamp(innerMask - center, 0.0, 1.0);

    float outerEdge =
    clamp(outerMask - innerMask, 0.0, 1.0);

    innerEdge =
    smoothstep(
        0.022,
        0.72,
        innerEdge
    );

    outerEdge =
    smoothstep(
        0.016,
        0.62,
        outerEdge
    );

    float wobbleA =
    0.5 + 0.5 *
    sin(
        gl_FragCoord.x * 0.23
        + gl_FragCoord.y * 0.17
        + Time * 4.2
    );

    float wobbleB =
    0.5 + 0.5 *
    sin(
        gl_FragCoord.x * -0.15
        + gl_FragCoord.y * 0.28
        - Time * 3.1
    );

    float wobble =
    smoothstep(
        0.18,
        0.88,
        wobbleA * 0.58 + wobbleB * 0.42
    );

    innerEdge *= mix(0.90, 1.06, wobble);
    outerEdge *= mix(0.84, 1.14, wobble);

    innerEdge = clamp(innerEdge, 0.0, 1.0);
    outerEdge = clamp(outerEdge, 0.0, 1.0);

    float cycleSeconds = 2.0;
    float cyclePos = fract(Time / cycleSeconds);

    float innerFlash =
    cooldownFade(cyclePos, 0.26);

    float outerFlash =
    cooldownFade(cyclePos, 0.46);

    float vertical = clamp(texCoord.y, 0.0, 1.0);

    vec3 innerTop =
    vec3(
    0.36,
    0.96,
    1.00
    );

    vec3 innerBottom =
    vec3(
    0.10,
    0.34,
    1.00
    );

    vec3 outerTop =
    vec3(
    0.025,
    0.43,
    0.34
    );

    vec3 outerBottom =
    vec3(
    0.02,
    0.14,
    0.42
    );

    vec3 innerColor =
    mix(
        innerBottom,
        innerTop,
        vertical
    );

    vec3 outerColor =
    mix(
        outerBottom,
        outerTop,
        vertical
    );


    float colorShift =
    0.5 + 0.5 *
    sin(
        Time * 1.9
        + texCoord.y * 8.0
        + magma * 2.5
    );

    innerColor =
    mix(
        innerColor,
        vec3(0.20, 0.55, 1.00),
        colorShift * 0.12
    );

    outerColor =
    mix(
        outerColor,
        vec3(0.03, 0.20, 0.56),
        colorShift * 0.16
    );

    float innerAlpha =
    innerEdge *
    mix(
        0.10,
        1.00,
        innerFlash
    );

    float outerAlpha =
    outerEdge *
    mix(
        0.22,
        0.92,
        outerFlash
    );

    if (innerAlpha > 0.001) {
        fragColor =
        vec4(
        innerColor,
        innerAlpha
        );
        return;
    }

    if (outerAlpha > 0.001) {
        fragColor =
        vec4(
        outerColor,
        outerAlpha
        );
        return;
    }

    discard;
}