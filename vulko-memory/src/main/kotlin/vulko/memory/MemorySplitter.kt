package vulko.memory

/**
 * Represents an object that owns memory and is able to split its memory over other memory owning objects.
 */
interface MemorySplitter {

    /**
     * Gives the memory that was previously borrowed to a 'child' back to this object. The childAddress parameter
     * should be the start address of the memory given to that child.
     *
     * After this call, the memory of the child is no longer owned by that child and thus the child should no longer
     * be used!
     */
    fun freeChild(childAddress: Long)
}