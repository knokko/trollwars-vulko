package vulko.window

import vulko.instance.VulkoInstance
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import vulko.instance.DEFAULT_DEBUG_CALLBACK
import vulko.util.Version
import vulko.util.assertSuccess
import java.nio.ByteBuffer

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

    private var destroyed = false

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
     * Creates the Vulko instance with the information provided in the parameters.
     * Destroying this window will automatically destroy the returned instance as well.
     * This method should be called at most once, calling it again will throw an IllegalStateException.
     */
    fun createInstance(
        /**
        * Whether or not to register a debug callback.
        * This is true by default because it is convenient for development, but it should be set to false in production.
        */
        debug: Boolean = true,
        /**
        * The app name that will be passed to the VkApplicationInfo struct
        */
        appName: String,
        /**
        * The app version that will be passed to the VkApplicationInfo struct
        */
        appVersion: Version,
        /**
        * The Vulkan API version that will be passed to the VkApplicationInfo, by default VK_API_VERSION_1_0
        */
        apiVersion: Int = VK10.VK_API_VERSION_1_0,
        /**
        * The engine name that will be passed to the VkApplicationInfo, by default "Vulko"
        */
        engineName: String = "Vulko",
        /**
        * The engine version that will be passed to the VkApplicationInfo, by default 1.0.0
        */
        engineVersion: Version = Version(1, 0, 0),
        /**
        * A set containing the names of all instance extensions required by your application.
        * If debug is true, "VK_EXT_debug_utils" will automatically be added to this set.
        * Also, all extension names returned by glfwGetRequiredInstanceExtensions() will be added automatically.
        *
        * Upon instance creation, this set will be converted to a PointerBuffer and passed to ppEnabledExtensionNames
        */
        requiredExtensions: MutableSet<String> = HashSet(),
        /**
        * A set containing the names of all layers required by your application.
        * If debug is true, "VK_LAYER_LUNARG_standard_validation" will automatically be added to this set.
        *
        * Upon instance creation, this set will be converted to a PointerBuffer and passed to ppEnabledLayerNames
        */
        requiredLayers: MutableSet<String> = HashSet(),
        /**
        * The debug callback to be used when debug is true.
        * If debug is false, it will be ignored.
        * By default, this will be DEFAULT_DEBUG_CALLBACK, which basically prints everything it gets.
        */
        debugCallback: (Int, Int, Long, Long) -> Int = DEFAULT_DEBUG_CALLBACK
    ) : VulkoInstance {

        if (instance != null){
            throw IllegalStateException("Another Vulko instance has been created already")
        }

        if (debug) {
            requiredExtensions.add("VK_EXT_debug_utils")
            requiredLayers.add("VK_LAYER_LUNARG_standard_validation")
        }

        val requiredGLFWExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions()
                ?: throw UnsupportedOperationException("No extensions for window surfaceHandle creation were found")
        for (index in 0 until requiredGLFWExtensions.capacity()){
            val pExtensionName = requiredGLFWExtensions[index]
            val extensionName = MemoryUtil.memUTF8(pExtensionName)
            requiredExtensions.add(extensionName)
        }

        val vulkanInstance = stackPush().use { stack ->
            val appInfo = VkApplicationInfo.callocStack(stack)
            appInfo.sType(VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO)
            appInfo.pApplicationName(stack.UTF8(appName))
            appInfo.applicationVersion(appVersion.vulkanVersion)
            appInfo.pEngineName(stack.UTF8(engineName))
            appInfo.engineVersion(engineVersion.vulkanVersion)
            appInfo.apiVersion(apiVersion)

            val createInfo = VkInstanceCreateInfo.callocStack(stack)
            createInfo.sType(VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
            createInfo.pApplicationInfo(appInfo)

            if (!GLFWVulkan.glfwVulkanSupported()) {
                throw UnsupportedOperationException("Vulkan is not supported")
            }

            val supportedExtensionCount = stack.mallocInt(1)
            assertSuccess(VK10.vkEnumerateInstanceExtensionProperties(null as ByteBuffer?, supportedExtensionCount, null))

            val supportedExtensions = VkExtensionProperties.callocStack(supportedExtensionCount[0], stack)
            assertSuccess(
                    VK10.vkEnumerateInstanceExtensionProperties(
                            null as ByteBuffer?,
                            supportedExtensionCount,
                            supportedExtensions
                    )
            )

            println("Supported extensions are:")
            for (extension in supportedExtensions) {
                println(extension.extensionNameString())
            }
            println("")

            val requiredExtensionsBuffer = stack.mallocPointer(requiredExtensions.size)
            for (extension in requiredExtensions) {
                requiredExtensionsBuffer.put(stack.UTF8(extension))
            }
            requiredExtensionsBuffer.flip()

            createInfo.ppEnabledExtensionNames(requiredExtensionsBuffer)

            val requiredLayersBuffer = stack.mallocPointer(requiredLayers.size)
            for (layer in requiredLayers) {
                requiredLayersBuffer.put(stack.UTF8(layer))
            }
            requiredLayersBuffer.flip()

            createInfo.ppEnabledLayerNames(requiredLayersBuffer)

            val pInstance = stack.mallocPointer(1)
            assertSuccess(VK10.vkCreateInstance(createInfo, null, pInstance))
            VkInstance(pInstance[0], createInfo)
        }

        var messenger: Long? = null
        if (debug) {
            messenger = stackPush().use {stack ->

                val debugCI = VkDebugUtilsMessengerCreateInfoEXT.callocStack(stack)
                debugCI.sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                debugCI.messageSeverity(EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT or EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT
                        or EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT or EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT)
                debugCI.messageType(EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT or EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT
                        or EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT)

                debugCI.pfnUserCallback(debugCallback)

                val pMessenger = stack.mallocLong(1)
                assertSuccess(EXTDebugUtils.vkCreateDebugUtilsMessengerEXT(vulkanInstance, debugCI, null, pMessenger))
                pMessenger[0]
            }
        }

        instance = VulkoInstance(vulkanInstance, this, requiredLayers.toTypedArray(), messenger)
        return instance!!
    }

    /**
     * Calls glfwWindowShouldClose and returns the result
     */
    fun shouldClose() : Boolean {
        return glfwWindowShouldClose(handle)
    }

    /**
     * Calls glfwPollEvents
     */
    fun poll() {
        glfwPollEvents()
    }

    /**
     * Destroys the window and cleans most Vulko resources up. This will destroy all Vulko resources that do not
     * indicate otherwise.
     *
     * If this window has already been destroyed, this method won't do anything.
     */
    fun destroy(){
        if (!destroyed) {
            destroyed = true
            if (instance != null) {
                instance!!.destroy()
            }

            glfwDestroyWindow(handle)
            glfwTerminate()
        }
    }
}

typealias ResizeListener = (newWidth: Int, newHeight: Int) -> Unit