#version 330

uniform sampler2D InSampler;

layout(std140) uniform SpectralBlur {
    vec2 HalfPixel;
    float Offset;
    float Shade;
};

in vec2 texCoord;

out vec4 fragColor;

// Dual-Kawase downsample: centre weighted 4, four diagonal taps.
void main() {
    vec2 o = HalfPixel * Offset;
    vec3 sum = texture(InSampler, texCoord).rgb * 4.0;
    sum += texture(InSampler, texCoord - o).rgb;
    sum += texture(InSampler, texCoord + o).rgb;
    sum += texture(InSampler, texCoord + vec2(o.x, -o.y)).rgb;
    sum += texture(InSampler, texCoord - vec2(o.x, -o.y)).rgb;
    fragColor = vec4(sum / 8.0, 1.0);
}
