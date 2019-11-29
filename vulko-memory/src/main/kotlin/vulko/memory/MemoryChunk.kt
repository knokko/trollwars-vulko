package vulko.memory

/**
 * Represents a chunk of memory that has been claimed and can be used to (temporarily) store and load data. Its space
 * can for instance be used to store texture data or vertex data.
 */
class MemoryChunk(private val parent: MemorySplitter, val address: Long, val size: Long) : AutoCloseable {

    private var closed = false

    override fun close(){
        if (closed){
            throw IllegalStateException("Memory buffer closed more than once")
        }
        closed = true
        parent.freeChild(address)
    }
}