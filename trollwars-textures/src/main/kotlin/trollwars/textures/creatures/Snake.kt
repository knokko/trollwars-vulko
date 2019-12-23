package trollwars.textures.creatures

import vulko.memory.VirtualStack
import vulko.textures.TextureBuilder
import vulko.util.math.ceilDiv
import java.util.*

class SnakeTextureProperties(val colorPattern: TextureBuilder, val skinColor: Int,
                             val scaleLength: Int = 55, val scaleWidth: Int = 35,
                             val scaleOffsetLength: Int = 42, val scaleOffsetWidth: Int = 21) {

    fun getFullWidth() : Long {
        return when (scaleLength <= scaleOffsetLength) {
            true -> colorPattern.width * scaleOffsetLength
            false -> (colorPattern.width - 1) * scaleOffsetLength + scaleLength
        }
    }

    fun getFullHeight() : Long {
        return 2 * (colorPattern.height - 1) * scaleOffsetWidth
    }
}

// Yeah yeah, I should give this type of snake a proper name instead of 'snake1'
fun createSnake1Properties(stack: VirtualStack, rng: Random = Random()) : SnakeTextureProperties {
    val inputWidth = 100L

    // NOTE THAT IT IS CURRENTLY REQUIRED THAT inputHeight IS EVEN
    val inputHeight = 20L
    val inputAddress = stack.push(4L * inputWidth * inputHeight)
    val inputTexture = TextureBuilder(inputAddress, inputWidth, inputHeight)
    val upperRed = 20
    val upperGreen = 20
    val upperBlue = 140
    val lowerRed = 100
    val lowerGreen = 10
    val lowerBlue = 200
    val halfHeight = (inputHeight - 1) / 2

    fun fix(x: Int) : Int {
        if (x < 0)
            return 0
        if (x > 255)
            return 255
        return x
    }

    fun rand(x: Int) : Int {
        return fix(x - x / 20 + rng.nextInt(x / 10 + 1))
    }

    for (distX in 0 .. halfHeight.toInt()){
        val upperFactor = halfHeight.toInt() - distX
        val lowerFactor = distX
        val divider = halfHeight.toInt()
        val red = (upperFactor * upperRed + lowerFactor * lowerRed) / divider
        val green = (upperFactor * upperGreen + lowerFactor * lowerGreen) / divider
        val blue = (upperFactor * upperBlue + lowerFactor * lowerBlue) / divider
        val smallY = halfHeight - distX
        val bigY = halfHeight + distX
        for (patternX in 0 until inputWidth){

            val currentRed = rand(red)
            val currentGreen = rand(green)
            val currentBlue = rand(blue)
            val rgba = inputTexture.rgbaFor(currentRed, currentGreen, currentBlue)
            inputTexture.setPixel(patternX, smallY, rgba)
            inputTexture.setPixel(patternX, bigY, rgba)
        }
    }

    val bottomRed = 50
    val bottomGreen = 150
    val bottomBlue = 200

    for (patternX in 0 until inputWidth){
        inputTexture.setPixel(patternX, inputTexture.height - 1L, rand(bottomRed), rand(bottomGreen), rand(bottomBlue))
    }

    return SnakeTextureProperties(inputTexture, inputTexture.rgbaFor(90, 0, 130))
}

fun snakePatternToTailTexture(stack: VirtualStack, props: SnakeTextureProperties) : TextureBuilder {
    val textureWidth = props.getFullWidth().toLong()
    val textureHeight = props.getFullHeight().toLong()
    val textureAddress = stack.push(4L * textureWidth * textureHeight)
    val texture = TextureBuilder(textureAddress, textureWidth, textureHeight)
    return snakePatternToTailTexture(texture, props)
}

fun snakePatternToTailTexture(texture: TextureBuilder, props: SnakeTextureProperties) : TextureBuilder {
    texture.clearColor(props.skinColor)
    for (patternX in 0 until props.colorPattern.width){

        val textureX = patternX * props.scaleOffsetLength

        // The last row of the colorPattern is reserved for the bottom of the snake
        for (patternY in 0 until props.colorPattern.height - 1){
            val textureY = patternY * props.scaleOffsetWidth

            for (relativeX in 0 until props.scaleLength){
                val relevantX: Int;
                when(relativeX < props.scaleLength / 2){
                    true -> relevantX = relativeX * 2
                    false -> relevantX = (props.scaleLength - relativeX) * 2
                }
                val relevantY = relevantX * props.scaleWidth / (props.scaleLength * 2)
                val minY = props.scaleWidth / 2 - relevantY
                val boundY = ceilDiv(props.scaleWidth, 2) + relevantY

                texture.fillVerticalLine(props.colorPattern.getPixel(patternX.toLong(), patternY.toLong()),
                    textureX + relativeX, textureY + minY, boundY.toLong() - minY)
            }
        }

        texture.fillRect(props.colorPattern.getPixel(patternX.toLong(), props.colorPattern.height - 1L),
            textureX.toLong(), texture.height / 2L, props.scaleLength.toLong(), texture.height / 2L)
    }
    return texture
}