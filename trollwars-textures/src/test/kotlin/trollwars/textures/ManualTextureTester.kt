package trollwars.textures

import trollwars.textures.creatures.createSnake1Properties
import trollwars.textures.terrain.drawGrass
import trollwars.textures.creatures.snakePatternToTailTexture
import vulko.memory.MemoryManager
import vulko.textures.TextureBuilder
import java.util.*

const val factor = 3

const val smallWidth = 1024L
const val smallHeight = 1024L

const val width = smallWidth * factor
const val height = smallHeight * factor

fun mainNew(){
    MemoryManager(100_000_000).use{ manager ->
        manager.claimStack(manager.capacity, "texture stack").use { stack ->
            val outputTexture = snakePatternToTailTexture(
                stack, createSnake1Properties(stack)
            )
            outputTexture.saveAsImage("output")
        }
    }
}

fun main(){
    MemoryManager(6 * (width * height + smallWidth * smallHeight)).use{ manager ->
        manager.claimStack(6 * (width * height + smallWidth * smallHeight), "small texture test").use { stack ->
            val textureAddress = stack.push(4 * width * height)
            val tb = TextureBuilder(textureAddress, width, height)

            val startTime = System.currentTimeMillis()
            drawGrass(
                stack, tb, tb.rgbaFor(0, 10, 40), tb.rgbaFor(200, 255, 150),
                tb.rgbaFor(30, 13, 0), scale = factor, rand = Random(10)
            )

            val smallAddress = stack.push(4 * smallWidth * smallHeight)
            val smallTexture = TextureBuilder(smallAddress, smallWidth, smallHeight)


            val endTime = System.currentTimeMillis()
            println("Took ${endTime - startTime} ms")
            tb.compress(factor, smallTexture)

            smallTexture.saveAsImage("grass")
        }
    }
}

fun checkColor(t: TextureBuilder, red: Int, green: Int, blue: Int, alpha: Int){
    val rgba = t.rgbaFor(red, green, blue, alpha)
    if (t.getRed(rgba) != red) throw Error("Red is ${t.getRed(rgba)}")
    if (t.getGreen(rgba) != green) throw Error("Green is ${t.getGreen(rgba)}")
    if (t.getBlue(rgba) != blue) throw Error("Blue is ${t.getBlue(rgba)}")
    if (t.getAlpha(rgba) != alpha) throw Error("Alpha is ${t.getAlpha(rgba)}")
}