package vulko.models.building

import vulko.memory.util.*

const val TRIANGLE_BYTES = 3 * 4

class BasicIndexBuffer internal constructor(startAddress: Long, boundAddress: Long) : AbstractBuilder(startAddress, boundAddress) {

    fun bindTriangle(vertexIndex1: Int, vertexIndex2: Int, vertexIndex3: Int){
        putInt(currentAddress, vertexIndex1)
        putInt(currentAddress + 4, vertexIndex2)
        putInt(currentAddress + 8, vertexIndex3)

        currentAddress += TRIANGLE_BYTES
    }

    fun getTrianglesSoFar() : Int {
        return (currentAddress - startAddress).toInt() / TRIANGLE_BYTES
    }
}

fun createBasicIndexBuffer(triangleCount: Int) : BasicIndexBuffer {

    val numBytes = (TRIANGLE_BYTES * triangleCount).toLong()
    val startAddress = malloc(numBytes)
    val boundAddress = startAddress + numBytes
    return BasicIndexBuffer(startAddress, boundAddress)
}

fun createBasicIndexBufferAt(startAddress: Long, boundAddress: Long) : BasicIndexBuffer {
    return BasicIndexBuffer(startAddress, boundAddress)
}