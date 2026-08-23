#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

uniform float Time;

uniform vec2 PortalBase;
uniform vec2 PortalTop;

uniform float BaseRadius;
uniform float TopRadius;

uniform float BaseDepth;
uniform float TopDepth;

uniform float Aspect;
uniform float Strength;

in vec2 texCoord;

out vec4 fragColor;



float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);

    return fract(p.x * p.y);
}


void main() {



    vec2 axis = PortalTop - PortalBase;

    float axisLengthSq = max(
        dot(axis, axis),
        0.000001
    );


    float t = clamp(
        dot(texCoord - PortalBase, axis)
        / axisLengthSq,

        0.0,
        1.0
    );


    vec2 center = mix(
        PortalBase,
        PortalTop,
        t
    );


    float radius = max(
        mix(
            BaseRadius,
            TopRadius,
            t
        ),

        0.0001
    );



    vec2 delta = texCoord - center;

    vec2 metricDelta = vec2(
    delta.x * Aspect,
    delta.y
    );


    float normalizedDistance =
    length(metricDelta)
    / radius;



    float columnMask =
    1.0
    - smoothstep(
        0.58,
        1.04,
        normalizedDistance
    );




    vec2 baseDelta = texCoord - PortalBase;

    vec2 baseMetric = vec2(
    baseDelta.x * Aspect,
    baseDelta.y
    );


    float baseDistance =
    length(baseMetric)
    / max(BaseRadius, 0.0001);


    float baseMask =
    1.0
    - smoothstep(
        0.62,
        1.16,
        baseDistance
    );



    float edgeNoise =
    sin(
        normalizedDistance * 19.0
        - Time * 4.2
        + t * 7.0
    ) * 0.055;


    float mask = max(
        columnMask + edgeNoise,
        baseMask * 0.82
    );


    mask = clamp(
        mask,
        0.0,
        1.0
    );




    float sceneDepth =
    texture(
        DepthSampler,
        texCoord
    ).r;


    float portalDepth =
    mix(
        BaseDepth,
        TopDepth,
        t
    );


    float behind =
    smoothstep(
        portalDepth - 0.003,
        portalDepth + 0.018,
        sceneDepth
    );


    float energy =
    mask
    * behind;




    float waveA =
    sin(
        normalizedDistance * 25.0
        - Time * 5.4
        + t * 8.0
    );


    float waveB =
    sin(
        texCoord.y * 68.0
        + texCoord.x * 29.0
        + Time * 2.7
    );


    float noise =
    hash21(
        floor(
            texCoord * 320.0
            + Time * 2.0
        )
    )
    - 0.5;


    vec2 radial =
    normalize(
        delta
        + vec2(0.00001)
    );



    vec2 tangentMetric =
    normalize(
        vec2(
        -metricDelta.y,
        metricDelta.x
        )
        + vec2(0.00001)
    );


    vec2 tangent = vec2(
    tangentMetric.x / Aspect,
    tangentMetric.y
    );


    float flow =
    waveA * 0.65
    + waveB * 0.27
    + noise * 0.08;


    vec2 offset =
    tangent
    * flow
    * Strength
    * radius
    * 6.0
    * energy;



    offset +=
    radial
    * sin(
        Time * 3.1
        + normalizedDistance * 17.0
    )
    * Strength
    * radius
    * 1.65
    * energy;


    vec2 distortedUv = clamp(
        texCoord + offset,
        vec2(0.001),
        vec2(0.999)
    );




    vec3 color;

    color.r =
    texture(
        DiffuseSampler,

        clamp(
            distortedUv + offset * 0.075,
            vec2(0.001),
            vec2(0.999)
        )
    ).r;


    color.g =
    texture(
        DiffuseSampler,
        distortedUv
    ).g;


    color.b =
    texture(
        DiffuseSampler,

        clamp(
            distortedUv - offset * 0.075,
            vec2(0.001),
            vec2(0.999)
        )
    ).b;



    color +=
    vec3(
    0.008,
    0.042,
    0.026
    )
    * energy;


    fragColor =
    vec4(
    color,
    1.0
    );
}