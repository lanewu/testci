package py.common;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.exception.NoAvailableBufferException;
import py.metrics.PYMetric;
import py.metrics.PYMetricRegistry;
import py.metrics.PYTimerContext;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

/**
 * An implementation of tlsf(two level segregated fit) memory allocator. The core of this allocator is structure to
 * store buffers and bit map to index buffers.
 * 
 * @author zjm
 */
public class TLSFFastBufferManager implements FastBufferManager {

    protected static final Logger logger = LoggerFactory.getLogger(TLSFFastBufferManager.class);

    /**
     * subdivisions of first level
     */
    private static final int FIRST_LEVEL_INDEX_COUNT = Long.SIZE;

    /**
     * subdivisions of second level
     */
    private static final int SECOND_LEVEL_INDEX_COUNT = Long.SIZE;

    /**
     * value for {@code SECOND_LEVEL_INDEX_COUNT} after function log with base 2
     */
    private static final int SECOND_LEVEL_INDEX_COUNT_LOG2 = Integer.numberOfTrailingZeros(SECOND_LEVEL_INDEX_COUNT);

    /**
     * The minimum alignment is 4 bytes
     */
    private static final int MIN_ALIGNMENT = 4;


    protected MetadataDescriptor metadataDescriptor;

    protected final long NULL_BUFFER_ADDRESS = Long.MAX_VALUE;

    /**
     * bit map for first level
     */
    private long firstLevelBitMap = 0l;

    /**
     * bit map for second level
     */
    private long[] secondLevelBitMap = new long[FIRST_LEVEL_INDEX_COUNT];

    /**
     * two-level segregated list
     */
    private long freeBuffers[][] = new long[FIRST_LEVEL_INDEX_COUNT][SECOND_LEVEL_INDEX_COUNT];

    // the least accessible memory size is total size of next_free_buffer_addr,
    // pre_free_buffer_addr and pre_physical_buffer
    private final long MIN_ACCESSIBLE_MEM_SIZE = 3 * MetadataDescriptor.METADATA_UNIT_BYTES;

    protected final static int MAX_BUFFER_QUEUE = 5000;

    private final static int DEFAULT_RATIO_OF_AVAILABLE_SPACE = 8;

    protected Queue<FastBuffer> bufferQueue = new LinkedList<FastBuffer>();

    /**
     * this value is always page size
     */
    private final long ALIGNMENT;

    private final long FIRST_LEVEL_INDEX_SHIFT;

    private final long memoryTotalSize;

    private final long memoryBaseAddress;

    private final long memoryEndAddress;

    // The range is 0 - 10
    private int RATIO_OF_AVAILABLE_SPACE;

    /**
     * it is used for synchronizing the allocation buffer and release buffer.
     */
    private final ReentrantLock lockForCreateAndRelease = new ReentrantLock(false);
    protected PYMetric counterOccupiedBuffer;
    protected PYMetric counterAllocatedBuffer;
    protected PYMetric counterReleasedBuffer;
    protected PYMetric meterAllocateBuf;
    protected PYMetric meterReleaseBuf;
    protected PYMetric counterUnusedSize;
    protected PYMetric counterAllocateFailure;
    protected PYMetric timerAllocateBuffers;

    public TLSFFastBufferManager(long totalSize) {
        this(MIN_ALIGNMENT, totalSize);
    }

    public TLSFFastBufferManager(long totalSize, String className) {
        this(MIN_ALIGNMENT, totalSize, DEFAULT_RATIO_OF_AVAILABLE_SPACE, className);
    }

    public TLSFFastBufferManager(int alignment, long totalSize, String className) {
        this(alignment, totalSize, DEFAULT_RATIO_OF_AVAILABLE_SPACE, className);
    }
    
    public TLSFFastBufferManager(int alignment, long totalSize) {
        this(alignment, totalSize, DEFAULT_RATIO_OF_AVAILABLE_SPACE);
    }

    public TLSFFastBufferManager(int alignment, long totalSize, int ratioOfAvailableBuffer) {
        this(alignment, totalSize, ratioOfAvailableBuffer, TLSFFastBufferManager.class.getSimpleName());
    }

    public TLSFFastBufferManager(int alignment, long totalSize, int ratioOfAvailableBuffer, String className) {
        this.RATIO_OF_AVAILABLE_SPACE = ratioOfAvailableBuffer;
        Validate.isTrue(ratioOfAvailableBuffer <= 10 && ratioOfAvailableBuffer > 0);
        this.ALIGNMENT = alignment;
        this.FIRST_LEVEL_INDEX_SHIFT = locateMostLeftOneBit(SECOND_LEVEL_INDEX_COUNT * ALIGNMENT);
        this.memoryTotalSize = totalSize;

        this.memoryBaseAddress = DirectAlignedBufferAllocator.allocateMemory(totalSize);
        this.memoryEndAddress = this.memoryBaseAddress + totalSize - 1;
        this.metadataDescriptor = new MetadataDescriptor();

        for (int i = 0; i < FIRST_LEVEL_INDEX_COUNT; i++) {
            for (int j = 0; j < SECOND_LEVEL_INDEX_COUNT; j++) {
                freeBuffers[i][j] = NULL_BUFFER_ADDRESS;
            }
        }

        metadataDescriptor.setPrePhysicalAddress(memoryBaseAddress, NULL_BUFFER_ADDRESS);
        metadataDescriptor.setAddress(memoryBaseAddress);
        // make the allocated memory as a buffer who has metadata and
        // accessible memory. All space are available for user to use
        // except the two metadata fields "size" and "current_phy_address"
        // Note that the metadata pre_physical_address is saved in the end of
        // previous buffer.
        long accessibleMemSize = memoryTotalSize - MetadataDescriptor.BUFFER_METADATA_OVERHEAD
                        - MetadataDescriptor.METADATA_UNIT_BYTES;

        metadataDescriptor.setAccessibleMemSize(memoryBaseAddress, accessibleMemSize);
        metadataDescriptor.setFree(memoryBaseAddress);
        // although the big buffer does not have a previous buffer, to unify the
        // process of preUsed bit, set this bit
        metadataDescriptor.setPreUsed(memoryBaseAddress);

        // merge this big buffer to two level segregated list
        // fli: firstLevelIndex
        // Sli: secondLevelIndex
        Pair<Integer, Integer> fliWithSli = mapping(metadataDescriptor.getAccessibleMemSize(memoryBaseAddress));
        insertFreeBuffer(memoryBaseAddress, fliWithSli);
        
        // init the metric for this fast buffer
        counterOccupiedBuffer = PYMetricRegistry.getMetricRegistry().register(
                MetricRegistry.name(className, "counter_occupied_buffer"), Counter.class);
        counterAllocatedBuffer = PYMetricRegistry.getMetricRegistry().register(
                MetricRegistry.name(className, "counter_allocated_buffer"), Counter.class);
        counterReleasedBuffer = PYMetricRegistry.getMetricRegistry().register(
                MetricRegistry.name(className, "counter_released_buffer"), Counter.class);
        counterUnusedSize = PYMetricRegistry.getMetricRegistry().register(
                MetricRegistry.name(className, "counter_Unused_buffer"), Counter.class);
        counterUnusedSize.incCounter(memoryTotalSize);
        meterAllocateBuf = PYMetricRegistry.getMetricRegistry().register(
                MetricRegistry.name(className, "meter_allocate_buffer"), Meter.class);
        meterReleaseBuf = PYMetricRegistry.getMetricRegistry().register(
                MetricRegistry.name(className, "meter_release_buffer"), Meter.class);
        counterAllocateFailure = PYMetricRegistry.getMetricRegistry().register(
                MetricRegistry.name(className, "counter_allocate_failure"), Counter.class);
        timerAllocateBuffers = PYMetricRegistry.getMetricRegistry().register(
                MetricRegistry.name(className, "timer_allocate_buffers"), Timer.class);
        logger.warn("initialize size {} successfully", totalSize);
    }

    @Override
    public FastBuffer allocateBuffer(long size) throws NoAvailableBufferException {
        if (size <= 0) {
            logger.error("Invalid size {}, the requested size to allocate must be positive", size);
            return null;
        }
        // align the requested size to alignment
        long adjustedSize = adjustRequestSize(size);

        FastBuffer fastBuffer = null;
        lockForCreateAndRelease.lock();
        try {
            long targetBufferAddress = pickoutFreeBuffer(adjustedSize);
            if (targetBufferAddress == NULL_BUFFER_ADDRESS) {
                counterAllocateFailure.incCounter();
                throw new NoAvailableBufferException();
            }

            prepareBufferForUse(targetBufferAddress, adjustedSize);

            fastBuffer = bufferQueue.poll();
            if (fastBuffer == null) {
                fastBuffer = new FastBufferImpl(targetBufferAddress, size);
            } else {
                fastBuffer = ((FastBufferImpl) fastBuffer).reuse(targetBufferAddress, size);
            }
        } finally {
            lockForCreateAndRelease.unlock();
        }

        counterOccupiedBuffer.incCounter();
        counterAllocatedBuffer.incCounter();
        meterAllocateBuf.mark();
        counterUnusedSize.decCounter(size);

        return fastBuffer;
    }

    @Override
    public List<FastBuffer> allocateBuffers(long size) throws NoAvailableBufferException {
        long alignment = getAlignmentSize();
        List<FastBuffer> fastBuffers = new ArrayList<FastBuffer>();
        lockForCreateAndRelease.lock();
        PYTimerContext timerAllocate = timerAllocateBuffers.time();
        try {
            try {
                FastBuffer fastBuffer = allocateBuffer(size);
                fastBuffers.add(fastBuffer);
                return fastBuffers;
            } catch (NoAvailableBufferException e) {
                if (size <= alignment) {
                    throw e;
                }
            }

            // It is failure to allocate a buffer in one time, and we want to allocate buffer which size is larger than
            // the alignment. so we should try to allocate buffers which count is the times of the alignment.
            boolean needReleaseBuffer = true;
            try {
                long leftSize = size;
                // TODO: i must optimize the allocate strategy?
                while (leftSize > 0) {
                    long min = Math.min(alignment, leftSize);
                    fastBuffers.add(allocateBuffer(min));
                    leftSize -= min;
                }

                needReleaseBuffer = false;
                return fastBuffers;
            } catch (Exception e1) {
                throw new NoAvailableBufferException("allocate failure in second time, size: " + size);
            } finally {
                if (needReleaseBuffer) {
                    for (FastBuffer fastBuffer : fastBuffers) {
                        releaseBuffer(fastBuffer);
                    }
                }
            }
        } finally {
            lockForCreateAndRelease.unlock();
            timerAllocate.stop();
        }
    }

    public  void releaseBuffer(FastBuffer retbuf) {
        Validate.isTrue(retbuf instanceof FastBufferImpl);
        long bufferSize = retbuf.size();
        lockForCreateAndRelease.lock();
        try {
            long address = ((FastBufferImpl) retbuf).accessibleMemAddress - MetadataDescriptor.ACCESSIBLE_MEM_OFFESET;
            if (address == NULL_BUFFER_ADDRESS) {
                logger.warn("The buffer has been released yet");
                return;
            }

            Validate.isTrue(address == metadataDescriptor.getAddress(address));
            retbuf = ((FastBufferImpl) retbuf).reuse(NULL_BUFFER_ADDRESS, 0);
            if (bufferQueue.size() < MAX_BUFFER_QUEUE) {
                bufferQueue.offer(retbuf);
            }

            metadataDescriptor.setFree(address);
            long nextPhysicalBufferAddress = metadataDescriptor.getNextPhysicalAddress(address);
            if (nextPhysicalBufferAddress != NULL_BUFFER_ADDRESS && !isEndOfMem(nextPhysicalBufferAddress)) {
                metadataDescriptor.setPrePhysicalAddress(nextPhysicalBufferAddress, address);
                metadataDescriptor.setPreFree(nextPhysicalBufferAddress);
            }
            merge(address);
        } finally {
            lockForCreateAndRelease.unlock();
        }

        counterOccupiedBuffer.decCounter();
        counterReleasedBuffer.incCounter();
        meterReleaseBuf.mark();
        counterUnusedSize.incCounter(bufferSize);
    }

    /**
     * The implementation of mapping function. The function is used for removing from or inserting to two-level
     * segregated list.
     * <p>
     * Mapping function: mapping(size) -> (f, s) <br/>
     * f = low_bound(log2(size)) <br/>
     * s = (size - power(2, f))*power(2, SLI)/power(2, f)
     * 
     * @param size
     * @return a pair of first level index and second level index
     */
    private Pair<Integer, Integer> mapping(long size) {
        int firstLevelIndex = 0, secondLevelIndex = 0;

        // TODO: use a smaller size as small accessible memory size
        if (size < SECOND_LEVEL_INDEX_COUNT * ALIGNMENT) {
            secondLevelIndex = (int) (size / ALIGNMENT);
        } else {
            firstLevelIndex = locateMostLeftOneBit(size);
            secondLevelIndex = (int) (((size >> (firstLevelIndex - SECOND_LEVEL_INDEX_COUNT_LOG2)) ^ (1 << SECOND_LEVEL_INDEX_COUNT_LOG2)));
            // no use of buffer whose size is smaller than most small buffer
            // size
            firstLevelIndex -= (FIRST_LEVEL_INDEX_SHIFT - 1);
        }

        return new MutablePair<Integer, Integer>(firstLevelIndex, secondLevelIndex);
    }

    /**
     * After get result of first level index and second level index, a suitable buffer may not locate at the position,
     * we need a search for it.
     * 
     * @param fliWithSli
     * @return the metadata of suitable buffer
     */
    private long searchSuitableBuffer(Pair<Integer, Integer> fliWithSli) {
        int firstLevelIndex = fliWithSli.getLeft(), secondLevelIndex = fliWithSli.getRight();

        long secondLevelMap = secondLevelBitMap[firstLevelIndex] & (~0l << secondLevelIndex);
        // the bit in first level bit map is empty
        if (secondLevelMap == 0) {
            long firstLevelMap = firstLevelBitMap & (~0l << (firstLevelIndex + 1));

            if (firstLevelMap == 0) {
                return NULL_BUFFER_ADDRESS;
            }

            firstLevelIndex = locateMostRightOneBit(firstLevelMap);
            secondLevelMap = secondLevelBitMap[firstLevelIndex];
        }

        secondLevelIndex = locateMostRightOneBit(secondLevelMap);

        ((MutablePair<Integer, Integer>) fliWithSli).setLeft(firstLevelIndex);
        ((MutablePair<Integer, Integer>) fliWithSli).setRight(secondLevelIndex);

        return freeBuffers[firstLevelIndex][secondLevelIndex];
    }

    /**
     * @param bufferMetadata
     * @param fliWithSli
     */
    private void removeFreeBuffer(long curFreeBufferAddress, Pair<Integer, Integer> fliWithSli) {
        Validate.isTrue(curFreeBufferAddress != NULL_BUFFER_ADDRESS);
        if (!metadataDescriptor.isFree(curFreeBufferAddress)) {
            logger.error("the buffer addressed at {} is not free. {}, {}, {}, {}, {} ", curFreeBufferAddress,
                            metadataDescriptor.getAccessibleMemSize(curFreeBufferAddress),
                            metadataDescriptor.getNextFreeAddress(curFreeBufferAddress),
                            metadataDescriptor.getNextPhysicalAddress(curFreeBufferAddress),
                            metadataDescriptor.getPreFreeAddress(curFreeBufferAddress),
                            metadataDescriptor.getPrePhysicalAddress(curFreeBufferAddress));
            Validate.isTrue(metadataDescriptor.isFree(curFreeBufferAddress));
        }

        long preFreeBufferAddress = metadataDescriptor.getPreFreeAddress(curFreeBufferAddress);
        long nextFreeBufferAddress = metadataDescriptor.getNextFreeAddress(curFreeBufferAddress);

        if (preFreeBufferAddress != NULL_BUFFER_ADDRESS) {
            metadataDescriptor.setNextFreeAddress(preFreeBufferAddress, nextFreeBufferAddress);
        }
        if (nextFreeBufferAddress != NULL_BUFFER_ADDRESS) {
            metadataDescriptor.setPreFreeAddress(nextFreeBufferAddress, preFreeBufferAddress);
        }

        int firstLevelIndex = fliWithSli.getLeft(), secondLevelIndex = fliWithSli.getRight();
        // current free buffer is the head of segregated list
        if (preFreeBufferAddress == NULL_BUFFER_ADDRESS) {
            freeBuffers[firstLevelIndex][secondLevelIndex] = nextFreeBufferAddress;

            // current free buffer is the end of segregated list, and the list
            // will be empty after remove current buffer
            if (nextFreeBufferAddress == NULL_BUFFER_ADDRESS) {
                // empty list
                secondLevelBitMap[firstLevelIndex] &= ~(1l << secondLevelIndex);

                // mark first level list empty
                if (secondLevelBitMap[firstLevelIndex] == 0) {
                    firstLevelBitMap &= ~(1l << firstLevelIndex);
                }

            }
        }
    }

    /**
     * Insert the free buffer metadata to head of segregated list.
     * 
     * @param bufferMetadata
     * @param fliWithSli
     */
    private void insertFreeBuffer(long address, Pair<Integer, Integer> fliWithSli) {
        int firstLevelIndex = fliWithSli.getLeft(), secondLevelIndex = fliWithSli.getRight();

        // put current buffer in head of two level segregated list
        long headBufferAddress = freeBuffers[firstLevelIndex][secondLevelIndex];
        if (headBufferAddress != NULL_BUFFER_ADDRESS) {
            metadataDescriptor.setPreFreeAddress(headBufferAddress, address);
        }
        // TODO: if accessable size is less than 2 * long.size
        metadataDescriptor.setNextFreeAddress(address, headBufferAddress);
        metadataDescriptor.setPreFreeAddress(address, NULL_BUFFER_ADDRESS);
        freeBuffers[firstLevelIndex][secondLevelIndex] = address;

        // set the two bit map relative position not empty
        firstLevelBitMap |= (1l << firstLevelIndex);
        secondLevelBitMap[firstLevelIndex] |= (1l << secondLevelIndex);
    }

    private long absorb(long preBufferAddress, long address) {
        Validate.isTrue(preBufferAddress != NULL_BUFFER_ADDRESS);
        Validate.isTrue(address != NULL_BUFFER_ADDRESS);

        long preBufferSize = metadataDescriptor.getAccessibleMemSize(preBufferAddress);
        long curBufferSize = metadataDescriptor.getAccessibleMemSize(address);
        long newSize = preBufferSize + curBufferSize + MetadataDescriptor.BUFFER_METADATA_OVERHEAD;

        metadataDescriptor.setAccessibleMemSize(preBufferAddress, newSize);

        long nextPhysicalBufferAddress = metadataDescriptor.getNextPhysicalAddress(address);
        if (nextPhysicalBufferAddress != NULL_BUFFER_ADDRESS && !isEndOfMem(nextPhysicalBufferAddress)) {
            metadataDescriptor.setPrePhysicalAddress(nextPhysicalBufferAddress, preBufferAddress);
            metadataDescriptor.setPreFree(nextPhysicalBufferAddress);
        }

        return preBufferAddress;
    }

    /**
     * merge current buffer with previous buffer
     * 
     * @param curBufferMetadata
     * @return
     */
    private long mergePre(long address) {
        Validate.isTrue(address != NULL_BUFFER_ADDRESS);
        Validate.isTrue(metadataDescriptor.isFree(address));

        if (metadataDescriptor.isPreFree(address)) {
            long prePhysicalBufferAddress = metadataDescriptor.getPrePhysicalAddress(address);
            long preBufferSize = metadataDescriptor.getAccessibleMemSize(prePhysicalBufferAddress);

            Pair<Integer, Integer> fliWithSli = mapping(preBufferSize);
            removeFreeBuffer(prePhysicalBufferAddress, fliWithSli);
            address = absorb(prePhysicalBufferAddress, address);
        }

        return address;
    }

    private long mergeNext(long address) {
        Validate.isTrue(address != NULL_BUFFER_ADDRESS);
        Validate.isTrue(metadataDescriptor.isFree(address));

        long nextPhysicalBufferAddress = metadataDescriptor.getNextPhysicalAddress(address);
        if (nextPhysicalBufferAddress != NULL_BUFFER_ADDRESS && !isEndOfMem(nextPhysicalBufferAddress)
                        && metadataDescriptor.isFree(nextPhysicalBufferAddress)) {
            Pair<Integer, Integer> fliWithSli = mapping(metadataDescriptor
                            .getAccessibleMemSize(nextPhysicalBufferAddress));
            removeFreeBuffer(nextPhysicalBufferAddress, fliWithSli);
            address = absorb(address, nextPhysicalBufferAddress);
        }

        return address;
    }

    protected void merge(long address) {
        address = mergePre(address);
        address = mergeNext(address);

        Pair<Integer, Integer> fliWithSli = mapping(metadataDescriptor.getAccessibleMemSize(address));
        insertFreeBuffer(address, fliWithSli);
    }

    /**
     * Split the buffer to two parts: one part is to allocate out, one is remaining.
     * 
     * @param bufferMetadata
     * @param size
     * @return
     */
    private long splitBuffer(long address, long size) {
        Validate.isTrue(address != NULL_BUFFER_ADDRESS);
        Validate.isTrue(metadataDescriptor.isFree(address));
        Validate.isTrue(metadataDescriptor.getAccessibleMemSize(address) >= size
                        + MetadataDescriptor.METADATA_SIZE_BYTES);

        long remainingBufferMetadataAddress = address + MetadataDescriptor.ACCESSIBLE_MEM_OFFESET
                        + (size - MetadataDescriptor.METADATA_UNIT_BYTES);
        long remainingAccessableMemSize = metadataDescriptor.getAccessibleMemSize(address)
                        - (size + MetadataDescriptor.BUFFER_METADATA_OVERHEAD);

        metadataDescriptor.setPrePhysicalAddress(remainingBufferMetadataAddress, address);
        metadataDescriptor.setAddress(remainingBufferMetadataAddress);
        metadataDescriptor.setAccessibleMemSize(remainingBufferMetadataAddress, remainingAccessableMemSize);
        metadataDescriptor.setFree(remainingBufferMetadataAddress);
        metadataDescriptor.setPreFree(remainingBufferMetadataAddress);

        long nextPhysicalBufferAddress = metadataDescriptor.getNextPhysicalAddress(address);
        if (nextPhysicalBufferAddress != NULL_BUFFER_ADDRESS && !isEndOfMem(nextPhysicalBufferAddress)) {
            // the buffer is not end buffer of memory
            metadataDescriptor.setPrePhysicalAddress(nextPhysicalBufferAddress, remainingBufferMetadataAddress);
            metadataDescriptor.setPreFree(nextPhysicalBufferAddress);
        }

        metadataDescriptor.setAccessibleMemSize(address, size);

        return remainingBufferMetadataAddress;
    }

    /**
     * remove remaining buffer from current buffer
     * 
     * @param bufferMetadata
     * @param size
     */
    private void trimFreeBuffer(long address, long size) {
        // check if the buffer can split
        if (metadataDescriptor.getAccessibleMemSize(address) >= size + MetadataDescriptor.METADATA_SIZE_BYTES) {
            long remainingBufferAddress = splitBuffer(address, size);
            Pair<Integer, Integer> fliWithSli = mapping(metadataDescriptor.getAccessibleMemSize(remainingBufferAddress));
            insertFreeBuffer(remainingBufferAddress, fliWithSli);
        }
    }

    /**
     * 
     * @param size
     * @return
     */
    protected long pickoutFreeBuffer(long size) {
        // make the size biggest in list
        if (size >= (1l << SECOND_LEVEL_INDEX_COUNT_LOG2)) {
            long round = (1l << (locateMostLeftOneBit(size) - SECOND_LEVEL_INDEX_COUNT_LOG2)) - 1;
            size += round;
        }

        Pair<Integer, Integer> fliWithSli = mapping(size);
        long suitableBufferAddress = searchSuitableBuffer(fliWithSli);

        if (suitableBufferAddress != NULL_BUFFER_ADDRESS) {
            removeFreeBuffer(suitableBufferAddress, fliWithSli);
        }

        return suitableBufferAddress;
    }

    /**
     * 
     * @param bufferMetadata
     * @param size
     * @return
     */
    protected void prepareBufferForUse(long address, long size) {
        Validate.isTrue(address != NULL_BUFFER_ADDRESS);

        trimFreeBuffer(address, size);

        long nextPhysicalBufferAddress = metadataDescriptor.getNextPhysicalAddress(address);
        if (nextPhysicalBufferAddress != NULL_BUFFER_ADDRESS && !isEndOfMem(nextPhysicalBufferAddress)) {
            metadataDescriptor.setPreUsed(nextPhysicalBufferAddress);
        }

        metadataDescriptor.setUsed(address);
    }

    private long roundUp(long size) {
        if (size >= (1 << SECOND_LEVEL_INDEX_COUNT_LOG2)) {
            final long round = (1 << (locateMostLeftOneBit(size) - SECOND_LEVEL_INDEX_COUNT_LOG2)) - 1;
            size += round;
        }

        return size;
    }

    protected boolean isEndOfMem(long address) {
        // return address + MetadataDescriptor.METADATA_SIZE_BYTES + MetadataDescriptor.METADATA_UNIT_BYTES >
        // this.memoryEndAddress;
        return address + MetadataDescriptor.METADATA_UNIT_BYTES > this.memoryEndAddress;
    }

    protected long adjustRequestSize(long size) {
        return Math.max(alignUp(size), MIN_ACCESSIBLE_MEM_SIZE);
    }

    private long alignUp(long size) {
        return (size + ALIGNMENT - 1) & ~(ALIGNMENT - 1);
    }

    private long alignDown(long size) {
        return size - (size & (ALIGNMENT - 1));
    }

    private static int locateMostRightOneBit(long value) {
        return Long.numberOfTrailingZeros(value);
    }

    private static int locateMostLeftOneBit(long value) {
        return Long.SIZE - Long.numberOfLeadingZeros(value) - 1;
    }

    public void printFliSli() {

    }

    public long getFirstAccessibleMemSize() {
        return metadataDescriptor.getAccessibleMemSize(memoryBaseAddress);
    }

    @Override
    public void close() {
    }

    @Override
    public long size() {
        return (memoryTotalSize * RATIO_OF_AVAILABLE_SPACE) / 10;
    }

    @Override
    public long getAlignmentSize() {
        return ALIGNMENT;
    }
}
