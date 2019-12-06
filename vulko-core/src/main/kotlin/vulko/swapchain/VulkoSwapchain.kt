package vulko.swapchain

import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.KHRSwapchain.vkDestroySwapchainKHR
import org.lwjgl.vulkan.VkSurfaceFormatKHR
import vulko.device.logical.VulkoLogicalDevice
import java.nio.LongBuffer

class VulkoSwapchain internal constructor(private val device: VulkoLogicalDevice,
                                          val handle: Long, val images: LongBuffer,
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
            memFree(images)

            // TODO Destroy the swapchain
            vkDestroySwapchainKHR(device.vulkanDevice, handle, null)
        }
    }
}