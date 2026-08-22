#version 150


/*
 * =========================================================
 * Azumaal Living Chaos Fog
 * =========================================================
 *
 * Основной стиль:
 *
 * - глубокая нефритово-циановая туманность
 * - медленная жидкая деформация
 * - мягкие светящиеся облака
 *
 * Дополнительно:
 *
 * - редкие голубые участки
 * - редкие фиолетовые участки
 * - очень лёгкая мозаика
 * - плавающие квадраты
 *
 * Но основной визуальный слой —
 * именно туманность, а не digital glitch.
 */


/*
 * =========================================================
 * Uniforms
 * =========================================================
 */

uniform float Time;

uniform float Reveal;

uniform float Alpha;


/*
 * Текущая GUI-панель передаёт
 * размеры именно так.
 */
uniform float PanelWidth;

uniform float PanelHeight;


in vec2 texCoord;


out vec4 fragColor;


/*
 * =========================================================
 * Random
 * =========================================================
 */

float hash11(
    float p
) {
    return fract(
        sin(
            p * 127.1
        )
        * 43758.5453123
    );
}


float hash21(
    vec2 p
) {
    return fract(
        sin(
            dot(
                p,

                vec2(
                127.1,
                311.7
                )
            )
        )
        * 43758.5453123
    );
}


/*
 * =========================================================
 * Value noise
 * =========================================================
 */

float noise(
    vec2 p
) {
    vec2 i =
    floor(
        p
    );


    vec2 f =
    fract(
        p
    );


/*
     * Smooth interpolation.
     */
    vec2 u =
    f
    * f
    * (
    3.0
    - 2.0
    * f
    );


    float a =
    hash21(
        i
    );


    float b =
    hash21(
        i
        + vec2(
        1.0,
        0.0
        )
    );


    float c =
    hash21(
        i
        + vec2(
        0.0,
        1.0
        )
    );


    float d =
    hash21(
        i
        + vec2(
        1.0,
        1.0
        )
    );


    return mix(
        mix(
            a,
            b,
            u.x
        ),

        mix(
            c,
            d,
            u.x
        ),

        u.y
    );
}


/*
 * =========================================================
 * Fractal noise
 * =========================================================
 */

float fbm(
    vec2 p
) {
    float result =
    0.0;


    float amplitude =
    0.5;


    for (
    int i = 0;
    i < 5;
    i++
    ) {

        result +=
        noise(
            p
        )
        * amplitude;


        p =
        p
        * 2.03

        + vec2(
        13.7,
        7.9
        );


        amplitude *=
        0.5;
    }


    return result;
}


/*
 * =========================================================
 * Rotation
 * =========================================================
 */

mat2 rotation(
    float angle
) {
    float s =
    sin(
        angle
    );


    float c =
    cos(
        angle
    );


    return mat2(
    c,
    -s,
    s,
    c
    );
}


/*
 * =========================================================
 * Square SDF
 * =========================================================
 */

float squareShape(
    vec2 p,
    float size
) {
    vec2 d =
    abs(
        p
    )
    - vec2(
    size
    );


    return max(
        d.x,
        d.y
    );
}


/*
 * =========================================================
 * Main
 * =========================================================
 */

void main() {

    vec2 uv =
    texCoord;


/*
     * =========================================================
     * Correct aspect
     * =========================================================
     *
     * ВАЖНО:
     *
     * раньше shader ожидал Aspect uniform.
     *
     * Теперь вычисляем его непосредственно
     * из размеров GUI-панели.
     */

    float aspect =
    PanelWidth
    / max(
        PanelHeight,
        1.0
    );


    vec2 p =
    (
    uv
    - 0.5
    )
    * vec2(
    aspect,
    1.0
    );


/*
     * Медленное время.
     */
    float t =
    Time;


/*
     * =========================================================
     * Primary liquid distortion
     * =========================================================
     */

    vec2 q;


    q.x =
    fbm(
        p
        * 1.20

        + vec2(
        t * 0.10,
        -t * 0.035
        )
    );


    q.y =
    fbm(
        p
        * 1.25

        + vec2(
        -t * 0.055,
        t * 0.075
        )

        + 5.7
    );


/*
     * Большие облачные массы
     * слегка изгибают пространство.
     */
    vec2 warped =
    p

    + (
    q
    - 0.5
    )
    * 0.85;


    float liquid =
    fbm(
        warped
        * 1.55

        + vec2(
        t * 0.055,
        0.0
        )
    );


/*
     * =========================================================
     * Long flowing waves
     * =========================================================
     */

    float waveA =
    sin(
        warped.x
        * 3.7

        + liquid
        * 5.0

        - t
        * 0.65
    );


    float waveB =
    sin(
        warped.x
        * 6.2

        - warped.y
        * 3.0

        + q.y
        * 4.0

        + t
        * 0.42
    );


/*
     * Третий более мягкий слой,
     * чтобы волны не выглядели
     * только как две синусоиды.
     */
    float waveC =
    sin(
        warped.x
        * 2.15

        + warped.y
        * 2.8

        + liquid
        * 3.4

        - t
        * 0.24
    );


    float waves =
    waveA
    * 0.48

    + waveB
    * 0.21

    + waveC
    * 0.12;


    waves =
    waves
    * 0.5
    + 0.5;


/*
     * =========================================================
     * Main jade palette
     * =========================================================
     */

    vec3 deepColor =
    vec3(
    0.008,
    0.055,
    0.055
    );


    vec3 jadeColor =
    vec3(
    0.018,
    0.285,
    0.270
    );


    vec3 cyanColor =
    vec3(
    0.115,
    0.650,
    0.635
    );


    vec3 brightColor =
    vec3(
    0.450,
    0.920,
    0.885
    );


/*
     * Дополнительные цвета.
     *
     * Они НЕ заменяют основной jade,
     * а только появляются отдельными
     * облачными областями.
     */

    vec3 blueColor =
    vec3(
    0.140,
    0.410,
    0.880
    );


    vec3 violetColor =
    vec3(
    0.470,
    0.135,
    0.720
    );


/*
     * =========================================================
     * Main fog field
     * =========================================================
     */

    float liquidMask =
    smoothstep(
        0.20,
        0.84,

        liquid

        + waves
        * 0.18
    );


    vec3 color =
    mix(
        deepColor,
        jadeColor,
        liquidMask
    );


/*
     * =========================================================
     * Cyan highlights
     * =========================================================
     */

    float highlight =
    smoothstep(
        0.62,
        0.94,

        liquid

        + waves
        * 0.20
    );


    color =
    mix(
        color,
        cyanColor,

        highlight
        * 0.64
    );


/*
     * =========================================================
     * Soft liquid highlights
     * =========================================================
     *
     * Делаем их шире и мягче,
     * чтобы снова не получить
     * "мыльные линии".
     */

    float liquidLine =
    1.0

    - smoothstep(
        0.0,
        0.19,

        abs(
            waves
            - 0.62
        )
    );


    color +=
    brightColor

    * liquidLine

    * 0.055;


/*
     * =========================================================
     * Large luminous fog
     * =========================================================
     */

    float cloud =
    fbm(
        warped
        * 0.72

        + vec2(
        -t * 0.025,
        t * 0.020
        )
    );


    float cloudMask =
    smoothstep(
        0.53,
        0.88,
        cloud
    );


    color +=
    cyanColor

    * cloudMask

    * 0.13;


/*
     * =========================================================
     * Purple fog
     * =========================================================
     *
     * Большие редкие области.
     *
     * Не цифровой glitch,
     * а та же самая туманность,
     * только другого цвета.
     */

    float violetFog =
    fbm(
        warped
        * 0.88

        + vec2(
        -t * 0.032,
        t * 0.018
        )

        + vec2(
        19.4,
        7.3
        )
    );


    float violetMask =
    smoothstep(
        0.64,
        0.89,
        violetFog
    );


/*
     * Медленная пульсация,
     * чтобы область будто дышала.
     */
    violetMask *=
    0.72

    + 0.28
    * (
    0.5
    + 0.5
    * sin(
        t
        * 0.48

        + violetFog
        * 4.5
    )
    );


    color =
    mix(
        color,
        violetColor,

        violetMask
        * 0.19
    );


/*
     * =========================================================
     * Blue fog
     * =========================================================
     */

    float blueFog =
    fbm(
        warped
        * 1.08

        + vec2(
        t * 0.021,
        t * 0.027
        )

        + vec2(
        4.2,
        31.7
        )
    );


    float blueMask =
    smoothstep(
        0.67,
        0.91,
        blueFog
    );


    color =
    mix(
        color,
        blueColor,

        blueMask
        * 0.15
    );


/*
     * =========================================================
     * VERY SUBTLE MOSAIC
     * =========================================================
     *
     * Здесь намеренно совсем немного.
     *
     * Основной фон остаётся туманным.
     */

    vec2 mosaicGrid =
    vec2(
    27.0,
    8.0
    );


    vec2 mosaicCell =
    floor(
        uv
        * mosaicGrid
    );


    float mosaicNoise =
    hash21(
        mosaicCell

        + floor(
            t
            * 0.16
        )
        * vec2(
        1.0,
        0.0
        )
    );


/*
     * Только примерно самые яркие
     * ~12% ячеек вообще видимы.
     */
    float mosaicMask =
    smoothstep(
        0.86,
        0.98,
        mosaicNoise
    );


/*
     * Второй noise делает ячейки
     * разными по цвету.
     */
    float mosaicColorNoise =
    hash21(
        mosaicCell
        + 13.7
    );


    vec3 mosaicColor =
    mix(
        vec3(
        0.080,
        0.440,
        0.470
        ),

        blueColor,

        mosaicColorNoise
    );


    mosaicColor =
    mix(
        mosaicColor,
        violetColor,

        smoothstep(
            0.72,
            0.96,
            mosaicColorNoise
        )
    );


    color +=
    mosaicColor

    * mosaicMask

    * 0.075;


/*
     * =========================================================
     * Small floating squares
     * =========================================================
     *
     * Они остались от первого shader,
     * но теперь встречаются реже
     * и менее яркие.
     */

    for (
    int i = 0;
    i < 18;
    i++
    ) {

        float index =
        float(
        i
        )
        + 1.0;


        float randomX =
        hash11(
            index
            * 4.17
        );


        float randomY =
        hash11(
            index
            * 9.73
        );


        float randomSpeed =
        hash11(
            index
            * 15.91
        );


        float randomColor =
        hash11(
            index
            * 31.73
        );


    /*
         * Плавное горизонтальное движение.
         */
        float squareX =
        fract(
            randomX

            + t
            * (
            0.008
            + randomSpeed
            * 0.010
            )
        );


    /*
         * Лёгкое вертикальное парение.
         */
        float squareY =
        randomY

        + sin(
            t
            * (
            0.28
            + randomSpeed
            * 0.22
            )

            + index
            * 2.1
        )
        * 0.065;


        squareY =
        fract(
            squareY
        );


        vec2 squarePos =
        uv

        - vec2(
        squareX,
        squareY
        );


    /*
         * Квадрат должен оставаться квадратом,
         * несмотря на широкую панель.
         */
        squarePos.x *=
        aspect;


        float angle =
        t
        * (
        0.22
        + randomSpeed
        * 0.34
        )

        + index
        * 0.93;


        squarePos =
        rotation(
            angle
        )
        * squarePos;


        float squareSize =
        mix(
            0.007,
            0.016,

            hash11(
                index
                * 21.3
            )
        );


        float squareDistance =
        squareShape(
            squarePos,
            squareSize
        );


        float squareMask =
        1.0

        - smoothstep(
            0.0,
            0.0035,
            squareDistance
        );


    /*
         * Плавно проявляются
         * и исчезают.
         */
        float fade =
        0.5

        + 0.5
        * sin(
            t
            * (
            0.65
            + randomSpeed
            * 0.25
            )

            + index
            * 1.73
        );


        fade =
        smoothstep(
            0.16,
            0.90,
            fade
        );


    /*
         * Cyan / blue / violet.
         */

        vec3 squareCyan =
        vec3(
        0.045,
        0.360,
        0.390
        );


        vec3 squareBlue =
        vec3(
        0.220,
        0.570,
        0.850
        );


        vec3 squareViolet =
        vec3(
        0.430,
        0.145,
        0.690
        );


        vec3 squareColor;


        if (randomColor < 0.56) {

            squareColor =
            squareCyan;

        } else if (randomColor < 0.82) {

            squareColor =
            squareBlue;

        } else {

            squareColor =
            squareViolet;
        }


        color +=
        squareColor

        * squareMask

        * fade

        * 0.40;
    }


/*
     * =========================================================
     * Tiny drifting dust
     * =========================================================
     *
     * Очень маленькие точки,
     * чтобы фон выглядел живее.
     */

    vec2 dustGrid =
    floor(
        uv
        * vec2(
        92.0,
        27.0
        )

        + vec2(
        -t * 0.70,
        t * 0.14
        )
    );


    float dustNoise =
    hash21(
        dustGrid
    );


    float dustMask =
    smoothstep(
        0.982,
        1.0,
        dustNoise
    );


    vec3 dustColor =
    mix(
        brightColor,
        blueColor,

        hash21(
            dustGrid
            + 4.2
        )
    );


    color +=
    dustColor

    * dustMask

    * 0.15;


/*
     * =========================================================
     * Soft vignette
     * =========================================================
     */

    float edgeX =
    smoothstep(
        0.0,
        0.14,
        uv.x
    )

    * smoothstep(
        0.0,
        0.14,
        1.0
        - uv.x
    );


    float edgeY =
    smoothstep(
        0.0,
        0.16,
        uv.y
    )

    * smoothstep(
        0.0,
        0.16,
        1.0
        - uv.y
    );


    float vignette =
    edgeX
    * edgeY;


    color *=
    0.58

    + vignette
    * 0.42;


/*
     * =========================================================
     * Center reveal
     * =========================================================
     */

    float centerDistance =
    abs(
        uv.x
        - 0.5
    )
    * 2.0;


    float revealMask =
    1.0

    - smoothstep(
        Reveal,
        Reveal
        + 0.035,

        centerDistance
    );


/*
     * Светящаяся граница раскрытия.
     */
    float revealEdge =
    1.0

    - smoothstep(
        0.0,
        0.045,

        abs(
            centerDistance
            - Reveal
        )
    );


/*
     * После полного раскрытия
     * линия должна исчезнуть.
     */
    revealEdge *=
    1.0

    - smoothstep(
        0.90,
        1.0,
        Reveal
    );


/*
     * Граница сама слегка меняет
     * cyan → violet.
     */

    vec3 revealColor =
    mix(
        brightColor,
        violetColor,

        0.5
        + 0.5
        * sin(
            t
            * 0.75
        )
    );


    color +=
    revealColor

    * revealEdge

    * 0.20;


/*
     * =========================================================
     * Final
     * =========================================================
     */

    color =
    clamp(
        color,
        0.0,
        1.0
    );


    fragColor =
    vec4(
    color,

    Alpha
    * revealMask
    * 0.96
    );
}