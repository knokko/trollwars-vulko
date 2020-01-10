package vulko.memory.util

import org.lwjgl.system.MemoryUtil.*

/*
fun putByte(address: Long, value: Byte) {
    UNSAFE.putByte(address, value)
}

fun putInt(address: Long, value: Int) {
    UNSAFE.putInt(address, value)
}

fun getByte(address: Long) : Byte {
    return UNSAFE.getByte(address)
}

fun getInt(address: Long) : Int {
    return UNSAFE.getInt(address)
}*/

fun copy(sourceAddress: Long, destAddress: Long, numBytes: Long) {
    memCopy(sourceAddress, destAddress, numBytes)
}

/**
 * Tries to allocate a block of memory of numBytes bytes.
 * If the allocation failed, a MallocException will be thrown.
 * If numBytes is not 0, this method will not return 0.
 * If numBytes is 0, the result of this method may or may not be 0.
 *
 * The memory address of the first byte of allocated memory will be returned.
 * That address should later be freed using the free() method.
 */
@Throws(MallocException::class)
fun malloc(numBytes: Long) : Long {
    val address = nmemAlloc(numBytes)
    if (address == NULL && numBytes != 0L)
        throw MallocException(numBytes)
    else
        return address;
}

/**
 * Sets all bytes in the memory address range [address, address + numBytes> to value.
 * This method will not check whether or not the memory is actually owned by the JVM.
 * If the JVM doesn't own the memory, it could crash.
 *
 * If any of the following conditions holds, an IllegalArgumentException will be thrown:
 *
 * -numBytes is negative
 *
 * -address is 0 and numBytes is not 0
 */
@Throws(IllegalArgumentException::class)
fun fill(address: Long, numBytes: Long, value: Byte) {
    if (numBytes < 0) {
        throw IllegalArgumentException("numBytes must not be negative, but is $numBytes")
    }
    if (address == 0L && numBytes != 0L) {
        throw IllegalArgumentException("address must not be 0")
    }
    memSet(address, value.toInt(), numBytes)
}

/**
 * Frees a block of memory that was previously allocated using the malloc method.
 */
fun free(address: Long) {
    nmemFree(address)
}

fun putByte(address: Long, value: Byte) {
    memPutByte(address, value)
}

fun putShort(address: Long, value: Short) {
    memPutShort(address, value)
}

fun putChar(address: Long, value: Char) {

    // I wonder why they left out MemoryUtil.memPutChar
    memPutShort(address, value.toShort())
}

fun putInt(address: Long, value: Int) {
    memPutInt(address, value)
}

fun putFloat(address: Long, value: Float) {
    memPutFloat(address, value)
}

fun putLong(address: Long, value: Long) {
    memPutLong(address, value)
}

fun putDouble(address: Long, value: Double) {
    memPutDouble(address, value)
}

fun getByte(address: Long) : Byte {
    return memGetByte(address)
}

fun getShort(address: Long) : Short {
    return memGetShort(address)
}

fun getChar(address: Long) : Char {

    // I wonder why they left out MemoryUtil.memGetChar
    return memGetShort(address).toChar()
}

fun getInt(address: Long) : Int {
    return memGetInt(address)
}

fun getFloat(address: Long) : Float {
    return memGetFloat(address)
}

fun getLong(address: Long) : Long {
    return memGetLong(address)
}

fun getDouble(address: Long) : Double {
    return memGetDouble(address)
}

class MallocException(val numBytes: Long) : RuntimeException("Couldn't allocate $numBytes of memory")