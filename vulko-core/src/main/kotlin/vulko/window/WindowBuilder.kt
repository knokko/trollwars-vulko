package vulko.window

import org.lwjgl.glfw.GLFW.*

import org.lwjgl.system.MemoryUtil.NULL

val Boolean.int
    get() = if (this) 1 else 0

/**
 * The WindowBuilder is the starting point of Vulko. It contains all variables needed to create the window.
 * They can either be set by assigning values to the constructor parameters or by modifying the builder after
 * it has been constructed.
 *
 * To create the VulkoWindow, invoke the build() method after all variables are the way you like it.
 */
class WindowBuilder(
    /**
     * The title the window will get, its value will be passed to glfwCreateWindow
     */
    var title: String = "No title chosen",
    /**
     * The preferred initial width of the window, its value will be passed to glfwCreateWindow
     */
    var width: Int = 800,
    /**
     * The preferred initial height of the window, its value will be passed to glfwCreateWindow
     */
    var height: Int = 600){

    private val windowHints = arrayOfNulls<Boolean?>(WINDOW_HINTS.size)
    private val framebufferHints = arrayOfNulls<Int?>(FRAMEBUFFER_HINTS.size)
    private val monitorHints = arrayOfNulls<Int?>(MONITOR_HINTS.size)
    private val contextHints = arrayOfNulls<Int?>(CONTEXT_HINTS.size)
    private val macHints = arrayOfNulls<Int?>(MAC_HINTS.size)
    private val x11Hints = arrayOfNulls<Int?>(X11_HINTS.size)

    init {
        setContextHint(ContextHint.CLIENT_API, GLFW_NO_API)
    }

    /**
     * Sets the value of a window-related hint. This method uses boolean values because all window-related hints are
     * basically booleans.
     */
    fun setWindowHint(hint: WindowHint, value: Boolean = true){
        windowHints[hint.ordinal] = value
    }

    /**
     * Sets the value of a framebuffer-related hint.
     */
    fun setFramebufferHint(hint: FramebufferHint, value: Int){
        framebufferHints[hint.ordinal] = value
    }

    /**
     * Sets the value of a monitor-related hint.
     */
    fun setMonitorHint(hint: MonitorHint, value: Int){
        monitorHints[hint.ordinal] = value
    }

    /**
     * Sets the value of a content-related hint.
     */
    fun setContextHint(hint: ContextHint, value: Int){
        contextHints[hint.ordinal] = value
    }

    /**
     * Sets the value of a MacOS (only) hint.
     */
    fun setMacHint(hint: MacHint, value: Int){
        macHints[hint.ordinal] = value
    }

    /**
     * Sets the value of an X11 (only) hint.
     */
    fun setX11Hint(hint: X11Hint, value: Int){
        x11Hints[hint.ordinal] = value
    }

    /**
     * Builds the VulkoWindow from the properties of this WindowBuilder. Calling this method more than once will create
     * multiple windows.
     */
    fun build() : VulkoWindow {

        glfwInit()

        for ((index, value) in windowHints.withIndex()) {
            if (value != null){
                glfwWindowHint(WINDOW_HINTS[index].numericValue, value.int)
            }
        }

        for ((index, value) in framebufferHints.withIndex()) {
            if (value != null) {
                glfwWindowHint(FRAMEBUFFER_HINTS[index].numericValue, value)
            }
        }

        for ((index, value) in monitorHints.withIndex()) {
            if (value != null) {
                glfwWindowHint(MONITOR_HINTS[index].numericValue, value)
            }
        }

        for ((index, value) in contextHints.withIndex()) {
            if (value != null) {
                glfwWindowHint(CONTEXT_HINTS[index].numericValue, value)
            }
        }

        for ((index, value) in macHints.withIndex()) {
            if (value != null) {
                glfwWindowHint(MAC_HINTS[index].numericValue, value)
            }
        }

        for ((index, value) in x11Hints.withIndex()) {
            if (value != null){
                glfwWindowHint(X11_HINTS[index].numericValue, value)
            }
        }

        val windowHandle = glfwCreateWindow(width, height, title, NULL, NULL)

        return VulkoWindow(windowHandle)
    }
}