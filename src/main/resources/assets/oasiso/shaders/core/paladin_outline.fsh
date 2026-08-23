#version 150

uniform sampler2D GoldMask;
uniform sampler2D SwordMask;

uniform vec2 TexelSize;
uniform float Time;

in vec2 texCoord;

out vec4 fragColor;


float maskAlpha(sampler2D maskTexture, vec2 uv) {
    return texture(maskTexture, uv).a;
}


float sobel(
    sampler2D maskTexture,
    vec2 uv,
    float radius
) {
    vec2 px = TexelSize * radius;

    float tl = maskAlpha(maskTexture, uv + px * vec2(-1.0,  1.0));
    float tc = maskAlpha(maskTexture, uv + px * vec2( 0.0,  1.0));
    float tr = maskAlpha(maskTexture, uv + px * vec2( 1.0,  1.0));

    float ml = maskAlpha(maskTexture, uv + px * vec2(-1.0,  0.0));
    float mr = maskAlpha(maskTexture, uv + px * vec2( 1.0,  0.0));

    float bl = maskAlpha(maskTexture, uv + px * vec2(-1.0, -1.0));
    float bc = maskAlpha(maskTexture, uv + px * vec2( 0.0, -1.0));
    float br = maskAlpha(maskTexture, uv + px * vec2( 1.0, -1.0));

    float gx =
    -tl - 2.0 * ml - bl
    + tr + 2.0 * mr + br;

    float gy =
    tl + 2.0 * tc + tr
    -bl - 2.0 * bc - br;

    return clamp(
        sqrt(gx * gx + gy * gy),
        0.0,
        1.0
    );
}


float smootherPulse(float time) {
    float p =
    0.5
    + 0.5 * sin(time * 1.20);

    return p * p * p
    * (
    p * (p * 6.0 - 15.0)
    + 10.0
    );
}


void main() {

    float pulse =
    smootherPulse(Time);



    float core =
    sobel(
        SwordMask,
        texCoord,
        0.56
    );

    float mid =
    sobel(
        SwordMask,
        texCoord,
        1.00
    );

    float outer =
    sobel(
        SwordMask,
        texCoord,
        1.85
    );

    float spikeEdge =
    sobel(
        SwordMask,
        texCoord,
        2.75
    );


    float center =
    maskAlpha(
        SwordMask,
        texCoord
    );


    float outside =
    1.0
    - center * 0.86;


    core *= outside;
    mid *= outside;
    outer *= outside;
    spikeEdge *= outside;



    float spikeWaveA =
    0.5
    + 0.5
    * sin(
        gl_FragCoord.x * 0.21
        + gl_FragCoord.y * 0.13
        + Time * 5.3
    );


    float spikeWaveB =
    0.5
    + 0.5
    * sin(
        gl_FragCoord.x * -0.08
        + gl_FragCoord.y * 0.31
        - Time * 3.7
    );


    float spikePattern =
    smoothstep(
        0.58,
        0.94,
        spikeWaveA * 0.60
        + spikeWaveB * 0.40
    );


    spikePattern *=
    0.30
    + pulse * 0.70;


    float coreAlpha =
    core
    * (
    0.76
    + pulse * 0.08
    );

    float midAlpha =
    mid
    * mix(
        0.03,
        0.18,
        pulse
    );

    float outerAlpha =
    outer
    * mix(
        0.02,
        0.16,
        pulse
    );

    float spikeAlpha =
    spikeEdge
    * spikePattern
    * mix(
        0.08,
        0.34,
        pulse
    );


    float alpha =
    clamp(
        coreAlpha
        + midAlpha
        + outerAlpha
        + spikeAlpha,
        0.0,
        1.0
    );


    if (alpha < 0.006) {
        discard;
    }


    vec3 jade =
    vec3(
    0.08,
    0.91,
    0.55
    );


    vec3 brightJade =
    vec3(
    0.42,
    1.00,
    0.73
    );


    vec3 electricCyan =
    vec3(
    0.28,
    1.00,
    0.88
    );


    float hot =
    clamp(
        core * 0.36
        + midAlpha * 0.55
        + outerAlpha * 0.35
        + spikeAlpha * 0.55
        + pulse * 0.16,
        0.0,
        1.0
    );


    vec3 color =
    mix(
        jade,
        brightJade,
        hot
    );


    color =
    mix(
        color,
        electricCyan,
        clamp(
            spikeAlpha * 1.8,
            0.0,
            0.65
        )
    );


    fragColor =
    vec4(
    color,
    alpha
    );
}