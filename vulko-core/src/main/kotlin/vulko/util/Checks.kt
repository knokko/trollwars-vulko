package vulko.util

import org.lwjgl.vulkan.VK10.VK_SUCCESS

/**
 * A simple helper function that will throw a RuntimeException if the parameter is not VK_SUCCESS.
 *
 * It is used to assert that the return codes from all Vulkan calls succeeded.
 */
fun assertSuccess(returnCode: Int){
    if (returnCode != VK_SUCCESS){
        throw RuntimeException("Return code should have been VK_SUCCESS ($VK_SUCCESS), but was $returnCode")
    }
}