package vulko.models.building

import vulko.memory.util.*
import org.lwjgl.system.MemoryUtil.memByteBuffer
import java.nio.ByteBuffer

abstract class AbstractBuilder protected constructor(val startAddress: Long, val boundAddress: Long){

    protected var currentAddress = startAddress

    fun checkBounds(){
        if (currentAddress > boundAddress){
            throw IndexOutOfBoundsException("bound address was $boundAddress, but current address is $currentAddress")
        }
    }

    fun checkBoundsExact(){
        if (currentAddress != boundAddress){
            throw IndexOutOfBoundsException("end address is $boundAddress, but current address is $currentAddress")
        }
    }

    private fun getByteSize() : Int {
        checkBounds()
        return (currentAddress - startAddress).toInt()
    }

    fun createBackingBuffer() : ByteBuffer {
        val buffer = memByteBuffer(startAddress, getByteSize())
        buffer.position(0)
        buffer.limit(buffer.capacity())
        return buffer
    }

    fun freeMemory(){

        // Don't free the memory twice
        if (currentAddress != 0L) {
            free(startAddress)
            checkBounds()

            // Make sure we throw an error if an attempt is made to use this buffer after it has been freed
            currentAddress = 0
        }
    }
}