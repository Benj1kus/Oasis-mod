#version 150

uniform sampler2D Sampler0;
uniform float Time;

in vec2 vUv;

out vec4 fragColor;

float maskAlpha(vec2 uv) {
    return texture(
        Sampler0,
        clamp(uv, vec2(0.0), vec2(1.0))
    ).a;
}

void main() {
    vec2 textureSizePx =
        vec2(textureSize(Sampler0, 0));

    vec2 px =
        1.0 / max(
            textureSizePx,
            vec2(1.0)
        );

    float center =
        maskAlpha(vUv);

    float pulse =
        0.5
        + 0.5
        * sin(Time * 2.15);

    float nearRadius =
        1.05
        + pulse * 0.25;

    float farRadius =
        2.05
        + pulse * 0.70;

    vec2 nX = vec2(px.x, 0.0);
    vec2 nY = vec2(0.0, px.y);
    vec2 nD1 = vec2(px.x, px.y);
    vec2 nD2 = vec2(px.x, -px.y);

    float nearMax = 0.0;
    float nearMin = 1.0;

    float s;

    s = maskAlpha(vUv + nX * nearRadius);
    nearMax = max(nearMax, s);
    nearMin = min(nearMin, s);

    s = maskAlpha(vUv - nX * nearRadius);
    nearMax = max(nearMax, s);
    nearMin = min(nearMin, s);

    s = maskAlpha(vUv + nY * nearRadius);
    nearMax = max(nearMax, s);
    nearMin = min(nearMin, s);

    s = maskAlpha(vUv - nY * nearRadius);
    nearMax = max(nearMax, s);
    nearMin = min(nearMin, s);

    s = maskAlpha(vUv + nD1 * nearRadius);
    nearMax = max(nearMax, s);
    nearMin = min(nearMin, s);

    s = maskAlpha(vUv - nD1 * nearRadius);
    nearMax = max(nearMax, s);
    nearMin = min(nearMin, s);

    s = maskAlpha(vUv + nD2 * nearRadius);
    nearMax = max(nearMax, s);
    nearMin = min(nearMin, s);

    s = maskAlpha(vUv - nD2 * nearRadius);
    nearMax = max(nearMax, s);
    nearMin = min(nearMin, s);

    float farMax = 0.0;

    farMax = max(
        farMax,
        maskAlpha(vUv + nX * farRadius)
    );

    farMax = max(
        farMax,
        maskAlpha(vUv - nX * farRadius)
    );

    farMax = max(
        farMax,
        maskAlpha(vUv + nY * farRadius)
    );

    farMax = max(
        farMax,
        maskAlpha(vUv - nY * farRadius)
    );

    farMax = max(
        farMax,
        maskAlpha(vUv + nD1 * farRadius)
    );

    farMax = max(
        farMax,
        maskAlpha(vUv - nD1 * farRadius)
    );

    farMax = max(
        farMax,
        maskAlpha(vUv + nD2 * farRadius)
    );

    farMax = max(
        farMax,
        maskAlpha(vUv - nD2 * farRadius)
    );

    float outside =
        1.0
        - smoothstep(
            0.06,
            0.28,
            center
        );

    float crispOuter =
        smoothstep(
            0.05,
            0.62,
            nearMax
        ) * outside;

    float softOuter =
        smoothstep(
            0.04,
            0.50,
            farMax
        ) * outside;

    float inside =
        smoothstep(
            0.20,
            0.72,
            center
        );

    float innerEdge =
        inside
        * (
            1.0
            - smoothstep(
                0.18,
                0.80,
                nearMin
            )
        );

    float strongAlpha =
        crispOuter
        * (
            0.80
            + pulse * 0.18
        );

    float glowAlpha =
        max(
            0.0,
            softOuter - crispOuter * 0.55
        )
        * (
            0.12
            + pulse * 0.13
        );

    float innerAlpha =
        innerEdge
        * 0.22;

    float alpha =
        clamp(
            strongAlpha
            + glowAlpha
            + innerAlpha,
            0.0,
            1.0
        );

    if (alpha < 0.008) {
        discard;
    }

    vec3 cyanDeep =
        vec3(
            0.00,
            0.67,
            0.84
        );

    vec3 cyanHot =
        vec3(
            0.08,
            1.00,
            0.98
        );

    vec3 color =
        mix(
            cyanDeep,
            cyanHot,
            0.48
            + pulse * 0.42
        );


    color +=
        vec3(
            0.02,
            0.10,
            0.10
        ) * crispOuter;

    fragColor =
        vec4(
            color,
            alpha
        );
}
