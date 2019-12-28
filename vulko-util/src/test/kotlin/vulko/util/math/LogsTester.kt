package vulko.util.math

import io.kotlintest.shouldThrow
import org.junit.Assert.assertEquals
import org.junit.Test
import vulko.util.math.isPowerOf2
import vulko.util.math.nextPowerOf2

class LogsTester {

    @Test
    fun testIsPowerOf2(){
        assert(isPowerOf2(1))
        assert(isPowerOf2(2))
        assert(isPowerOf2(2048))
        assert(isPowerOf2(Integer.MAX_VALUE / 2 + 1))

        assert(!isPowerOf2(0))
        assert(!isPowerOf2(15))
        assert(!isPowerOf2(17))
        assert(!isPowerOf2(Integer.MAX_VALUE))

        assert(!isPowerOf2(Integer.MIN_VALUE))
        assert(!isPowerOf2(-4))
    }

    @Test
    fun testNextPowerOf2(){

        // Basic cases
        assertEquals(16, nextPowerOf2(15))
        assertEquals(16, nextPowerOf2(16))
        assertEquals(32, nextPowerOf2(17))

        // Low edge cases
        assertEquals(1, nextPowerOf2(0))
        assertEquals(1, nextPowerOf2(1))

        // High edge cases
        assertEquals(1 shl 30, nextPowerOf2(1 shl 30))
        assertEquals(1 shl 30, nextPowerOf2((1 shl 30) - 1))

        // Negative exceptions
        shouldThrow<IllegalArgumentException> {
            nextPowerOf2(-1)
        }

        // Big exceptions
        shouldThrow<IllegalArgumentException> {
            nextPowerOf2((1 shl 30) + 1)
        }
        shouldThrow<IllegalArgumentException> {
            nextPowerOf2(Integer.MAX_VALUE)
        }
    }
}