package vulko.device.physical

import org.lwjgl.vulkan.VK10.*

/**
 * Every physical device has an ordered list of queue families.
 * Each queue family can support different operations.
 *
 * Every instance of QueueFamilyIndices will belong to a physical device.
 * The methods of that instance can be used to obtain the index of the recommended queue family for a given operation.
 * The entire list of queue families that support a given operation can be obtained by directly accessing the properties
 * of that instance, the recommended queue family indices will always appear first.
 *
 * Instances of QueueFamilyIndices can be obtained by calling the getQueueFamilyIndices method of a VulkoPhysicalDevice.
 */
class QueueFamilyIndices internal constructor(
    /**
     * A list containing the indices of all queue families that support graphics commands. If there are multiple
     * queue families that support graphics operations, the indices of the 'better' queue families will appear
     * earlier in this list. 'Better' means that the queue support little other commands and that it thus doesn't need
     * to be shared for other commands.
     */
    val graphicsFamilies: List<Int>,
    /**
     * A list containing the indices of all queue families that support present commands. If there are multiple
     * queue families that support present operations, the indices of the 'better' queue families will appear
     * earlier in this list. 'Better' means that the queue support little other commands and that it thus doesn't need
     * to be shared for other commands.
     */
    val presentFamilies: List<Int>,
    /**
     * A list containing the indices of all queue families that support compute commands. If there are multiple
     * queue families that support compute operations, the indices of the 'better' queue families will appear
     * earlier in this list. 'Better' means that the queue support little other commands and that it thus doesn't need
     * to be shared for other commands.
     */
    val computeFamilies: List<Int>,
    /**
     * A list containing the indices of all queue families that support transfer commands. If there are multiple
     * queue families that support transfer operations, the indices of the 'better' queue families will appear
     * earlier in this list. 'Better' means that the queue support little other commands and that it thus doesn't need
     * to be shared for other commands.
     */
    val transferFamilies: List<Int>,
    /**
     * A list containing the indices of all queue families that support sparse commands. If there are multiple
     * queue families that support sparse operations, the indices of the 'better' queue families will appear
     * earlier in this list. 'Better' means that the queue support little other commands and that it thus doesn't need
     * to be shared for other commands.
     */
    val sparseFamilies: List<Int>) {

    override fun toString() : String {
        return "QueueFamilyIndices(graphics: $graphicsFamilies, present: $presentFamilies, compute: $computeFamilies" +
                ", transfer: $transferFamilies, sparse: $sparseFamilies)"
    }

    /**
     * Gets the recommended queue family index to use as present queue
     */
    fun getPresentFamilyIndex() : Int {
        return presentFamilies[0]
    }

    /**
     * Gets the recommended queue family index to use as graphics queue
     */
    fun getGraphicsFamilyIndex() : Int {
        return graphicsFamilies[0]
    }

    /**
     * Gets the recommended queue family index to use as compute queue
     */
    fun getComputeFamilyIndex() : Int {
        return computeFamilies[0]
    }

    /**
     * Gets the recommended queue family index to use as transfer queue
     */
    fun getTransferFamilyIndex() : Int {
        return transferFamilies[0]
    }

    /**
     * Gets the recommended queue family index to use as sparse binding queue
     */
    fun getSparseBindingFamilyIndex() : Int {
        return sparseFamilies[0]
    }

    /**
     * Gets the recommended queue family index to use for the queue with the given bit.
     * For instance, VK_QUEUE_GRAPHICS_BIT or VK_QUEUE_TRANSFER_BIT
     */
    fun getFamilyIndex(queueBit: Int) : Int {
        return when(queueBit) {
            VK_QUEUE_GRAPHICS_BIT -> getGraphicsFamilyIndex()
            VK_QUEUE_COMPUTE_BIT -> getComputeFamilyIndex()
            VK_QUEUE_TRANSFER_BIT -> getTransferFamilyIndex()
            VK_QUEUE_SPARSE_BINDING_BIT -> getSparseBindingFamilyIndex()
            else -> throw IllegalArgumentException("Unknown queue bit $queueBit")
        }
    }
}