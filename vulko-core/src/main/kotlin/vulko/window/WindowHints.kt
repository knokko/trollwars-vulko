package vulko.window

/**
 * An enum containing all window-related window hints
 */
enum class WindowHint(val numericValue: Int) {

    FOCUSED(0x00020001),
    RESIZABLE(0x00020003),
    VISIBLE(0x00020004),
    DECORATED(0x00020005),
    AUTO_ICONIFY(0x00020006),
    FLOATING(0x00020007),
    MAXIMIZED(0x00020008),
    CENTER_CURSOR(0x00020009),
    TRANSPARENT_FRAMEBUFFER(0x0002000A),
    FOCUS_ON_SHOW(0x0002000B),
    SCALE_TO_MONITOR(0x0002200C)
}

/**
 * An array containing all window-related window hints
 */
val WINDOW_HINTS = WindowHint.values()

/**
 * An enum containing all framebuffer-related window hints
 */
enum class FramebufferHint(val numericValue: Int){

    RED_BITS(0x00021001),
    GREEN_BITS(0x00021002),
    BLUE_BITS(0x00021003),
    ALPHA_BITS(0x00021004),
    DEPTH_BITS(0x00021005),
    STENCIL_BITS(0x00021006),
    ACCUM_RED_BITS(0x00021007),
    ACCUM_GREEN_BITS(0x00021008),
    ACCUM_BLUE_BITS(0x00021009),
    ACCUM_ALPHA_BITS(0x0002100A),
    AUX_BUFFERS(0x0002100B),
    STERIO(0x0002100C),
    SAMPLES(0x0002100D),
    SRGB_CAPABLE(0x0002100E),
    DOUBLE_BUFFER(0x00021010)
}

/**
 * An array containing all framebuffer-related window hints
 */
val FRAMEBUFFER_HINTS = FramebufferHint.values()

/**
 * An enum containing the only monitor-related window hint
 */
enum class MonitorHint(val numericValue: Int){

    REFRESH_RATE(0x0002100F)
}

/**
 * An array containing all monitor-related window hints (which is only the refresh rate)
 */
val MONITOR_HINTS = MonitorHint.values()

/**
 * An enum containing all context-related window hints
 */
enum class ContextHint(val numericValue: Int){

    CLIENT_API(0x00022001),
    CONTEXT_CREATION_API(0x0002200B),
    CONTEXT_VERSION_MAYOR(0x00022002),
    CONTEXT_VERSION_MINOR(0x00022003),
    OPENGL_FORWARD_COMPAT(0x00022006),
    OPENGL_DEBUG_CONTEXT(0x00022007),
    OPENGL_PROFILE(0x00022008),
    CONTEXT_ROBUSTNESS(0x00022005),
    CONTEXT_RELEASE_BEHAVIOR(0x00022009),
    CONTEXT_NO_ERROR(0x0002200A)
}

/**
 * An array containing all context-related window hints
 */
val CONTEXT_HINTS = ContextHint.values()

/**
 * An enum containing all Mac hints
 */
enum class MacHint(val numericValue: Int){

    COCOA_RETINA_FRAMEBUFFER(0x00023001),
    COCOA_FRAME_NAME(0x00023002),
    COCOA_GRAPHICS_SWITCHING(0x00023003)
}

/**
 * An array containing all Mac hints
 */
val MAC_HINTS = MacHint.values()

/**
 * An enum containing all X11 hints
 */
enum class X11Hint(val numericValue: Int){

    X11_CLASS_NAME(0x00024001),
    X11_INSTANCE_NAME(0x00024002)
}


/**
 * An array containing all X11 hints
 */
val X11_HINTS = X11Hint.values()