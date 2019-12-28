package vulko.textures

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import vulko.memory.util.*
import vulko.textures.TextureBuilder
import kotlin.IllegalArgumentException

class TestTextureBuilder {

    private fun withTexture(width: Long, height: Long, test: (TextureBuilder) -> Unit) {
        val textureBytes = 4L * width * height
        val bufferAddress = malloc(5 * textureBytes)
        val textureAddress = bufferAddress + 2 * textureBytes

        // Fill the buffer with the test byte and check later if the buffer wasn't touched
        val testValue: Byte = 15
        fill(bufferAddress, textureAddress - bufferAddress, testValue)
        // intentionally don't touch the texture space itself
        fill(textureAddress + textureBytes, 2 * textureBytes, testValue)
        val texture = TextureBuilder(textureAddress, width, height)

        // Perform the actual tests
        test(texture)

        // Check that the memory outside the texture space wasn't touched, note that this is only a best attempt
        for (testAddress in bufferAddress until textureAddress) {
            assertEquals(testValue, getByte(testAddress))
        }
        for (testAddress in textureAddress + textureBytes until bufferAddress + 5 * textureBytes) {
            assertEquals(testValue, getByte(testAddress))
        }

        // Finally free the buffer memory
        free(bufferAddress)
    }

    private fun testColorConversion(tb: TextureBuilder, red: Int, green: Int, blue: Int, alpha: Int) {
        val rgba = tb.rgbaFor(red, green, blue, alpha)

        assertEquals(red, tb.getRed(rgba))
        assertEquals(green, tb.getGreen(rgba))
        assertEquals(blue, tb.getBlue(rgba))
        assertEquals(alpha, tb.getAlpha(rgba))
    }

    @Test
    fun testColorConversion(){
        withTexture(1, 1) {
            testColorConversion(it, 0, 0, 0, 0)
            testColorConversion(it, 255, 255, 255, 255)
            testColorConversion(it, 0, 127, 128, 255)
            testColorConversion(it, 140, 23, 97, 214)
        }
    }

    private fun testAddressFor(tb: TextureBuilder, rgba: Int, x: Long, y: Long) {
        putInt(tb.addressFor(x, y), rgba)
        assertEquals(rgba, tb.getPixel(x, y))
    }

    private fun testBadAddressFor(tb: TextureBuilder, x: Long, y: Long) {
        try {
            tb.addressFor(x, y)
            throw AssertionError("Should have thrown an IllegalArgumentException on input($x, $y)")
        } catch (ex: IllegalArgumentException){}
    }

    @Test
    fun testAddressFor(){
        withTexture(5, 9) {

            // Good input
            testAddressFor(it, -348, 0, 0)
            testAddressFor(it, 12345, 3, 2)
            testAddressFor(it, 3498, 2, 7)
            testAddressFor(it, -934823, 4, 4)
            testAddressFor(it, 34984323, 4, 2)
            testAddressFor(it, -2639, 4, 7)
            testAddressFor(it, -8, 4, 8)

            // Bad input
            testBadAddressFor(it, 5, 8)
            testBadAddressFor(it, 4, 9)
            testBadAddressFor(it, 5, 9)
            testBadAddressFor(it, -1, 0)
            testBadAddressFor(it, -1, 8)
            testBadAddressFor(it, -1, 9)
            testBadAddressFor(it, 0, -1)
            testBadAddressFor(it, 4, -1)
            testBadAddressFor(it, 5, -1)
            testBadAddressFor(it, 7, 14)
            testBadAddressFor(it, -5, -89)
            testBadAddressFor(it, 2, Long.MAX_VALUE)
            testBadAddressFor(it, Long.MIN_VALUE, 6)
        }
    }

    private fun testGetPixel(tb: TextureBuilder, x: Long, y: Long) {
        assertEquals(getInt(tb.addressFor(x, y)), tb.getPixel(x, y))
    }

    private fun testBadGetPixel(tb: TextureBuilder, x: Long, y: Long) {
        try {
            tb.getPixel(x, y)
            throw AssertionError("Should have thrown IllegalArgumentException on input($x, $y)")
        } catch (ex: IllegalArgumentException) {}
    }

    @Test
    fun testGetPixel(){
        withTexture(7, 3){

            // Good edge cases
            testGetPixel(it, 0, 0)
            testGetPixel(it, 6, 0)
            testGetPixel(it, 0, 2)
            testGetPixel(it, 6, 2)

            // Good normal cases
            testGetPixel(it, 2, 2)
            testGetPixel(it, 5, 1)

            // Bad edge cases
            testBadGetPixel(it, -1, 0)
            testBadGetPixel(it, 0, -1)
            testBadGetPixel(it, -1, -1)
            testBadGetPixel(it, -1, 2)
            testBadGetPixel(it, -1, 3)
            testBadGetPixel(it, 6, -1)
            testBadGetPixel(it, 7, -1)
            testBadGetPixel(it, 7, 0)
            testBadGetPixel(it, 7, 2)
            testBadGetPixel(it, 7, 3)
            testBadGetPixel(it, 2, Long.MAX_VALUE)
            testBadGetPixel(it, Long.MIN_VALUE, 2)

            // Bad normal cases
            testBadGetPixel(it, 10, 20)
            testBadGetPixel(it, -4, -8)
        }
    }
    
    private fun testSetPixel(tb: TextureBuilder, rgba: Int, x: Long, y: Long){
        tb.setPixel(x, y, rgba)
        assertEquals(getInt(tb.addressFor(x, y)), rgba)
    }
    
    private fun testBadSetPixel(tb: TextureBuilder, x: Long, y: Long){
        try {
            tb.setPixel(x, y, 90)
            throw AssertionError("setPixel($x, $y) should have thrown an IllegalArgumentException")
        } catch (ex: IllegalArgumentException) {}
    }

    @Test
    fun testSetPixel(){
        withTexture(7, 3){

            // Good edge cases
            testSetPixel(it, -34834, 0, 0)
            testSetPixel(it, 4238345, 6, 0)
            testSetPixel(it, -34, 0, 2)
            testSetPixel(it, 0, 6, 2)

            // Good normal cases
            testSetPixel(it, 134345, 2, 2)
            testSetPixel(it, -12, 5, 1)

            // Bad edge cases
            testBadSetPixel(it, -1, 0)
            testBadSetPixel(it, 0, -1)
            testBadSetPixel(it, -1, -1)
            testBadSetPixel(it, -1, 2)
            testBadSetPixel(it, -1, 3)
            testBadSetPixel(it, 6, -1)
            testBadSetPixel(it, 7, -1)
            testBadSetPixel(it, 7, 0)
            testBadSetPixel(it, 7, 2)
            testBadSetPixel(it, 7, 3)
            testBadSetPixel(it, 2, Long.MAX_VALUE)
            testBadSetPixel(it, Long.MIN_VALUE, 2)

            // Bad normal cases
            testBadSetPixel(it, 10, 20)
            testBadSetPixel(it, -4, -8)
        }
    }

    @Test
    fun testClear() {
        withTexture(8, 12) {
            it.clearColor(12345678)
            for (x in 0 until it.width) {
                for (y in 0 until it.height) {
                    assertEquals(12345678, it.getPixel(x.toLong(), y.toLong()))
                }
            }
        }
    }

    private fun testFillRect(tb: TextureBuilder, rgba: Int, minX: Long, minY: Long, maxX: Long, maxY: Long) {

        // Before filling the rect, every pixel must be different from the target rgba, to test things accurately
        for (x in 0 until tb.width.toLong()) {
            for (y in 0 until tb.height.toLong()) {
                assertNotEquals(rgba, tb.getPixel(x, y))
            }
        }

        tb.fillRect(rgba, minX, minY, maxX - minX + 1, maxY - minY + 1)

        // Afterwards, exactly the pixels in the target rectangle should have been set to rgba
        for (x in 0 until tb.width.toLong()) {
            for (y in 0 until tb.height.toLong()) {
                if (x in minX..maxX && y in minY..maxY){
                    assertEquals(rgba, tb.getPixel(x, y))
                } else {
                    assertNotEquals(rgba, tb.getPixel(x, y))
                }
            }
        }
    }

    private fun testBadFillRect(tb: TextureBuilder, minX: Long, minY: Long, maxX: Long, maxY: Long) {

        try {
            tb.fillRect(1, minX, minY, maxX - minX + 1, maxY - minY + 1)
            throw AssertionError("Filling rect from ($minX, $minY) to ($maxX, $maxY) " +
                    "should have thrown an IllegalArgumentException")
        } catch (ex: IllegalArgumentException) {}
    }

    @Test
    fun testFillRect(){
        withTexture(7, 15) {

            // Make sure every pixel is 0 initially (otherwise the initial pixel colors would be undefined)
            it.clearColor(0)

            // The correct edge cases
            testFillRect(it, 1, 0, 0, 6, 14)
            testFillRect(it, 2, 0, 0, 4, 6)
            testFillRect(it, 3, 4, 8, 6, 14)

            // Some correct cases
            testFillRect(it, 4, 2, 1, 5, 10)
            testFillRect(it, 5, 2, 5, 2, 8)
            testFillRect(it, 6, 4, 1, 6, 1)

            // The bad edge cases
            testBadFillRect(it, -1, 0, 4, 5)
            testBadFillRect(it, 0, -1, 6, 8)
            testBadFillRect(it, 3, 4, 7, 10)
            testBadFillRect(it, 4, 6, 5, 15)
            testBadFillRect(it, Long.MIN_VALUE, 2, 2, 5)
            testBadFillRect(it, 3, 1, 5, Long.MAX_VALUE)
        }
    }

    private fun testHorizontalLine(tb: TextureBuilder, rgba: Int, lineY: Long, minX: Long, maxX: Long){

        // First ensure that no single pixel already has the rgba for the line to draw
        for (x in 0 until tb.width.toLong()) {
            for (y in 0 until tb.height.toLong()) {
                assertNotEquals(rgba, tb.getPixel(x, y))
            }
        }

        tb.fillHorizontalLine(rgba, minX, lineY, maxX - minX + 1)

        // Ensure that exactly the pixels on the line changed to the rgba
        for (x in 0 until tb.width.toLong()) {
            for (y in 0 until tb.height.toLong()) {
                if (y == lineY && x in minX..maxX) {
                    assertEquals(rgba, tb.getPixel(x, y))
                } else {
                    assertNotEquals(rgba, tb.getPixel(x, y))
                }
            }
        }
    }

    private fun testBadHorizontalLine(tb: TextureBuilder, lineY: Long, minX: Long, maxX: Long){
        try {
            tb.fillHorizontalLine(0, minX, lineY, maxX - minX + 1)
            throw AssertionError("fillHorizontalLine($minX, $lineY, ${maxX - minX + 1}) should have thrown IllegalArgumentException")
        } catch (ex: IllegalArgumentException){}
    }

    @Test
    fun testHorizontalLine(){
        withTexture(16, 9){
            it.clearColor(0)

            // Good edge cases
            testHorizontalLine(it, 1, 0, 0, 15)
            testHorizontalLine(it, 2, 8, 0, 15)
            testHorizontalLine(it, 3, 0, 2, 6)
            testHorizontalLine(it, 4, 4, 0, 15)

            // Good normal cases
            testHorizontalLine(it, 5, 7, 2, 4)
            testHorizontalLine(it, 6, 2, 4, 10)

            // Bad edge cases
            testBadHorizontalLine(it, -1, 2, 5)
            testBadHorizontalLine(it, 9, 3, 10)
            testBadHorizontalLine(it, 2, -1, 8)
            testBadHorizontalLine(it, 3, 2, 16)
            testBadHorizontalLine(it, 5, 3, 2)
            testBadHorizontalLine(it, 7, 7, Long.MAX_VALUE)
        }
    }

    private fun testVerticalLine(tb: TextureBuilder, rgba: Int, lineX: Long, minY: Long, maxY: Long){

        // First ensure that no single pixel already has the rgba for the line to draw
        for (x in 0 until tb.width.toLong()) {
            for (y in 0 until tb.height.toLong()) {
                assertNotEquals(rgba, tb.getPixel(x, y))
            }
        }

        tb.fillVerticalLine(rgba, lineX, minY, maxY - minY + 1)

        // Ensure that exactly the pixels on the line changed to the rgba
        for (x in 0 until tb.width.toLong()) {
            for (y in 0 until tb.height.toLong()) {
                if (x == lineX && y in minY..maxY) {
                    assertEquals(rgba, tb.getPixel(x, y))
                } else {
                    assertNotEquals(rgba, tb.getPixel(x, y))
                }
            }
        }
    }

    private fun testBadVerticalLine(tb: TextureBuilder, lineX: Long, minY: Long, maxY: Long){
        try {
            tb.fillVerticalLine(0, lineX, minY, maxY - minY + 1)
            throw AssertionError("fillVerticalLine should have thrown IllegalArgumentException")
        } catch (ex: IllegalArgumentException) { }
    }

    @Test
    fun testVerticalLine() {
        withTexture(10, 20) {
            it.clearColor(0)

            // Edge cases
            testVerticalLine(it, 1, 0, 0, 19)
            testVerticalLine(it, 2, 9, 0, 19)

            // Normal cases
            testVerticalLine(it, 3, 4, 2, 7)
            testVerticalLine(it, 4, 8, 10, 19)

            // Bad edge cases
            testBadVerticalLine(it, -1, 4, 15)
            testBadVerticalLine(it, 20, 10, 19)
            testBadVerticalLine(it, 4, 5, 20)
            testBadVerticalLine(it, 9, -1, 17)
            testBadVerticalLine(it, Long.MAX_VALUE, 10, 13)
            testBadVerticalLine(it, 9, Long.MIN_VALUE, 10)
            testBadVerticalLine(it, 2, 3, Long.MAX_VALUE)
            testBadVerticalLine(it, 6, 10, 9)
        }
    }

    fun testCopy(source: TextureBuilder, dest: TextureBuilder, sourceX: Long, sourceY: Long, destX: Long, destY: Long,
                 copyWidth: Long, copyHeight: Long){

        // Do a manual copy to check if the result of the copy() method is equal to the manual result
        val testCopyAddress = malloc(4L * copyWidth * copyHeight)
        val testCopy = TextureBuilder(testCopyAddress, copyWidth, copyHeight)
        for (x in 0 until copyWidth){
            for (y in 0 until copyHeight){
                val rgba = source.getPixel(x + sourceX, y + sourceY)
                testCopy.setPixel(x, y, source.getRed(rgba), source.getGreen(rgba), source.getBlue(rgba), source.getAlpha(rgba))
            }
        }

        source.copy(dest, sourceX, sourceY, destX, destY, copyWidth, copyHeight)

        // Compare dest area with the manual copy
        for (x in 0 until copyWidth){
            for (y in 0 until copyHeight){
                val d = dest.getPixel(destX + x, destY + y)
                val t = testCopy.getPixel(x, y)
                assertEquals(testCopy.getRed(t), dest.getRed(d))
                assertEquals(testCopy.getGreen(t), dest.getGreen(d))
                assertEquals(testCopy.getBlue(t), dest.getBlue(d))
                assertEquals(testCopy.getAlpha(t), dest.getAlpha(d))
            }
        }

        // Free the memory of the test texture
        free(testCopyAddress)
    }

    fun testBadCopy(source: TextureBuilder, dest: TextureBuilder, sourceX: Long, sourceY: Long,
                    destX: Long, destY: Long, copyWidth: Long, copyHeight: Long){
        try {
            source.copy(dest, sourceX, sourceY, destX, destY, copyWidth, copyHeight)
            throw AssertionError("copy should have thrown an IllegalArgumentException")
        } catch (ex: IllegalArgumentException) {}
    }

    @Test
    fun testCopy(){
        withTexture(7, 11){small ->
            withTexture(8, 14){large ->

                // Edge cases
                testCopy(small, large, 0, 0, 0, 0, 7, 11)
                testCopy(small, large, 0, 0, 1, 3, 7, 11)
                testCopy(small, large, 1, 1, 0, 0, 5, 6)
                testCopy(large, small, 0, 0, 0, 0, 7, 11)
                testCopy(large, small, 0, 0, 0, 0, 1, 11)
                testCopy(large, small, 0, 10, 1, 0, 6, 1)

                // TODO Copy to self cases

                // Normal cases
                testCopy(small, large, 2, 1, 4, 5, 4, 8)
                testCopy(large, small, 1, 3, 5, 4, 2, 3)

                // Bad edge cases
            }
        }
    }

    // TODO Test compress, build and BufferedImage operations
}