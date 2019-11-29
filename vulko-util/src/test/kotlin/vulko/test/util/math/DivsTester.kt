package vulko.test.util.math

import org.junit.Assert.assertEquals
import org.junit.Test
import vulko.util.math.ceilDiv
import java.lang.Integer.MAX_VALUE
import java.lang.Integer.MIN_VALUE

class DivsTester {

    @Test
    fun testCeilDiv(){

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
        assertEquals(1, ceilDiv(MAX_VALUE, MAX_VALUE))
        assertEquals(1, ceilDiv(MIN_VALUE, MIN_VALUE))
        assertEquals(-1, ceilDiv(MIN_VALUE, MAX_VALUE))
        assertEquals(0, ceilDiv(MAX_VALUE, MIN_VALUE))
    }
}