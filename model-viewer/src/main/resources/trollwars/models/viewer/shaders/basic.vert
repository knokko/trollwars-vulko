#version 130

in vec3 modelPosition;
in vec3 modelNormal;
in vec2 textureCoords;
in float matrixIndex;

out vec2 passTextureCoords;

uniform mat4 baseMatrix;
uniform mat4 subMatrices[50];

void main(){
    int matrixIndexInt = int(matrixIndex);
    float matrixIndexFract = matrixIndex - matrixIndexInt;
    mat4 subMatrix = subMatrices[matrixIndexInt] * (1 - matrixIndexFract) + matrixIndexFract * subMatrices[matrixIndexInt + 1];
    gl_Position = baseMatrix * subMatrix * vec4(modelPosition, 1.0);
    passTextureCoords = textureCoords;
}