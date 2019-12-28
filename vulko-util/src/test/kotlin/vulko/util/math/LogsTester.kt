package vulko.util.math

import io.kotlintest.shouldThrow
import org.junit.Assert.assertEquals
import org.junit.Test
import vulko.util.math.isPowerOf2
import vulko.util.math.nextPowerOf2

class LogsTester {

    @Test
    fun testIsPowerOf2Int(){

        // Good cases
        assert(isPowerOf2(1))
        assert(isPowerOf2(2))
        assert(isPowerOf2(2048))
        assert(isPowerOf2(Integer.MAX_VALUE / 2 + 1))

        // Bad positive cases
        assert(!isPowerOf2(0))
        assert(!isPowerOf2(15))
        assert(!isPowerOf2(17))
        assert(!isPowerOf2(Integer.MAX_VALUE))

        // Bad negative cases
        assert(!isPowerOf2(Integer.MIN_VALUE))
        assert(!isPowerOf2(-4))
    }

    @Test
    fun testIsPowerOf2Long(){

        // Good cases
        assert(isPowerOf2(1L))
        assert(isPowerOf2(2L))
        assert(isPowerOf2(2048L))
        assert(isPowerOf2(Long.MAX_VALUE / 2L + 1L))

        // Bad positive cases
        assert(!isPowerOf2(0L))
        assert(!isPowerOf2(15L))
        assert(!isPowerOf2(17L))
        assert(!isPowerOf2(Long.MAX_VALUE))

        // Bad negative cases
        assert(!isPowerOf2(Long.MIN_VALUE))
        assert(!isPowerOf2(-4L))
    }

    @Test
    fun testNextPowerOf2Int(){

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

    @Test
    fun testNextPowerOf2Long(){

        // Basic cases
        assertEquals(16L, nextPowerOf2(15L))
        assertEquals(16L, nextPowerOf2(16L))
        assertEquals(32L, nextPowerOf2(17L))

        // Low edge cases
        assertEquals(1L, nextPowerOf2(0L))
        assertEquals(1L, nextPowerOf2(1L))

        // High edge cases
        assertEquals(1 shl 62, nextPowerOf2(1 shl 62))
        assertEquals(1 shl 62, nextPowerOf2((1 shl 62) - 1))

        // Negative exceptions
        shouldThrow<IllegalArgumentException> {
            nextPowerOf2(-1)
        }

        // Big exceptions
        shouldThrow<IllegalArgumentException> {
            nextPowerOf2((1 shl 62) + 1)
        }
        shouldThrow<IllegalArgumentException> {
            nextPowerOf2(Long.MAX_VALUE)
        }
    }
}