#version 150

uniform sampler2D PatternSampler;

uniform float Time;
uniform float Cover;
uniform float Aspect;

in vec2 texCoord;

out vec4 fragColor;




float luminance(vec3 color) {
    return dot(
        color,
        vec3(
        0.299,
        0.587,
        0.114
        )
    );
}


void main() {

    vec2 uv = texCoord;




    vec2 patternUv =
    vec2(
    uv.x * Aspect,
    uv.y
    )
    * 6.0;




    patternUv += vec2(
    Time * 0.035,
    sin(Time * 0.65) * 0.025
    );


    vec4 patternSample =
    texture(
        PatternSampler,
        fract(patternUv)
    );


    float patternLight =
    luminance(
        patternSample.rgb
    );




    float pattern =
    mix(
        0.5,
        patternLight,
        patternSample.a
    );



    float smoothCover =
    Cover
    * Cover
    * (3.0 - 2.0 * Cover);



    float halfGap =
    0.5
    * (1.0 - smoothCover);




    float leftWave =
    sin(
        uv.y * 19.0
        + Time * 2.35
    ) * 0.010;


    leftWave +=
    sin(
        uv.y * 43.0
        - Time * 1.30
    ) * 0.0045;


    float rightWave =
    sin(
        uv.y * 17.0
        - Time * 2.05
        + 1.6
    ) * 0.010;


    rightWave +=
    sin(
        uv.y * 39.0
        + Time * 1.15
    ) * 0.0045;




    float ditherStrength =
    1.0
    - smoothstep(
        0.88,
        0.995,
        smoothCover
    );


    float patternOffset =
    (pattern - 0.5)
    * 0.105
    * ditherStrength;


    float leftFront =
    0.5
    - halfGap
    + leftWave
    + patternOffset;




    float rightPattern =
    texture(
        PatternSampler,
        fract(
            vec2(
            1.0 - patternUv.x,
            patternUv.y + 0.37
            )
        )
    ).r;


    float rightOffset =
    (rightPattern - 0.5)
    * 0.105
    * ditherStrength;


    float rightFront =
    0.5
    + halfGap
    + rightWave
    - rightOffset;




    float feather =
    0.006;


    float leftMask =
    1.0
    - smoothstep(
        leftFront - feather,
        leftFront + feather,
        uv.x
    );


    float rightMask =
    smoothstep(
        rightFront - feather,
        rightFront + feather,
        uv.x
    );


    float wallMask =
    max(
        leftMask,
        rightMask
    );




    wallMask *=
    smoothstep(
        0.008,
        0.055,
        smoothCover
    );




    if (smoothCover > 0.985) {
        wallMask = 1.0;
    }




    vec3 deepNavy =
    vec3(
    0.006,
    0.014,
    0.045
    );


    vec3 navyHighlight =
    vec3(
    0.014,
    0.035,
    0.085
    );


    float internalWave =
    0.5
    + 0.5
    * sin(
        uv.y * 24.0
        + uv.x * 7.0
        - Time * 1.4
    );


    float textureEnergy =
    pattern * 0.55
    + internalWave * 0.45;


    vec3 wallColor =
    mix(
        deepNavy,
        navyHighlight,
        textureEnergy * 0.26
    );




    float leftDistance =
    abs(
        uv.x - leftFront
    );


    float rightDistance =
    abs(
        uv.x - rightFront
    );


    float edgeDistance =
    min(
        leftDistance,
        rightDistance
    );


    float cyanEdge =
    1.0
    - smoothstep(
        0.001,
        0.017,
        edgeDistance
    );




    cyanEdge *=
    1.0
    - smoothstep(
        0.88,
        0.99,
        smoothCover
    );


    cyanEdge *=
    smoothstep(
        0.06,
        0.20,
        smoothCover
    );



    cyanEdge *=
    0.45
    + pattern * 0.55;


    vec3 cyan =
    vec3(
    0.10,
    0.78,
    0.67
    );


    vec3 color =
    wallColor
    + cyan
    * cyanEdge
    * 0.28;


    if (wallMask < 0.002) {
        discard;
    }


    fragColor =
    vec4(
    color,
    wallMask
    );
}