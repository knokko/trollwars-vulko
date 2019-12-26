package vulko.memory

/**
 * Represents a chunk of memory that has been claimed and can be used to (temporarily) store and load data. Its space
 * can for instance be used to store texture data or vertex data.
 */
class MemoryChunk

/**
 * Constructs a new MemoryChunk with the given parent, address and size. This constructor will
 * not allocate any memory! The caller of the constructor must ensure that the memory at the given
 * address is safe to use for the user of the MemoryChunk!
 *
 * The memory addresses between address (inclusive) and address + size (exclusive) are considered
 * owned by the new MemoryChunk.
 */
constructor(
        /**
         * The parent of this memory chunk, that is, the MemorySplitter that owns the memory of
         * this chunk. Its freeChild method will be called when the close method of this chunk is
         * called (for the first time).
         */
        private val parent: MemorySplitter,

        /**
         * The lowest memory address of this MemoryChunk. The highest memory address of this
         * MemoryChunk will be address + size - 1.
         */
        val address: Long,

        /**
         * The size of this MemoryChunk, in bytes.
         */
        val size: Long) : AutoCloseable {

    init {
        if (size < 0L) {
            throw IllegalArgumentException("size must not be negative, but is $size")
        }
        if (address == 0L) {
            throw IllegalArgumentException("address must not be 0")
        }
    }

    private var closed = false

    /**
     * Closes this MemoryChunk if it has not yet been closed. If it is already closed, this method
     * will do nothing. If it has not yet been closed, the freeChild method of the parent of this
     * MemoryChunk will be invoked.
     *
     * After this method has been called, this MemoryChunk no longer 'owns' its memory and therefore
     * its memory should not be used anymore!
     */
    override fun close(){
        if (!closed) {
            closed = true
            parent.freeChild(address)
        }
    }

    /**
     * Checks if this MemoryChunk has been closed already, that is, when the close method has
     * been called at least once on this MemoryChunk.
     */
    fun isClosed() : Boolean {
        return closed
    }
}