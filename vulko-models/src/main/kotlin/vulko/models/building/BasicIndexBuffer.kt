package vulko.models.building

import vulko.memory.util.UNSAFE

const val TRIANGLE_BYTES = 3 * 4

class BasicIndexBuffer internal constructor(startAddress: Long, boundAddress: Long) : AbstractBuilder(startAddress, boundAddress) {

    fun bindTriangle(vertexIndex1: Int, vertexIndex2: Int, vertexIndex3: Int){
        UNSAFE.putInt(currentAddress, vertexIndex1)
        UNSAFE.putInt(currentAddress + 4, vertexIndex2)
        UNSAFE.putInt(currentAddress + 8, vertexIndex3)

        currentAddress += TRIANGLE_BYTES
    }

    fun getTrianglesSoFar() : Int {
        return (currentAddress - startAddress).toInt() / TRIANGLE_BYTES
    }
}

fun createBasicIndexBuffer(triangleCount: Int) : BasicIndexBuffer {

    val numBytes = (TRIANGLE_BYTES * triangleCount).toLong()
    val startAddress = UNSAFE.allocateMemory(numBytes)
    val boundAddress = startAddress + numBytes
    return BasicIndexBuffer(startAddress, boundAddress)
}

fun createBasicIndexBufferAt(startAddress: Long, boundAddress: Long) : BasicIndexBuffer {
    return BasicIndexBuffer(startAddress, boundAddress)
}