package vulko.models.buffer

import org.joml.Matrix4f
import org.junit.Assert.assertEquals
import org.junit.Test
import vulko.memory.util.fill
import vulko.memory.util.free
import vulko.memory.util.getByte
import vulko.memory.util.malloc

class TestMatrixBuffer {

    private fun testGoodConstructor(address: Long, numMatrices: Int){
        val buffer = MatrixBuffer(address, numMatrices)
        assertEquals(address, buffer.address)
        assertEquals(numMatrices, buffer.numMatrices)
    }

    private fun testBadConstructor(address: Long, numMatrices: Int){
        try {
            MatrixBuffer(address, numMatrices)
            throw AssertionError("MatrixBuffer($address, $numMatrices) should" +
                    "have thrown an IllegalArgumentException")
        } catch (ex: IllegalArgumentException){}
    }

    @Test
    fun testConstructor(){

        // Good edge cases
        testGoodConstructor(1, 5)
        testGoodConstructor(-1, 1)
        testGoodConstructor(10, 0)

        // Good normal cases
        testGoodConstructor(123, 84)
        testGoodConstructor(34587343948394839L, 92347)

        // Bad edge cases
        testBadConstructor(0, 123)
        testBadConstructor(123, -1)

        // Another bad case
        testBadConstructor(348, -34576)
    }

    private fun withBuffer(numMatrices: Int, user: (MatrixBuffer) -> Unit){

        // Allocate 3 times more memory than we need to make it easier to scan for out of bounds
        // memory changes
        val claimedSize = numMatrices * MATRIX_BYTE_SIZE * 3
        val address = malloc(claimedSize)

        // Set all bytes in the claimed region to -3
        val testByte: Byte = -3
        fill(address, claimedSize, testByte)
        user(MatrixBuffer(address + numMatrices * MATRIX_BYTE_SIZE, numMatrices))

        // Check for changes outside the MatrixBuffer domain
        for (testAddress in address until address + numMatrices * MATRIX_BYTE_SIZE){
            assertEquals(testByte, getByte(testAddress))
        }
        for (testAddress in address + 2 * numMatrices * MATRIX_BYTE_SIZE until address + claimedSize){
            assertEquals(testByte, getByte(testAddress))
        }
    }

    private fun testGoodSetAll(numMatrices: Int, value: Matrix4f){
        withBuffer(numMatrices){buffer ->
            buffer.setAll(value)
            val dest = Matrix4f()
            for (index in 0 until numMatrices){
                buffer.get(index, dest)
                assertEquals(value, dest)
            }
        }
    }

    @Test
    fun testSetAll(){

        // Edge cases
        testGoodSetAll(0, Matrix4f())
        testGoodSetAll(1, Matrix4f().translate(1f, 1f, 1f))

        // Normal cases
        testGoodSetAll(3, Matrix4f().scale(4f))
        testGoodSetAll(90, Matrix4f().translate(-1f, 0f, 0f))

        // There aren't really bad cases
    }

    private fun testZero(numMatrices: Int){
        withBuffer(numMatrices){buffer ->
            buffer.setZero()
            val dest = Matrix4f()
            val zero = Matrix4f().zero()
            for (index in 0 until numMatrices){
                buffer.get(index, dest)
                assertEquals(zero, dest)
            }
        }
    }

    @Test
    fun testZero(){

        // Edge cases
        testZero(0)
        testZero(1)

        // Normal cases
        testZero(2)
        testZero(96)
    }

    private fun testIdentity(numMatrices: Int){
        withBuffer(numMatrices){buffer ->
            buffer.setIdentity()
            val dest = Matrix4f()
            val identity = Matrix4f()
            for (index in 0 until numMatrices){
                buffer.get(index, dest)
                assertEquals(identity, dest)
            }
        }
    }

    @Test
    fun testIdentity(){

        // Edge cases
        testIdentity(0)
        testIdentity(1)

        // Normal cases
        testIdentity(2)
        testIdentity(17)
    }

    private fun testBadAddressOf(buffer: MatrixBuffer, index: Int){
        try {
            buffer.addressOf(index)
            throw AssertionError("addressOf($index) should have thrown IllegalArgumentException")
        } catch (ex: IllegalArgumentException){}
    }

    @Test
    fun testAddressOf(){

        // Edge cases
        assertEquals(10, MatrixBuffer(10, 1).addressOf(0))
        assertEquals(10 + MATRIX_BYTE_SIZE, MatrixBuffer(10, 2).addressOf(1))
        assertEquals(20, MatrixBuffer(20, 30).addressOf(0))

        // Normal cases
        run {
            val buffer = MatrixBuffer(100, 200)
            assertEquals(100 + MATRIX_BYTE_SIZE, buffer.addressOf(1))
            assertEquals(100 + 8 * MATRIX_BYTE_SIZE, buffer.addressOf(8))
        }

        // Bad edge cases
        testBadAddressOf(MatrixBuffer(10, 0), 0)
        testBadAddressOf(MatrixBuffer(10, 10), -1)
        testBadAddressOf(MatrixBuffer(10, 10), 10)

        // Bad cases
        run {
            val buffer = MatrixBuffer(100, 15)
            testBadAddressOf(buffer, -10)
            testBadAddressOf(buffer, 120)
        }
    }

    private fun testGoodGetSet(buffer: MatrixBuffer, index: Int, matrix: Matrix4f){

        // Set the matrix
        buffer.set(index, matrix)

        // Get the matrix 'back' and put into dest
        val dest = Matrix4f()
        buffer.get(index, dest)
        assertEquals(matrix, dest)

        // Claim some memory for the address get/set methods
        val dummyAddress = malloc(MATRIX_BYTE_SIZE)
        buffer.get(index, dummyAddress)
        dest.identity()
        dest.setFromAddress(dummyAddress)
        assertEquals(matrix, dest)

        buffer.set(index, dummyAddress)
        dest.identity()
        buffer.get(index, dest)
        assertEquals(dest, matrix)

        // Release the dummy memory
        free(dummyAddress)
    }

    private fun testBadGetSet(buffer: MatrixBuffer, index: Int){
        val dummy = Matrix4f()

        /*
         * If the matrix buffer tries to read/write, the operating system will cause the
         * JVM to halt, which will 'fail' the test.
         */
        val dummyAddress = 1L
        try {
            buffer.get(index, dummy)
            throw AssertionError("get($index,...) should have thrown IllegalArgumentException")
        } catch (ex: IllegalArgumentException){}
        try {
            buffer.set(index, dummy)
            throw AssertionError("set($index,...) should have thrown IllegalArgumentException")
        } catch (ex: IllegalArgumentException){}
        try {
            buffer.get(index, dummyAddress)
            throw AssertionError("get($index,...) should have thrown IllegalArgumentException")
        } catch (ex: IllegalArgumentException){}
        try {
            buffer.set(index, dummyAddress)
            throw AssertionError("set($index,...) should have thrown IllegalArgumentException")
        } catch (ex: IllegalArgumentException){}
    }

    @Test
    fun testGetSet(){

        // Good edge cases
        withBuffer(1){buffer ->
            testGoodGetSet(buffer, 0, Matrix4f().translate(1f, 2f, 3f))
        }
        withBuffer(10){buffer ->
            testGoodGetSet(buffer, 9, Matrix4f().scale(0.05f))
        }

        // Normal cases
        withBuffer(20){buffer ->
            testGoodGetSet(buffer, 3, Matrix4f().rotate(15f, 1f, 1f, 1f))
            testGoodGetSet(buffer, 8, Matrix4f().zero())
            testGoodGetSet(buffer, 18, Matrix4f().translate(2f, 1f, 5f))
        }

        // Bad edge cases
        testBadGetSet(MatrixBuffer(10, 0), 0)
        testBadGetSet(MatrixBuffer(15, 1), -1)
        testBadGetSet(MatrixBuffer(15, 1), 1)

        // Bad cases
        withBuffer(20){buffer ->
            testBadGetSet(buffer, -15)
            testBadGetSet(buffer, 46)
        }
    }

    private fun testGoodChildBuffer(parent: MatrixBuffer, startIndex: Int, numMatrices: Int){
        val child = parent.createChildBuffer(startIndex, numMatrices)

        // Check if the fields are correct
        assertEquals(numMatrices, child.numMatrices)
        assertEquals(parent.address + MATRIX_BYTE_SIZE * startIndex, child.address)

        // Test a simple get/set
        if (numMatrices != 0) {
            val matrix = Matrix4f().translate(1f, 2f, 3f)
            child.set(numMatrices - 1, matrix)
            val dest = Matrix4f()
            parent.get(startIndex + numMatrices - 1, dest)
            assertEquals(matrix, dest)
        }
    }

    private fun testBadChildBuffer(parent: MatrixBuffer, startIndex: Int, numMatrices: Int){
        try {
            parent.createChildBuffer(startIndex, numMatrices)
            throw AssertionError("createChildBuffer($startIndex, $numMatrices)" +
                    "should have thrown IllegalArgumentException")
        } catch (ex: IllegalArgumentException){}
    }

    @Test
    fun testChildBuffer(){

        // Good edge cases
        withBuffer(1){buffer ->
            testGoodChildBuffer(buffer, 0, 0)
            testGoodChildBuffer(buffer, 0, 1)
        }
        withBuffer(10){buffer ->
            testGoodChildBuffer(buffer, 0, 10)
        }

        // Normal cases
        withBuffer(25){buffer ->
            testGoodChildBuffer(buffer, 1, 4)
            testGoodChildBuffer(buffer, 10, 1)
            testGoodChildBuffer(buffer, 1, 23)
        }

        // Bad edge cases
        withBuffer(5){buffer ->
            testBadChildBuffer(buffer, 0, 6)
            testBadChildBuffer(buffer, 5, 0)
            testBadChildBuffer(buffer, -1, 0)
            testBadChildBuffer(buffer, -1, 1)
        }

        // Bad cases
        withBuffer(25){buffer ->
            testBadChildBuffer(buffer, -5, 10)
            testBadChildBuffer(buffer, -20, 10)
            testBadChildBuffer(buffer, 20, 10)
            testBadChildBuffer(buffer, 40, 20)
        }
    }
}