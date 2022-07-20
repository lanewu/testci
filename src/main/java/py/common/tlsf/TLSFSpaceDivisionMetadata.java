package py.common.tlsf;

/**
 * There is a metadata for each division space allocated by tlsf.
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
 * @author zjm
 */
public interface TLSFSpaceDivisionMetadata {

    /**
     * Members of buffer metadata are all type of {@link Long}, we say the size of each member unit bytes.
     */
    public static final int METADATA_UNIT_BYTES = Long.SIZE / Byte.SIZE;

    /**
     * There are 5 members in metadata.
     */
    public static final int METADATA_SIZE_BYTES = 5 * METADATA_UNIT_BYTES;

    /**
     * After allocate a buffer to user, two member of buffer metadata is not available for user to use. One is the
     * address of the buffer, another is the size of accessible memory. Other than that, 3 metadata fields can be used
     * by users
     */
    public static final int BUFFER_METADATA_OVERHEAD = 2 * METADATA_UNIT_BYTES;

    public static final long PRE_PHY_OFFSET = 0l;

    public static final long CUR_PHY_OFFSET = PRE_PHY_OFFSET + METADATA_UNIT_BYTES;

    public static final long SIZE_FIELD_OFFSET = CUR_PHY_OFFSET + METADATA_UNIT_BYTES;

    public static final long NEXT_FREE_OFFSET = SIZE_FIELD_OFFSET + METADATA_UNIT_BYTES;

    public static final long PRE_FREE_OFFSET = NEXT_FREE_OFFSET + METADATA_UNIT_BYTES;

    public static final long ASSEMEM_OFFSET = NEXT_FREE_OFFSET;
    
    /**
     * buffer-free bit in size field, which indicates if the current buffer is free or allocated
     */
    static final long BUFFER_FREE_BIT = 1l << 0;

    /**
     * pre-buffer-free bit in size field, which indicates if the pre-buffer is free or allocated Note that this bit
     * can be set when pre-buffer is released to the manager
     */
    static final long PRE_BUFFER_FREE_BIT = 1l << 1;

    public boolean seperated();

    public long getPrePhysicalAddress(long address);

    public void setPrePhysicalAddress(long address, long prePhysicalAddress);

    public long getNextPhysicalAddress(long address);

    public long getAddress(long address);

    public void setAddress(long address);

    public long getAccessibleMemSize(long address);

    public void setAccessibleMemSize(long address, long accessibleMemSize);

    public long getPreFreeAddress(long address);

    public void setPreFreeAddress(long address, long preFreeAddress);

    public long getNextFreeAddress(long address);

    public void setNextFreeAddress(long address, long nextFreeAddress);

    public boolean isFree(long address);

    public void setFree(long address);

    public boolean isPreFree(long address);

    public void setPreFree(long address);

    public void setUsed(long address);

    public void setPreUsed(long address);
}
