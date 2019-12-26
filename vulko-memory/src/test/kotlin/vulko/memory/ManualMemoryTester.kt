package vulko.memory

import vulko.memory.MemoryManager
import vulko.memory.VirtualStack
import kotlin.random.Random

fun main(){
    println("Allocating memory...")
    val startTime = System.currentTimeMillis()
    MemoryManager(500_000_000, false).use {manager ->
        manager.claimStack(10, "force init").use {}
        val endTime = System.currentTimeMillis()
        println("Claiming memory took ${endTime - startTime} ms")
        val startTimeStack = System.nanoTime()
        manager.claimStack(1000, "test stack").use {stack ->
            val endTimeStack = System.nanoTime()
            println("Claiming stack took ${endTimeStack - startTimeStack} ns")
            val testInt = 2_000_000_000
            val testByte: Byte = 120
            stack.pushByte(testByte)
            stack.pushInt(testInt)
            manager.summarizeClaims()
            manager.summarizeFragments()
            if (stack.popInt() != testInt){
                throw Error("popInt returned wrong result")
            }
            if (stack.popByte() != testByte){
                throw Error("popByte returned wrong result")
            }
        }

        println("Results after the first simple check:")
        manager.summarizeClaims()
        manager.summarizeFragments()

        val rand = Random.Default
        val numStacks = 2000
        val numPrimitives = 20
        val stackList = ArrayList<VirtualStack>(numStacks)
        val startTimeClaims = System.nanoTime()
        for (index in 0 until numStacks){
            stackList.add(manager.claimStack(2500 + rand.nextLong(2500), "test stack $index"))
        }
        val endTimeClaims = System.nanoTime()
        println("Claiming stacks took ${(endTimeClaims - startTimeClaims) / 1_000L} us")

        var pushPullTime = 0L
        while (stackList.isNotEmpty()){
            val index = rand.nextInt(stackList.size)
            stackList[index].use { stack ->
                val bytes = ByteArray(numPrimitives)
                rand.nextBytes(bytes)

                // No nextShort method ;(
                val shorts = ShortArray(numPrimitives) { rand.nextInt().toShort() }
                val chars = CharArray(numPrimitives) { rand.nextInt().toChar() }
                val ints = IntArray(numPrimitives) { rand.nextInt() }
                val floats = FloatArray(numPrimitives) { Float.fromBits(rand.nextInt()) }
                val longs = LongArray(numPrimitives) { rand.nextLong() }
                val doubles = DoubleArray(numPrimitives) { Double.fromBits(rand.nextLong()) }
                val jumps = LongArray(numPrimitives) { rand.nextLong(100) }
                val jumpAddresses = LongArray(numPrimitives) { -1 }

                val startTimePush = System.nanoTime()

                for (byte in bytes) {
                    stack.pushByte(byte)
                }
                for (short in shorts) {
                    stack.pushShort(short)
                }
                for ((jumpIndex, jump) in jumps.withIndex()) {
                    jumpAddresses[jumpIndex] = stack.push(jump)
                }
                for (char in chars) {
                    stack.pushChar(char)
                }
                for (int in ints) {
                    stack.pushInt(int)
                }
                for (float in floats) {
                    stack.pushFloat(float)
                }
                for (long in longs) {
                    stack.pushLong(long)
                }
                for (double in doubles) {
                    stack.pushDouble(double)
                }

                for (double in doubles.reversed()) {
                    if (stack.popDouble() != double && double == double) throw Error("Double error")
                }
                for (long in longs.reversed()) {
                    if (stack.popLong() != long) throw Error("Long error")
                }
                for (float in floats.reversed()) {
                    if (stack.popFloat() != float && float == float) throw Error("Float error")
                }
                for (int in ints.reversed()){
                    if (stack.popInt() != int) throw Error("Int error")
                }
                for (char in chars.reversed()){
                    if (stack.popChar() != char) throw Error("Char error")
                }
                for ((jumpIndex, jump) in jumps.reversed().withIndex()){
                    if (stack.pop(jump) != jumpAddresses[numPrimitives - jumpIndex - 1]) throw Error("Jump address")
                }
                for (short in shorts.reversed()){
                    if (stack.popShort() != short) throw Error("Short error")
                }
                for (byte in bytes.reversed()){
                    if (stack.popByte() != byte) throw Error("Byte error")
                }

                val endTimePull = System.nanoTime()
                pushPullTime += endTimePull - startTimePush
            }
            stackList.removeAt(index)
        }
        println("Final summary:")
        manager.summarizeFragments()
        manager.summarizeClaims()
        println("Pushing and pulling took ${pushPullTime / numStacks.toLong()} ns on average")
    }
    println("Closed manager")
}