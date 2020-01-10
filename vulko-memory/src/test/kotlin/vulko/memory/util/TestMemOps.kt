package vulko.memory.util

import org.junit.Test

class TestMemOps {

    private fun testGoodMalloc(numBytes: Long){

        // Allocate the memory
        val address = malloc(numBytes)

        // 'Check' that no JVM-crash occurs when writing to the memory
        for (currentAddress in address until address + numBytes){
            putByte(currentAddress, 7)
        }

        free(address)
    }

    private fun testBigMalloc(numBytes: Long){
        try {
            malloc(numBytes)
            throw AssertionError("malloc($numBytes) should have thrown a MallocException")
        } catch (ex: MallocException){}
    }

    @Test
    fun testMalloc(){

        // Edge cases
        testGoodMalloc(0)
        testGoodMalloc(1)

        // Normal cases
        testGoodMalloc(123)
        testGoodMalloc(934)

        /* This test will only work under the (reasonable) assumption that the device on
         * which this test is executed won't be able to allocate a block of 2^64 - 1 bytes.
         */
        testBigMalloc(-1)
    }
}