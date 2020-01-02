package vulko.memory

import org.junit.Assert.assertEquals
import org.junit.Test

class TestVirtualStack {

    private fun testBadConstructor(startAddress: Long, capacity: Long){
        try {
            VirtualStack(DUMMY_SPLITTER, startAddress, capacity)
            throw AssertionError("VirtualStack(DUMMY_SPLITTER, $startAddress, $capacity) should have thrown an IllegalArgumentException")
        } catch (ex: IllegalArgumentException) {}
    }

    private fun testGoodConstructor(startAddress: Long, capacity: Long){
        val stack = VirtualStack(DUMMY_SPLITTER, startAddress, capacity)
        assertEquals(startAddress, stack.startAddress)
        assertEquals(startAddress + capacity, stack.boundAddress)
    }

    @Test
    fun testConstructor(){

        // Edge cases
        testGoodConstructor(1, 10)
        testGoodConstructor(-1, 10)
        testGoodConstructor(123, 0)
        testGoodConstructor(321, 1)

        // Normal cases
        testGoodConstructor(-1234, 156)
        testGoodConstructor(83473, 347)

        // Bad edge cases
        testBadConstructor(0, 1234)
        testBadConstructor(123, -1)

        // Bad cases
        testBadConstructor(-34934, -3847485)
        testBadConstructor(348734, -1234)
    }
}