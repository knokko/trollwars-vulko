package vulko.instance

import java.nio.ByteBuffer

import org.lwjgl.glfw.GLFWVulkan.*
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDebugUtils.*;
import org.lwjgl.vulkan.VK10.*

import vulko.util.*
import java.lang.IllegalArgumentException

/**
 * An InstanceBuilder is necessary to configure the future Vulkan instance and VulkoInstance.
 *
 * The constructor has a lot of parameters, so it is recommended to use named parameters to invoke the constructor.
 * Many parameters of the constructor have default values, so you can quickly get started by filling in only the
 * required parameters and choosing the others later on.
 *
 * Most parameters can be modified after the InstanceBuilder has been created, so it's not necessary to choose all
 * values when invoking the constructor.
 *
 * To use the InstanceBuilder, call the createInstance method of the VulkoWindow with the InstanceBuilder as the
 * parameter.
 */
class InstanceBuilder(
    /**
     * Whether or not to register a debug callback.
     * This is true by default because it is convenient for development, but it should be set to false in production.
     */
    val debug: Boolean = true,
    /**
     * The app name that will be passed to the VkApplicationInfo struct
     */
    var appName: String,
    /**
     * The app version that will be passed to the VkApplicationInfo struct
     */
    var appVersion: Version,
    /**
     * The Vulkan API version that will be passed to the VkApplicationInfo, by default VK_API_VERSION_1_0
     */
    var apiVersion: Int = VK_API_VERSION_1_0,
    /**
     * The engine name that will be passed to the VkApplicationInfo, by default "Vulko"
     */
    var engineName: String = "Vulko",
    /**
     * The engine version that will be passed to the VkApplicationInfo, by default 1.0.0
     */
    var engineVersion: Version = Version(1, 0, 0),
    /**
     * A set containing the names of all instance extensions required by your application.
     * If debug is true, "VK_EXT_debug_utils" will automatically be added to this set.
     * Also, all extension names returned by glfwGetRequiredInstanceExtensions() will be added automatically.
     *
     * Upon instance creation, this set will be converted to a PointerBuffer and passed to ppEnabledExtensionNames
     */
    val requiredExtensions: MutableSet<String> = HashSet(),
    /**
     * A set containing the names of all layers required by your application.
     * If debug is true, "VK_LAYER_LUNARG_standard_validation" will automatically be added to this set.
     *
     * Upon instance creation, this set will be converted to a PointerBuffer and passed to ppEnabledLayerNames
     */
    val requiredLayers: MutableSet<String> = HashSet(),
    /**
     * The debug callback to be used when debug is true.
     * If debug is false, it will be ignored.
     * By default, this will be DEFAULT_DEBUG_CALLBACK, which basically prints everything it gets.
     */
    var debugCallback: (Int, Int, Long, Long) -> Int = DEFAULT_DEBUG_CALLBACK) {

    init {
        if (debug) {
            requiredExtensions.add("VK_EXT_debug_utils")
            requiredLayers.add("VK_LAYER_LUNARG_standard_validation")
        }

        val requiredGLFWExtensions = glfwGetRequiredInstanceExtensions()
            ?: throw UnsupportedOperationException("No extensions for window surfaceHandle creation were found")
        for (index in 0 until requiredGLFWExtensions.capacity()){
            val pExtensionName = requiredGLFWExtensions[index]
            val extensionName = memUTF8(pExtensionName)
            requiredExtensions.add(extensionName)
        }
    }

    internal fun createInstance() : VkInstance {
        stackPush().use { stack ->
            val appInfo = VkApplicationInfo.callocStack(stack)
            appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
            appInfo.pApplicationName(stack.UTF8(appName))
            appInfo.applicationVersion(appVersion.vulkanVersion)
            appInfo.pEngineName(stack.UTF8(engineName))
            appInfo.engineVersion(engineVersion.vulkanVersion)
            appInfo.apiVersion(apiVersion)

            val createInfo = VkInstanceCreateInfo.callocStack(stack)
            createInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
            createInfo.pApplicationInfo(appInfo)

            if (!glfwVulkanSupported()) {
                throw UnsupportedOperationException("Vulkan is not supported")
            }

            val supportedExtensionCount = stack.mallocInt(1)
            assertSuccess(vkEnumerateInstanceExtensionProperties(null as ByteBuffer?, supportedExtensionCount, null))

            val supportedExtensions = VkExtensionProperties.callocStack(supportedExtensionCount[0], stack)
            assertSuccess(
                vkEnumerateInstanceExtensionProperties(
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
            assertSuccess(vkCreateInstance(createInfo, null, pInstance))
            return VkInstance(pInstance[0], createInfo)
        }
    }

    internal fun createDebugMessenger(instance: VkInstance) : Long {
        stackPush().use {stack ->

            val debugCI = VkDebugUtilsMessengerCreateInfoEXT.callocStack(stack)
            debugCI.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
            debugCI.messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT or VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT
            or VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT or VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT)
            debugCI.messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT or VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT
            or VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT)

            debugCI.pfnUserCallback(debugCallback)

            val pMessenger = stack.mallocLong(1)
            assertSuccess(vkCreateDebugUtilsMessengerEXT(instance, debugCI, null, pMessenger))
            return pMessenger[0]
        }
    }
}

/**
 * The default debug callback.
 * It will simply print every message along with the message severity and message type.
 */
val DEFAULT_DEBUG_CALLBACK = {messageSeverity: Int, messageTypes: Int, pCallbackData: Long, _: Long ->
    val severityString = when(messageSeverity) {
        VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT -> "Verbose"
        VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT -> "Info"
        VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT -> "Warning"
        VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT -> "Error"
        else -> throw IllegalArgumentException("Unknown messageSeverity: $messageSeverity")
    }

    var messageTypeString = ""
    if (messageTypes and VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT != 0){
        messageTypeString += "General"
    }
    if (messageTypes and VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT != 0){
        messageTypeString += "Validation"
    }
    if (messageTypes and VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT != 0){
        messageTypeString += "Performance"
    }

    val callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData)
    val message = callbackData.pMessageString()
    val messageID = callbackData.messageIdNumber()

    println("[$severityString][$messageTypeString]: $message ($messageID)")
    VK_FALSE
}