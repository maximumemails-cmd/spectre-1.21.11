#version 330

uniform sampler2D InSampler;

layout(std140) uniform SpectralBlur {
    vec2 HalfPixel;
    float Offset;
    float Shade;
};

in vec2 texCoord;

out vec4 fragColor;

// Dual-Kawase upsample (8 taps). On the final pass, Shade > 0 cools, darkens and slightly
// desaturates the result so text on glass panels stays readable.
void main() {
    vec2 o = HalfPixel * Offset;
    vec3 sum = texture(InSampler, texCoord + vec2(-o.x * 2.0, 0.0)).rgb;
    sum += texture(InSampler, texCoord + vec2(-o.x, o.y)).rgb * 2.0;
    sum += texture(InSampler, texCoord + vec2(0.0, o.y * 2.0)).rgb;
    sum += texture(InSampler, texCoord + vec2(o.x, o.y)).rgb * 2.0;
    sum += texture(InSampler, texCoord + vec2(o.x * 2.0, 0.0)).rgb;
    sum += texture(InSampler, texCoord + vec2(o.x, -o.y)).rgb * 2.0;
    sum += texture(InSampler, texCoord + vec2(0.0, -o.y * 2.0)).rgb;
    sum += texture(InSampler, texCoord + vec2(-o.x, -o.y)).rgb * 2.0;
    vec3 color = sum / 12.0;
    if (Shade > 0.0) {
        float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
        vec3 cool = mix(color, vec3(luma), 0.22) * vec3(0.86, 0.84, 0.98);
        color = mix(color, cool * 0.82, Shade);
    }
    fragColor = vec4(color, 1.0);
}
