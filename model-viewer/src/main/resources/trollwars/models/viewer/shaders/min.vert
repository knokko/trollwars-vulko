#version 130

in vec3 position;
in vec3 normals;
in vec2 textureCoords;
in int matrixIndex;

out vec2 passTextureCoords;

uniform mat4 baseMatrix;
uniform mat4 subMatrices[100];

void main(){
    gl_Position = baseMatrix * subMatrices[matrixIndex] * vec4(position, 1.0);
    passTextureCoords = textureCoords;
}