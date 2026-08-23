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
    float thickness
) {
    vec2 px = TexelSize * thickness;

    float tl = maskAlpha(maskTexture, uv + px * vec2(-1.0,  1.0));
    float tc = maskAlpha(maskTexture, uv + px * vec2( 0.0,  1.0));
    float tr = maskAlpha(maskTexture, uv + px * vec2( 1.0,  1.0));

    float ml = maskAlpha(maskTexture, uv + px * vec2(-1.0,  0.0));
    float mr = maskAlpha(maskTexture, uv + px * vec2( 1.0,  0.0));

    float bl = maskAlpha(maskTexture, uv + px * vec2(-1.0, -1.0));
    float bc = maskAlpha(maskTexture, uv + px * vec2( 0.0, -1.0));
    float br = maskAlpha(maskTexture, uv + px * vec2( 1.0, -1.0));

    float gx =
    -tl
    -2.0 * ml
    -bl
    +tr
    +2.0 * mr
    +br;

    float gy =
    tl
    +2.0 * tc
    +tr
    -bl
    -2.0 * bc
    -br;

    return clamp(
        sqrt(gx * gx + gy * gy),
        0.0,
        1.0
    );
}




float breathingPulse(float time, float speed, float phase) {
    float pulse =
    0.5
    + 0.5
    * sin(time * speed + phase);

    return pulse * pulse * (3.0 - 2.0 * pulse);
}


void main() {



    float goldPulse =
    breathingPulse(
        Time,
        2.10,
        0.0
    );

    float swordPulse =
    breathingPulse(
        Time,
        2.18,
        0.28
    );




    float goldCoreThickness =
    mix(
        0.68,
        0.92,
        goldPulse
    );

    float goldMidThickness =
    mix(
        1.05,
        2.10,
        goldPulse
    );

    float goldOuterThickness =
    mix(
        1.35,
        3.35,
        goldPulse
    );


    float goldCore =
    sobel(
        GoldMask,
        texCoord,
        goldCoreThickness
    );

    float goldMid =
    sobel(
        GoldMask,
        texCoord,
        goldMidThickness
    );

    float goldOuter =
    sobel(
        GoldMask,
        texCoord,
        goldOuterThickness
    );




    float swordCoreThickness =
    mix(
        0.58,
        0.82,
        swordPulse
    );

    float swordMidThickness =
    mix(
        0.92,
        1.80,
        swordPulse
    );

    float swordOuterThickness =
    mix(
        1.20,
        2.85,
        swordPulse
    );


    float swordCore =
    sobel(
        SwordMask,
        texCoord,
        swordCoreThickness
    );

    float swordMid =
    sobel(
        SwordMask,
        texCoord,
        swordMidThickness
    );

    float swordOuter =
    sobel(
        SwordMask,
        texCoord,
        swordOuterThickness
    );




    float goldCenter =
    maskAlpha(
        GoldMask,
        texCoord
    );

    float swordCenter =
    maskAlpha(
        SwordMask,
        texCoord
    );


    float goldOutside =
    1.0
    - goldCenter * 0.82;

    float swordOutside =
    1.0
    - swordCenter * 0.82;


    goldCore *= goldOutside;
    goldMid *= goldOutside;
    goldOuter *= goldOutside;

    swordCore *= swordOutside;
    swordMid *= swordOutside;
    swordOuter *= swordOutside;




    float goldCoreAlpha =
    goldCore
    * (
    0.78
    + goldPulse * 0.14
    );

    float goldMidAlpha =
    goldMid
    * (
    0.06
    + goldPulse * 0.31
    );

    float goldOuterAlpha =
    goldOuter
    * (
    0.015
    + goldPulse * 0.24
    );


    float swordCoreAlpha =
    swordCore
    * (
    0.82
    + swordPulse * 0.12
    );

    float swordMidAlpha =
    swordMid
    * (
    0.05
    + swordPulse * 0.32
    );

    float swordOuterAlpha =
    swordOuter
    * (
    0.01
    + swordPulse * 0.25
    );


    float goldAlpha =
    clamp(
        goldCoreAlpha
        + goldMidAlpha
        + goldOuterAlpha,
        0.0,
        1.0
    );

    float swordAlpha =
    clamp(
        swordCoreAlpha
        + swordMidAlpha
        + swordOuterAlpha,
        0.0,
        1.0
    );




    vec3 goldBase =
    vec3(
    1.00,
    0.67,
    0.10
    );

    vec3 goldHot =
    vec3(
    1.00,
    0.95,
    0.60
    );


    vec3 cyanBase =
    vec3(
    0.05,
    0.66,
    1.00
    );

    vec3 cyanHot =
    vec3(
    0.58,
    0.97,
    1.00
    );




    vec3 goldColor =
    mix(
        goldBase,
        goldHot,
        0.12 + goldPulse * 0.42
    );

    vec3 cyanColor =
    mix(
        cyanBase,
        cyanHot,
        0.10 + swordPulse * 0.44
    );




    vec3 goldHaloColor =
    vec3(
    1.00,
    0.82,
    0.32
    );

    vec3 cyanHaloColor =
    vec3(
    0.25,
    0.88,
    1.00
    );


    float goldHaloAmount =
    clamp(
        goldMidAlpha
        + goldOuterAlpha,
        0.0,
        1.0
    );

    float swordHaloAmount =
    clamp(
        swordMidAlpha
        + swordOuterAlpha,
        0.0,
        1.0
    );


    vec3 goldFinal =
    mix(
        goldColor,
        goldHaloColor,
        goldHaloAmount * 0.36
    );


    vec3 swordFinal =
    mix(
        cyanColor,
        cyanHaloColor,
        swordHaloAmount * 0.40
    );




    vec3 color =
    goldFinal
    * goldAlpha;




    color =
    mix(
        color,
        swordFinal,
        swordAlpha
    );


    float alpha =
    max(
        goldAlpha,
        swordAlpha
    );


    if (alpha < 0.006) {
        discard;
    }


    fragColor =
    vec4(
    color,
    alpha
    );
}