package py.common;

/**
 * There is a metadata for each buffer allocated by tlsf.
 * <p>
 * The metadata looks like below:
 * <p>
 * |-------pre_phy_buffer-----|</br>
 * 
 * |-------cur_phy_buffer-----|</br>
 * 
 * |-------size field-----P-F-|</br>
 * 
 * |-------next_free----------|</br>
 * 
 * |-------pre_free-----------|</br>
 * 
 */
class MetadataDescriptor {
    /**
     * Members of buffer metadata are all type of {@link Long}, we say the size of each member unit bytes.
     */
    static final int METADATA_UNIT_BYTES = Long.SIZE / Byte.SIZE;

    /**
     * There are 5 members in metadata.
     */
    static final int METADATA_SIZE_BYTES = 5 * METADATA_UNIT_BYTES;

    /**
     * After allocate a buffer to user, two member of buffer metadata is not available for user to use. One is the
     * address of the buffer, another is the size of accessible memory. Other than that, 3 metadata fields can be
     * used by users
     */
    static final int BUFFER_METADATA_OVERHEAD = 2 * METADATA_UNIT_BYTES;

    /**
     * buffer-free bit in size field, which indicates if the current buffer is free or allocated
     */
    static final long BUFFER_FREE_BIT = 1l << 0;

    /**
     * pre-buffer-free bit in size field, which indicates if the pre-buffer is free or allocated Note that this bit
     * can be set when pre-buffer is released to the manager
     */
    static final long PRE_BUFFER_FREE_BIT = 1l << 1;

    /**
     * Metadata members offset.
     */
    static final int PRE_ADDRESS_OFFSET = 0;
    static final int ADDRESS_OFFSET = PRE_ADDRESS_OFFSET + METADATA_UNIT_BYTES;
    static final int SIZE_OFFSET = ADDRESS_OFFSET + METADATA_UNIT_BYTES;
    static final int PRE_FREE_ADDRESS_OFFSET = SIZE_OFFSET + METADATA_UNIT_BYTES;
    static final int NEXT_FREE_ADDRESS_OFFSET = PRE_FREE_ADDRESS_OFFSET + METADATA_UNIT_BYTES;
    /**
     * Accessible memory start behind metadata.
     */
    static final int ACCESSIBLE_MEM_OFFESET = PRE_FREE_ADDRESS_OFFSET;

    long getPrePhysicalAddress(long address) {
        return DirectAlignedBufferAllocator.getLong(address + PRE_ADDRESS_OFFSET);
    }

    void setPrePhysicalAddress(long address, long prePhysicalAddress) {
        DirectAlignedBufferAllocator.putLong(address + PRE_ADDRESS_OFFSET, prePhysicalAddress);
    }

    long getNextPhysicalAddress(long address) {
        return address + ACCESSIBLE_MEM_OFFESET + getAccessibleMemSize(address) - METADATA_UNIT_BYTES;
    }

    long getAddress(long address) {
        return DirectAlignedBufferAllocator.getLong(address + ADDRESS_OFFSET);
    }

    void setAddress(long address) {
        DirectAlignedBufferAllocator.putLong(address + ADDRESS_OFFSET, address);
    }

    long getAccessibleMemSize(long address) {
        long sizeField = DirectAlignedBufferAllocator.getLong(address + SIZE_OFFSET);
        return sizeField & ~(BUFFER_FREE_BIT | PRE_BUFFER_FREE_BIT);
    }

    void setAccessibleMemSize(long address, long accessibleMemSize) {
        long sizeField = DirectAlignedBufferAllocator.getLong(address + SIZE_OFFSET);
        sizeField = accessibleMemSize | (sizeField & (BUFFER_FREE_BIT | PRE_BUFFER_FREE_BIT));
        DirectAlignedBufferAllocator.putLong(address + SIZE_OFFSET, sizeField);
    }

    long getPreFreeAddress(long address) {
        return DirectAlignedBufferAllocator.getLong(address + PRE_FREE_ADDRESS_OFFSET);
    }

    void setPreFreeAddress(long address, long preFreeAddress) {
        DirectAlignedBufferAllocator.putLong(address + PRE_FREE_ADDRESS_OFFSET, preFreeAddress);
    }

    long getNextFreeAddress(long address) {
        return DirectAlignedBufferAllocator.getLong(address + NEXT_FREE_ADDRESS_OFFSET);
    }

    void setNextFreeAddress(long address, long nextFreeAddress) {
        DirectAlignedBufferAllocator.putLong(address + NEXT_FREE_ADDRESS_OFFSET, nextFreeAddress);
    }

    boolean isFree(long address) {
        long sizeField = DirectAlignedBufferAllocator.getLong(address + SIZE_OFFSET);
        return (sizeField & BUFFER_FREE_BIT) != 0;
    }

    void setFree(long address) {
        long sizeField = DirectAlignedBufferAllocator.getLong(address + SIZE_OFFSET);
        sizeField |= BUFFER_FREE_BIT;
        DirectAlignedBufferAllocator.putLong(address + SIZE_OFFSET, sizeField);
    }

    boolean isPreFree(long address) {
        long sizeField = DirectAlignedBufferAllocator.getLong(address + SIZE_OFFSET);
        return (sizeField & PRE_BUFFER_FREE_BIT) != 0;
    }

    void setPreFree(long address) {
        long sizeField = DirectAlignedBufferAllocator.getLong(address + SIZE_OFFSET);
        sizeField |= PRE_BUFFER_FREE_BIT;
        DirectAlignedBufferAllocator.putLong(address + SIZE_OFFSET, sizeField);
    }

    void setUsed(long address) {
        long sizeField = DirectAlignedBufferAllocator.getLong(address + SIZE_OFFSET);
        sizeField &= ~BUFFER_FREE_BIT;
        DirectAlignedBufferAllocator.putLong(address + SIZE_OFFSET, sizeField);
    }

    void setPreUsed(long address) {
        long sizeField = DirectAlignedBufferAllocator.getLong(address + SIZE_OFFSET);
        sizeField &= ~PRE_BUFFER_FREE_BIT;
        DirectAlignedBufferAllocator.putLong(address + SIZE_OFFSET, sizeField);
    }
}
