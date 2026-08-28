#version 150

uniform float Time;
uniform float Intro;
uniform float Heartbeat;
uniform float Aspect;

in vec2 vUv;

out vec4 fragColor;

float softBand(float distanceToCurve, float coreWidth, float feather) {
    return 1.0 - smoothstep(coreWidth, coreWidth + feather, distanceToCurve);
}

float gaussianGlow(vec2 uv, vec2 center, float strength) {
    vec2 d = uv - center;
    d.x *= Aspect;
    return exp(-dot(d, d) * strength);
}

void main() {
    vec2 uv = vUv;

    vec3 topColor = vec3(0.018, 0.072, 0.090);
    vec3 bottomColor = vec3(0.017, 0.028, 0.070);

    vec3 color = mix(topColor, bottomColor, smoothstep(0.05, 1.0, uv.y));

    vec2 centered = (uv - vec2(0.5)) * vec2(Aspect, 1.0);
    float radial = length(centered);
    float vignette = 1.0 - smoothstep(0.42, 0.92, radial);
    color *= mix(0.77, 1.0, vignette);

    float lowerMask = smoothstep(0.42, 0.77, uv.y);

    float curveCyan =
          0.735
        + sin(uv.x * 8.8 + Time * 0.78) * 0.043
        + sin(uv.x * 20.0 - Time * 0.27) * 0.010;

    float curvePurple =
          0.825
        + sin(uv.x * 7.1 - Time * 0.56 + 1.55) * 0.034
        + sin(uv.x * 16.0 + Time * 0.20) * 0.008;

    float curveBlue =
          0.905
        + sin(uv.x * 10.4 + Time * 0.44 + 2.30) * 0.025;

    float cyanWave = softBand(abs(uv.y - curveCyan), 0.006, 0.060) * lowerMask;
    float purpleWave = softBand(abs(uv.y - curvePurple), 0.006, 0.052) * lowerMask;
    float blueWave = softBand(abs(uv.y - curveBlue), 0.005, 0.040) * lowerMask;

    vec3 cyan = vec3(0.035, 1.000, 0.900);
    vec3 purple = vec3(0.650, 0.260, 1.000);
    vec3 blue = vec3(0.080, 0.720, 1.000);

    color += cyan * cyanWave * 0.105 * Intro;
    color += purple * purpleWave * 0.090 * Intro;
    color += blue * blueWave * 0.055 * Intro;

    float hazeField =
          sin(uv.x * 5.2 + uv.y * 4.5 + Time * 0.31)
        + sin(uv.x * 9.0 - uv.y * 3.0 - Time * 0.18);

    hazeField = hazeField * 0.5 + 0.5;
    float hazeMask = smoothstep(0.50, 0.92, uv.y);

    color += mix(purple, cyan, uv.x)
            * hazeField
            * hazeMask
            * 0.014
            * Intro;

    float heartA = gaussianGlow(uv, vec2(0.04, 0.14), 52.0);
    float heartB = gaussianGlow(uv, vec2(0.96, 0.86), 52.0);

    color += cyan
            * (heartA + heartB)
            * Heartbeat
            * 0.075;

    float bottomCurve =
          0.965
        + sin(uv.x * 12.0 - Time * 0.45) * 0.010;

    float bottomLine = softBand(abs(uv.y - bottomCurve), 0.002, 0.014);
    color += cyan * bottomLine * 0.045 * Intro;

    fragColor = vec4(color, 0.965);
}
