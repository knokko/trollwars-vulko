package trollwars.textures.terrain

import vulko.memory.VirtualStack
import vulko.memory.util.UNSAFE
import vulko.textures.building.TextureBuilder
import java.lang.StrictMath.*
import java.util.Random

const val TWO_PI = 2.0 * PI
const val MAX_VERT_ANGLE = 70.0 * PI / 180.0

fun drawGrass(stack: VirtualStack, texture: TextureBuilder, baseColor: Int, brightColor: Int, backgroundColor: Int,
              minX: Int = 0, minY: Int = 0, drawWidth: Int = texture.width, drawHeight: Int = texture.height,
              scale: Int = 1, rand: Random = Random()){

    val startTime = System.currentTimeMillis()
    texture.fillRect(backgroundColor, minX.toLong(), minY.toLong(), drawWidth.toLong(), drawHeight.toLong())

    val maxX = minX + drawWidth - 1
    val maxY = minY + drawHeight - 1

    val redBase = texture.getRed(baseColor)
    val greenBase = texture.getGreen(baseColor)
    val blueBase = texture.getBlue(baseColor)

    val redLeft = texture.getRed(brightColor) - redBase
    val greenLeft = texture.getGreen(brightColor) - greenBase
    val blueLeft = texture.getBlue(brightColor) - blueBase

    val area = drawWidth.toLong() * drawHeight.toLong()
    val heightTableAddress = stack.push(area)
    UNSAFE.setMemory(heightTableAddress, area * 2, 0)

    val numGrassLines = (area / 70 / (scale * scale)).toInt()
    for (counter in 0 until numGrassLines){
        val startX = minX + rand.nextInt(drawWidth)
        val startY = minY + rand.nextInt(drawHeight)
        val angle = rand.nextDouble() * TWO_PI

        val vertAngle = rand.nextDouble() * MAX_VERT_ANGLE
        val sinVertAngle = sin(vertAngle)

        val length = (50.0 + rand.nextDouble() * 30.0) * sinVertAngle * scale
        val invLength = 1.0 / length

        val cosVertAngleTimesLength = cos(vertAngle) * length
        val sinAngle = sin(angle)
        val cosAngle = cos(angle)

        val width = (4.0 + 3.0 * rand.nextDouble()) * scale

        val endX = startX + (cosAngle * length).toInt()
        val endY = startY + (sinAngle * length).toInt()

        val widthX = (sinAngle * width).toInt()
        val widthY = (cosAngle * width).toInt()

        // The line through these coordinates will be perpendicular to angle and go through (startX,startY)
        val startX1 = startX - widthX
        val startY1 = startY + widthY
        val startX2 = startX + widthX
        val startY2 = startY - widthY

        // The next variables will make it easier to loop
        val localMinX = min(min(startX1, startX2), endX)
        val localMinY = min(min(startY1, startY2), endY)
        val localMaxX = max(max(startX1, startX2), endX)
        val localMaxY = max(max(startY1, startY2), endY)
        val effectiveWidth = localMaxX - localMinX + 1
        val effectiveHeight = localMaxY - localMinY + 1
        val fictiveStartX = (startX - localMinX).toDouble()
        val fictiveStartY = (startY - localMinY).toDouble()

        // Loop over all relevant coordinates
        for (x in 0 until effectiveWidth){

            var realX = localMinX + x

            // If we get a little outside of the texture range, we continue on the other side
            if (realX > maxX){
                realX -= drawWidth
            }
            if (realX < minX){
                realX += drawWidth
            }

            val heightTableX = realX - minX

            val dx = x - fictiveStartX

            for (y in 0 until effectiveHeight){

                // Rotate (x - fictiveStartX, y - fictiveStartY)
                val dy = y - fictiveStartY
                val transformedX = sinAngle * dx - cosAngle * dy
                val transformedY = sinAngle * dy + cosAngle * dx

                // Check if the pixel at this location should be affected
                if (transformedX > -width && transformedX < width && transformedY >= 0 && transformedY < length){
                    val progress = transformedY * invLength
                    if (abs(transformedX) <= sqrt(1.0 - progress) * width){

                        // Let's now get the actual y-coordinate
                        var realY = localMinY + y

                        while (realY > maxY){
                            realY -= drawHeight
                        }
                        while (realY < minY){
                            realY += drawHeight
                        }

                        // Finally test if we are not 'below' some other grass 'line'
                        val realHeight = (cosVertAngleTimesLength * progress).toShort()
                        val currentHeightAddress = heightTableAddress + 2 * (heightTableX + drawWidth * (realY - minY))
                        if (realHeight >= UNSAFE.getShort(currentHeightAddress)){
                            UNSAFE.putShort(currentHeightAddress, realHeight)

                            val extraColor = sinVertAngle * progress * progress
                            val newRed = (redBase + extraColor * redLeft).toInt()
                            val newGreen = (greenBase + extraColor * greenLeft).toInt()
                            val newBlue = (blueBase + extraColor * blueLeft).toInt()
                            texture.setPixel(realX.toLong(), realY.toLong(), newRed, newGreen, newBlue)
                        }
                    }
                }
            }
        }
    }

    stack.pop(area)

    val endTime = System.currentTimeMillis()
    println("Drawing grass took ${endTime - startTime} ms")
}