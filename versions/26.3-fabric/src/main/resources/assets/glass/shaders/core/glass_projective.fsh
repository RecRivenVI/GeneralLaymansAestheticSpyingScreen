#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

layout(location = 0) in vec4 texProj0;
layout(location = 1) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

void main() {
    if (vertexColor.a * ColorModulator.a <= 0.00625) {
        discard;
    }
    vec3 projectedColor = textureProj(Sampler0, texProj0).rgb;
    vec3 color = mix(projectedColor, vec3(1.0), clamp(vertexColor.r, 0.0, 1.0));
    fragColor = vec4(color * ColorModulator.rgb, vertexColor.a * ColorModulator.a);
}
