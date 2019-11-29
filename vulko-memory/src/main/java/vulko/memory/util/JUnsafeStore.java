package vulko.memory.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

class JUnsafeStore {

    static final Unsafe UNSAFE;

    static {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            UNSAFE = (Unsafe) unsafeField.get(null);
        } catch (Exception ex){
            throw new Error("Couldn't get UNSAFE:", ex);
        }
    }
}
