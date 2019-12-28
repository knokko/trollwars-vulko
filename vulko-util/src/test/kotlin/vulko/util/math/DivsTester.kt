package vulko.util.math

import org.junit.Assert.assertEquals
import org.junit.Test

class DivsTester {

    @Test
    fun testCeilDivInt(){

        // Positive numbers
        assertEquals(0, ceilDiv(0, 3))
        assertEquals(1, ceilDiv(1, 3))
        assertEquals(1, ceilDiv(2, 3))
        assertEquals(1, ceilDiv(3, 3))
        assertEquals(2, ceilDiv(4, 3))

        // Negative numbers
        assertEquals(0, ceilDiv(-0, 3))
        assertEquals(0, ceilDiv(-1, 3))
        assertEquals(0, ceilDiv(-2, 3))
        assertEquals(-1, ceilDiv(-3, 3))
        assertEquals(-1, ceilDiv(-4, 3))

        // Edge cases
        assertEquals(1, ceilDiv(Integer.MAX_VALUE, Integer.MAX_VALUE))
        assertEquals(1, ceilDiv(Integer.MIN_VALUE, Integer.MIN_VALUE))
        assertEquals(-1, ceilDiv(Integer.MIN_VALUE, Integer.MAX_VALUE))
        assertEquals(0, ceilDiv(Integer.MAX_VALUE, Integer.MIN_VALUE))
    }

    @Test
    fun testCeilDivLong(){

        // Positive numbers
        assertEquals(0L, ceilDiv(0L, 3L))
        assertEquals(1L, ceilDiv(1L, 3L))
        assertEquals(1L, ceilDiv(2L, 3L))
        assertEquals(1L, ceilDiv(3L, 3L))
        assertEquals(2L, ceilDiv(4L, 3L))

        // Negative numbers
        assertEquals(0L, ceilDiv(-0L, 3L))
        assertEquals(0L, ceilDiv(-1L, 3L))
        assertEquals(0L, ceilDiv(-2L, 3L))
        assertEquals(-1L, ceilDiv(-3L, 3L))
        assertEquals(-1L, ceilDiv(-4L, 3L))

        // Edge cases
        assertEquals(1L, ceilDiv(Long.MAX_VALUE, Long.MAX_VALUE))
        assertEquals(1L, ceilDiv(Long.MIN_VALUE, Long.MIN_VALUE))
        assertEquals(-1L, ceilDiv(Long.MIN_VALUE, Long.MAX_VALUE))
        assertEquals(0L, ceilDiv(Long.MAX_VALUE, Long.MIN_VALUE))
    }
}