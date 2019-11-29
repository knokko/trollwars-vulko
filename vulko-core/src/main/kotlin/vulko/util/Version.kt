package vulko.util

import org.lwjgl.vulkan.VK10.VK_MAKE_VERSION

/**
 * This class represents an application/library version.
 *
 * When an instance of this class is created, VK_MAKE_VERSION will be used to convert the parameters into a version
 * number. This class is used as parameter rather than just *int* in Vulko methods to have some extra type safety.
 */
class Version(major: Int, minor: Int, patch: Int){

    val vulkanVersion = VK_MAKE_VERSION(major, minor, patch)
}