package vulko.util.math

import java.lang.StrictMath.floorDiv

/**
 * Returns ceil(a / b), which is a / b rounded upwards (towards positive infinity)
 */
fun ceilDiv(a: Int, b: Int) : Int {
    if (a == Integer.MIN_VALUE){

        // This case needs to be done separately because we can't negate a
        return a / b
    }
    return -floorDiv(-a, b)
}

/**
 * Returns ceil(a / b), which is a / b rounded upwards (towards positive infinity)
 */
fun ceilDiv(a: Long, b: Long) : Long {
    if (a == Long.MIN_VALUE){

        // This case needs to be done separately because we can't negate a
        return a / b
    }
    return -floorDiv(-a, b)
}