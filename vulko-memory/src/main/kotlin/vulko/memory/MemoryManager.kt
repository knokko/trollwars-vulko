package vulko.memory

import vulko.memory.util.*
import java.lang.UnsupportedOperationException
import java.util.*
import kotlin.IllegalStateException

/**
 * Memory managers can be used to manage Unsafe memory. They have methods for claiming and releasing memory. Memory
 * managers are always thread-safe, but some objects returned by claim methods are possibly not thread-safe, for
 * instance virtual stacks.
 *
 * Memory managers will allocate memory upon construction using Unsafe.allocateMemory (and allocate more if
 * allowExternalMemory is true and it doesn't have enough contiguous memory for a requested claim). Calling claim
 * methods will return memory-holding objects that will own a portion of the memory of the manager.
 *
 * Each memory claim requires a description (as String). The summarizeClaims() method prints an overview of the
 * currently claimed memory blocks along with their description.
 */
class MemoryManager

@Throws(MallocException::class)
constructor(val capacity: Long, val allowExternalMemory: Boolean = true, private val parent: MemorySplitter? = null, val address: Long = malloc(capacity)) : AutoCloseable, MemorySplitter {

    private val claimsTree = TreeSet<ClaimEntry>()

    private val sizeFragmentsTree = TreeSet<MemoryFragment>{ a: MemoryFragment, b: MemoryFragment ->
        (a.size - b.size).toInt()
    }
    private val addressFragmentsTree = TreeSet<MemoryFragment>{ a: MemoryFragment, b: MemoryFragment ->
        (a.address - b.address).toInt()
    }

    init {

        // We start with 1 big fragment
        val initialFragment = MemoryFragment(address, capacity)
        sizeFragmentsTree.add(initialFragment)
        addressFragmentsTree.add(initialFragment)
    }

    @Throws(MallocException::class)
    private fun claim(capacityToClaim: Long, purpose: String) : Long {
        synchronized(this) {
            val fakeFragment = MemoryFragment(-1, capacityToClaim)
            val fragment = sizeFragmentsTree.ceiling(fakeFragment)
            if (fragment == null) {

                // Uh ooh, no fragment is big enough!
                if (allowExternalMemory) {
                    println("Went outside of manager memory to allocate $capacity bytes for $purpose")
                    return malloc(capacity)
                } else {
                    throw UnsupportedOperationException("Can't allocate $capacity bytes of memory for $purpose")
                }
            } else {
                val claimToAdd = ClaimEntry(fragment.address, capacityToClaim, System.nanoTime(), purpose)
                claimsTree.add(claimToAdd)
                if (fragment.size == capacityToClaim) {

                    // The entire fragment was consumed, so just remove it from the sets
                    sizeFragmentsTree.remove(fragment)
                    addressFragmentsTree.remove(fragment)
                } else {

                    // Not the entire fragment was consumed, so we just need to make it smaller.
                    // But since changing the size directly would violate the sizeFragmentsTree, we need to reinsert it.
                    sizeFragmentsTree.remove(fragment)
                    fragment.address += capacityToClaim
                    fragment.size -= capacityToClaim
                    sizeFragmentsTree.add(fragment)

                    // Note that there can't be any other fragment within the current fragment,
                    // so addressFragmentsTree will still be sorted correctly.
                }

                return claimToAdd.address
            }
        }
    }

    /**
     * Claims a chunk with the given capacity (in bytes) of memory from the memory of this manager and returns it.
     * Closing the returned memory chunk will give the claimed memory back to this manager.
     *
     * If the allowExternalMemory of this manager is false and no contiguous memory of at least the requested capacity
     * is available, an UnsupportedOperationException will be thrown.
     */
    @Throws(MallocException::class)
    fun claimChunk(capacity: Long, purpose: String) : MemoryChunk {
        return MemoryChunk(this, claim(capacity, purpose), capacity)
    }

    /**
     * Claims a virtual with the given capacity (in bytes) of memory from the memory of this manager and returns it.
     * Closing the returned stack will give the claimed memory back to this manager.
     *
     * If the allowExternalMemory of this manager is false and no contiguous memory of at least the requested capacity
     * is available, an UnsupportedOperationException will be thrown.
     */
    @Throws(MallocException::class)
    fun claimStack(capacity: Long, purpose: String) : VirtualStack {
        return VirtualStack(this, claim(capacity, purpose), capacity)
    }

    /**
     * Claims a (sub) memory manager with the given capacity (in bytes) of memory from the memory of this manager and
     * returns it. Closing the returned memory manager will give the claimed memory back to this manager.
     *
     * The returned memory manager will manage a part of the memory of this memory manager.
     *
     * If the allowExternalMemory of this manager is false and no contiguous memory of at least the requested capacity
     * is available, an UnsupportedOperationException will be thrown.
     */
    @Throws(MallocException::class)
    fun claimSubManager(capacity: Long, purpose: String) : MemoryManager {
        return MemoryManager(capacity, allowExternalMemory, this, claim(capacity, purpose))
    }

    /**
     * Prints (to standard output stream) an overview of the currently claimed memory fragments of this memory manager.
     * This method is made to track down memory leaks.
     */
    fun summarizeClaims(){
        println("Current memory claims:")
        synchronized(this) {
            for (claim in claimsTree) {
                println("Claim ${claim.purpose} has ${claim.capacity} bytes")
            }
        }
        println()
    }

    /**
     * Prints (to standard output stream) an overview of the current fragmentation of the memory of this memory manager.
     * This method is made for debugging and for checking how bad the fragmentation can become.
     */
    fun summarizeFragments(){
        synchronized(this) {
            println("Current memory fragments sorted by size:")
            for (fragment in sizeFragmentsTree.descendingIterator()) {
                println("Fragment of size ${fragment.size} at ${fragment.address}")
            }
            println("Current memory fragments sorted by address:")
            for (fragment in addressFragmentsTree) {
                println("Fragment of size ${fragment.size} at ${fragment.address}")
            }
        }
        println()
    }

    override fun freeChild(childAddress: Long){
        if (childAddress >= address && childAddress < address + capacity){
            synchronized(this) {
                val dummyClaim = ClaimEntry(childAddress, -1, -1, "freeChild dummy")
                val actualClaim = claimsTree.floor(dummyClaim)
                if (actualClaim == null || actualClaim.address != childAddress) {
                    throw IllegalArgumentException("There is no current claim with address $childAddress")
                }
                claimsTree.remove(actualClaim)

                val dummyFragment = MemoryFragment(childAddress, -1)
                val fragmentBefore = addressFragmentsTree.floor(dummyFragment)
                var mergedFragment = false
                if (fragmentBefore != null && fragmentBefore.address + fragmentBefore.size == childAddress) {
                    sizeFragmentsTree.remove(fragmentBefore)
                    fragmentBefore.size += actualClaim.capacity

                    // We will re-add fragmentBefore later to prevent another needless reinsertion
                    mergedFragment = true
                }
                val fragmentAfter = addressFragmentsTree.ceiling(dummyFragment)
                if (fragmentAfter != null && childAddress + actualClaim.capacity == fragmentAfter.address) {
                    if (mergedFragment) {

                        // We merged the freed memory fragment with the fragment before it, so we can merge that one
                        // with the fragment after it as well because we just checked that fragmentAfter comes right after
                        // the freed fragment.
                        fragmentBefore.size += fragmentAfter.size
                        sizeFragmentsTree.add(fragmentBefore)
                        sizeFragmentsTree.remove(fragmentAfter)
                        addressFragmentsTree.remove(fragmentAfter)
                    } else {

                        // We didn't merge with the fragment before the freed fragment, so we can only merge the freed
                        // fragment with the next fragment
                        fragmentAfter.address -= actualClaim.capacity
                        sizeFragmentsTree.remove(fragmentAfter)
                        fragmentAfter.size += actualClaim.capacity
                        sizeFragmentsTree.add(fragmentAfter)

                        mergedFragment = true
                    }
                } else if (mergedFragment) {

                    // This code block will be called when we merged with the fragment before, but not with the fragment after
                    // Since we initially removed fragmentBefore, we should re-add it
                    sizeFragmentsTree.add(fragmentBefore)
                }

                if (!mergedFragment) {

                    // If we reach this code block, we couldn't merge the freed fragment with another fragment
                    // This means that its neighbours are still claimed elsewhere
                    val freedFragment = MemoryFragment(childAddress, actualClaim.capacity)
                    sizeFragmentsTree.add(freedFragment)
                    addressFragmentsTree.add(freedFragment)
                }
            }
        } else {
            if (allowExternalMemory){

                // Now we simply deallocate the memory we allocated before
                free(childAddress)
            } else {

                // Since allowExternalMemory is false, we could not have allocated this piece of memory outside our
                // bounds, so childAddress can't have been assigned by this memory manager
                throw IllegalArgumentException("Attempted to free out of bounds memory address $childAddress of memory manager with address $address and capacity $capacity")
            }
        }
    }

    /**
     * Closes this memory manager after doing several validation checks. If any memory claims are still active, an
     * IllegalStateException will be thrown.
     *
     * If this memory manager has a parent from which it is borrowing memory, all the memory of this manager will be
     * given back to that parent.
     * If not, all memory of this manager will be freed with Unsafe.freeMemory
     */
    override fun close(){
        synchronized(this) {
            if (claimsTree.isNotEmpty()) {
                summarizeClaims()
                throw IllegalStateException("Attempted to close memory manager with claims left, see list above")
            }
            if (addressFragmentsTree.size != 1 || sizeFragmentsTree.size != 1) {
                summarizeFragments()
                throw IllegalStateException("Invalid number of remaining memory fragments, see above")
            }
            if (sizeFragmentsTree.first().size != capacity) {
                throw IllegalStateException("Size of memory fragment should be $capacity, but is ${addressFragmentsTree.first().size}")
            }
            if (addressFragmentsTree.first().address != address) {
                throw IllegalStateException("Address of memory fragment should be $address, but is ${addressFragmentsTree.first().address}")
            }
            if (parent == null) {
                free(address)
            } else {
                parent.freeChild(address)
            }
        }
    }
}

internal class ClaimEntry(val address: Long, val capacity: Long, val claimTime: Long, val purpose: String) : Comparable<ClaimEntry> {

    override operator fun compareTo(other: ClaimEntry) : Int {
        return (address - other.address).toInt()
    }
}

internal class MemoryFragment(var address: Long, var size: Long)