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

@Throws(MallocException::class)
fun malloc(numBytes: Long) : Long {
    val address = nmemAlloc(numBytes)
    if (address == NULL)
        throw MallocException(numBytes)
    else
        return address;
}

fun fill(address: Long, numBytes: Long, value: Byte) {
    memSet(address, value.toInt(), numBytes)
}

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