#version 150

uniform float Time;
uniform float Reveal;
uniform float Despawn;
uniform float Seed;
uniform float GlowOnly;

in vec2 texCoord;
out vec4 fragColor;

const float PI  = 3.14159265359;
const float TAU = 6.28318530718;




float hash11(float p) {
    p = fract(p * 0.1031);
    p *= p + 33.33;
    p *= p + p;

    return fract(p);
}


float hashSeed(float value) {
    return hash11(
        value + Seed * 17.731
    );
}




float noise2(vec2 p) {
    vec2 cell = floor(p);
    vec2 f = fract(p);

    f = f * f * (3.0 - 2.0 * f);

    float a = hashSeed(
        dot(
            cell,
            vec2(127.1, 311.7)
        )
    );

    float b = hashSeed(
        dot(
            cell + vec2(1.0, 0.0),
            vec2(127.1, 311.7)
        )
    );

    float c = hashSeed(
        dot(
            cell + vec2(0.0, 1.0),
            vec2(127.1, 311.7)
        )
    );

    float d = hashSeed(
        dot(
            cell + vec2(1.0, 1.0),
            vec2(127.1, 311.7)
        )
    );

    return mix(
        mix(a, b, f.x),
        mix(c, d, f.x),
        f.y
    );
}


float fbm(vec2 p) {
    float result = 0.0;
    float amplitude = 0.5;

    for (int i = 0; i < 5; i++) {
        result += noise2(p) * amplitude;

        p = p * 2.02
        + vec2(7.31, 3.17);

        amplitude *= 0.5;
    }

    return result;
}




mat2 rotate2D(float angle) {
    float s = sin(angle);
    float c = cos(angle);

    return mat2(
    c, -s,
    s,  c
    );
}


float sdCircle(
    vec2 p,
    vec2 center,
    float radius
) {
    return length(p - center) - radius;
}


float smin(
    float a,
    float b,
    float k
) {
    float h =
    max(
        k - abs(a - b),
        0.0
    ) / k;

    return min(a, b)
    - h * h * h * k
    * (1.0 / 6.0);
}




float bubbleCircle(
    vec2 p,
    vec2 baseCenter,
    float baseRadius,
    float id
) {
    float t1 =
    Time
    * (
    0.42
    + hashSeed(id * 3.1) * 0.20
    );

    float t2 =
    Time
    * (
    0.31
    + hashSeed(id * 5.7) * 0.25
    );


    vec2 center = baseCenter;

    center.x +=
    sin(
        t1
        + id * 1.73
    ) * 0.040;

    center.y +=
    cos(
        t2
        + id * 2.11
    ) * 0.045;


    float radius = baseRadius;

    radius +=
    sin(
        Time
        * (
        0.55
        + hashSeed(id * 8.1) * 0.18
        )
        + id * 1.19
    ) * 0.020;


    radius +=
    (
    fbm(
        baseCenter * 4.0
        + vec2(
        Time * 0.05,
        -Time * 0.04
        )
    )
    - 0.5
    ) * 0.018;


    return sdCircle(
        p,
        center,
        radius
    );
}


float mainPortalDistance(vec2 p) {
    vec2 q = p;



    q.x +=
    (
    fbm(
        q * 2.30
        + vec2(
        Time * 0.07,
        -Time * 0.05
        )
    )
    - 0.5
    ) * 0.090;


    q.y +=
    (
    fbm(
        q * 2.10
        + vec2(
        -Time * 0.04,
        Time * 0.06
        )
    )
    - 0.5
    ) * 0.080;


    float d =
    bubbleCircle(
        q,
        vec2(0.00, 0.00),
        0.34,
        1.0
    );


    d = smin(
        d,
        bubbleCircle(
            q,
            vec2(-0.48, 0.02),
            0.28,
            2.0
        ),
        0.14
    );


    d = smin(
        d,
        bubbleCircle(
            q,
            vec2(0.46, 0.08),
            0.27,
            3.0
        ),
        0.14
    );


    d = smin(
        d,
        bubbleCircle(
            q,
            vec2(-0.31, 0.42),
            0.24,
            4.0
        ),
        0.14
    );


    d = smin(
        d,
        bubbleCircle(
            q,
            vec2(0.00, 0.48),
            0.22,
            5.0
        ),
        0.14
    );


    d = smin(
        d,
        bubbleCircle(
            q,
            vec2(0.33, 0.39),
            0.25,
            6.0
        ),
        0.14
    );


    d = smin(
        d,
        bubbleCircle(
            q,
            vec2(-0.47, -0.26),
            0.24,
            7.0
        ),
        0.14
    );


    d = smin(
        d,
        bubbleCircle(
            q,
            vec2(0.46, -0.21),
            0.22,
            8.0
        ),
        0.14
    );


    d = smin(
        d,
        bubbleCircle(
            q,
            vec2(-0.18, -0.54),
            0.20,
            9.0
        ),
        0.14
    );


    d = smin(
        d,
        bubbleCircle(
            q,
            vec2(0.12, -0.60),
            0.24,
            10.0
        ),
        0.14
    );



    d -=
    sin(
        Time * 0.62
        + q.y * 3.2
    ) * 0.010;


    d -=
    sin(
        Time * 0.39
        - q.x * 4.4
    ) * 0.008;


    return d;
}


vec2 constructionHeadPosition(
    float progress
) {
    progress =
    clamp(
        progress,
        0.0,
        1.0
    );


    float angle =
    PI * 0.50
    - progress
    * TAU
    * 2.35;


    float radius =
    mix(
        0.015,
        0.78,

        pow(
            progress,
            0.82
        )
    );


    return vec2(
    cos(angle)
    * radius
    * 0.90,

    sin(angle)
    * radius
    * 1.04
    );
}


float constructionTrail(
    vec2 p,
    float progress
) {
    float best = 10.0;


    for (int i = 0; i < 32; i++) {

        float t =
        float(i)
        / 31.0;


        float active =
        1.0
        - step(
            progress,
            t
        );


        float angle =
        PI * 0.50
        - t
        * TAU
        * 2.35;


        float radius =
        mix(
            0.015,
            0.78,

            pow(
                t,
                0.82
            )
        );


        vec2 center =
        vec2(
        cos(angle)
        * radius
        * 0.90,

        sin(angle)
        * radius
        * 1.04
        );


        float blobRadius =
        mix(
            0.050,
            0.165,
            t
        );


        blobRadius *=
        0.90
        + 0.10
        * sin(
            t * 28.0
            + Seed * 0.13
        );


        float d =
        sdCircle(
            p,
            center,
            blobRadius
        );


        d +=
        (
        1.0
        - active
        ) * 5.0;


        best =
        min(
            best,
            d
        );
    }


    return 1.0
    - smoothstep(
        0.010,
        0.115,
        best
    );
}


float constructionFragments(
    vec2 p,
    float progress
) {
    vec2 head =
    constructionHeadPosition(
        progress
    );


    float result = 0.0;


    for (int i = 0; i < 6; i++) {

        float fi = float(i);


        float angle =
        hashSeed(
            fi * 17.13
        ) * TAU;


        float orbit =
        0.045
        + hashSeed(
            fi * 9.17
        ) * 0.110;


        orbit *=
        0.6
        + 0.4
        * sin(
            Time * 2.1
            + fi * 1.7
        );


        vec2 center =
        head
        + vec2(
        cos(angle),
        sin(angle)
        ) * orbit;


        float radius =
        0.010
        + hashSeed(
            fi * 27.9
        ) * 0.026;


        float piece =
        1.0
        - smoothstep(
            radius,
            radius + 0.025,

            length(
                p - center
            )
        );


        result =
        max(
            result,
            piece
        );
    }


    return result;
}


void getConstructionMasks(
    vec2 p,

out float buildMask,
out float headCore,
out float headHalo,
out float fragments
) {
    float progress =
    smoothstep(
        0.015,
        0.78,
        Reveal
    );


    float seedRadius =
    mix(
        0.035,
        0.075,

        smoothstep(
            0.00,
            0.10,
            Reveal
        )
    );


    float seedMask =
    1.0
    - smoothstep(
        seedRadius,
        seedRadius + 0.035,

        length(p)
    );


    float trail =
    constructionTrail(
        p,
        progress
    );


    float condensation =
    smoothstep(
        0.69,
        0.98,
        Reveal
    );


    buildMask =
    max(
        seedMask
        * (
        1.0
        - smoothstep(
            0.08,
            0.28,
            Reveal
        )
        ),

        trail
    );


    buildMask =
    max(
        buildMask,
        condensation
    );


    vec2 head =
    constructionHeadPosition(
        progress
    );


    float headDistance =
    length(
        p - head
    );


    headCore =
    1.0
    - smoothstep(
        0.028,
        0.090,
        headDistance
    );


    headHalo =
    exp(
        -headDistance
        * 15.0
    );


    float headLife =
    1.0
    - smoothstep(
        0.76,
        0.94,
        Reveal
    );


    headCore *=
    headLife;


    headHalo *=
    headLife;


    fragments =
    constructionFragments(
        p,
        progress
    )
    * headLife;
}

float outerDetachedPiecesDistance(
    vec2 p
) {
    float best = 10.0;


    for (int i = 0; i < 12; i++) {

        float fi = float(i);


        float cycleDuration =
        2.9
        + hashSeed(
            fi * 7.17
        ) * 1.8;


        float piecePhase =
        fract(
            Time / cycleDuration
            + hashSeed(
                fi * 13.9
            )
        );


        float life =
        smoothstep(
            0.02,
            0.15,
            piecePhase
        )

        * (
        1.0
        - smoothstep(
            0.68,
            0.98,
            piecePhase
        )
        );


        float angle =
        hashSeed(
            fi * 21.31
        ) * TAU;


        vec2 dir =
        vec2(
        cos(angle),
        sin(angle)
        );


        vec2 tangent =
        vec2(
        -dir.y,
        dir.x
        );


        float outward =
        smoothstep(
            0.08,
            0.80,
            piecePhase
        )

        * (
        0.05
        + hashSeed(
            fi * 4.3
        ) * 0.24
        );


        float slide =
        sin(
            Time
            * (
            0.55
            + hashSeed(
                fi * 3.8
            ) * 0.30
            )

            + fi * 1.71
        ) * 0.07;


        vec2 center =
        dir
        * vec2(
        0.66,
        0.79
        );


        center +=
        dir * outward;


        center +=
        tangent
        * slide
        * 0.28;


        center +=
        vec2(
        sin(
            Time * 0.45
            + fi * 0.9
        ),

        cos(
            Time * 0.36
            + fi * 1.3
        )
        ) * 0.030;


        float radius =
        mix(
            0.028,
            0.090,

            hashSeed(
                fi * 8.81
            )
        );


        radius *=
        0.78
        + 0.22
        * sin(
            Time
            * (
            0.74
            + hashSeed(
                fi * 5.2
            ) * 0.26
            )

            + fi
        );


        float d =
        sdCircle(
            p,
            center,
            radius
        );


        d +=
        (
        1.0
        - life
        ) * 2.0;


        best =
        min(
            best,
            d
        );
    }


    return best;
}


float innerCloudFragments(
    vec2 p
) {
    float result = 0.0;


    for (int i = 0; i < 10; i++) {

        float fi = float(i);


        float cycleDuration =
        2.4
        + hashSeed(
            fi * 6.73
        ) * 1.9;


        float cloudPhase =
        fract(
            Time / cycleDuration
            + hashSeed(
                fi * 14.2
            )
        );


        float life =
        smoothstep(
            0.04,
            0.18,
            cloudPhase
        )

        * (
        1.0
        - smoothstep(
            0.60,
            0.98,
            cloudPhase
        )
        );


        float baseAngle =
        hashSeed(
            fi * 19.7
        ) * TAU;


        float baseRadius =
        mix(
            0.05,
            0.52,

            hashSeed(
                fi * 8.4
            )
        );


        vec2 center =
        vec2(
        cos(baseAngle)
        * baseRadius
        * 0.75,

        sin(baseAngle)
        * baseRadius
        * 0.95
        );


        center +=
        vec2(
        sin(
            Time
            * (
            0.32
            + hashSeed(
                fi * 3.1
            ) * 0.25
            )
            + fi * 1.7
        ),

        cos(
            Time
            * (
            0.27
            + hashSeed(
                fi * 5.9
            ) * 0.22
            )
            + fi * 1.4
        )
        ) * 0.10;


        float radiusA =
        mix(
            0.05,
            0.16,

            hashSeed(
                fi * 17.2
            )
        );


        float radiusB =
        radiusA
        * (
        0.48
        + hashSeed(
            fi * 21.3
        ) * 0.35
        );


        vec2 offset =
        vec2(
        sin(
            Time * 0.61
            + fi * 2.1
        ),

        cos(
            Time * 0.52
            + fi * 1.8
        )
        )

        * 0.06
        * smoothstep(
            0.18,
            0.78,
            cloudPhase
        );


        float c1 =
        1.0
        - smoothstep(
            radiusA,
            radiusA + 0.12,

            length(
                p - center
            )
        );


        float c2 =
        1.0
        - smoothstep(
            radiusB,
            radiusB + 0.10,

            length(
                p
                - (
                center
                + offset
                )
            )
        );


        float fragment =
        max(
            c1,
            c2
        ) * life;


        result =
        max(
            result,
            fragment
        );
    }


    return clamp(
        result,
        0.0,
        1.0
    );
}


float portalDust(
    vec2 p
) {
    float dust = 0.0;


    for (int i = 0; i < 18; i++) {

        float fi = float(i);


        float angle =
        hashSeed(
            fi * 7.3
            + 11.0
        ) * TAU;


        float radius =
        mix(
            0.04,
            0.60,

            hashSeed(
                fi * 13.9
                + 5.0
            )
        );


        vec2 center =
        vec2(
        cos(angle)
        * radius
        * 0.82,

        sin(angle)
        * radius
        * 0.96
        );


        center +=
        vec2(
        sin(
            Time
            * (
            0.22
            + hashSeed(
                fi * 2.1
            ) * 0.18
            )
            + fi
        ),

        cos(
            Time
            * (
            0.18
            + hashSeed(
                fi * 4.7
            ) * 0.16
            )
            + fi * 1.9
        )
        ) * 0.045;


        float size =
        mix(
            0.006,
            0.020,

            hashSeed(
                fi * 17.4
            )
        );


        float pulse =
        0.45
        + 0.55
        * sin(
            Time
            * (
            0.90
            + hashSeed(
                fi * 5.5
            ) * 0.70
            )

            + fi * 1.3
        );


        pulse =
        max(
            pulse,
            0.0
        );


        float mote =
        exp(
            -length(
                p - center
            ) / size
        ) * pulse;


        dust += mote;
    }


    return clamp(
        dust * 0.35,
        0.0,
        1.0
    );
}


float despawnFragments(
    vec2 p
) {
    float result = 0.0;


    for (int i = 0; i < 18; i++) {

        float fi = float(i);


        float start =
        hashSeed(
            fi * 13.17
            + 51.0
        ) * 0.55;


        float deathPhase =
        clamp(
            (
            Despawn
            - start
            )
            / max(
                0.001,
                1.0 - start
            ),

            0.0,
            1.0
        );


        float life =
        smoothstep(
            0.0,
            0.09,
            deathPhase
        )

        * (
        1.0
        - smoothstep(
            0.60,
            1.0,
            deathPhase
        )
        );


        float angle =
        hashSeed(
            fi * 19.41
            + 7.0
        ) * TAU;


        vec2 direction =
        vec2(
        cos(angle),
        sin(angle)
        );


        vec2 tangent =
        vec2(
        -direction.y,
        direction.x
        );


        float startRadius =
        mix(
            0.18,
            0.68,

            hashSeed(
                fi * 8.73
                + 3.0
            )
        );


        vec2 center =
        direction
        * vec2(
        startRadius * 0.88,
        startRadius
        );


        center +=
        direction
        * deathPhase
        * mix(
            0.18,
            0.58,

            hashSeed(
                fi * 4.91
                + 9.0
            )
        );


        center +=
        tangent
        * sin(
            deathPhase * PI
            + fi * 2.1
        ) * 0.10;


        float radius =
        mix(
            0.025,
            0.075,

            hashSeed(
                fi * 27.3
            )
        );


        radius *=
        1.0
        - deathPhase * 0.55;


        float piece =
        1.0
        - smoothstep(
            radius,
            radius + 0.035,

            length(
                p - center
            )
        );


        result =
        max(
            result,
            piece * life
        );
    }


    return clamp(
        result,
        0.0,
        1.0
    );
}


float starShape(
    vec2 p
) {
    vec2 a =
    abs(p);


    float vertical =
    exp(
        -a.x * 95.0
    )
    * exp(
        -a.y * 10.0
    );


    float horizontal =
    exp(
        -a.y * 95.0
    )
    * exp(
        -a.x * 15.0
    );


    float core =
    exp(
        -length(p)
        * 54.0
    );


    return clamp(
        vertical
        + horizontal
        + core * 1.4,

        0.0,
        1.0
    );
}


float sequentialStars(
    vec2 p
) {
    float period = 4.7;


    float starTime =
    Time
    + Seed * 0.091;


    float cycle =
    floor(
        starTime / period
    );



    float starPhase =
    fract(
        starTime / period
    );


    float allowed =
    step(
        0.34,

        hash11(
            cycle * 4.97
            + Seed * 0.77
        )
    );


    float result = 0.0;


    for (int i = 0; i < 3; i++) {

        float fi = float(i);


        float start =
        0.08
        + fi * 0.16

        + hash11(
            cycle * 7.3
            + fi * 3.8
            + Seed
        ) * 0.04;


        float appear =
        smoothstep(
            start,
            start + 0.020,
            starPhase
        );


        float disappear =
        1.0
        - smoothstep(
            start + 0.060,
            start + 0.120,
            starPhase
        );


        float flash =
        appear
        * disappear
        * allowed;


        float angle =
        hash11(
            cycle * 14.1
            + fi * 5.9
            + Seed
        ) * TAU;


        float radius =
        mix(
            0.42,
            0.88,

            hash11(
                cycle * 22.3
                + fi * 7.1
                + Seed * 1.7
            )
        );


        vec2 center =
        vec2(
        cos(angle)
        * radius
        * 0.95,

        sin(angle)
        * radius
        * 1.04
        );


        float size =
        mix(
            0.72,
            1.10,

            hash11(
                cycle * 31.2
                + fi * 4.6
                + Seed * 2.2
            )
        );


        result +=
        starShape(
            (
            p
            - center
            ) / size
        )
        * flash;
    }


    return clamp(
        result,
        0.0,
        1.0
    );
}


float tinyStarShape(
    vec2 p
) {
    vec2 a =
    abs(p);


    float vertical =
    exp(
        -a.x * 250.0
    )
    * exp(
        -a.y * 38.0
    );


    float horizontal =
    exp(
        -a.y * 250.0
    )
    * exp(
        -a.x * 38.0
    );


    float core =
    exp(
        -length(p)
        * 150.0
    );


    return clamp(
        vertical
        + horizontal
        + core,

        0.0,
        1.0
    );
}


float tinyCoreStars(
    vec2 p
) {
    float result = 0.0;


    for (int i = 0; i < 8; i++) {

        float fi = float(i);


        float period =
        mix(
            0.65,
            1.55,

            hashSeed(
                fi * 7.37
            )
        );


        float tinyPhase =
        fract(
            Time / period
            + hashSeed(
                fi * 17.93
                + 13.0
            )
        );


        float flash =
        smoothstep(
            0.00,
            0.055,
            tinyPhase
        )

        * (
        1.0
        - smoothstep(
            0.10,
            0.23,
            tinyPhase
        )
        );


        float angle =
        hashSeed(
            fi * 21.71
            + 33.0
        ) * TAU;


        float radius =
        mix(
            0.035,
            0.34,

            hashSeed(
                fi * 11.27
                + 5.0
            )
        );


        vec2 center =
        vec2(
        cos(angle)
        * radius,

        sin(angle)
        * radius
        * 0.82
        );


        result +=
        tinyStarShape(
            p - center
        )
        * flash;
    }


    return clamp(
        result,
        0.0,
        1.0
    );
}



void main() {


    vec2 raw =
    (
    texCoord
    - vec2(0.5)
    )
    * vec2(
    2.20,
    2.00
    );



    vec2 p =
    rotate2D(
        -Time * 0.095
    ) * raw;



    float buildMask;
    float buildHead;
    float buildHeadHalo;
    float buildFragments;


    getConstructionMasks(
        p,

        buildMask,
        buildHead,
        buildHeadHalo,
        buildFragments
    );


    float mainD =
    mainPortalDistance(
        p
    );


    float piecesReveal =
    smoothstep(
        0.76,
        0.98,
        Reveal
    );


    float outerPiecesD =
    outerDetachedPiecesDistance(
        p
    )

    + (
    1.0
    - piecesReveal
    ) * 2.0;


    float d =
    smin(
        mainD,
        outerPiecesD,
        0.12
    );


    float fullBody =
    1.0
    - smoothstep(
        -0.010,
        0.016,
        d
    );


    float fullBorder =
    1.0
    - smoothstep(
        0.008,
        0.024,
        abs(d)
    );


    fullBorder =
    pow(
        fullBorder,
        1.55
    );


    float body =
    fullBody
    * buildMask;


    float border =
    fullBorder
    * buildMask;


    border *=
    smoothstep(
        0.14,
        0.84,
        Reveal
    );

    float breakupNoise =
    fbm(
        p * 5.8
        + vec2(
        Time * 0.11,
        -Time * 0.085
        )
    );


    float breakupThreshold =
    mix(
        1.22,
        -0.18,
        Despawn
    );


    float survive =
    1.0
    - smoothstep(
        breakupThreshold - 0.12,
        breakupThreshold + 0.12,
        breakupNoise
    );


    survive *=
    1.0
    - smoothstep(
        0.91,
        1.0,
        Despawn
    );


    body *=
    survive;


    border *=
    survive;


    float halo =
    exp(
        -max(
            d,
            0.0
        ) * 7.6
    ) * 0.28;


    halo *=
    (
    1.0
    - smoothstep(
        -0.02,
        0.14,
        d
    )
    );


    halo *=
    smoothstep(
        0.46,
        0.92,
        Reveal
    );


    halo *=
    mix(
        1.0,
        0.15,
        Despawn
    );


    float deathPieces =
    despawnFragments(
        p
    );


    vec2 q =
    p;


    q +=
    vec2(
    fbm(
        q * 2.7
        + vec2(
        Time * 0.05,
        -Time * 0.04
        )
    )
    - 0.5,

    fbm(
        q * 2.4
        + vec2(
        -Time * 0.04,
        Time * 0.06
        )
    )
    - 0.5
    ) * 0.14;

    float cloudA =
    fbm(
        q * 2.10
        + vec2(
        Time * 0.030,
        -Time * 0.022
        )
    );


    float cloudB =
    fbm(
        rotate2D(0.55)
        * q
        * 2.70

        + vec2(
        -Time * 0.026,
        Time * 0.035
        )
    );


    float cloudC =
    fbm(
        rotate2D(-0.80)
        * q
        * 3.20

        + vec2(
        Time * 0.022,
        Time * 0.028
        )
    );


    float waveA =
    0.5
    + 0.5
    * sin(
        q.y * 4.8
        + cloudB * 4.8
        - Time * 0.42
    );


    float waveB =
    0.5
    + 0.5
    * sin(
        q.x * 4.2
        - cloudC * 4.1
        + Time * 0.33
    );


    float waveC =
    0.5
    + 0.5
    * sin(
        (
        q.x
        + q.y
        ) * 3.6

        + cloudA * 5.2
        - Time * 0.26
    );


    float nebulaCyan =
    smoothstep(
        0.42,
        0.90,

        mix(
            cloudA,
            waveA,
            0.42
        )
    );


    float nebulaBlue =
    smoothstep(
        0.46,
        0.92,

        mix(
            cloudB,
            waveB,
            0.45
        )
    );


    float nebulaGreen =
    smoothstep(
        0.43,
        0.88,

        mix(
            cloudC,
            waveC,
            0.40
        )
    );

    float innerFragments =
    innerCloudFragments(
        q
    );


    innerFragments *=
    smoothstep(
        0.63,
        0.95,
        Reveal
    );


    innerFragments *=
    1.0
    - smoothstep(
        0.25,
        0.90,
        Despawn
    );

    float dust =
    portalDust(
        q
    );


    dust *=
    smoothstep(
        0.70,
        0.98,
        Reveal
    );


    dust *=
    1.0
    - smoothstep(
        0.25,
        0.88,
        Despawn
    );


    vec3 interior =
    vec3(
    0.003,
    0.008,
    0.018
    );


    interior +=
    vec3(
    0.02,
    0.23,
    0.18
    )
    * nebulaCyan
    * 0.92;


    interior +=
    vec3(
    0.03,
    0.10,
    0.30
    )
    * nebulaBlue
    * 0.98;


    interior +=
    vec3(
    0.00,
    0.17,
    0.10
    )
    * nebulaGreen
    * 0.82;


    float mixingAB =
    nebulaCyan
    * nebulaBlue;


    float mixingAC =
    nebulaCyan
    * nebulaGreen;


    interior +=
    vec3(
    0.02,
    0.12,
    0.14
    )
    * mixingAB
    * 0.55;


    interior +=
    vec3(
    0.01,
    0.10,
    0.08
    )
    * mixingAC
    * 0.42;


    interior +=
    vec3(
    0.02,
    0.11,
    0.13
    )
    * innerFragments
    * 0.42;


    interior +=
    vec3(
    0.01,
    0.05,
    0.09
    )
    * innerFragments
    * (
    0.55
    + 0.45 * nebulaBlue
    )
    * 0.25;

    float centerGlow =
    1.0
    - smoothstep(
        0.04,
        0.60,
        length(q)
    );


    interior *=
    mix(
        1.0,
        1.28,
        centerGlow
    );


    interior +=
    vec3(
    0.015,
    0.145,
    0.115
    )
    * centerGlow
    * (
    0.45
    + cloudA * 0.55
    );

    vec3 dustColor =
    mix(
        vec3(
        0.45,
        0.90,
        1.00
        ),

        vec3(
        0.65,
        0.84,
        0.94
        ),

        0.5
        + 0.5
        * sin(
            Time * 0.8
            + q.x * 2.0
        )
    );


    interior +=
    dustColor
    * dust
    * 0.22;

    float edgeNoise =
    fbm(
        p * 3.2
        + vec2(
        Time * 0.12,
        -Time * 0.09
        )
    );


    float hue =
    0.5
    + 0.5
    * sin(
        p.x * 2.0
        - p.y * 2.3
        - Time * 0.95
        + edgeNoise * 2.8
    );


    vec3 cyanGreen =
    vec3(
    0.34,
    1.00,
    0.86
    );


    vec3 aquaBlue =
    vec3(
    0.48,
    0.66,
    1.00
    );


    vec3 borderColor =
    mix(
        cyanGreen,
        aquaBlue,
        hue
    );


    borderColor +=
    vec3(
    0.00,
    0.10,
    0.08
    )
    * (
    0.5
    + 0.5
    * sin(
        Time * 1.55
        + p.y * 8.0
    )
    );

    vec3 constructionColor =
    mix(
        vec3(
        0.32,
        1.00,
        0.79
        ),

        vec3(
        0.42,
        0.72,
        1.00
        ),

        0.5
        + 0.5
        * sin(
            Time * 3.2
        )
    );

    vec3 despawnColor =
    mix(
        cyanGreen,
        aquaBlue,
        0.45
    );

    float stars =
    sequentialStars(
        p
    );


    stars *=
    smoothstep(
        0.86,
        1.0,
        Reveal
    );


    stars *=
    1.0
    - smoothstep(
        0.0,
        0.65,
        Despawn
    );


    float coreStars =
    tinyCoreStars(
        q
    );


    coreStars *=
    smoothstep(
        0.88,
        1.0,
        Reveal
    );


    coreStars *=
    1.0
    - smoothstep(
        0.0,
        0.70,
        Despawn
    );


    vec3 starColor =
    mix(
        vec3(
        0.55,
        0.94,
        1.00
        ),

        vec3(
        0.67,
        0.80,
        1.00
        ),

        0.5
        + 0.5
        * sin(
            Time * 1.2
        )
    );

    if (GlowOnly < 0.5) {

        vec3 color =
        interior
        * body;


        color +=
        borderColor
        * border
        * 1.03;

        color +=
        constructionColor
        * buildFragments
        * 0.70
        * (
        1.0 - Despawn
        );


        color +=
        constructionColor
        * buildHead
        * 1.20
        * (
        1.0 - Despawn
        );

        color +=
        despawnColor
        * deathPieces
        * 0.82;


        float alpha =
        body * 0.985
        + border * 0.80

        + buildFragments
        * 0.80
        * (
        1.0 - Despawn
        )

        + buildHead
        * (
        1.0 - Despawn
        )

        + deathPieces * 0.88;


        if (alpha <= 0.006) {
            discard;
        }


        fragColor =
        vec4(
        color,

        clamp(
            alpha,
            0.0,
            1.0
        )
        );


        return;
    }

    float bloom =
    exp(
        -max(
            d,
            0.0
        ) * 5.8
    ) * 0.30;


    bloom *=
    (
    1.0
    - smoothstep(
        -0.03,
        0.18,
        d
    )
    );


    bloom *=
    smoothstep(
        0.42,
        0.92,
        Reveal
    );


    bloom *=
    mix(
        1.0,
        0.10,
        Despawn
    );


    float constructionGlow =
    (
    buildHeadHalo * 1.30
    + buildFragments * 0.75
    )
    * (
    1.0 - Despawn
    );


    float trailGlow =
    buildMask
    * fullBody

    * (
    1.0
    - smoothstep(
        0.70,
        0.98,
        Reveal
    )
    )

    * 0.18

    * (
    1.0 - Despawn
    );


    float glow =
    border * 0.78
    + halo * 0.78
    + bloom * 0.72
    + stars * 1.05
    + coreStars * 0.95
    + dust * 0.14
    + constructionGlow
    + trailGlow
    + deathPieces * 0.75;


    if (glow <= 0.006) {
        discard;
    }


    vec3 glowColor =
    borderColor
    * (
    border * 0.90
    + halo * 0.70
    + bloom * 0.58
    );


    glowColor +=
    constructionColor
    * (
    buildHeadHalo * 1.20
    + buildFragments * 0.80
    + trailGlow
    )
    * (
    1.0 - Despawn
    );


    glowColor +=
    starColor
    * stars
    * 1.28;


    glowColor +=
    vec3(
    0.46,
    1.00,
    0.88
    )
    * coreStars
    * 1.15;


    glowColor +=
    dustColor
    * dust
    * 0.18;


    glowColor +=
    despawnColor
    * deathPieces
    * 0.85;


    fragColor =
    vec4(
    glowColor,

    clamp(
        glow * 0.74,
        0.0,
        1.0
    )
    );
}