package vulko.models.building

import vulko.memory.MemoryChunk
import vulko.memory.MemoryManager
import vulko.memory.VirtualStack
import vulko.models.buffer.BasicModelBuffer
import vulko.textures.TextureBuilder
import vulko.util.math.isPowerOf2
import vulko.util.math.nextPowerOf2
import java.lang.StrictMath.*
import java.util.*

class BasicModelBuilder<M>(val name: String, val modelData: M,
                           val vertexChunk: MemoryChunk, val indexChunk: MemoryChunk, val textureStack: VirtualStack) : AutoCloseable {

    val vertexBuilder = createBasicVertexBufferAt(vertexChunk.address, vertexChunk.address + vertexChunk.size)
    val indexBuilder = createBasicIndexBufferAt(indexChunk.address, indexChunk.address + indexChunk.size)
    private val textureList = ArrayList<TextureEntry>()
    private var nextMatrixID = 0

    constructor(name: String, modelData: M, manager: MemoryManager,
                vertexCapacity: Int, indexCapacity: Int, textureCapacity: Int) : this(name, modelData,
        manager.claimChunk(vertexCapacity.toLong(), "vertices for $name"),
        manager.claimChunk(indexCapacity.toLong(), "indices for $name"),
        manager.claimStack(textureCapacity.toLong(), "texture for $name")
    )

    fun claimTextureSpace(width: Long, height: Long) : TextureEntry {
        val capacity = width.toLong() * height.toLong() * 4L
        val textureAddress = textureStack.push(capacity)
        val newTexture = TextureBuilder(textureAddress, width, height)
        val textureID = textureList.size
        val newEntry = TextureEntry(newTexture, textureID)
        textureList.add(newEntry)
        return newEntry
    }

    fun getTextureBuilder(id: Int) : TextureEntry {
        return textureList[id]
    }

    fun getNextMatrixID() : Int {
        return nextMatrixID++
    }

    /**
     * The returned BasicModelBuffer will become invalid when this BasicModelBuilder is closed!
     */
    fun build() : BasicModelBuffer {

        val fullTexture: TextureBuilder

        // The first part of building is combining all texture fragments into a single texture
        run {

            if (textureList.size != 1) {
                var totalArea = 0L
                var largestWidth = 0L
                for (entry in textureList) {
                    totalArea += entry.texture.width * entry.texture.height
                    if (entry.texture.width > largestWidth){
                        largestWidth = entry.texture.width
                    }
                }

                // The width and height should become roughly the same
                // The width will initially be bigger because the height will be increasing if things don't fit perfectly
                val sqrtArea = sqrt(totalArea.toDouble())
                val width = max(largestWidth, nextPowerOf2(sqrtArea.toLong()))

                // There are probably better algorithms for this, but this should be reasonable enough
                val heightSorted = ArrayList<TextureEntry>(textureList)
                heightSorted.sortBy { it.texture.height }

                var localHeight = 0L
                var currentX = 0L
                var currentY = 0L
                for (entry in heightSorted) {

                    entry.offsetX = currentX
                    entry.offsetY = currentY
                    currentX += entry.texture.width
                    if (currentX > width) {
                        currentX = entry.texture.width
                        currentY += localHeight
                        entry.offsetX = 0
                        entry.offsetY = currentY
                    }

                    // The entries are sorted by height, so don't bother checking if its not smaller than the previous value
                    localHeight = entry.texture.height
                }

                // Height (and width) must be a power of 2
                val height = nextPowerOf2(currentY + localHeight)
                val fullTextureCapacity = 4L * width.toLong() * height.toLong()
                val fullTextureAddress = textureStack.push(fullTextureCapacity)
                fullTexture = TextureBuilder(fullTextureAddress, width, height)

                fullTexture.clearColor(200, 0, 200)

                for (entry in textureList) {
                    entry.texture.copy(fullTexture, entry.offsetX!!, entry.offsetY!!)
                }
            } else {
                val texture = textureList[0].texture
                if (isPowerOf2(texture.width) && isPowerOf2(texture.height)){
                    fullTexture = texture;
                } else {
                    val width = nextPowerOf2(texture.width)
                    val height = nextPowerOf2(texture.height)
                    val fullTextureCapacity = 4L * width.toLong() * height.toLong()
                    val fullTextureAddress = textureStack.push(fullTextureCapacity)
                    fullTexture = TextureBuilder(fullTextureAddress, width, height)
                    texture.copy(fullTexture, 0, 0)
                }
                textureList[0].offsetX = 0
                textureList[0].offsetY = 0
            }
        }

        val vertexCount = vertexBuilder.getVertexCountSoFar()
        vertexBuilder.build(textureList, fullTexture.width, fullTexture.height)

        fullTexture.saveAsImage("model image")
        fullTexture.build()

        return BasicModelBuffer(vertexChunk.address, indexChunk.address, fullTexture.address,
            vertexCount, indexBuilder.getTrianglesSoFar() * 3, fullTexture.width, fullTexture.height)
    }

    override fun close() {
        vertexChunk.close()
        indexChunk.close()
        textureStack.close()
        textureList.clear()
    }
}

class TextureEntry(val texture: TextureBuilder, val id: Int){

    internal var offsetX: Long? = null
    internal var offsetY: Long? = null
}