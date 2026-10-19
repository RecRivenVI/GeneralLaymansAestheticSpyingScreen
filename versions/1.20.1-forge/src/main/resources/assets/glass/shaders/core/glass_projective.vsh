#version 150
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
in vec3 Position;
in vec4 Color;
out vec4 texProj0;
out vec4 vertexColor;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texProj0 = vec4((gl_Position.xy + gl_Position.ww) * 0.5, gl_Position.zw);
    vertexColor = Color;
}
