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
    //mat4 subMatrix = subMatrices[matrixIndexInt];
    gl_Position = baseMatrix * subMatrix * vec4(modelPosition + vec3(0.0, matrixIndexFract * 0.1, 0.0), 1.0);
    passTextureCoords = textureCoords;
}