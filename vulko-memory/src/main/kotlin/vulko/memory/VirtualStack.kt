package vulko.memory

import vulko.memory.util.*

/**
 * A chunk of memory that is to be used like a stack, but allocated on the heap.
 * Virtual stacks can be used to quickly push data to and pop data from.
 *
 * Virtual stacks have a start address, stack pointer and a bound address.
 * Pushing data onto the stack will increase the stack pointer and popping data will decrease it.
 * Unlike 'the stack', virtual stacks can be very big, so they can also be used to store a lot of data.
 *
 * When attempting to push data at or after the bound address, a VirtualStackOverflow will be thrown.
 * When attempting to pop data before the start address, a VirtualStackUnderflow will be thrown.
 */
class VirtualStack

/**
 * Creates a new VirtualStack with the given parent with the given startAddress and capacity.
 */
constructor(
    /**
     * The owner of the memory this virtual stack borrows. Its freeChild method will be called upon closing this stack.
     */
    private val parent: MemorySplitter,
   /**
    * The start address of this virtual stack. This is the smallest memory address this stack will
    * write and read data from. It is used to check whether or not this stack will underflow before
    * doing a pop()
    */
   val startAddress: Long,
    /**
     * The capacity (maximum size) of this stack, in bytes.
     */
    capacity: Long) : AutoCloseable, MemorySplitter {

    /**
     * The bound address of this virtual stack. This virtual stack will throw a VirtualStackOverflow if an attempt is
     * made to push data at or beyond this address.
     */
    val boundAddress = startAddress + capacity

    private var stackPointer = startAddress

    private var closed = false

    /**
     * Closes this virtual stack and calls the freeChild method of its parent to free the memory claimed by the stack.
     * If this stack has already been closed, an IllegalStateException will be thrown.
     */
    override fun close() {
        if (closed){
            throw IllegalStateException("Virtual stack closed more than once")
        }
        closed = true
        parent.freeChild(startAddress)
    }

    /**
     * Closes this virtual stack and calls the freeChild method of its parent to free the memory claimed by the stack.
     * If the stack pointer is not equal to the start address (and thus not all pushed data has been popped), an
     * IllegalStateException will be thrown.
     * If this stack has already been closed, an IllegalStateException will also be thrown.
     */
    fun closeExact(){
        if (stackPointer != startAddress){
            throw IllegalStateException("Virtual stack didn't pop all data it pushed")
        }
        close()
    }

    /**
     * The freeChild implementation of VirtualStack. This method will literally set the stack pointer to the given
     * childAddress, so it must be called at exactly the right moment.
     *
     * If the childAddress is outside the memory owned by this stack, an IllegalArgumentException will be thrown.
     */
    override fun freeChild(childAddress: Long){
        if (childAddress < startAddress){
            throw IllegalArgumentException("childAddress $childAddress is smaller than the start address $startAddress")
        }
        if (childAddress > stackPointer){
            throw IllegalArgumentException("childAddress $childAddress is larger than the stack pointer $stackPointer")
        }
        stackPointer = childAddress
    }

    /**
     * Increases the stack pointer by numBytes and returns the old value of the stack pointer. After this call, a hole
     * in this stack of size numBytes will have been created at the returned address. This space is then safe to use
     * for storing data until it is popped or this stack is closed.
     *
     * Using this method directly is discouraged, but allowed. It is recommended to use pushForChunk to create a hole
     * in this stack or use methods like pushInt and pushByte to push individual values onto this stack.
     *
     * Usually, there should be a pop() call for every push() call. The pop calls should be done in the opposite order
     * of the push calls (the first pop corresponds to the last push call).
     *
     * If this stack has been closed already, an IllegalStateException will be thrown.
     * If this push causes this stack to overflow, a VirtualStackOverflow will be thrown.
     */
    fun push(numBytes: Long) : Long {
        if (closed){
            throw IllegalStateException("This virtual stack has been closed")
        }
        val result = stackPointer
        if (stackPointer + numBytes > boundAddress){
            throw VirtualStackOverflow(stackPointer, numBytes, boundAddress)
        }
        stackPointer += numBytes
        return result
    }

    /**
     * Increases the stack pointer by numBytes to make space for a memory chunk of size numBytes. The returned memory
     * chunk will be safe to use until this stack is closed or the chunk is closed. Closing the chunk is considered the
     * same as popping it, so it must be done at exactly the right moment. It is recommended to use a use-statement to
     * ensure that it will be closed at the right moment.
     *
     * If this stack has been closed already, an IllegalStateException will be thrown.
     * If this push causes this stack to overflow, a VirtualStackOverflow will be thrown.
     */
    fun pushForChunk(numBytes: Long) : MemoryChunk {
        val chunkAddress = push(numBytes)
        return MemoryChunk(this, chunkAddress, numBytes)
    }

    /**
     * Increases the stack pointer by numBytes to make space for a virtual stack with a capacity of numBytes. The
     * returned virtual stack will be safe to use until this stack is closed or that stack is closed. Closing the stack
     * is considered the same as popping it, so it must be done at exactly the right moment. It is recommended to use a use-statement to
     * ensure that it will be closed at the right moment.
     *
     * The child stack can push and pop independently from this stack, so changing the stack pointer of either stack
     * won't mess with the stack pointer of the other.
     *
     * If numBytes is not specified, the default value will be used, which is all remaining capacity. If that is used,
     * this stack can't do anything until the child stack is closed.
     *
     * If this stack has been closed already, an IllegalStateException will be thrown.
     * If this push causes this stack to overflow, a VirtualStackOverflow will be thrown.
     */
    fun pushForChildStack(numBytes: Long = boundAddress - stackPointer) : VirtualStack {
        val childAddress = push(numBytes)
        return VirtualStack(this, childAddress, numBytes)
    }

    /**
     * Decreases the stack pointer by numBytes and returns the new value of the stack pointer. If used correctly, the
     * result will be the same address as the result of the corresponding push call.
     *
     * Using this method directly is allowed, but discouraged. It is recommended to use popForChunk or methods like
     * popInt and popFloat instead.
     *
     * Every pop() call will pop data pushed by the last push() call that has not yet been popped. The numBytes
     * parameters of both calls should be the same.
     *
     * If this stack has been closed already, an IllegalStateException will be thrown.
     * If this push causes this stack to underflow (pop more data than was pushed), a VirtualStackUnderflow will be
     * thrown.
     */
    fun pop(numBytes: Long) : Long {
        if (closed){
            throw IllegalStateException("This virtual stack has been closed")
        }
        if (stackPointer - numBytes < startAddress){
            throw VirtualStackUnderflow(stackPointer, numBytes, startAddress)
        }
        stackPointer -= numBytes
        return stackPointer
    }

    /**
     * Puts the given byte at the stack pointer and increases the stack pointer by 1.
     *
     * If this stack has been closed already, an IllegalStateException will be thrown.
     * If this push causes this stack to overflow, a VirtualStackOverflow will be thrown.
     */
    fun pushByte(value: Byte){
        putByte(push(1), value)
    }

    /**
     * Puts the given short at the stack pointer and increases the stack pointer by 2.
     *
     * If this stack has been closed already, an IllegalStateException will be thrown.
     * If this push causes this stack to overflow, a VirtualStackOverflow will be thrown.
     */
    fun pushShort(value: Short){
        putShort(push(2), value)
    }

    /**
     * Puts the given char at the stack pointer and increases the stack pointer by 2.
     *
     * If this stack has been closed already, an IllegalStateException will be thrown.
     * If this push causes this stack to overflow, a VirtualStackOverflow will be thrown.
     */
    fun pushChar(value: Char){
        putChar(push(2), value)
    }

    /**
     * Puts the given int at the stack pointer and increases the stack pointer by 4.
     *
     * If this stack has been closed already, an IllegalStateException will be thrown.
     * If this push causes this stack to overflow, a VirtualStackOverflow will be thrown.
     */
    fun pushInt(value: Int){
        putInt(push(4), value)
    }

    /**
     * Puts the given float at the stack pointer and increases the stack pointer by 4.
     *
     * If this stack has been closed already, an IllegalStateException will be thrown.
     * If this push causes this stack to overflow, a VirtualStackOverflow will be thrown.
     */
    fun pushFloat(value: Float){
        putFloat(push(4), value)
    }

    /**
     * Puts the given long at the stack pointer and increases the stack pointer by 8.
     *
     * If this stack has been closed already, an IllegalStateException will be thrown.
     * If this push causes this stack to overflow, a VirtualStackOverflow will be thrown.
     */
    fun pushLong(value: Long){
        putLong(push(8), value)
    }

    /**
     * Puts the given double at the stack pointer and increases the stack pointer by 8.
     *
     * If this stack has been closed already, an IllegalStateException will be thrown.
     * If this push causes this stack to overflow, a VirtualStackOverflow will be thrown.
     */
    fun pushDouble(value: Double){
        putDouble(push(8), value)
    }

    /**
     * Decreases the stack pointer by 1 and returns the byte value at the new stack pointer.
     *
     * If this stack has been closed already, an IllegalStateException will be thrown.
     * If this push causes this stack to underflow (pop more data than was pushed), a VirtualStackUnderflow will be
     * thrown.
     */
    fun popByte() : Byte {
        return getByte(pop(1))
    }

    /**
     * Decreases the stack pointer by 2 and returns the short value at the new stack pointer.
     *
     * If this stack has been closed already, an IllegalStateException will be thrown.
     * If this push causes this stack to underflow (pop more data than was pushed), a VirtualStackUnderflow will be
     * thrown.
     */
    fun popShort() : Short {
        return getShort(pop(2))
    }

    /**
     * Decreases the stack pointer by 2 and returns the char value at the new stack pointer.
     *
     * If this stack has been closed already, an IllegalStateException will be thrown.
     * If this push causes this stack to underflow (pop more data than was pushed), a VirtualStackUnderflow will be
     * thrown.
     */
    fun popChar() : Char {
        return getChar(pop(2))
    }

    /**
     * Decreases the stack pointer by 4 and returns the int value at the new stack pointer.
     *
     * If this stack has been closed already, an IllegalStateException will be thrown.
     * If this push causes this stack to underflow (pop more data than was pushed), a VirtualStackUnderflow will be
     * thrown.
     */
    fun popInt() : Int {
        return getInt(pop(4))
    }

    /**
     * Decreases the stack pointer by 4 and returns the float value at the new stack pointer.
     *
     * If this stack has been closed already, an IllegalStateException will be thrown.
     * If this push causes this stack to underflow (pop more data than was pushed), a VirtualStackUnderflow will be
     * thrown.
     */
    fun popFloat() : Float {
        return getFloat(pop(4))
    }

    /**
     * Decreases the stack pointer by 8 and returns the long value at the new stack pointer.
     *
     * If this stack has been closed already, an IllegalStateException will be thrown.
     * If this push causes this stack to underflow (pop more data than was pushed), a VirtualStackUnderflow will be
     * thrown.
     */
    fun popLong() : Long {
        return getLong(pop(8))
    }

    /**
     * Decreases the stack pointer by 8 and returns the double value at the new stack pointer.
     *
     * If this stack has been closed already, an IllegalStateException will be thrown.
     * If this push causes this stack to underflow (pop more data than was pushed), a VirtualStackUnderflow will be
     * thrown.
     */
    fun popDouble() : Double {
        return getDouble(pop(8))
    }
}

class VirtualStackOverflow(stackPointer: Long, numBytes: Long, boundAddress: Long) : RuntimeException("Stack pointer is $stackPointer and $numBytes bytes of data need to be pushed, but the bound address is $boundAddress")
class VirtualStackUnderflow(stackPointer: Long, numBytes: Long, startAddress: Long) : RuntimeException("Stack pointer is $stackPointer and $numBytes bytes of data need to be popped, but the start address is $startAddress")