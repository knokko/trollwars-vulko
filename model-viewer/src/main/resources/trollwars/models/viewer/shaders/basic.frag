#version 130

in vec2 passTextureCoords;

out vec4 color;

uniform sampler2D textureSampler;

void main(){
    vec4 textureColor = texture(textureSampler, passTextureCoords);
    color = textureColor;
}