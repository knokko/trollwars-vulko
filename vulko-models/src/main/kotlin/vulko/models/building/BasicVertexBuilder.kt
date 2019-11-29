package vulko.models.building

import vulko.memory.util.UNSAFE
import vulko.models.buffer.BasicVertexBuffer

private const val VALUES_POSITION = 3
private const val VALUES_NORMALS = 3
private const val VALUES_TEX_COORDS = 3
private const val VALUES_MATRIX = 1

const val STEP_SIZE = 4 * (VALUES_POSITION + VALUES_NORMALS + VALUES_TEX_COORDS + VALUES_MATRIX)

private const val OFFSET_POSITION = 0L
private const val OFFSET_NORMALS = OFFSET_POSITION + 4 * VALUES_POSITION
private const val OFFSET_TEX_COORDS = OFFSET_NORMALS + 4 * VALUES_NORMALS
private const val OFFSET_MATRIX = OFFSET_TEX_COORDS + 4 * VALUES_TEX_COORDS

class BasicVertexBuilder internal constructor(startAddress: Long, boundAddress: Long) : AbstractBuilder(startAddress, boundAddress){

    fun add(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, textureID: Int, u: Int, v: Int, matrix: Int){
        UNSAFE.putFloat(currentAddress, x)
        UNSAFE.putFloat(currentAddress + 4, y)
        UNSAFE.putFloat(currentAddress + 8, z)

        UNSAFE.putFloat(currentAddress + 12, nx)
        UNSAFE.putFloat(currentAddress + 16, ny)
        UNSAFE.putFloat(currentAddress + 20, nz)

        UNSAFE.putInt(currentAddress + 24, textureID)
        UNSAFE.putInt(currentAddress + 28, u)
        UNSAFE.putInt(currentAddress + 32, v)

        UNSAFE.putInt(currentAddress + 36, matrix)

        currentAddress += STEP_SIZE
    }

    fun getVertexCountSoFar() : Int {
        return (currentAddress - startAddress).toInt() / STEP_SIZE
    }

    /**
     * After a call to this method, this BasicVertexBuilder mustn't be used anymore!
     */
    fun build(textureList: List<TextureEntry>, textureWidth: Int, textureHeight: Int) : BasicVertexBuffer {
        checkBounds()

        val floatWidth = textureWidth.toFloat()
        val floatHeight = textureHeight.toFloat()
        var writeAddress = startAddress + OFFSET_TEX_COORDS
        var readAddress = writeAddress

        val vertexCount = getVertexCountSoFar()

        run {
            val textureID = UNSAFE.getInt(readAddress)
            val textureEntry = textureList[textureID]
            val absoluteU = UNSAFE.getInt(readAddress + 4) + textureEntry.offsetX!!
            val absoluteV = UNSAFE.getInt(readAddress + 8) + textureEntry.offsetY!!
            readAddress += 12

            UNSAFE.putFloat(writeAddress, absoluteU.toFloat() / floatWidth)
            UNSAFE.putFloat(writeAddress + 4, absoluteV.toFloat() / floatHeight)
            writeAddress += 8
        }

        val copySize = 4L * (VALUES_MATRIX + VALUES_POSITION + VALUES_NORMALS)

        for (currentVertex in 1 until vertexCount){
            UNSAFE.copyMemory(readAddress, writeAddress, copySize)
            readAddress += copySize
            writeAddress += copySize

            val textureID = UNSAFE.getInt(readAddress)
            val textureEntry = textureList[textureID]
            val absoluteU = UNSAFE.getInt(readAddress + 4) + textureEntry.offsetX!!
            val absoluteV = UNSAFE.getInt(readAddress + 8) + textureEntry.offsetY!!
            readAddress += 12

            UNSAFE.putFloat(writeAddress, absoluteU.toFloat() / floatWidth)
            UNSAFE.putFloat(writeAddress + 4, absoluteV.toFloat() / floatHeight)
            writeAddress += 8
        }

        // The final matrix index
        UNSAFE.putInt(writeAddress, UNSAFE.getInt(readAddress))

        // Prevent any further add calls by making sure they will cause JVM crash
        currentAddress = 0

        return BasicVertexBuffer(startAddress, vertexCount)
    }
}

fun createBasicVertexBuffer(vertexCount: Int) : BasicVertexBuilder {
    val byteSize = (vertexCount * STEP_SIZE).toLong()
    val address = UNSAFE.allocateMemory(byteSize)
    val bound = address + byteSize
    return BasicVertexBuilder(address, bound)
}

fun createBasicVertexBufferAt(startAddress: Long, boundAddress: Long) : BasicVertexBuilder {
    return BasicVertexBuilder(startAddress, boundAddress)
}