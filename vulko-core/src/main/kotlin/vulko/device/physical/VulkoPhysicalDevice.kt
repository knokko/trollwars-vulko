package vulko.device.physical

import vulko.instance.VulkoInstance
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.VK10.*
import vulko.device.logical.VulkoLogicalDevice
import vulko.device.logical.VulkoQueue

import vulko.util.assertSuccess
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.*

/**
 * Instances of this class are wrappers for instances of VkPhysicalDevice.
 * The instance of VkPhysicalDevice is freely accessible and can be used as target for Vulkan functions.
 *
 * This class has some fields that make it easier to access the properties of its backing physical device.
 * All fields use lazy initialization, so they won't query Vulkan for information until they are used and they won't
 * query Vulkan for information more than once.
 *
 * Instances of this class can be obtained by using the *physicalDevices* property of the VulkoInstance
 * or by calling its choosePhysicalDevice method.
 */
class VulkoPhysicalDevice internal constructor(val vulkanDevice: VkPhysicalDevice, val instance: VulkoInstance){

    private var gotExtensionProperties = false
    /**
     * Provides lazy access to the VkExtensionProperties array/Buffer of this physical device
     */
    val extensionProperties: VkExtensionProperties.Buffer by lazy {
        stackPush().use {stack ->
            val pExtensionCount = stack.mallocInt(1)
            val layerName: ByteBuffer? = null
            assertSuccess(vkEnumerateDeviceExtensionProperties(vulkanDevice, layerName, pExtensionCount, null))
            val extensionProperties = VkExtensionProperties.malloc(pExtensionCount[0])
            assertSuccess(vkEnumerateDeviceExtensionProperties(vulkanDevice, layerName, pExtensionCount, extensionProperties))
            gotExtensionProperties = true
            extensionProperties
        }
    }

    /**
     * Provides lazy access to a simple overview of the queue family indices of this physical device
     */
    val queueFamilyIndices by lazy {
        stackPush().use {stack ->
            val pNumQueueFamilies = stack.mallocInt(1)
            vkGetPhysicalDeviceQueueFamilyProperties(vulkanDevice, pNumQueueFamilies, null)
            val numQueueFamilies = pNumQueueFamilies[0]

            val queueFamilyProperties = VkQueueFamilyProperties.callocStack(numQueueFamilies, stack)
            vkGetPhysicalDeviceQueueFamilyProperties(vulkanDevice, pNumQueueFamilies, queueFamilyProperties)

            val presentFamilySupport = BooleanArray(numQueueFamilies)
            val graphicsFamilySupport = BooleanArray(numQueueFamilies)
            val computeFamilySupport = BooleanArray(numQueueFamilies)
            val transferFamilySupport = BooleanArray(numQueueFamilies)
            val sparseFamilySupport = BooleanArray(numQueueFamilies)
            val familySupportCount = IntArray(numQueueFamilies)

            for ((index, queueFamilyProps) in queueFamilyProperties.withIndex()){
                if (queueFamilyProps.queueCount() > 0) {
                    val flags = queueFamilyProps.queueFlags()
                    if (flags and VK_QUEUE_GRAPHICS_BIT != 0) {
                        graphicsFamilySupport[index] = true
                        familySupportCount[index]++
                    }
                    if (flags and VK_QUEUE_COMPUTE_BIT != 0){
                        computeFamilySupport[index] = true
                        familySupportCount[index]++
                    }
                    if (flags and VK_QUEUE_TRANSFER_BIT != 0){
                        transferFamilySupport[index] = true
                        familySupportCount[index]++
                    }
                    if (flags and VK_QUEUE_SPARSE_BINDING_BIT != 0){
                        sparseFamilySupport[index] = true
                        familySupportCount[index]++
                    }

                    val presentationSupported = stack.mallocInt(1)
                    assertSuccess(
                        vkGetPhysicalDeviceSurfaceSupportKHR(
                            vulkanDevice,
                            index,
                            instance.surfaceHandle,
                            presentationSupported
                        )
                    )

                    if (presentationSupported[0] == VK_TRUE) {
                        presentFamilySupport[index] = true
                        familySupportCount[index]++
                    }
                }
            }

            val numPresentFamilies = presentFamilySupport.count { value -> value}
            val numGraphicsFamilies = graphicsFamilySupport.count { value -> value }
            val numComputeFamilies = computeFamilySupport.count { value -> value }
            val numTransferFamilies = transferFamilySupport.count { value -> value }
            val numSparseFamilies = sparseFamilySupport.count { value -> value }

            val presentFamilies = IntArray(numPresentFamilies)
            val graphicsFamilies = IntArray(numGraphicsFamilies)
            val computeFamilies = IntArray(numComputeFamilies)
            val transferFamilies = IntArray(numTransferFamilies)
            val sparseFamilies = IntArray(numSparseFamilies)

            fun partialSort(dest: IntArray, destSupport: BooleanArray, supportCount: IntArray) {
                var nextDestIndex = 0

                val tempArray: Array<Pair<Int,Int>> = Array(dest.size){_ -> Pair(-1, -1)}

                for (index in 0 until numQueueFamilies){
                    if (destSupport[index]){
                        tempArray[nextDestIndex++] = Pair(index, supportCount[index])
                    }
                }

                tempArray.sortBy { it.second }
                for ((index, pair) in tempArray.withIndex()){
                    dest[index] = pair.first
                }
            }

            partialSort(graphicsFamilies, graphicsFamilySupport, familySupportCount)
            partialSort(presentFamilies, presentFamilySupport, familySupportCount)
            partialSort(computeFamilies, computeFamilySupport, familySupportCount)
            partialSort(transferFamilies, transferFamilySupport, familySupportCount)
            partialSort(sparseFamilies, sparseFamilySupport, familySupportCount)

            println("graphics family support is ${Arrays.toString(graphicsFamilySupport)}")
            println("present family support is ${Arrays.toString(presentFamilySupport)}")
            println("compute family support is ${Arrays.toString(computeFamilySupport)}")
            println("transfer family support is ${Arrays.toString(transferFamilySupport)}")
            println("sparse family support is ${Arrays.toString(sparseFamilySupport)}")

            QueueFamilyIndices(graphicsFamilies.asList(), presentFamilies.asList(), computeFamilies.asList(),
                transferFamilies.asList(), sparseFamilies.asList())
        }
    }

    private var gotProperties = false
    /**
     * Provides lazy access to the VkPhysicalDeviceProperties of this physical device
     */
    val properties: VkPhysicalDeviceProperties by lazy {
        val properties = VkPhysicalDeviceProperties.malloc()
        vkGetPhysicalDeviceProperties(vulkanDevice, properties)
        gotProperties = true
        properties
    }

    private var gotFeatures = false
    /**
     * Provides lazy access to the VkPhysicalDeviceFeatures of this physical device
     */
    val features: VkPhysicalDeviceFeatures by lazy {
        val features = VkPhysicalDeviceFeatures.malloc()
        vkGetPhysicalDeviceFeatures(vulkanDevice, features)
        gotFeatures = true
        features
    }

    private var gotSurfaceCapabilities = false
    /**
     * Provides lazy access to the VkSurfaceCapabilitiesKHR of this physical device
     */
    val surfaceCapabilities: VkSurfaceCapabilitiesKHR by lazy {
        val surfaceCaps = VkSurfaceCapabilitiesKHR.malloc()
        assertSuccess(vkGetPhysicalDeviceSurfaceCapabilitiesKHR(vulkanDevice, instance.surfaceHandle, surfaceCaps))
        gotSurfaceCapabilities = true
        surfaceCaps
    }

    private var gotSurfaceFormats = false
    /**
     * Provides lazy access to the VkSurfaceFormatKHR array/Buffer of this physical device
     */
    val surfaceFormats: VkSurfaceFormatKHR.Buffer by lazy {
        stackPush().use {stack ->
            val pFormatCount = stack.mallocInt(1)
            assertSuccess(vkGetPhysicalDeviceSurfaceFormatsKHR(vulkanDevice, instance.surfaceHandle, pFormatCount, null))
            val formats = VkSurfaceFormatKHR.malloc(pFormatCount[0])
            assertSuccess(vkGetPhysicalDeviceSurfaceFormatsKHR(vulkanDevice, instance.surfaceHandle, pFormatCount, formats))
            formats
        }
    }

    private var gotSurfacePresentModes = false
    /**
     * Provides lazy access to the surface present modes of this physical device
     */
    val surfacePresentModes: IntBuffer by lazy {
        stackPush().use {stack ->
            val pModeCount = stack.mallocInt(1)
            assertSuccess(vkGetPhysicalDeviceSurfacePresentModesKHR(vulkanDevice, instance.surfaceHandle, pModeCount, null))
            val presentModes = stack.mallocInt(pModeCount[0])
            assertSuccess(vkGetPhysicalDeviceSurfacePresentModesKHR(vulkanDevice, instance.surfaceHandle, pModeCount, presentModes))
            presentModes
        }
    }

    override fun toString() : String {
        return "PhysicalDevice(${properties.deviceNameString()}, vulkanDevice: $vulkanDevice, $queueFamilyIndices)"
    }

    private val logicalDevices = ArrayList<VulkoLogicalDevice>()

    /**
     * Creates a logical device to interface with the physical device backed by this VulkoPhysicalDevice.
     * The queues for the logical device should be obtained using the queueMap of the resulting VulkoLogicalDevice.
     *
     * The logical device will automatically be destroyed whenever this physical device is being destroyed
     */
    fun createLogicalDevice(
        /**
         * The features of this physical device that should be enabled for the logical device.
         * It will be passed directly to pEnabledFeatures.
         * If you leave it null, no special features will be enabled.
         */
        requiredFeatures: VkPhysicalDeviceFeatures? = null,
        /**
         * A list of pairs containing the indices of the queue families to enable for the logical device to create
         * along with their priority.
         * If the index of a queue family appears more than once, the highest of their priorities will be assigned to
         * the queue family.
         *
         * A queue will be created for each unique queue family index and can be obtained by inserting the queue family
         * index in the queueMap of the returned VulkoLogicalDevice.
         */
        queueFamilyIndices: List<Pair<Int, Float>> = arrayListOf(
        Pair(this.queueFamilyIndices.getGraphicsFamilyIndex(), 1.0f), Pair(this.queueFamilyIndices.getPresentFamilyIndex(), 1.0f)),
        /**
         * An array of extension names to enable. It will be converted to a PointerBuffer and then passed to
         * ppEnabledExtensionNames.
         *
         * By default, this contains only "VK_KHR_swapchain". If another value is set to this parameter, it should also
         * contain "VK_KHR_swapchain".
         */
        requiredDeviceExtensions: Array<String> = arrayOf("VK_KHR_swapchain")) : VulkoLogicalDevice {

        // Ensure that we have no duplicate indices
        val queueMap = HashMap<Int, Float>(queueFamilyIndices.size)
        for (pair in queueFamilyIndices){
            val old = queueMap[pair.first]

            // Take the highest priority for each queue
            if (old == null || pair.second > old){
                queueMap[pair.first] = pair.second
            }
        }

        stackPush().use {stack ->
            val queueCreateInfos = VkDeviceQueueCreateInfo.callocStack(queueMap.size, stack)

            for ((index,entry) in queueMap.entries.withIndex()){
                val queueCreateInfo = queueCreateInfos[index]
                queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                queueCreateInfo.queueFamilyIndex(entry.key)
                queueCreateInfo.pQueuePriorities(stack.floats(entry.value))
            }

            val deviceCI = VkDeviceCreateInfo.callocStack(stack)
            deviceCI.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
            deviceCI.pQueueCreateInfos(queueCreateInfos)

            // requiredFeatures is allowed to be null, but so is pEnabledFeatures
            deviceCI.pEnabledFeatures(requiredFeatures)

            val extensionsBuffer = stack.mallocPointer(requiredDeviceExtensions.size)
            for ((index, extension) in requiredDeviceExtensions.withIndex()){
                extensionsBuffer.put(index, stack.UTF8(extension))
            }
            deviceCI.ppEnabledExtensionNames(extensionsBuffer)

            // This is ignored by modern implementations, but may be used by older ones
            val layerNames = instance.requiredLayers
            val layerNamesBuffer = stack.mallocPointer(layerNames.size)
            for ((index,layerName) in layerNames.withIndex()){
                layerNamesBuffer.put(index, stack.UTF8(layerName))
            }
            deviceCI.ppEnabledLayerNames(layerNamesBuffer)

            val pDevice = stack.mallocPointer(1)
            assertSuccess(vkCreateDevice(vulkanDevice, deviceCI, null, pDevice))

            val logicQueueMap = HashMap<Int, VulkoQueue>(queueMap.size)
            val device = VulkoLogicalDevice(VkDevice(pDevice.get(0), vulkanDevice, deviceCI), logicQueueMap, this)
            logicalDevices.add(device)

            val pQueue = stack.mallocPointer(1)

            for (queueFamIndex in queueMap.keys){
                vkGetDeviceQueue(device.vulkanDevice, queueFamIndex, 0, pQueue)
                logicQueueMap[queueFamIndex] = VulkoQueue(VkQueue(pQueue[0], device.vulkanDevice))
            }

            return device
        }
    }

    internal fun destroy(){
        for (logDevice in logicalDevices){
            logDevice.destroy()
        }
        if (gotSurfaceFormats){
            surfaceFormats.free()
        }
        if (gotSurfacePresentModes){
            memFree(surfacePresentModes)
        }
        if (gotSurfaceCapabilities){
            surfaceCapabilities.free()
        }
        if (gotProperties){
            properties.free()
        }
        if (gotExtensionProperties){
            extensionProperties.free()
        }
        if (gotFeatures){
            features.free()
        }
    }
}