package vulko.textures

import vulko.memory.util.*
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.min

class TextureBuilder(val address: Long, val width: Long, val height: Long) {

    val boundAddress = address + 4 * width * height

    @Throws(IllegalArgumentException::class)
    private fun checkMinLengthBoundsX(min: Long, length: Long, minName: String = "minX", lengthName: String = "length",
                                      widthName: String = "this.width") {
        if (length <= 0){
            throw IllegalArgumentException("$lengthName must be positive, but is $length")
        }
        if (min + length > width){
            throw IllegalArgumentException("$minName and $lengthName are $min and $length, but $widthName is $width")
        }
        if (min + length < min){
            throw IllegalArgumentException("Adding $lengthName to $minName (Adding $length to $min) would cause overflow")
        }
    }

    @Throws(IllegalArgumentException::class)
    private fun checkMinLengthBoundsY(min: Long, length: Long, minName: String = "minY", lengthName: String = "length",
                                      heightName: String = "this.height") {
        if (length <= 0){
            throw IllegalArgumentException("$lengthName must be positive, but is $length")
        }
        if (min + length > height){
            throw IllegalArgumentException("$minName and $lengthName are $min and $length, but $heightName is $height")
        }
        if (min + length < min){
            throw IllegalArgumentException("Adding $lengthName to $minName (Adding $length to $min) would cause overflow")
        }
    }

    fun rgbaFor(red: Int, green: Int, blue: Int, alpha: Int = 255) : Int {
        return red + (green shl 8) + (blue shl 16) + (alpha shl 24)
    }

    @Throws(IllegalArgumentException::class)
    fun addressFor(x: Long, y: Long) : Long {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            throw IllegalArgumentException("The given coordinates are ($x, $y), but the size of this texture is ($width, $height)")
        }
        return address + 4 * (x + y * width)
    }

    @Throws(IllegalArgumentException::class)
    fun fillRect(rgba: Int, minX: Long, minY: Long, width: Long, height: Long){
        checkMinLengthBoundsX(minX, width, lengthName="width")
        checkMinLengthBoundsY(minY, height, lengthName="height")
        val boundY = minY + height
        val startAddress = addressFor(minX, minY)

        // Fill the first row
        var fillAddress = startAddress
        for (counterX in 0 until width){
            putInt(fillAddress, rgba)
            fillAddress += 4
        }

        // Copy the first row to the remaining rows
        val rowLength = 4L * this.width
        val fillLength = 4L * width
        fillAddress = startAddress
        for (currentY in minY + 1 until boundY) {
            fillAddress += rowLength
            copy(startAddress, fillAddress, fillLength)
        }
    }

    @Throws(IllegalArgumentException::class)
    fun fillRect(red: Int, green: Int, blue: Int, alpha: Int = 255, minX: Long, minY: Long, width: Long, height: Long){
        fillRect(rgbaFor(red, green, blue, alpha), minX, minY, width, height)
    }

    @Throws(IllegalArgumentException::class)
    fun fillHorizontalLine(rgba: Int, minX: Long, y: Long, length: Long){
        checkMinLengthBoundsX(minX, length)

        // Let addressFor do the rest of the bounds checking
        var address = addressFor(minX, y)
        for (counter in 0 until length){
            putInt(address, rgba)
            address += 4
        }
    }

    @Throws(IllegalArgumentException::class)
    fun fillVerticalLine(rgba: Int, x: Long, minY: Long, length: Long){
        checkMinLengthBoundsY(minY, length)

        // The addressFor method will do the other bounds checks
        var address = addressFor(x, minY)
        for (counter in 0 until length){
            putInt(address, rgba)
            address += 4 * width
        }
    }

    fun clearColor(rgba: Int){
        for (currentAddress in (address until boundAddress).step(4)){
            putInt(currentAddress, rgba)
        }
    }

    fun clearColor(red: Int, green: Int, blue: Int, alpha: Int = 255){
        clearColor(rgbaFor(red, green, blue, alpha))
    }

    /**
     * Assigns the given rgba value to the pixel at the given coordinates. This method is meant to be very fast and it
     * will NOT do any blending if the previous color happened to be partially transparent.
     */
    @Throws(IllegalArgumentException::class)
    fun setPixel(x: Long, y: Long, rgba: Int){
        putInt(addressFor(x, y), rgba)
    }

    @Throws(IllegalArgumentException::class)
    fun setPixel(x: Long, y: Long, red: Int, green: Int, blue: Int, alpha: Int = 255){
        setPixel(x, y, rgbaFor(red, green, blue, alpha))
    }

    // TODO Add the blendPixel method to properly mix alpha values if necessary

    /**
     * Gets the current rgba value of the pixel at the given coordinates. Use the getRed, getGreen, getBlue or getAlpha
     * method to extract the components from the value.
     */
    @Throws(IllegalArgumentException::class)
    fun getPixel(x: Long, y: Long) : Int {
        return getInt(addressFor(x, y))
    }

    fun getRed(rgba: Int) : Int {
        return rgba and 255
    }

    fun getGreen(rgba: Int) : Int {
        return (rgba shr 8) and 255
    }

    fun getBlue(rgba: Int) : Int {
        return (rgba shr 16) and 255
    }

    fun getAlpha(rgba: Int) : Int {
        return rgba ushr 24
    }

    // TODO Rename to downscale and create upscale method
    fun compress(factor: Int, dest: TextureBuilder){
        val fs = factor * factor
        if (dest.width * factor != width){
            throw IllegalArgumentException("dest.width (${dest.width}) * factor ($factor) is not equal to width ($width)")
        }
        if (dest.height * factor != height){
            throw IllegalArgumentException("dest.height (${dest.height}) * factor ($factor) is not equal to height ($height)")
        }
        for (destX in 0L until dest.width){
            for (destY in 0L until dest.height){
                var totalRed = 0
                var totalGreen = 0
                var totalBlue = 0
                var totalAlpha = 0
                for (ox in 0 until factor){
                    for (oy in 0 until factor){
                        val rgba = getPixel(destX * factor + ox, destY * factor + oy)
                        totalRed += getRed(rgba)
                        totalGreen += getGreen(rgba)
                        totalBlue += getBlue(rgba)
                        totalAlpha += getAlpha(rgba)
                    }
                }

                val destRed = totalRed / fs
                val destGreen = totalGreen / fs
                val destBlue = totalBlue / fs
                val destAlpha = totalAlpha / fs
                dest.setPixel(destX, destY, destRed, destGreen, destBlue, destAlpha)
            }
        }
    }

    fun saveAsImage(name: String){
        val image = BufferedImage(width.toInt(), height.toInt(), BufferedImage.TYPE_INT_ARGB)
        var currentAddress = address
        for (y in 0 until height.toInt()){
            for (x in 0 until width.toInt()){
                val storedRGBA = getInt(currentAddress)
                currentAddress += 4

                // Do an explicit conversion to avoid any possible endian trouble
                image.setRGB(x, y, Color(getRed(storedRGBA), getGreen(storedRGBA), getBlue(storedRGBA), getAlpha(storedRGBA)).rgb)
            }
        }

        ImageIO.write(image, "PNG", File("$name.png"))
    }

    fun putBufferedImage(image: BufferedImage, minX: Long, minY: Long){
        if (minX < 0 || minY < 0){
            throw IllegalArgumentException("minX is $minX and minY is $minY")
        }
        val copyWidth = min(image.width, (width - minX).toInt())
        val copyHeight = min(image.height, (height - minY).toInt())
        for (imageY in 0 until copyHeight){
            var address = addressFor(minX, (minY + imageY))
            for (imageX in 0 until copyWidth){
                val color = Color(image.getRGB(imageX, imageY))
                putInt(address, rgbaFor(color.red, color.green, color.blue, color.alpha))
                address += 4
            }
        }
    }

    fun copy(destAddress: Long) : TextureBuilder {
        copy(address, destAddress, 4L * width.toLong() * height.toLong())
        return TextureBuilder(destAddress, width, height)
    }

    fun copy(dest: TextureBuilder, sourceX: Long = 0, sourceY: Long = 0, destX: Long = 0, destY: Long = 0,
             copyWidth: Long = min(dest.width - destX, width - sourceX),
             copyHeight: Long = min(dest.height - destY, height - sourceY)){

        checkMinLengthBoundsX(sourceX, copyWidth, "sourceX", "copyWidth")
        checkMinLengthBoundsY(sourceY, copyHeight, "sourceX", "copyHeight")
        dest.checkMinLengthBoundsX(destX, copyWidth, "destX", "copyWidth", "dest.width")
        dest.checkMinLengthBoundsY(destY, copyHeight, "destY", "copyHeight", "dest.height")

        val byteCopyWidth = copyWidth * 4L
        for (offsetY in 0 until copyHeight){
            val srcAddress = addressFor(sourceX, sourceY + offsetY)
            val destAddress = dest.addressFor(destX, destY + offsetY)
            copy(srcAddress, destAddress, byteCopyWidth)
        }
    }

    /**
     * Prepares this texture to be used as buffer for OpenGL or Vulkan. This method will store the pixels of
     * this texture builder in RGBA format at its current address.
     * Note that the internal texture format might also be RGBA in the right endianness, but do not rely on this.
     * After this call, the methods of this texture builder must no longer be used.
     */
    fun build(){
        var currentAddress = address
        for (y in 0 until height){
            for (x in 0 until width){
                val storedRGBA = getInt(currentAddress)

                putByte(currentAddress++, getRed(storedRGBA).toByte())
                putByte(currentAddress++, getGreen(storedRGBA).toByte())
                putByte(currentAddress++, getBlue(storedRGBA).toByte())
                putByte(currentAddress++, getAlpha(storedRGBA).toByte())
            }
        }
    }
}