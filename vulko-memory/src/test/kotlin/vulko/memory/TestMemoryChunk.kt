package vulko.memory

import org.junit.Assert.*
import org.junit.Test
import vulko.memory.util.malloc

class TestMemoryChunk {

    private fun testGoodConstructor(size: Long) {
        val address = malloc(size)
        val chunk = MemoryChunk(SIMPLE_SPLITTER, address, size)
        assertEquals(chunk.address, address)
        assertEquals(chunk.size, size)
        chunk.close()
    }

    private fun testGoodConstructor(address: Long, size: Long) {
        val splitter = TestSplitter()
        val chunk = MemoryChunk(splitter, address, size)
        assertEquals(address, chunk.address)
        assertEquals(size, chunk.size)
        assertEquals(null, splitter.lastFreedAddress)
        chunk.close()
        assertEquals(address, splitter.lastFreedAddress)
    }

    private fun testBadConstructor(address: Long, size: Long){
        try {
            MemoryChunk(DUMMY_SPLITTER, address, size)
            throw AssertionError("MemoryChunk(DUMMY_SPLITTER, $address, $size) " +
                    "should have thrown an IllegalArgumentException")
        } catch (ex: IllegalArgumentException) {}
    }

    @Test
    fun testConstructor(){

        // Good edge cases
        testGoodConstructor(1, 0)
        testGoodConstructor(-1, 1)
        testGoodConstructor(1)
        testGoodConstructor(384784357, Integer.MAX_VALUE + 1L)
        testGoodConstructor(123, Long.MAX_VALUE)

        // Normal cases
        testGoodConstructor(100)
        testGoodConstructor(12345)

        // Bad edge cases
        testBadConstructor(0, 123)
        testBadConstructor(100, -1)

        // Bad normal cases
        testBadConstructor(123, -34587)
        testBadConstructor(3458745, Integer.MIN_VALUE - 10L)
    }

    @Test
    fun testClose(){
        val splitter = TestSplitter()
        val chunk = MemoryChunk(splitter,1, 1)
        assertEquals(null, splitter.lastFreedAddress)
        chunk.close()
        assertEquals(1L as Long?, splitter.lastFreedAddress)
        splitter.lastFreedAddress = 4L
        chunk.close()
        assertEquals(4L as Long?, splitter.lastFreedAddress)
    }

    @Test
    fun testIsClosed(){
        val chunk = MemoryChunk(DUMMY_SPLITTER, 1, 1)
        assertFalse(chunk.isClosed())
        chunk.close()
        assertTrue(chunk.isClosed())
        chunk.close()
        assertTrue(chunk.isClosed())
    }
}