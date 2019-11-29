package vulko.window

import vulko.instance.InstanceBuilder
import vulko.instance.VulkoInstance
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryStack.stackPush

/**
 * The VulkoWindow class represents the window the application will run in.
 * It contains both the GLFW window handle and the Vulko instance, which makes it pretty much the entry point of Vulko.
 *
 * The destroy() method should be used to close the window and clean most GLFW and Vulkan resources up. If any Vulko
 * resource doesn't mention anything about cleaning up, it will be cleaned up by the destroy method.
 *
 * The VulkoWindow instance can be created by using the build method of a WindowBuilder.
 */
class VulkoWindow internal constructor(
    /**
     * The window handle, as returned by glfwCreateWindow
     */
    val handle: Long){

    private val resizeListeners = ArrayList<ResizeListener>()

    private var instance: VulkoInstance? = null

    var windowWidth = -1
    private set
    var windowHeight = -1
    private set

    init {
        stackPush().use {stack ->
            val pWidth = stack.mallocInt(1)
            val pHeight = stack.mallocInt(1)
            glfwGetWindowSize(handle, pWidth, pHeight)
            windowWidth = pWidth[0]
            windowHeight = pHeight[0]
        }

        val callback = {windowHandle: Long, newWidth: Int, newHeight: Int ->
            if (windowHandle == handle){
                windowWidth = newWidth
                windowHeight = newHeight
                for (listener in resizeListeners) {
                    listener(newWidth, newHeight)
                }
            }
        }
        glfwSetFramebufferSizeCallback(handle, callback)
    }

    /**
     * Registers a ResizeListener. All registered resize listeners will be notified whenever the window resizes.
     */
    fun addResizeListener(listener: ResizeListener){
        resizeListeners.add(listener)
    }

    /**
     * Removes a ResizeListener if it is in the list of resize listeners. If it's not on the list, nothing will happen.
     */
    fun removeResizeListener(listener: ResizeListener){
        resizeListeners.remove(listener)
    }

    /**
     * Creates the Vulko instance with the information provided in the InstanceBuilder parameter.
     * This method should be called at most once, calling it again will throw an IllegalStateException.
     */
    fun createInstance(builder: InstanceBuilder) : VulkoInstance {
        if (instance != null){
            throw IllegalStateException("Another Vulko instance has been created already")
        }

        val vulkanInstance = builder.createInstance()
        var messenger: Long? = null
        if (builder.debug) {
            messenger = builder.createDebugMessenger(vulkanInstance)
        }
        instance = VulkoInstance(vulkanInstance, this, builder.requiredLayers.toTypedArray(), messenger)
        return instance!!
    }

    /**
     * Destroys the window and cleans most Vulko resources up. This will destroy all Vulko resources that do not
     * indicate otherwise.
     */
    fun destroy(){
        if (instance != null){
            instance!!.destroy()
        }

        glfwDestroyWindow(handle)
        glfwTerminate()
    }
}

typealias ResizeListener = (newWidth: Int, newHeight: Int) -> Unit