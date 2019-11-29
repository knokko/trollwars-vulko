package vulko.util.math

import java.lang.Integer.MIN_VALUE
import java.lang.StrictMath.floorDiv

/**
 * Returns ceil(a / b), which is a / b rounded upwards (towards positive infinity)
 */
fun ceilDiv(a: Int, b: Int) : Int {
    if (a == MIN_VALUE){

        // This case needs to be done separately because we can't negative a
        return a / b
    }
    return -floorDiv(-a, b)
}