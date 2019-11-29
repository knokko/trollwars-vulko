package vulko.device.logical

import org.lwjgl.vulkan.*

/**
 * Represents a VulKan queue of a logical device.
 *
 * To obtain an instance of this class, use the queueMap of a VulkoLogicalDevice.
 */
class VulkoQueue(
    /**
     * A reference to the VkQueue backed by this VulkoQueue
     */
    val vulkanQueue: VkQueue) {

    override fun toString() : String {
        return "Queue($vulkanQueue)"
    }

    override fun equals(other: Any?): Boolean {
        if (other is VulkoQueue){
            return other.vulkanQueue === vulkanQueue
        } else {
            return false
        }
    }
}