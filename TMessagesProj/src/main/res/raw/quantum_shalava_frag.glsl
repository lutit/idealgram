precision highp float;
varying vec2 uv;
uniform float t;
uniform vec2 r;

#define PI 3.14159265359
#define TAU 6.28318530718

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
        mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x),
        f.y
    );
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += a * noise(p);
        p *= 2.0;
        a *= 0.5;
    }
    return v;
}

vec3 palette(float t) {
    vec3 a = vec3(0.5);
    vec3 b = vec3(0.5);
    vec3 c = vec3(1.0);
    vec3 d = vec3(0.0, 0.33, 0.67);
    return a + b * cos(TAU * (c * t + d));
}

void main() {
    vec2 aspect = vec2(r.x / r.y, 1.0);
    vec2 uvC = (uv - 0.5) * aspect;

    vec3 photo = vec3(0.0);
    vec3 col = photo;

    float dist = length(uvC);
    float angle = atan(uvC.y, uvC.x);

    float rays = 0.0;
    for (int i = 0; i < 8; i++) {
        float fi = float(i);
        float a = fi * PI / 4.0 + t * 0.3;
        float ray = pow(max(0.0, cos(angle - a)), 30.0);
        ray *= smoothstep(1.0, 0.0, dist) * smoothstep(0.0, 0.2, dist);
        rays += ray;
    }
    col += palette(angle / TAU + t * 0.2) * rays * 0.4;

    float glow = exp(-dist * 3.0) * 0.5;
    col += palette(t * 0.3) * glow * 0.6;

    float ring = smoothstep(0.02, 0.0, abs(dist - 0.3 - sin(t) * 0.05));
    col += palette(t * 0.5 + 0.3) * ring * 0.4;

    float ring2 = smoothstep(0.015, 0.0, abs(dist - 0.5 - cos(t * 0.7) * 0.03));
    col += palette(t * 0.4 + 0.6) * ring2 * 0.3;

    for (int i = 0; i < 40; i++) {
        float fi = float(i);
        float speed = 0.05 + hash(vec2(fi, 0.0)) * 0.1;
        vec2 pos = vec2(
            hash(vec2(fi * 7.0, 0.0)),
            mod(hash(vec2(0.0, fi * 13.0)) + t * speed, 1.0)
        );
        pos.x += sin(t + fi) * 0.02;

        float size = 0.003 + hash(vec2(fi, fi)) * 0.005;
        float d = length(uv - pos);
        float particle = smoothstep(size, 0.0, d);
        float particleGlow = smoothstep(size * 4.0, 0.0, d) * 0.3;

        vec3 pColor = palette(fi * 0.1 + t * 0.5);
        col += pColor * (particle + particleGlow) * 0.5;
    }

    float prism = pow(fbm(uvC * 5.0 + t * 0.5), 2.0) * 0.4;
    col += palette(fbm(uvC * 3.0 - t * 0.3) + t * 0.2) * prism;

    for (int i = 0; i < 15; i++) {
        float fi = float(i);
        float phase = t * 0.3 + fi * 1.3;
        vec2 bokehPos = vec2(
            0.5 + sin(phase) * 0.4 + sin(phase * 1.7) * 0.1,
            0.5 + cos(phase * 0.8) * 0.3 + cos(phase * 2.1) * 0.1
        );

        float bokehDist = length(uv - bokehPos);
        float bokeh = smoothstep(0.08, 0.0, bokehDist);
        float bokehRing = smoothstep(0.003, 0.0, abs(bokehDist - 0.05)) * 0.5;

        vec3 bokehColor = palette(fi * 0.2 + t * 0.3);
        col += bokehColor * (bokeh * 0.2 + bokehRing * 0.3);
    }

    vec2 corner1 = vec2(0.0, 1.0);
    vec2 corner2 = vec2(1.0, 1.0);

    float cornerDist1 = length(uv - corner1);
    float cornerDist2 = length(uv - corner2);

    float cornerGlow1 = exp(-cornerDist1 * 2.0) * 0.3;
    float cornerGlow2 = exp(-cornerDist2 * 2.5) * 0.25;

    col += vec3(0.3, 0.5, 1.0) * cornerGlow1;
    col += vec3(1.0, 0.3, 0.5) * cornerGlow2;

    float fog = fbm(uv * 3.0 + t * 0.2) * 0.15;
    col += palette(t * 0.2) * fog;

    float sparkle = pow(noise(uv * 100.0 + t * 5.0), 20.0) * 2.0;
    col += vec3(1.0) * sparkle;

    float vignette = 1.0 - dist * 0.4;
    col *= vignette;

    col += photo * smoothstep(0.3, 0.6, dist) * 0.1;

    col = pow(col, vec3(0.95));
    col.r += 0.02;
    col.b += 0.03;

    col += hash(uv + t) * 0.02 - 0.01;

    gl_FragColor = vec4(col, 0.85);
}
