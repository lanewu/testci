package py.common.tlsf;

/**
 * 
 * @author zjm
 *
 */
public interface TLSFSpaceManager {

    public static final long NULL_SPACE_ADDRESS = Long.MAX_VALUE;

    /**
     * subdivisions of first level
     */
    public static final int FIRST_LEVEL_INDEX_COUNT = Long.SIZE;

    /**
     * subdivisions of second level
     */
    public static final int SECOND_LEVEL_INDEX_COUNT = Long.SIZE;

    /**
     * value for {@code SECOND_LEVEL_INDEX_COUNT} after function log with base 2
     */
    public static final int SECOND_LEVEL_INDEX_COUNT_LOG2 = Integer.numberOfTrailingZeros(SECOND_LEVEL_INDEX_COUNT);

    /**
     * The minimum alignment is 4 bytes
     */
    public static final int MIN_ALIGNMENT = 4;

    /**
     * Return index of most left one bit of the given value. That is "floor(log<sub>2</sub>(value))".
     * <p>
     * Index starts from 0 and it is the most right bit of the given value. e.g. Index of most left one bit of value 1
     * is 0; Index of most left one bit of value 3 is 1.
     * 
     * @param value
     * @return index of most left one bit of the given value.
     */
    public static int locateMostLeftOneBit(long value) {
        return Long.SIZE - Long.numberOfLeadingZeros(value) - 1;
    }

    /**
     * 
     * @param value
     * @return
     */
    public static int locateMostRightOneBit(long value) {
        return Long.numberOfTrailingZeros(value);
    }

    /**
     * 
     * @param size
     * @return
     */
    public long allocate(long size) throws OutOfSpaceException;

    /**
     * Try my best to find a suitable free space for the given size unless manager doesn't have any free space.
     * <p>
     * If manger has free space larger than the given size, then use it. Otherwise, the manager will try its best to
     * find the larggest free space for the desired size.
     * 
     * @param size
     *            desired size
     * @return address of free space of which size could be larger than, equal to and less than the given size.
     * @throws OutOfSpaceException
     *             if there is not any space in manager.
     */
    public long tryAllocate(long size) throws OutOfSpaceException;

    /**
     * 
     * @param pos2Len
     */
    public void release(long pos) throws OutOfSpaceException;

    /**
     * 
     * @param beginningPos
     * @param size
     */
    public void extend(long beginningPos, long size);

    public long size();
}
