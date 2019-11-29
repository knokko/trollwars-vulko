package vulko.models.building

import vulko.memory.util.UNSAFE

class SimpleVertexBuilder internal constructor(startAddress: Long, boundAddress: Long) : AbstractBuilder(startAddress, boundAddress) {

    fun add(x: Float, y: Float, z: Float){
        UNSAFE.putFloat(currentAddress, x)
        UNSAFE.putFloat(currentAddress + 4, y)
        UNSAFE.putFloat(currentAddress + 8, z)

        currentAddress += 12
    }
}

fun createSimpleVertexBuilder(vertexCount: Int) : SimpleVertexBuilder {
    val byteSize = (vertexCount * 12).toLong()
    val startAddress = UNSAFE.allocateMemory(byteSize)
    val boundAddress = startAddress + byteSize
    return SimpleVertexBuilder(startAddress, boundAddress)
}