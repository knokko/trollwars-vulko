package vulko.models.buffer

import org.lwjgl.system.MemoryUtil.memByteBuffer
import java.nio.ByteBuffer

class BasicModelBuffer(val vertexDataAddress: Long, val indexDataAddress: Long, val textureAddress: Long,
                       val vertexCount: Int, val indexCount: Int, val textureWidth: Int, val textureHeight: Int) {

    fun createBackingVertexBuffer() : ByteBuffer {
        return memByteBuffer(vertexDataAddress, vertexCount * BYTES_PER_VERTEX)
    }

    fun createBackingIndexBuffer() : ByteBuffer {
        return memByteBuffer(indexDataAddress, 4 * indexCount)
    }

    fun createBackingTextureBuffer() : ByteBuffer {
        return memByteBuffer(textureAddress, 4 * textureWidth * textureHeight)
    }
}