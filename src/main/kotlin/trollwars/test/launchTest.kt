package trollwars.test

import vulko.instance.InstanceBuilder
import org.lwjgl.glfw.GLFW
import vulko.util.Version
import vulko.window.WindowBuilder

fun main(){
    val windowBuilder = WindowBuilder(title="Hello Vulko", width=1000, height=500)
    val window = windowBuilder.build()

    val instanceBuilder = InstanceBuilder(true, "VulkoTester", Version(1, 0, 0))
    val instance = window.createInstance(instanceBuilder)
    val bestPhysicalDevice = instance.choosePhysicalDevice()
    println("best device is $bestPhysicalDevice")
    val device = bestPhysicalDevice!!.createLogicalDevice()
    val queueFamIndices = bestPhysicalDevice.queueFamilyIndices
    val graphicsFamilyIndex = queueFamIndices.getGraphicsFamilyIndex()
    val presentFamilyIndex = queueFamIndices.getPresentFamilyIndex()
    val graphicsQueue = device.queueMap[graphicsFamilyIndex]
    println("graphicsQueue is $graphicsQueue")
    val presentQueue = device.queueMap[presentFamilyIndex]
    println("presentQueue is $presentQueue")
    val swapchain = device.createSwapchain(graphicsFamilyIndex, presentFamilyIndex)
    println("The swapchain has become $swapchain")

    val resizeListener = {newWidth: Int, newHeight: Int ->
        println("Resized to ($newWidth, $newHeight)")
    }
    window.addResizeListener(resizeListener)

    while (!GLFW.glfwWindowShouldClose(window.handle)){
        GLFW.glfwPollEvents()
        Thread.sleep(100)
    }

    window.destroy()
}