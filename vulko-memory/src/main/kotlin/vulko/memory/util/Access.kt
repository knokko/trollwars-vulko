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

fun putByte(address: Long, value: Byte) {
    memPutByte(address, value)
}

fun putInt(address: Long, value: Int) {
    memPutInt(address, value)
}

fun getByte(address: Long) : Byte {
    return memGetByte(address)
}

fun getInt(address: Long) : Int {
    return memGetInt(address)
}
