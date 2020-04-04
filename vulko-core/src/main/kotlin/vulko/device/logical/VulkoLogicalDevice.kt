package vulko.device.logical

import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK10.*
import vulko.device.physical.VulkoPhysicalDevice
import vulko.swapchain.VulkoSwapchain
import vulko.util.assertSuccess
import java.lang.Math.max
import java.lang.Math.min
import java.nio.IntBuffer
import java.util.*

/**
 * Represents a VulKan (logical) device.
 *
 * To obtain an instance of this class, call the createLogicalDevice method of a VulkoPhysicalDevice.
 */
class VulkoLogicalDevice(
    /**
     * A refence to the VkDevice associated with this VulkoLogicalDevice.
     */
    val vulkanDevice: VkDevice,

    /**
     * This maps each queue family index supplied to the createLogicalDevice method call that created this
     * VulkoLogicalDevice to to the queue that was created for it.
     */
    val queueMap: Map<Int, VulkoQueue>,

    /**
     * A reference to the VulkoPhysicalDevice that created this VulkoLogicalDevice
     */
    val physicalDevice: VulkoPhysicalDevice) {

    private var destroyed = false

    /**
     * The list containing all swapchains created by the createSwapchain() method of this device that are not (yet)
     * destroyed.
     */
    internal val swapchains = LinkedList<VulkoSwapchain>()

    override fun toString() : String {
        return "LogicalDevice(vulkanDevice: $vulkanDevice, physical device: $physicalDevice)"
    }

    /**
     * Creates and returns a newly created swapchain using the given parameters.
     *
     * The returned swapchain will automatically be destroyed when this device is destroyed, but it can be manually
     * destroyed earlier.
     */
    fun createSwapchain(
        /** The index of the graphics family to use for the swapchain */
        graphicsFamilyIndex: Int,
        /** The index of the present family to use for the swapchain */
        presentFamilyIndex: Int,
        /**
         * The so-called present mode picker. It must be a lambda expression taking an IntBuffer and returning an Int.
         *
         * The lambda will be called once and the input IntBuffer will be an IntBuffer containing all available present
         * modes for the swapchain. It should return one of the present modes in the given IntBuffer. That present mode
         * will then be used for the swapchain.
         *
         * The given IntBuffer can be read from and written to during the execution of the lambda, not NOT thereafter!
         * Using it after the call (for instance by storing it somewhere) will cause undefined behavior.
         *
         * If no presentModePicker is specified, the default picker will be used, which will return
         * VK_PRESENT_MODE_MAILBOX_KHR if available and otherwise VK_PRESENT_MODE_FIFO_KHR.
         */
        presentModePicker: PresentModePicker = DEFAULT_PRESENT_MODE_PICKER,
        /**
         * The so-called surface format picker. It must be a lambda expression taking a VkSurfaceFormatKHR.Buffer as
         * input and returning a VkSurfaceFormatKHR.
         *
         * The lambda will be called once and the input buffer will be a VkSurfaceFormatKHR.Buffer containing all
         * available surface formats for the physical device and surface. The lambda must return a single
         * VkSurfaceFormatKHR that was in the buffer. That returned surface format will then be used for the swapchain.
         *
         * The given VkSurfaceFormatKHR.Buffer can be read from and written to during the execution of the lambda, but
         * NOT thereafter! Using it after the call (for instance by storing it somewhere) will cause undefined behavior.
         *
         * If no surfaceFormatPicker is specified, the default picker will be used, which will try to find a surface
         * format with format VK_FORMAT_B8G8R8A8_UNORM. If no was found, it will return the first surface format in the
         * buffer. If multiple were found, it will try to pick one of them with color space
         * VK_COLOR_SPACE_SRGB_NONLINEAR_KHR. If none of them has it, the first surface format with format
         * VK_FORMAT_B8G8R8A8_UNORM will be returned.
         */
        surfaceFormatPicker: SurfaceFormatPicker = DEFAULT_SURFACE_FORMAT_PICKER,
        /**
         * The so-called image size chooser. It must be a lambda expression taking 6 Int's and returning 2 Int's.
         *
         * The lambda will be called once if the width and height of the current extent of the surface capabilities of
         * the physical device aren't fixed (when the width of the extent equals -1). Otherwise, the lambda will not be
         * called at all.
         *
         * The input integers will be the (minWidth, maxWidth, windowWidth, minHeight, maxHeight, windowHeight)
         * explicitly in that order.
         * The minWidth and maxWidth are the minimum and maximum width of the image extent for the surface capabilities.
         * Similarly, minHeight and maxHeight are the minimum and maximum height of the image extent for the surface
         * capabilities. The windowWidth and windowHeight are the (current) width and height of the window.
         *
         * The returned integers are (width, height) where width and height will be used as the image extent of the
         * swapchain. It must hold that minWidth <= width <= maxWidth and minHeight <= height <= maxHeight.
         *
         * If no imageSizeChooser is specified, the default image size chooser will be used, which will return
         * (windowWidth, windowHeight) is they are between minWidth and maxWidth and minHeight and maxHeight. Otherwise,
         * it will clamp (windowWidth, windowHeight) to the allowed range.
         */
        imageSizeChooser: ImageSizeChooser = DEFAULT_IMAGE_SIZE_CHOOSER,
        /**
         * The image usage of the swapchain images, it will be supplied to the imageUsage field of the
         * VkSwapchainCreateInfoKHR.
         *
         * If no imageUsage is specified, the default value of VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT will be used.
         */
        imageUsage: Int = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT,
        /**
         * The value to be supplied to the imageArrayLayers field of the VkSwapchainCreateInfoKHR.
         *
         * If no imageArrayLayers is specified, the default value of 1 will be used.
         */
        imageArrayLayers: Int = 1) : VulkoSwapchain {
        return stackPush().use {stack ->
            val surfaceCaps = physicalDevice.surfaceCapabilities

            val imageWidth: Int;
            val imageHeight: Int;

            // The maximum value of uint will be -1 if converted to int
            // That special value indicates that indicates that the width and height aren't fixed
            if (surfaceCaps.currentExtent().width() != -1){
                imageWidth = surfaceCaps.currentExtent().width()
                imageHeight = surfaceCaps.currentExtent().height()
            } else {
                val imageSize = imageSizeChooser(
                    surfaceCaps.minImageExtent().width(), surfaceCaps.maxImageExtent().width(), physicalDevice.instance.window.windowWidth,
                    surfaceCaps.minImageExtent().height(), surfaceCaps.maxImageExtent().height(), physicalDevice.instance.window.windowHeight)
                imageWidth = imageSize.first
                imageHeight = imageSize.second
            }

            val chosenSurfaceFormat = surfaceFormatPicker(physicalDevice.surfaceFormats)
            val chosenPresentMode = presentModePicker(physicalDevice.surfacePresentModes)

            var imageCount = surfaceCaps.minImageCount()

            // If maxImageCount is 0, there is no maximum
            // It is recommended to have at least 1 more image than the minimum
            if (surfaceCaps.maxImageCount() == 0 || surfaceCaps.maxImageCount() > imageCount){
                imageCount++;
            }

            val swapchainCI = VkSwapchainCreateInfoKHR.callocStack(stack)
            swapchainCI.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
            swapchainCI.surface(physicalDevice.instance.surfaceHandle)
            swapchainCI.minImageCount(imageCount)
            swapchainCI.imageFormat(chosenSurfaceFormat.format())
            swapchainCI.imageColorSpace(chosenSurfaceFormat.colorSpace())
            swapchainCI.imageExtent(VkExtent2D.mallocStack(stack).set(imageWidth, imageHeight))
            swapchainCI.imageArrayLayers(imageArrayLayers)
            swapchainCI.imageUsage(imageUsage)

            if (graphicsFamilyIndex == presentFamilyIndex){
                swapchainCI.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
            } else {
                swapchainCI.imageSharingMode(VK_SHARING_MODE_CONCURRENT)
                swapchainCI.pQueueFamilyIndices(stack.ints(graphicsFamilyIndex, presentFamilyIndex))
            }

            swapchainCI.preTransform(surfaceCaps.currentTransform())
            swapchainCI.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
            swapchainCI.presentMode(chosenPresentMode)
            swapchainCI.clipped(true)
            swapchainCI.oldSwapchain(VK_NULL_HANDLE)

            val pSwapchain = stack.mallocLong(1)
            assertSuccess(vkCreateSwapchainKHR(vulkanDevice, swapchainCI, null, pSwapchain))
            val swapchainHandle = pSwapchain[0]

            val pImageCount = stack.mallocInt(1)
            assertSuccess(vkGetSwapchainImagesKHR(vulkanDevice, swapchainHandle, pImageCount, null))
            val pImages = memAllocLong(pImageCount[0])
            assertSuccess(vkGetSwapchainImagesKHR(vulkanDevice, swapchainHandle, pImageCount, pImages))

            val pImageViews = memAllocLong(pImageCount[0])
            for (index in 0 until pImageCount[0]) {
                stackPush().use {innerStack ->
                    val imageViewCI = VkImageViewCreateInfo.callocStack(innerStack)

                    imageViewCI.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    imageViewCI.image(pImageViews[index])
                    imageViewCI.viewType(VK_IMAGE_VIEW_TYPE_2D)
                    imageViewCI.format(chosenSurfaceFormat.format())

                    imageViewCI.components(VkComponentMapping.callocStack(innerStack).set(
                            VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY,
                            VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY
                    ))

                    imageViewCI.subresourceRange(VkImageSubresourceRange.callocStack(innerStack).set(
                            VK_IMAGE_ASPECT_COLOR_BIT,
                            0, 1,
                            0, 1
                    ))

                    pImageViews.position(index)
                    assertSuccess(vkCreateImageView(vulkanDevice, imageViewCI, null, pImageViews))
                }
            }

            pImageViews.position(0)

            val swapchain = VulkoSwapchain(this, swapchainHandle, pImages, pImageViews,
                    imageWidth, imageHeight, chosenPresentMode, chosenSurfaceFormat)
            swapchains.add(swapchain)
            swapchain
        }
    }

    fun isDestroyed() : Boolean {
        return destroyed
    }

    /**
     * Destroys this VulkoLogicalDevice along with its swapchains if it has not yet been destroyed.
     */
    fun destroy(){
        if (!destroyed) {
            destroyed = true
            for (swapchain in swapchains) {
                swapchain.destroy()
            }
            vkDestroyDevice(vulkanDevice, null)
        }
    }
}

typealias PresentModePicker = (IntBuffer) -> Int

val DEFAULT_PRESENT_MODE_PICKER = lambda@{presentModes: IntBuffer ->
    for (index in 0 until presentModes.limit()){
        val presentMode = presentModes[index]
        println("Present mode is $presentMode")
        if (presentMode == VK_PRESENT_MODE_MAILBOX_KHR){
            return@lambda presentMode
        }
    }
    VK_PRESENT_MODE_FIFO_KHR
}

typealias SurfaceFormatPicker = (VkSurfaceFormatKHR.Buffer) -> VkSurfaceFormatKHR

val DEFAULT_SURFACE_FORMAT_PICKER: SurfaceFormatPicker = lambda@{formats ->
    var bestFormat = formats[0]
    for (format in formats){

        // The if is to skip the first iteration
        if (format != bestFormat){

            // The 'best' format is probably VK_FORMAT_B8G8R8A8_UNORM
            if (format.format() == VK_FORMAT_B8G8R8A8_UNORM && bestFormat.format() != VK_FORMAT_B8G8R8A8_UNORM){
                bestFormat = format
            } else if (bestFormat.format() != VK_FORMAT_B8G8R8A8_UNORM){

                // The 'best' color space is probably VK_COLOR_SPACE_SRGB_NONLINEAR_KHR
                if (format.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR && bestFormat.colorSpace() != VK_COLOR_SPACE_SRGB_NONLINEAR_KHR){
                    bestFormat = format
                }
            }
        }
    }
    bestFormat
}

typealias ImageSizeChooser = (minWidth: Int, maxWidth: Int, windowWidth: Int, minHeight: Int, maxHeight: Int, windowHeight: Int) -> Pair<Int,Int>

val DEFAULT_IMAGE_SIZE_CHOOSER: ImageSizeChooser = { minWidth, maxWidth, windowWidth, minHeight, maxHeight, windowHeight ->
    val imageWidth = max(minWidth, min(maxWidth, windowWidth))
    val imageHeight = max(minHeight, min(maxHeight, windowHeight))
    Pair(imageWidth, imageHeight)
}