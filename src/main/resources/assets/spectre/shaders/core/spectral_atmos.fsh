#version 330

layout(std140) uniform SpectralAtmos {
    vec4 Tint;    // accent rgb, unused w
    vec4 Params;  // time, enabled, aspect, vignette
};

in vec2 texCoord;

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p = p * 2.03 + vec2(17.1, 9.2);
        a *= 0.5;
    }
    return v;
}

// Low drifting fog (denser near the bottom) and a violet vignette. Output alpha is the layer's
// coverage; ImGui composites it over the frame with normal alpha blending.
void main() {
    float time = Params.x;
    float enabled = Params.y;
    vec2 uv = texCoord;
    vec2 p = vec2(uv.x * Params.z, uv.y) * 2.2;
    float drift = fbm(p + vec2(time * 0.035, -time * 0.02));
    float fog = fbm(p * 0.7 + vec2(drift * 1.6 - time * 0.02, time * 0.012));
    float low = smoothstep(0.85, 0.0, uv.y); // texCoord.y = 0 is the bottom of the screen
    float density = smoothstep(0.42, 0.95, fog) * (0.35 + 0.65 * low) * enabled;
    vec3 fogColor = mix(vec3(0.42, 0.36, 0.62), Tint.rgb, 0.35) * (0.55 + 0.45 * fog);

    vec2 c = uv - 0.5;
    c.x *= Params.z;
    float vig = smoothstep(0.35, 1.05, length(c)) * Params.w;

    float fogA = clamp(density * 0.22, 0.0, 1.0);
    float vigA = clamp(vig * 0.55, 0.0, 1.0) * (1.0 - fogA);
    float alpha = fogA + vigA;
    vec3 color = (fogColor * fogA + vec3(0.02, 0.015, 0.05) * vigA) / max(alpha, 1e-4);
    fragColor = vec4(color, alpha);
}
