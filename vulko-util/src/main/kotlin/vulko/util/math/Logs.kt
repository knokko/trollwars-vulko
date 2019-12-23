package vulko.util.math

private const val POWER_30 = 1 shl 30
private const val POWER_62 = 1 shl 62

/**
 * Checks if the given number is a positive power of 2.
 * If the given number is a positive power of 2, this method will return true.
 * If not, this method will return false.
 */
fun isPowerOf2(number: Int) : Boolean {
    return number >= 0 && Integer.bitCount(number) == 1
}

fun isPowerOf2(number: Long) : Boolean {
    return number >= 0 && java.lang.Long.bitCount(number) == 1
}

/**
 * Returns the smallest power of 2 that is at least as big as the given number.
 * If number is negative or greater than 2^30, an IllegalArgumentException will be thrown instead.
 */
fun nextPowerOf2(number: Int) : Int {
    if (number < 0 || number > POWER_30){
        throw IllegalArgumentException("Number must be between 0 and 2^30, but is $number")
    }
    val power = 32 - Integer.numberOfLeadingZeros(number - 1)
    return 1 shl power
}

fun nextPowerOf2(number: Long) : Long {
    if (number < 0 || number > POWER_62){
        throw IllegalArgumentException("Number must be between 0 and 2^62, but is $number")
    }
    val power = 64 - java.lang.Long.numberOfLeadingZeros(number - 1L)
    return 1L shl power
}