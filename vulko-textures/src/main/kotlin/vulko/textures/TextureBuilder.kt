package vulko.textures

import vulko.memory.util.UNSAFE
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.lang.Integer.min
import javax.imageio.ImageIO

class TextureBuilder(val address: Long, val width: Int, val height: Int) {

    val boundAddress = address + 4 * width.toLong() * height.toLong()

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
        if (width <= 0 || height <= 0) {
            throw IllegalArgumentException("Width and height must be positive, but are ($width, $height)")
        }
        if (minX + width < minX) {
            throw IllegalArgumentException("Adding width ($width) to minX ($minX) would cause overflow")
        }
        if (minY + height < minY) {
            throw IllegalArgumentException("Adding height ($height) to minY ($minY) would cause overflow")
        }
        if (minX < 0 || minY < 0 || minX + width > this.width || minY + height > this.height){
            throw IllegalArgumentException("Own size is (${this.width},${this.height}) and params are ($minX,$minY,$width,$height)")
        }
        val boundY = minY + height
        val startAddress = addressFor(minX, minY)

        // Fill the first row
        var fillAddress = startAddress
        for (counterX in 0 until width){
            UNSAFE.putInt(fillAddress, rgba)
            fillAddress += 4
        }

        // Copy the first row to the remaining rows
        val rowLength = 4L * this.width
        val fillLength = 4L * width
        fillAddress = startAddress
        for (currentY in minY + 1 until boundY) {
            fillAddress += rowLength
            UNSAFE.copyMemory(startAddress, fillAddress, fillLength)
        }
    }

    fun fillRect(red: Int, green: Int, blue: Int, alpha: Int = 255, minX: Long, minY: Long, width: Long, height: Long){
        fillRect(rgbaFor(red, green, blue, alpha), minX, minY, width, height)
    }

    fun fillHorizontalLine(rgba: Int, minX: Int, y: Int, length: Int){
        if (y < 0 || y >= height || minX < 0 || minX + length > width){
            throw IllegalArgumentException("Own size is ($width,$height) and params are ($minX,$y,$length)")
        }
        var address = addressFor(minX.toLong(), y.toLong())
        for (counter in 0 until length){
            UNSAFE.putInt(address, rgba)
            address += 4
        }
    }

    fun fillVerticalLine(rgba: Int, x: Int, minY: Int, length: Int){
        if (x < 0 || x >= width || minY < 0 || minY + length > height){
            throw IllegalArgumentException("Own size is ($width,$height) and params are ($x,$minY,$length)")
        }
        var address = addressFor(x.toLong(), minY.toLong())
        for (counter in 0 until length){
            UNSAFE.putInt(address, rgba)
            address += 4 * width
        }
    }

    fun clearColor(rgba: Int){
        for (currentAddress in (address until boundAddress).step(4)){
            UNSAFE.putInt(currentAddress, rgba)
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
        UNSAFE.putInt(addressFor(x, y), rgba)
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
        return UNSAFE.getInt(addressFor(x, y))
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
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        var currentAddress = address
        for (y in 0 until height){
            for (x in 0 until width){
                val storedRGBA = UNSAFE.getInt(currentAddress)
                currentAddress += 4

                // Do an explicit conversion to avoid any possible endian trouble
                image.setRGB(x, y, Color(getRed(storedRGBA), getGreen(storedRGBA), getBlue(storedRGBA), getAlpha(storedRGBA)).rgb)
            }
        }

        ImageIO.write(image, "PNG", File("$name.png"))
    }

    fun putBufferedImage(image: BufferedImage, minX: Int, minY: Int){
        if (minX < 0 || minY < 0){
            throw IllegalArgumentException("minX is $minX and minY is $minY")
        }
        val copyWidth = min(image.width, width - minX)
        val copyHeight = min(image.height, height - minY)
        for (imageY in 0 until copyHeight){
            var address = addressFor(minX.toLong(), (minY + imageY).toLong())
            for (imageX in 0 until copyWidth){
                val color = Color(image.getRGB(imageX, imageY))
                UNSAFE.putInt(address, rgbaFor(color.red, color.green, color.blue, color.alpha))
                address += 4
            }
        }
    }

    fun copyTo(destAddress: Long) : TextureBuilder {
        UNSAFE.copyMemory(address, destAddress, 4L * width.toLong() * height.toLong())
        return TextureBuilder(destAddress, width, height)
    }

    fun copyTo(dest: TextureBuilder, destX: Int, destY: Int){
        if (destX < 0 || destY < 0){
            throw IllegalArgumentException("destX is $destX and destY is $destY")
        }
        val copyWidth = min(dest.width - destX, width)
        val copyHeight = min(dest.height - destY, height)
        val byteCopyWidth = copyWidth.toLong() * 4L
        for (ownY in 0 until copyHeight){
            val srcAddress = addressFor(0L, ownY.toLong())
            val destAddress = dest.addressFor(destX.toLong(), (ownY + destY).toLong())
            UNSAFE.copyMemory(srcAddress, destAddress, byteCopyWidth)
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
                val storedRGBA = UNSAFE.getInt(currentAddress)

                UNSAFE.putByte(currentAddress++, getRed(storedRGBA).toByte())
                UNSAFE.putByte(currentAddress++, getGreen(storedRGBA).toByte())
                UNSAFE.putByte(currentAddress++, getBlue(storedRGBA).toByte())
                UNSAFE.putByte(currentAddress++, getAlpha(storedRGBA).toByte())
            }
        }
    }
}