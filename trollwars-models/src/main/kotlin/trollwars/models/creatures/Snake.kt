package trollwars.models.creatures

import org.joml.Matrix4f
import org.lwjgl.system.MemoryUtil.memByteBuffer
import trollwars.models.MODEL_MEMORY_MANAGER
import trollwars.textures.creatures.createSnake1Properties
import trollwars.textures.creatures.snakePatternToTailTexture
import vulko.memory.VirtualStack
import vulko.models.buffer.BasicModelBuffer
import vulko.models.building.BasicModelBuilder
import vulko.models.building.STEP_SIZE
import vulko.models.building.TRIANGLE_BYTES
import java.io.Closeable
import java.lang.Math.sqrt
import java.nio.FloatBuffer
import kotlin.math.pow

class SnakeModelProperties(val tailLength: Float = 2.5f, val tailRadius: Float = 0.1f,
                           val tailParts: Int = 20, val verticesPerRing: Int = 20,
                           val tailRadiusFunction: (Float) -> Float = DEFAULT_TAIL_RADIUS_FUNCTION)

val DEFAULT_TAIL_RADIUS_FUNCTION: (Float) -> Float = {progress ->
    (progress + 0.1f).pow(0.4f)
}

fun createModelSnake1(stack: VirtualStack, modelProps: SnakeModelProperties, user: ModelUser) : RotationSnake {
    val vertexCapacity = modelProps.tailParts * modelProps.verticesPerRing * STEP_SIZE * 3 / 2
    val indexCapacity = (modelProps.tailParts - 1) * (modelProps.verticesPerRing - 1) * 2 * TRIANGLE_BYTES
    stack.pushForChunk(vertexCapacity.toLong()).use { vertexChunk ->
        stack.pushForChunk(indexCapacity.toLong()).use { indexChunk ->
            val textureProps = createSnake1Properties(stack)

            stack.pushForChildStack().use { childStack ->
                val builder = BasicModelBuilder("snake1", 0, vertexChunk, indexChunk, childStack)

                val vertices = builder.vertexBuilder
                val indices = builder.indexBuilder
                val textureSpace = builder.claimTextureSpace(textureProps.getFullWidth(), textureProps.getFullHeight())
                snakePatternToTailTexture(textureSpace.texture, textureProps)

                val textureID = textureSpace.id
                val textureWidth = textureSpace.texture.width
                val textureHeight = textureSpace.texture.height

                val tailMatrices = Array(modelProps.tailParts) { builder.getNextMatrixID() }

                for (length in 0 until modelProps.tailParts) {
                    val currentRadius = modelProps.tailRadius * modelProps.tailRadiusFunction(length / (modelProps.tailParts - 1f))
                    for (angleI in 0..modelProps.verticesPerRing) {
                        val angle = angleI * 2.0 * StrictMath.PI / modelProps.verticesPerRing.toDouble()
                        vertices.add(
                            StrictMath.cos(angle).toFloat() * currentRadius,
                            StrictMath.sin(angle).toFloat() * currentRadius,
                            0f,
                            StrictMath.cos(angle).toFloat(),
                            StrictMath.sin(angle).toFloat(),
                            0f,
                            textureID,
                            length * textureWidth / modelProps.tailParts,
                            angleI * textureHeight / modelProps.verticesPerRing,
                            tailMatrices[length]
                        )
                    }
                }

                for (height in 1 until modelProps.tailParts) {
                    val prevHeight = height - 1
                    for (angleI in 1..modelProps.verticesPerRing) {
                        val prevAngle = angleI - 1
                        val vertex1 = prevAngle + prevHeight * (modelProps.verticesPerRing + 1)
                        val vertex2 = angleI + prevHeight * (modelProps.verticesPerRing + 1)
                        val vertex3 = angleI + height * (modelProps.verticesPerRing + 1)
                        val vertex4 = prevAngle + height * (modelProps.verticesPerRing + 1)
                        indices.bindTriangle(vertex1, vertex2, vertex3)
                        indices.bindTriangle(vertex3, vertex4, vertex1)
                    }
                }

                textureSpace.texture.saveAsImage("pre build texture")

                val buffer = builder.build()

                user(buffer)

                // By closing the use-statements, the memory is returned
                return RotationSnake(tailMatrices, modelProps.tailLength / modelProps.tailParts)
            }
        }
    }
}

typealias ModelUser = (BasicModelBuffer) -> Unit

class RotationSnake(private val tailMatrices: Array<Int>, private val partLength: Float) : Closeable {

    private val baseMatrixIndex = (tailMatrices.size - 1) * 4 / 5

    private val globalMatrixData = MODEL_MEMORY_MANAGER.claimChunk(64L * tailMatrices.size, "Snake rotation global buffer")
    private val globalMatrixBuffer = memByteBuffer(globalMatrixData.address, globalMatrixData.size.toInt()).asFloatBuffer()

    private val localMatrixData = MODEL_MEMORY_MANAGER.claimChunk(64L * tailMatrices.size, "Snake rotation local buffer")
    private val localMatrixBuffer = memByteBuffer(localMatrixData.address, localMatrixData.size.toInt()).asFloatBuffer()

    private var needsUpdate = true
    private var closed = false

    /**
     * Make sure to call updateRotation() every time before using the returned address!
     */
    fun getMatrixBufferAddress() : Long {
        return globalMatrixData.address
    }

    /**
     * Make sure to call updateRotation() every time before using the returned buffer!
     */
    fun getBackingMatrixBuffer() : FloatBuffer {
        return globalMatrixBuffer
    }

    /**
     * Ensures that the matrices stored at getMatrixBufferAddress() are up to date
     */
    fun updateRotation(){

        if (needsUpdate) {

            // Obtain the base matrix
            localMatrixBuffer.position(tailMatrices[baseMatrixIndex] * 16)
            val baseMatrix = Matrix4f(localMatrixBuffer)

            // The baseMatrix should be the same in the local and global buffer
            baseMatrix.get(tailMatrices[baseMatrixIndex] * 16, globalMatrixBuffer)

            val prevMatrix = Matrix4f(baseMatrix)
            val nextMatrix = Matrix4f()

            // Update the lower indices (towards the end of the tail)
            for (tailIndex in baseMatrixIndex - 1 downTo 0){

                // Load the next local matrix into nextMatrix
                localMatrixBuffer.position(tailMatrices[tailIndex] * 16)
                nextMatrix.set(localMatrixBuffer)

                // Store nextMatrix * prevMatrix into prevMatrix
                nextMatrix.mul(prevMatrix, prevMatrix)

                // Store nextMatrix * prevMatrix into the global buffer
                globalMatrixBuffer.position(tailMatrices[tailIndex] * 16)
                prevMatrix.get(globalMatrixBuffer)
            }

            // Update the higher indices (towards the head)
            prevMatrix.set(baseMatrix)
            for (tailIndex in baseMatrixIndex + 1 until tailMatrices.size){

                // Load the next local matrix into nextMatrix
                localMatrixBuffer.position(tailMatrices[tailIndex] * 16)
                nextMatrix.set(localMatrixBuffer)

                // Store nextMatrix * prevMatrix into prevMatrix
                nextMatrix.mul(prevMatrix, prevMatrix)

                // Store nextMatrix * prevMatrix into the global buffer
                globalMatrixBuffer.position(tailMatrices[tailIndex] * 16)
                prevMatrix.get(globalMatrixBuffer)
            }

            // Set the positions of the matrix buffers back to 0
            localMatrixBuffer.position(0)
            globalMatrixBuffer.position(0)

            needsUpdate = false
        }
    }

    /**
     * (Re)sets the rotation such that the snake will lie in a straight line
     */
    fun resetRotation(){

        val theMatrix = Matrix4f()

        // Set the base matrix to the identity matrix
        theMatrix.get(tailMatrices[baseMatrixIndex] * 16, localMatrixBuffer)

        // Set the tail parts going towards the head to straight forward
        theMatrix.translate(0f, 0f, partLength)
        for (matrixIndex in baseMatrixIndex + 1 until tailMatrices.size){
            theMatrix.get(tailMatrices[matrixIndex] * 16, localMatrixBuffer)
        }

        // Set the tail parts going towards the tail end to straight backward
        theMatrix.translate(0f, 0f, -2 * partLength)
        for (matrixIndex in baseMatrixIndex - 1 downTo 0){
            theMatrix.get(tailMatrices[matrixIndex] * 16, localMatrixBuffer)
        }

        needsUpdate = true
    }

    fun rotateTailHorizontally(totalAngle: Float){
        val anglePerPart = totalAngle / baseMatrixIndex

        val matrix = Matrix4f()
        for (tailIndex in 0 until baseMatrixIndex){
            localMatrixBuffer.position(tailMatrices[tailIndex] * 16)
            matrix.set(localMatrixBuffer)
            matrix.rotateY(anglePerPart)
            matrix.get(localMatrixBuffer)
        }

        localMatrixBuffer.position(0)
        needsUpdate = true
    }

    fun rotateTailVertically(totalAngle: Float){
        val anglePerPart = totalAngle / baseMatrixIndex

        val matrix = Matrix4f()
        for (tailIndex in 0 until baseMatrixIndex){
            localMatrixBuffer.position(tailMatrices[tailIndex] * 16)
            matrix.set(localMatrixBuffer)
            matrix.rotateX(anglePerPart)
            matrix.get(localMatrixBuffer)
        }

        localMatrixBuffer.position(0)
        needsUpdate = true
    }

    override fun close(){
        if (!closed) {
            globalMatrixData.close()
            localMatrixData.close()
            closed = true
        }
    }
}