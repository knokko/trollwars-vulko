package vulko.models.building

import vulko.memory.util.UNSAFE

class SimpleIndexBuilder internal constructor(startAddress: Long, boundAddress: Long) : AbstractBuilder(startAddress, boundAddress) {

    fun bindTriangle(vertex1: Int, vertex2: Int, vertex3: Int){
        UNSAFE.putInt(currentAddress, vertex1)
        UNSAFE.putInt(currentAddress + 4, vertex2)
        UNSAFE.putInt(currentAddress + 8, vertex3)

        currentAddress += 12
    }

    fun getIndicesSoFar() : Int {
        return (currentAddress - startAddress).toInt() / 4
    }
}

fun createSimpleIndexBuilder(numTriangles: Int) : SimpleIndexBuilder {
    val byteSize = (12 * numTriangles).toLong()
    val address = UNSAFE.allocateMemory(byteSize)
    val boundAddress = address + byteSize
    return SimpleIndexBuilder(address, boundAddress)
}