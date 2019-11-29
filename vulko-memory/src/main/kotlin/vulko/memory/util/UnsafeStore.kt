package vulko.memory.util

import sun.misc.Unsafe

/**
 * A reference to the Unsafe. This reference was obtained by reflection.
 *
 * This reference is very important because this library uses Unsafe a lot.
 */
val UNSAFE: Unsafe = JUnsafeStore.UNSAFE