#version 130

in vec2 passTextureCoords;

out vec4 outColor;

void main(){
    outColor = vec4(1.0, passTextureCoords.x, passTextureCoords.y, 1.0);
}