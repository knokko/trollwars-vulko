package vulko.models.buffer

import org.joml.Matrix4f
import vulko.memory.util.copy
import vulko.memory.util.fill

const val MATRIX_BYTE_SIZE = 4L * (4L * 4L);

/**
 * A MatrixBuffer is a piece of memory dedicated to store 4x4 float matrices. It provides
 * simple get/set methods as well as methods to (re)set all matrices in the buffer.
 */
class MatrixBuffer

/**
 * Constructs a new MatrixBuffer with numMatrices matrices starting from the given memory
 * address.
 *
 * The first byte of the first matrix will be stored at address and the last byte of the last
 * matrix will be stored at address + MATRIX_BYTE_SIZE * numMatrices - 1.
 *
 * It is the responsibility of the caller to make sure that the given memory is freely available
 * for the new MatrixBuffer.
 *
 * It is recommended to call setZero() or setIdentity() right after constructing a new
 * MatrixBuffer, but not required. If you do not call them, the initial values of the matrices
 * will we whatever there was at the given memory address.
 *
 * If address is 0 or numMatrices is negative, an IllegalArgumentException will be thrown.
 */
@Throws(IllegalArgumentException::class)
constructor(val address: Long, val numMatrices: Int) {

    init {
        if (address == 0L){
            throw IllegalArgumentException("Address can't be 0")
        }
        if (numMatrices < 0){
            throw IllegalArgumentException("numMatrices can't be negative, but is $numMatrices")
        }
    }

    /**
     * Copies source to all matrices in this buffer.
     */
    fun setAll(source: Matrix4f){

        // Could probably be optimized by using more memory copies
        for (matrixIndex in 0 until numMatrices) {
            source.getToAddress(addressOf(matrixIndex))
        }
    }

    /**
     * Sets all matrices in this buffer to the identity matrix
     */
    fun setIdentity(){
        setAll(Matrix4f())
    }

    /**
     * Sets all matrices in this buffer to the zero matrix
     */
    fun setZero(){
        fill(address, numMatrices * MATRIX_BYTE_SIZE, 0)
    }

    /**
     * Returns the memory address of the matrix at index matrixIndex. The memory used for that
     * matrix will occupy the memory addresses returnValue (including) until returnValue +
     * MATRIX_BYTE_SIZE (exclusive).
     *
     * If matrixIndex is negative or not smaller than numMatrices, an IllegalArgumentException
     * will be thrown.
     */
    @Throws(IllegalArgumentException::class)
    fun addressOf(matrixIndex: Int) : Long {
        if (matrixIndex < 0){
            throw IllegalArgumentException("matrixIndex can't be negative, but is $matrixIndex")
        }
        if (matrixIndex >= numMatrices){
            throw IllegalArgumentException("matrixIndex must be smaller than $numMatrices, but is $matrixIndex")
        }
        return address + matrixIndex * MATRIX_BYTE_SIZE
    }

    /**
     * Copies the matrix at index matrixIndex to dest.
     *
     * If matrixIndex is negative or not smaller than numMatrices, an IllegalArgumentException
     * will be thrown.
     */
    @Throws(IllegalArgumentException::class)
    fun get(matrixIndex: Int, dest: Matrix4f){
        dest.setFromAddress(addressOf(matrixIndex))
    }

    /**
     * Copies the matrix at index matrixIndex to destAddress. The first byte of the matrix will
     * be stored at destAddress and the last byte of the matrix will be stored at destAddress +
     * MATRIX_BYTE_SIZE - 1.
     *
     * If matrixIndex is negative or not smaller than numMatrices, an IllegalArgumentException
     * will be thrown.
     */
    @Throws(IllegalArgumentException::class)
    fun get(matrixIndex: Int, destAddress: Long){
        copy(addressOf(matrixIndex), destAddress, MATRIX_BYTE_SIZE)
    }

    /**
     * Copies value into the matrix at index matrixIndex.
     *
     * If matrixIndex is negative or not smaller than numMatrices, an IllegalArgumentException
     * will be thrown.
     */
    @Throws(IllegalArgumentException::class)
    fun set(matrixIndex: Int, value: Matrix4f){
        value.getToAddress(addressOf(matrixIndex))
    }

    /**
     * Copies the matrix stored between sourceAddress (inclusive) and sourceAddress +
     * MATRIX_BYTE_SIZE (exclusive) into the matrix at index matrixIndex.
     *
     * It is the responsibility of the caller to ensure that there is really a matrix stored
     * at sourceAddress. This will not be checked by this method.
     *
     * If matrixIndex is negative or not smaller than numMatrices, an IllegalArgumentException
     * will be thrown.
     */
    @Throws(IllegalArgumentException::class)
    fun set(matrixIndex: Int, sourceAddress: Long){
        copy(sourceAddress, addressOf(matrixIndex), MATRIX_BYTE_SIZE)
    }

    /**
     * Creates and returns a 'child' MatrixBuffer of this MatrixBuffer. The child will have
     * a subset of the matrices of this buffer and changing the matrices of the child will
     * change the matrices of this buffer and vice versa.
     *
     * The first matrix of the child buffer will be the matrix at index startIndex of this
     * buffer and the numMatrices field of the child buffer will be numChildMatrices.
     *
     * If any of the following conditions is satisfied, an IllegalArgumentException will be
     * thrown:
     * 1. startIndex is negative
     * 2. numChildMatrices is negative
     * 3. startIndex + numChildMatrices is not smaller than numMatrices
     * 4. Adding numChildMatrices to startIndex causes integer overflow
     */
    @Throws(IllegalArgumentException::class)
    fun createChildBuffer(startIndex: Int, numChildMatrices: Int) : MatrixBuffer {

        // The explicit bounds checking
        if (numChildMatrices < 0){
            throw IllegalArgumentException("numChildMatrices can't be negative, but is $numChildMatrices")
        }
        if (startIndex + numChildMatrices < startIndex){
            throw IllegalArgumentException("Adding numChildMatrices ($numChildMatrices) to" +
                    " startIndex ($startIndex) would cause overflow")
        }
        if (startIndex + numChildMatrices >= numMatrices){
            throw IllegalArgumentException("startIndex + numChildMatrices must be smaller than" +
                    "numMatrices ($numMatrices), but is ${startIndex + numChildMatrices}")
        }

        // Using addressOf will do the implicit bounds checking
        return MatrixBuffer(addressOf(startIndex), numChildMatrices)
    }

    // TODO Write tests
}