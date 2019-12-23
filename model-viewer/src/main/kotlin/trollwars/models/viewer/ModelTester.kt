package trollwars.models.viewer

import vulko.models.util.Resources
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.BufferUtils

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL43.*
import org.lwjgl.system.MemoryUtil.*
import trollwars.models.creatures.RotationSnake
import trollwars.models.creatures.SnakeModelProperties
import trollwars.models.creatures.createModelSnake1
import vulko.memory.MemoryManager
import vulko.models.buffer.*
import java.lang.StrictMath.*
import java.nio.ByteBuffer
import java.util.*

const val WIDTH = 1000
const val HEIGHT = 800

fun printByteBuffer(buffer: ByteBuffer){
    val testFloatArray = FloatArray(min(buffer.capacity() / 4, 200))
    buffer.asFloatBuffer().get(testFloatArray)
    println("Test float array is ${Arrays.toString(testFloatArray)}")

    val testIntArray = IntArray(min(buffer.capacity() / 4, 200))
    buffer.asIntBuffer().get(testIntArray)
    println("Test int array is ${Arrays.toString(testIntArray)}")

    val testByteArray = ByteArray(min(buffer.capacity(), 200))
    buffer.get(testByteArray)
    println("Test byte array is ${Arrays.toString(testByteArray)}")
}

const val FULL_CAPACITY = 100_000_000L

fun main(){

    glfwInit()
    glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API)
    glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GL_TRUE)
    val window = glfwCreateWindow(WIDTH, HEIGHT, "ModelTester", NULL, NULL)
    glfwMakeContextCurrent(window)

    GL.createCapabilities()

    glClearColor(0f, 0f, 1f, 1f)

    val vertexShader = glCreateShader(GL_VERTEX_SHADER)
    glShaderSource(vertexShader, Resources.readResourceAsString("trollwars/models/viewer/shaders/basic.vert", "\n"))
    glCompileShader(vertexShader)
    println("vertex compile info: ${glGetShaderInfoLog(vertexShader)}")

    val fragmentShader = glCreateShader(GL_FRAGMENT_SHADER)
    glShaderSource(fragmentShader, Resources.readResourceAsString("trollwars/models/viewer/shaders/basic.frag", "\n"))
    glCompileShader(fragmentShader)
    println("fragment compile info: ${glGetShaderInfoLog(fragmentShader)}")

    val basicShaderProgram = glCreateProgram()
    glAttachShader(basicShaderProgram, vertexShader)
    glAttachShader(basicShaderProgram, fragmentShader)
    println("basic shader program log: ${glGetProgramInfoLog(basicShaderProgram)}")
    glBindAttribLocation(basicShaderProgram, 0, "modelPosition")
    glBindAttribLocation(basicShaderProgram, 1, "modelNormal")
    glBindAttribLocation(basicShaderProgram, 2, "textureCoords")
    glBindAttribLocation(basicShaderProgram, 3, "matrixIndex")
    glLinkProgram(basicShaderProgram)
    glValidateProgram(basicShaderProgram)

    val uniformBaseMatrix = glGetUniformLocation(basicShaderProgram, "baseMatrix")
    val uniformSubMatrices = glGetUniformLocation(basicShaderProgram, "subMatrices")
    val uniformTextureSampler = glGetUniformLocation(basicShaderProgram, "textureSampler")
    println("Uniforms are $uniformBaseMatrix, $uniformSubMatrices, $uniformTextureSampler")
    println("basic shader program log: ${glGetProgramInfoLog(basicShaderProgram)}")
    println("Error1: ${glGetError()}")

    val vertexArrays = glGenVertexArrays()
    glBindVertexArray(vertexArrays)

    val vertexBuffer = glGenBuffers()
    val indexBuffer = glGenBuffers()

    var indexCount: Int? = null
    var textureID: Int? = null

    var rotation: RotationSnake? = null

    MemoryManager(FULL_CAPACITY).use {memory ->
        memory.claimStack(FULL_CAPACITY, "snake1 stack").use { stack ->
            rotation = createModelSnake1(stack, SnakeModelProperties()){modelBuffer ->
                indexCount = modelBuffer.indexCount

                glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer)
                glBufferData(GL_ARRAY_BUFFER, modelBuffer.createBackingVertexBuffer(), GL_STATIC_DRAW)

                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer)
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, modelBuffer.createBackingIndexBuffer(), GL_STATIC_DRAW)

                textureID = glGenTextures()

                glBindTexture(GL_TEXTURE_2D, textureID!!)
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, modelBuffer.textureWidth.toInt(), modelBuffer.textureHeight.toInt(), 0, GL_RGBA, GL_UNSIGNED_BYTE, modelBuffer.createBackingTextureBuffer())
                printByteBuffer(modelBuffer.createBackingTextureBuffer())
            }
        }
    }

    glUseProgram(basicShaderProgram)
    rotation!!.resetRotation()
    val camera = Camera(rotation!!, uniformSubMatrices)
    val transformMatrix = Matrix4f().translate(1.1f, 0.1f, 0.1f)
    val baseMatrix = Matrix4f()

    fun updateSubMatrices(){
        rotation!!.updateRotation()
        glUniformMatrix4fv(uniformSubMatrices, false, rotation!!.getBackingMatrixBuffer())
    }

    rotation!!.resetRotation()
    updateSubMatrices()

    val baseMatrixBuffer = BufferUtils.createFloatBuffer(16)
    baseMatrixBuffer.limit(16)

    fun updateBaseMatrix(){
        camera.processInput(window)
        camera.put(baseMatrix)
        baseMatrix.mul(transformMatrix)
        baseMatrix.get(baseMatrixBuffer)
        glUniformMatrix4fv(uniformBaseMatrix, false, baseMatrixBuffer)
    }

    println("Error2: ${glGetError()}")
    glUniform1i(uniformTextureSampler, 0)
    println("Error3: ${glGetError()}")
    glEnable(GL_DEPTH_TEST)
    glEnableVertexAttribArray(0)
    glEnableVertexAttribArray(1)
    glEnableVertexAttribArray(2)
    glEnableVertexAttribArray(3)
    glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer)
    glVertexAttribPointer(0, 3, GL_FLOAT, false, BYTES_PER_VERTEX, OFFSET_POSITIONS.toLong())
    glVertexAttribPointer(1, 3, GL_FLOAT, false, BYTES_PER_VERTEX, OFFSET_NORMALS.toLong())
    glVertexAttribPointer(
        2, 2, GL_FLOAT, false,
        BYTES_PER_VERTEX,
        OFFSET_TEXTURE_COORDS.toLong()
    )
    glVertexAttribIPointer(3, 1, GL_INT, BYTES_PER_VERTEX, OFFSET_MATRIX.toLong())

    println("Error4: ${glGetError()}")

    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer)

    glActiveTexture(GL_TEXTURE0)
    glBindTexture(GL_TEXTURE_2D, textureID!!)

    val widthBuffer = BufferUtils.createIntBuffer(1)
    val heightBuffer = BufferUtils.createIntBuffer(1)

    while (!glfwWindowShouldClose(window)) {
        updateBaseMatrix()
        glfwGetFramebufferSize(window, widthBuffer, heightBuffer)
        glViewport(0, 0, widthBuffer[0], heightBuffer[0])
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        glDrawElements(GL_TRIANGLES, indexCount!!, GL_UNSIGNED_INT, 0)
        glfwSwapBuffers(window)
        glfwPollEvents()
        if (glfwGetKey(window, GLFW_KEY_X) == GLFW_PRESS){
            rotation!!.resetRotation()
            updateSubMatrices()
        }
        Thread.sleep(15)
    }

    glDisableVertexAttribArray(3)
    glDisableVertexAttribArray(2)
    glDisableVertexAttribArray(1)
    glDisableVertexAttribArray(0)

    glDeleteTextures(textureID!!)

    glDeleteVertexArrays(vertexArrays)
    glDeleteBuffers(vertexBuffer)
    glDeleteBuffers(indexBuffer)

    glUseProgram(0)
    glDetachShader(basicShaderProgram, vertexShader)
    glDetachShader(basicShaderProgram, fragmentShader)
    glDeleteShader(vertexShader)
    glDeleteShader(fragmentShader)
    glDeleteProgram(basicShaderProgram)

    glfwDestroyWindow(window)
    glfwTerminate()
}

private class Camera(val rotation: RotationSnake, val uniformSubMatrices: Int) {

    private val projection: Matrix4f = Matrix4f().perspective(70f, WIDTH.toFloat() / HEIGHT.toFloat(), 0.01f, 10_000f)

    var x = 0f
    var y = 0f
    var z = 2f

    var pitch = 0f
    var yaw = 0f

    private val forwardVector = Vector3f(0f, 0f, -1f)
    private val rightVector = Vector3f(1f, 0f, 0f)
    private val upVector = Vector3f(0f, 1f, 0f)

    private val mouseBufferX = BufferUtils.createDoubleBuffer(1)
    private val mouseBufferY = BufferUtils.createDoubleBuffer(1)

    fun put(matrix: Matrix4f){
        matrix.identity()
        matrix.rotate(pitch, 1f, 0f, 0f)
        matrix.rotate(yaw, 0f, 1f, 0f)
        matrix.translate(-x, -y, -z)
        projection.mul(matrix, matrix)
    }

    fun processInput(window: Long){
        val speed = 0.05f
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
            x -= rightVector.x * speed
            y -= rightVector.y * speed
            z -= rightVector.z * speed
        }
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS){
            x += rightVector.x * speed
            y += rightVector.y * speed
            z += rightVector.z * speed
        }
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS){
            x += forwardVector.x * speed
            y += forwardVector.y * speed
            z += forwardVector.z * speed
        }
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS){
            x -= forwardVector.x * speed
            y -= forwardVector.y * speed
            z -= forwardVector.z * speed
        }
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS){
            x += upVector.x * speed
            y += upVector.y * speed
            z += upVector.z * speed
        }
        if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS){
            x -= upVector.x * speed
            y -= upVector.y * speed
            z -= upVector.z * speed
        }

        val mouseSpeed = 0.01f
        val prevMouseX = mouseBufferX[0]
        val prevMouseY = mouseBufferY[0]
        glfwGetCursorPos(window, mouseBufferX, mouseBufferY)
        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
            if (glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS){
                rotation.rotateTailVertically((mouseBufferY[0] - prevMouseY).toFloat() * -0.01f)
                rotation.rotateTailHorizontally((mouseBufferX[0] - prevMouseX).toFloat() * -0.01f)
                rotation.updateRotation()
                glUniformMatrix4fv(uniformSubMatrices, false, rotation.getBackingMatrixBuffer())
            } else {
                pitch += (mouseBufferY[0] - prevMouseY).toFloat() * mouseSpeed
                yaw += (mouseBufferX[0] - prevMouseX).toFloat() * mouseSpeed
                forwardVector.set(0f, 0f, -1f).rotateX(-pitch).rotateY(-yaw)
                rightVector.set(1f, 0f, 0f).rotateX(-pitch).rotateY(-yaw)
                upVector.set(0f, 1f, 0f).rotateX(-pitch).rotateY(-yaw)
            }
        }
    }
}