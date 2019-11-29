package test.textures

import trollwars.textures.creatures.createSnake1Properties
import trollwars.textures.terrain.drawGrass
import trollwars.textures.creatures.snakePatternToTailTexture
import vulko.memory.MemoryManager
import vulko.textures.building.TextureBuilder
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
    MemoryManager(6 * (width * height + smallWidth * smallHeight)).use{manager ->
        manager.claimStack(6 * (width * height + smallWidth * smallHeight), "small texture test").use {stack ->
            val textureAddress = stack.push(4 * width * height)
            val tb = TextureBuilder(textureAddress, width.toInt(), height.toInt())

            checkColor(tb, 10, 20, 30, 40)
            checkColor(tb, 210, 220, 230, 240)

            tb.setPixel(11, 11, tb.rgbaFor(100, 10, 20, 255))
            if (tb.getRed(tb.getPixel(11, 11)) != 100) throw Error("Red at (11,11) is ${tb.getRed(tb.getPixel(11, 11))}")
            tb.saveAsImage("test texture")

            drawGrass(
                stack, tb, tb.rgbaFor(0, 10, 40), tb.rgbaFor(200, 255, 150),
                tb.rgbaFor(30, 13, 0), scale = factor, rand = Random(10)
            )

            val smallAddress = stack.push(4 * smallWidth * smallHeight)
            val smallTexture = TextureBuilder(smallAddress, smallWidth.toInt(), smallHeight.toInt())

            val startTime = System.currentTimeMillis()
            tb.compress(factor, smallTexture)
            val endTime = System.currentTimeMillis()
            println("Compressing took ${endTime - startTime} ms")

            smallTexture.saveAsImage("grass")

            /*
            val smallAddress = stack.push(4 * smallWidth * smallHeight)
            val small = TextureBuilder(smallAddress, smallWidth.toInt(), smallHeight.toInt())
            drawGrass(
                stack, small, small.rgbaFor(40, 10, 0), small.rgbaFor(255, 150, 150),
                small.rgbaFor(30, 13, 0)
            )
            small.copyTo(tb, 40, 50)
            tb.saveAsImage("mixed grass")

            small.saveAsImage("small grass")*/
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