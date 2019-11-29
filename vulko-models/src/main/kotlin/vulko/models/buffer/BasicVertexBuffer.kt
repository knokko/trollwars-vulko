package vulko.models.buffer

import org.lwjgl.system.MemoryUtil.memByteBuffer
import java.nio.ByteBuffer

const val BYTES_PER_VERTEX = 4 * (3 + 3 + 2 + 1)

const val OFFSET_POSITIONS = 0
const val OFFSET_NORMALS = 4 * 3
const val OFFSET_TEXTURE_COORDS = OFFSET_NORMALS + 4 * 3
const val OFFSET_MATRIX = OFFSET_TEXTURE_COORDS + 4 * 2
class BasicVertexBuffer(val address: Long, val vertexCount: Int) {

    fun createBackingBuffer() : ByteBuffer {
        val buffer = memByteBuffer(address, vertexCount * BYTES_PER_VERTEX)
        buffer.position(0)
        buffer.limit(buffer.capacity())
        return buffer
    }
}