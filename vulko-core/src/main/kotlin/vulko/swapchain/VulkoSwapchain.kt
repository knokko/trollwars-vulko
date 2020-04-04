package vulko.swapchain

import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.KHRSwapchain.vkDestroySwapchainKHR
import org.lwjgl.vulkan.VK10.vkDestroyImageView
import org.lwjgl.vulkan.VkSurfaceFormatKHR
import vulko.device.logical.VulkoLogicalDevice
import java.nio.LongBuffer

class VulkoSwapchain (private val device: VulkoLogicalDevice,
                                          val handle: Long, val images: LongBuffer,
                                          val imageViews: LongBuffer,
                                          val imageWidth: Int, val imageHeight: Int,
                                          val presentMode: Int, val surfaceFormat: VkSurfaceFormatKHR) {

    private var destroyed = false

    override fun toString() : String {
        return "Swapchain(handle: $handle, image size: ($imageWidth, $imageHeight), present mode: $presentMode, $surfaceFormat)"
    }

    fun destroy(){
        if (!destroyed) {
            destroyed = true
            device.swapchains.remove(this)

            for (index in 0 until imageViews.capacity()) {
                val imageView = imageViews[index];
                vkDestroyImageView(device.vulkanDevice, imageView, null)
            }

            memFree(images)
            memFree(imageViews)

            vkDestroySwapchainKHR(device.vulkanDevice, handle, null)
        }
    }
}