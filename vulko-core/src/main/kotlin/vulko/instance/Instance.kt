package vulko.instance

import vulko.device.physical.VulkoPhysicalDevice

import org.lwjgl.glfw.GLFWVulkan.*
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDebugUtils.*
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.VK10.*

import vulko.util.assertSuccess
import vulko.window.VulkoWindow
import java.lang.IllegalArgumentException

/**
 * Represents the Vulko/Vulkan instance. This class has the *vulkanInstance* property, which is the actual Vulkan
 * instance. This class also contains the window surface handle and the list of physical devices.
 *
 * The methods of this class are usually more convenient to use than the raw Vulkan functions, but it won't cover
 * everything.
 *
 * The Vulko instance should be obtained by calling the createInstance method of the VulkoWindow.
 */
class VulkoInstance internal constructor(
    /**
     * A reference to the VkInstance backed by this VulkoInstance
     */
    val vulkanInstance: VkInstance,
    /**
     * A reference to the VulkoWindow that this VulkoInstance was created for
     */
    val window: VulkoWindow,
    /**
     * The array of layer names to be supplied to ppEnabledLayerNames.
     * The content of this array could have been set by adding layer names to the requiredLayers of the InstanceBuilder
     * that created this Instance (before creating this instance). If debug was enabled for that InstanceBuilder, this
     * array is guaranteed to contain "VK_LAYER_LUNARG_standard_validation"
     */
    val requiredLayers: Array<String>,
    /**
     * The handle for the VkDebugUtilsMessengerEXT of this instance if 'debug' was true for the InstanceBuilder that
     * created this instance. If 'debug' was false, it will be null.
     */
    val debugMessenger: Long?){

    private var destroyed = false

    /**
     * The handle of the window surface.
     * It will be initialized automatically upon creation of this Instance.
     */
    val surfaceHandle: Long

    /**
     * The list of physical devices accessible by the VkInstance backed by this Instance.
     * This list will be filled and initialized automatically upon creation of this Instance.
     */
    val physicalDevices: List<VulkoPhysicalDevice>

    init {
        surfaceHandle = stackPush().use { stack ->
            val pSurface = stack.mallocLong(1)
            assertSuccess(glfwCreateWindowSurface(vulkanInstance, window.handle, null, pSurface))
            pSurface[0]
        }

        physicalDevices = stackPush().use {stack ->
            val pDeviceCount = stack.mallocInt(1)
            assertSuccess(vkEnumeratePhysicalDevices(vulkanInstance, pDeviceCount, null))
            val deviceCount = pDeviceCount[0]

            val pDevices = stack.mallocPointer(deviceCount)
            assertSuccess(vkEnumeratePhysicalDevices(vulkanInstance, pDeviceCount, pDevices))

            Array(deviceCount) {index ->
                VulkoPhysicalDevice(VkPhysicalDevice(pDevices[index], vulkanInstance), this)
            }.asList()
        }
    }

    /**
     * Chooses the best physical device based on the scores of the *deviceRater*. A device rater has to give an optional
     * score to each available physical device. Higher scores are considered better and the device that gets the highest
     * score will be returned by this method.
     *
     * A score of null indicates that the given physical device does not satisfy the minimum requirements for the
     * application. If all physical devices receive a score of null, this method returns null.
     */
    fun choosePhysicalDevice(deviceRater: (VulkoPhysicalDevice) -> Int? = DEFAULT_DEVICE_RATER) : VulkoPhysicalDevice? {

        var bestDevice: VulkoPhysicalDevice? = null
        var bestScore: Int? = null
        for (device in physicalDevices){
            val score = deviceRater(device)
            if (score != null && (bestScore == null || score > bestScore)){
                bestDevice = device
                bestScore = score
            }
        }

        return bestDevice
    }

    /**
     * Checks if this VulkoInstance has already been destroyed
     */
    fun isDestroyed() : Boolean {
        return destroyed
    }

    /**
     * Destroys this VulkoInstance along with its physical devices, surface and debug messenger if it has not yet been
     * destroyed.
     */
    fun destroy(){

        if (!destroyed) {
            destroyed = true
            for (device in physicalDevices) {
                device.destroy()
            }

            vkDestroySurfaceKHR(vulkanInstance, surfaceHandle, null)

            if (debugMessenger != null) {
                vkDestroyDebugUtilsMessengerEXT(vulkanInstance, debugMessenger, null)
            }

            vkDestroyInstance(vulkanInstance, null)
        }
    }
}

/**
 * The default physical device rater. It will be used by the choosePhysicalDevice method of Instance if no alternative
 * physical device rater was supplied.
 *
 * This rater will assign scores only to physical devices that have at least 1 queue family supporting graphics commands,
 * at least 1 queue family supporting present commands (possible the same as the one supporting graphics commands) and
 * have swapchain support.
 * The devices that fulfill those requirements will get a high score if they are discrete GPUs and get a small bonus
 * depending on how many graphics and present queues they have.
 */
val DEFAULT_DEVICE_RATER = {device: VulkoPhysicalDevice ->
    val queueFam = device.queueFamilyIndices
    if (queueFam.graphicsFamilies.isEmpty()){
        println("Device ${device.properties.deviceNameString()} doesn't have any graphics queue families")
        println()
        null
    } else if (queueFam.presentFamilies.isEmpty()){
        println("Device ${device.properties.deviceNameString()} doesn't have any present queue families")
        println()
        null
    } else {
        val extensions = device.extensionProperties
        var hasSwapchain = false
        for (extension in extensions){
            if (extension.extensionNameString() == "VK_KHR_swapchain"){
                hasSwapchain = true
                break;
            }
        }
        if (!hasSwapchain){
            println("Device ${device.properties.deviceNameString()} doesn't have swapchain support")
            println()
            null
        } else {
            if (device.surfacePresentModes.limit() == 0){
                println("Device ${device.properties.deviceNameString()} doesn't have any surface present modes")
                println()
                null
            } else if (device.surfaceFormats.limit() == 0){
                println("Device ${device.properties.deviceNameString()} doesn't have any surface formats")
                println()
                null
            } else {
                var score = 0
                if (device.properties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) {
                    score += 200
                }
                score += queueFam.graphicsFamilies.size + queueFam.presentFamilies.size
                println("Device ${device.properties.deviceNameString()} is suitable and its score is $score")
                println()
                score
            }
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