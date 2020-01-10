package vulko.memory

import org.junit.Assert.*
import org.junit.Test
import vulko.memory.util.free
import vulko.memory.util.malloc

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

    @Test
    fun testClose() {
        val splitter = TestSplitter()
        val stack = VirtualStack(splitter, 123, 100)
        assertFalse(stack.isClosed())
        assertEquals(null, splitter.lastFreedAddress)
        stack.close()
        assertTrue(stack.isClosed())
        assertEquals(123L as Long?, splitter.lastFreedAddress)
        splitter.lastFreedAddress = null
        try {
            stack.close()
            throw AssertionError("Closing a VirtualStack twice should throw" +
                    "an IllegalStateException")
        } catch (ex: IllegalStateException) {
            assertEquals(null as Long?, splitter.lastFreedAddress)
        }
    }

    @Test
    fun testCloseExact() {

        // Set-up
        val splitter = TestSplitter()
        val capacity = 4L
        val address = malloc(capacity)
        val stack = VirtualStack(splitter, address, capacity)

        // The actual test
        stack.pushInt(12)
        try {
            stack.closeExact()
            throw AssertionError("VirtualStack.closeExact should have thrown" +
                    "an IllegalStateException")
        } catch (ex: IllegalStateException) {

            // The close should have been aborted before any action was taken
            assertEquals(null, splitter.lastFreedAddress)
            assertFalse(stack.isClosed())
        }

        stack.popInt()
        stack.closeExact()
        assertTrue(stack.isClosed())
        assertEquals(address as Long?, splitter.lastFreedAddress)

        // Clean-up
        free(address)
    }
}