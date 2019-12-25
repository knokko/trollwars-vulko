package vulko.models.building

import vulko.memory.util.*
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

    fun add(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, textureID: Int, u: Int, v: Int, matrix: Float){
        putFloat(currentAddress, x)
        putFloat(currentAddress + 4, y)
        putFloat(currentAddress + 8, z)

        putFloat(currentAddress + 12, nx)
        putFloat(currentAddress + 16, ny)
        putFloat(currentAddress + 20, nz)

        putInt(currentAddress + 24, textureID)
        putInt(currentAddress + 28, u)
        putInt(currentAddress + 32, v)

        putFloat(currentAddress + 36, matrix)

        currentAddress += STEP_SIZE
    }

    fun getVertexCountSoFar() : Int {
        return (currentAddress - startAddress).toInt() / STEP_SIZE
    }

    /**
     * After a call to this method, this BasicVertexBuilder mustn't be used anymore!
     */
    fun build(textureList: List<TextureEntry>, textureWidth: Long, textureHeight: Long) : BasicVertexBuffer {
        checkBounds()

        val floatWidth = textureWidth.toFloat()
        val floatHeight = textureHeight.toFloat()
        var writeAddress = startAddress + OFFSET_TEX_COORDS
        var readAddress = writeAddress

        val vertexCount = getVertexCountSoFar()

        run {
            val textureID = getInt(readAddress)
            val textureEntry = textureList[textureID]
            val absoluteU = getInt(readAddress + 4) + textureEntry.offsetX!!
            val absoluteV = getInt(readAddress + 8) + textureEntry.offsetY!!
            readAddress += 12

            putFloat(writeAddress, absoluteU.toFloat() / floatWidth)
            putFloat(writeAddress + 4, absoluteV.toFloat() / floatHeight)
            writeAddress += 8
        }

        val copySize = 4L * (VALUES_MATRIX + VALUES_POSITION + VALUES_NORMALS)

        for (currentVertex in 1 until vertexCount){
            copy(readAddress, writeAddress, copySize)
            readAddress += copySize
            writeAddress += copySize

            val textureID = getInt(readAddress)
            val textureEntry = textureList[textureID]
            val absoluteU = getInt(readAddress + 4) + textureEntry.offsetX!!
            val absoluteV = getInt(readAddress + 8) + textureEntry.offsetY!!
            readAddress += 12

            putFloat(writeAddress, absoluteU.toFloat() / floatWidth)
            putFloat(writeAddress + 4, absoluteV.toFloat() / floatHeight)
            writeAddress += 8
        }

        // The final matrix index
        putFloat(writeAddress, getFloat(readAddress))

        // Prevent any further add calls by making sure they will cause JVM crash
        currentAddress = 0

        return BasicVertexBuffer(startAddress, vertexCount)
    }
}

fun createBasicVertexBuffer(vertexCount: Int) : BasicVertexBuilder {
    val byteSize = (vertexCount * STEP_SIZE).toLong()
    val address = malloc(byteSize)
    val bound = address + byteSize
    return BasicVertexBuilder(address, bound)
}

fun createBasicVertexBufferAt(startAddress: Long, boundAddress: Long) : BasicVertexBuilder {
    return BasicVertexBuilder(startAddress, boundAddress)
}