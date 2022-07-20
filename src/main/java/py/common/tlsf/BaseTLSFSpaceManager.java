package py.common.tlsf;

import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.common.struct.Pair;

/**
 * 
 * @author zjm
 *
 */
public class BaseTLSFSpaceManager implements TLSFSpaceManager {
    private static final Logger logger = LoggerFactory.getLogger(BaseTLSFSpaceManager.class);

    protected final ReentrantLock lockForAllocateOrRelease = new ReentrantLock(false);

    /**
     * Instance to manipulate TLSF metadata: bitmap for first level, bitmap for second level and segregated list.
     */
    protected TLSFMetadata metadata;

    /**
     * Instance to manipulate each metadata of divided space.
     */
    protected TLSFSpaceDivisionMetadata divisionMetadata;

    protected int alignment = MIN_ALIGNMENT;

    /**
     * Shift of first level index in bitmap. E.g. The index after mapping is 7 and shift is 6, the final index in first
     * level will be 7 - 6 = 1.
     */
    protected int firstLevelIndexShift;

    protected long beginningPos;

    protected long endingPos;
    
    protected BaseTLSFSpaceManager() {
        
    }

    public BaseTLSFSpaceManager(TLSFMetadata metadata, TLSFSpaceDivisionMetadata divisionMetadata, int alignment,
            long beginningPos, long size) {
        applyTLSFToSpace(metadata, divisionMetadata, alignment, beginningPos, size);
    }

    public BaseTLSFSpaceManager(TLSFMetadata metadata, TLSFSpaceDivisionMetadata divisionMetadata, int alignment,
            long beginningPos, long size, boolean tlsfApplied) {
        if (tlsfApplied) {
            this.metadata = metadata;
            this.divisionMetadata = divisionMetadata;
            this.alignment = alignment;
            this.firstLevelIndexShift = TLSFSpaceManager.locateMostLeftOneBit(FIRST_LEVEL_INDEX_COUNT * alignment) - 1;
            this.beginningPos = 0;
            this.endingPos = beginningPos + size;
        } else {
            applyTLSFToSpace(metadata, divisionMetadata, alignment, beginningPos, size);
        }
    }

    protected void applyTLSFToSpace(TLSFMetadata metadata, TLSFSpaceDivisionMetadata divisionMetadata, int alignment,
            long beginningPos, long size) {
        this.metadata = metadata;
        this.divisionMetadata = divisionMetadata;
        this.alignment = alignment;
        this.firstLevelIndexShift = TLSFSpaceManager.locateMostLeftOneBit((long) FIRST_LEVEL_INDEX_COUNT * alignment)
                - 1;
        this.beginningPos = beginningPos;
        this.endingPos = beginningPos + size;

        long accessibleMemSize;
        if (divisionMetadata.seperated()) {
            accessibleMemSize = size;
        } else {
            if (size < 3 * TLSFSpaceDivisionMetadata.METADATA_UNIT_BYTES) {
                throw new IllegalArgumentException(
                        "Total size represented by beginning pos: " + beginningPos + ", ending pos: " + endingPos);
            }

            accessibleMemSize = size - TLSFSpaceDivisionMetadata.BUFFER_METADATA_OVERHEAD
                    - TLSFSpaceDivisionMetadata.METADATA_UNIT_BYTES;
        }

        logger.debug("Initialize space with size {}", accessibleMemSize);
        divisionMetadata.setPrePhysicalAddress(beginningPos, TLSFSpaceManager.NULL_SPACE_ADDRESS);
        divisionMetadata.setAddress(beginningPos);
        divisionMetadata.setAccessibleMemSize(beginningPos, accessibleMemSize);
        divisionMetadata.setFree(beginningPos);
        divisionMetadata.setPreUsed(beginningPos);

        Pair<Integer, Integer> fliWithSli = mapping(divisionMetadata.getAccessibleMemSize(beginningPos));
        insertFreeSpace(beginningPos, fliWithSli);
    }

    @Override
    public long allocate(long size) throws OutOfSpaceException {
        if (size <= 0) {
            throw new IllegalArgumentException("Illegal size: " + size);
        }

        long alignedSize = Math.max(alignUp(size),
                TLSFSpaceDivisionMetadata.METADATA_SIZE_BYTES - TLSFSpaceDivisionMetadata.BUFFER_METADATA_OVERHEAD);

        lockForAllocateOrRelease.lock();
        try {
            long targetSpaceAddress = pickoutFreeBuffer(alignedSize);
            if (targetSpaceAddress == TLSFSpaceManager.NULL_SPACE_ADDRESS) {
                throw new OutOfSpaceException();
            } else {
                prepareSpaceForUse(targetSpaceAddress, alignedSize);
                return targetSpaceAddress;
            }
        } finally {
            lockForAllocateOrRelease.unlock();
        }
    }

    @Override
    public long tryAllocate(long size) throws OutOfSpaceException {
        if (size <= 0) {
            throw new IllegalArgumentException("Illegal size: " + size);
        }

        long alignedSize = Math.max(alignUp(size),
                TLSFSpaceDivisionMetadata.METADATA_SIZE_BYTES - TLSFSpaceDivisionMetadata.BUFFER_METADATA_OVERHEAD);

        lockForAllocateOrRelease.lock();
        try {
            long targetSpaceAddress = pickoutFreeBuffer(alignedSize);
            if (targetSpaceAddress == TLSFSpaceManager.NULL_SPACE_ADDRESS) {
                targetSpaceAddress = pickoutBiggestFreeSpace();
                if (targetSpaceAddress == TLSFSpaceManager.NULL_SPACE_ADDRESS) {
                    throw new OutOfSpaceException();
                }
            }

            prepareSpaceForUse(targetSpaceAddress, alignedSize);
            return targetSpaceAddress;
        } finally {
            lockForAllocateOrRelease.unlock();
        }
    }

    @Override
    public void release(long pos) {
        lockForAllocateOrRelease.lock();
        try {
            if (pos < beginningPos || pos >= endingPos) {
                throw new IllegalArgumentException("Illegal pos: " + pos);
            }

            if (pos != divisionMetadata.getAddress(pos)) {
                throw new IllegalArgumentException("Illegal pos: " + pos);
            }

            divisionMetadata.setFree(pos);

            if (!isTheEnd(pos)) {
                long nextPhysicalBufferAddress = divisionMetadata.getNextPhysicalAddress(pos);
                if (nextPhysicalBufferAddress != TLSFSpaceManager.NULL_SPACE_ADDRESS) {
                    divisionMetadata.setPrePhysicalAddress(nextPhysicalBufferAddress, pos);
                    divisionMetadata.setPreFree(nextPhysicalBufferAddress);
                }
            }

            merge(pos);
        } finally {
            lockForAllocateOrRelease.unlock();
        }
    }

    @Override
    public void extend(long beginningPos, long size) {
        long endingPos = beginningPos + size;
        long accessibleMemSize;

        lockForAllocateOrRelease.lock();
        try {
            if (divisionMetadata.seperated()) {
                accessibleMemSize = size;
            } else {
                // extend behind and extend before may be some different, but result is same
                accessibleMemSize = size - TLSFSpaceDivisionMetadata.BUFFER_METADATA_OVERHEAD
                        - ((this.beginningPos != TLSFSpaceManager.NULL_SPACE_ADDRESS) ? 0
                                : TLSFSpaceDivisionMetadata.METADATA_UNIT_BYTES);
            }

            if (beginningPos == this.endingPos) {
                logger.info("Appending extra area with assemble size {} to the existing space!", accessibleMemSize);

                if (this.beginningPos != TLSFSpaceManager.NULL_SPACE_ADDRESS) {
                    beginningPos -= (divisionMetadata.seperated()) ? 0 : TLSFSpaceDivisionMetadata.METADATA_UNIT_BYTES;
                } else {
                    divisionMetadata.setPrePhysicalAddress(beginningPos, TLSFSpaceManager.NULL_SPACE_ADDRESS);
                }

                divisionMetadata.setAddress(beginningPos);
                divisionMetadata.setAccessibleMemSize(beginningPos, accessibleMemSize);
                divisionMetadata.setFree(beginningPos);
                divisionMetadata.setPreUsed(beginningPos);

                Pair<Integer, Integer> fliWithSli = mapping(divisionMetadata.getAccessibleMemSize(beginningPos));
                insertFreeSpace(beginningPos, fliWithSli);

                this.endingPos = endingPos;
            } else if (endingPos == this.beginningPos) {
                logger.info("Inserting extra area with accemble size {} before the existing space!", accessibleMemSize);

                divisionMetadata.setPrePhysicalAddress(beginningPos, TLSFSpaceManager.NULL_SPACE_ADDRESS);
                divisionMetadata.setAddress(beginningPos);
                divisionMetadata.setAccessibleMemSize(beginningPos, accessibleMemSize);
                divisionMetadata.setFree(beginningPos);
                divisionMetadata.setPreUsed(beginningPos);

                long nextPhysicalBufferAddress = divisionMetadata.getNextPhysicalAddress(beginningPos);
                if (nextPhysicalBufferAddress != TLSFSpaceManager.NULL_SPACE_ADDRESS) {
                    divisionMetadata.setPrePhysicalAddress(nextPhysicalBufferAddress, beginningPos);
                    divisionMetadata.setPreFree(nextPhysicalBufferAddress);
                }

                merge(beginningPos);

                this.beginningPos = beginningPos;
            } else {
                logger.error("Extending area must be continuous with the existing space!");
                throw new IllegalArgumentException("Beginning pos: " + beginningPos + ", size: " + size);
            }
        } finally {
            lockForAllocateOrRelease.unlock();
        }
    }

    @Override
    public long size() {
        lockForAllocateOrRelease.lock();
        try {
            return (beginningPos == TLSFSpaceManager.NULL_SPACE_ADDRESS) ? 0 : endingPos - beginningPos;
        } finally {
            lockForAllocateOrRelease.unlock();
        }
    }

    public TLSFMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(TLSFMetadata metadata) {
        this.metadata = metadata;
    }

    public TLSFSpaceDivisionMetadata getDivisionMetadata() {
        return divisionMetadata;
    }

    public void setDivisionMetadata(TLSFSpaceDivisionMetadata divisionMetadata) {
        this.divisionMetadata = divisionMetadata;
    }

    public int getAlignment() {
        return alignment;
    }

    public void setAlignment(int alignment) {
        this.alignment = alignment;
    }

    public long getBeginningPos() {
        return beginningPos;
    }

    public void setBeginningPos(long beginningPos) {
        this.beginningPos = beginningPos;
    }

    public long getEndingPos() {
        return endingPos;
    }

    public void setEndingPos(long endingPos) {
        this.endingPos = endingPos;
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
    protected Pair<Integer, Integer> mapping(long size) {
        int firstLevelIndex = 0, secondLevelIndex = 0;

        if (size < (long) SECOND_LEVEL_INDEX_COUNT * alignment) {
            secondLevelIndex = (int) (size / alignment);
        } else {
            firstLevelIndex = TLSFSpaceManager.locateMostLeftOneBit(size);
            secondLevelIndex = (int) (((size >> (firstLevelIndex - SECOND_LEVEL_INDEX_COUNT_LOG2))
                    ^ (1 << SECOND_LEVEL_INDEX_COUNT_LOG2)));
            firstLevelIndex -= firstLevelIndexShift;
        }

        return new Pair<Integer, Integer>(firstLevelIndex, secondLevelIndex);
    }

    /**
     * After get result of first level index and second level index, a suitable buffer may not locate at the position,
     * we need a search for it.
     * 
     * @param fliWithSli
     * @return the metadata of suitable buffer
     */
    protected long searchSuitableSpace(Pair<Integer, Integer> fliWithSli) {
        int firstLevelIndex = fliWithSli.getFirst(), secondLevelIndex = fliWithSli.getSecond();

        long secondLevelMap = metadata.getSecondLevelBitmap(firstLevelIndex) & (~0l << secondLevelIndex);
        // Check if it is necessary to find bigger space from first level.
        if (secondLevelMap == 0) {
            long firstLevelMap = metadata.getFirstLevelBitmap() & (~0l << (firstLevelIndex + 1));
            if (firstLevelMap == 0) {
                // Unable to find a suitable space for required size
                return NULL_SPACE_ADDRESS;
            }

            firstLevelIndex = TLSFSpaceManager.locateMostRightOneBit(firstLevelMap);
            secondLevelMap = metadata.getSecondLevelBitmap(firstLevelIndex);
        }

        secondLevelIndex = TLSFSpaceManager.locateMostRightOneBit(secondLevelMap);

        // Update fli and sli after suitable space has been found
        fliWithSli.setFirst(firstLevelIndex);
        fliWithSli.setSecond(secondLevelIndex);

        return metadata.getItemFromSegregatedList(firstLevelIndex, secondLevelIndex);
    }

    /**
     * @param bufferMetadata
     * @param fliWithSli
     */
    protected void removeFreeSpace(long freeSpaceAddress, Pair<Integer, Integer> fliWithSli) {
        Validate.isTrue(freeSpaceAddress != TLSFSpaceManager.NULL_SPACE_ADDRESS);
        Validate.isTrue(divisionMetadata.isFree(freeSpaceAddress));

        long preFreeBufferAddress = divisionMetadata.getPreFreeAddress(freeSpaceAddress);
        long nextFreeBufferAddress = divisionMetadata.getNextFreeAddress(freeSpaceAddress);

        if (preFreeBufferAddress != TLSFSpaceManager.NULL_SPACE_ADDRESS) {
            divisionMetadata.setNextFreeAddress(preFreeBufferAddress, nextFreeBufferAddress);
        }

        if (nextFreeBufferAddress != TLSFSpaceManager.NULL_SPACE_ADDRESS) {
            divisionMetadata.setPreFreeAddress(nextFreeBufferAddress, preFreeBufferAddress);
        }

        int firstLevelIndex = fliWithSli.getFirst(), secondLevelIndex = fliWithSli.getSecond();
        // current free buffer is the head of segregated list
        if (preFreeBufferAddress == TLSFSpaceManager.NULL_SPACE_ADDRESS) {
            metadata.setItemToSegregatedList(firstLevelIndex, secondLevelIndex, nextFreeBufferAddress);

            // current free buffer is the end of segregated list, and the list
            // will be empty after remove current buffer
            if (nextFreeBufferAddress == TLSFSpaceManager.NULL_SPACE_ADDRESS) {
                long secondLevelBitmap = metadata.getSecondLevelBitmap(firstLevelIndex);
                secondLevelBitmap &= ~(1l << secondLevelIndex);
                metadata.setSecondLevelBitmap(firstLevelIndex, secondLevelBitmap);

                // mark first level list empty
                if (secondLevelBitmap == 0) {
                    long firstLevelBitMap = metadata.getFirstLevelBitmap();
                    firstLevelBitMap &= ~(1l << firstLevelIndex);
                    metadata.setFirstLevelBitmap(firstLevelBitMap);
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
    protected void insertFreeSpace(long address, Pair<Integer, Integer> fliWithSli) {
        int firstLevelIndex = fliWithSli.getFirst(), secondLevelIndex = fliWithSli.getSecond();

        // put current buffer in head of two level segregated list
        long headBufferAddress = metadata.getItemFromSegregatedList(firstLevelIndex, secondLevelIndex);
        if (headBufferAddress != TLSFSpaceManager.NULL_SPACE_ADDRESS) {
            divisionMetadata.setPreFreeAddress(headBufferAddress, address);
        }
        divisionMetadata.setNextFreeAddress(address, headBufferAddress);
        divisionMetadata.setPreFreeAddress(address, TLSFSpaceManager.NULL_SPACE_ADDRESS);
        metadata.setItemToSegregatedList(firstLevelIndex, secondLevelIndex, address);

        // set the two bit map relative position not empty
        long firstLevelBitMap = metadata.getFirstLevelBitmap(),
                secondLevelBitmap = metadata.getSecondLevelBitmap(firstLevelIndex);

        firstLevelBitMap |= (1l << firstLevelIndex);
        metadata.setFirstLevelBitmap(firstLevelBitMap);

        secondLevelBitmap |= (1l << secondLevelIndex);
        metadata.setSecondLevelBitmap(firstLevelIndex, secondLevelBitmap);
    }

    protected long absorb(long preBufferAddress, long address) {
        Validate.isTrue(preBufferAddress != TLSFSpaceManager.NULL_SPACE_ADDRESS);
        Validate.isTrue(address != TLSFSpaceManager.NULL_SPACE_ADDRESS);

        long preBufferSize = divisionMetadata.getAccessibleMemSize(preBufferAddress);
        long curBufferSize = divisionMetadata.getAccessibleMemSize(address);
        long newSize;
        if (divisionMetadata.seperated()) {
            newSize = preBufferSize + curBufferSize;
        } else {
            newSize = preBufferSize + curBufferSize + TLSFSpaceDivisionMetadata.BUFFER_METADATA_OVERHEAD;
        }

        divisionMetadata.setAccessibleMemSize(preBufferAddress, newSize);

        if (!isTheEnd(address)) {
            long nextPhysicalBufferAddress = divisionMetadata.getNextPhysicalAddress(address);
            if (nextPhysicalBufferAddress != TLSFSpaceManager.NULL_SPACE_ADDRESS) {
                divisionMetadata.setPrePhysicalAddress(nextPhysicalBufferAddress, preBufferAddress);
                divisionMetadata.setPreFree(nextPhysicalBufferAddress);
            }
        }

        if (divisionMetadata instanceof SimpleTLSFSpaceDivisionMetadata) {
            ((SimpleTLSFSpaceDivisionMetadata) divisionMetadata).clear(address);
        }
        return preBufferAddress;
    }

    /**
     * merge current buffer with previous buffer
     * 
     * @param curBufferMetadata
     * @return
     */
    protected long mergePre(long address) {
        Validate.isTrue(address != TLSFSpaceManager.NULL_SPACE_ADDRESS);
        Validate.isTrue(divisionMetadata.isFree(address));

        if (divisionMetadata.isPreFree(address)) {
            long prePhysicalBufferAddress = divisionMetadata.getPrePhysicalAddress(address);
            long preBufferSize = divisionMetadata.getAccessibleMemSize(prePhysicalBufferAddress);

            Pair<Integer, Integer> fliWithSli = mapping(preBufferSize);
            removeFreeSpace(prePhysicalBufferAddress, fliWithSli);
            address = absorb(prePhysicalBufferAddress, address);
        }

        return address;
    }

    protected long mergeNext(long address) {
        Validate.isTrue(address != TLSFSpaceManager.NULL_SPACE_ADDRESS);
        Validate.isTrue(divisionMetadata.isFree(address));

        if (isTheEnd(address)) {
            return address;
        }

        long nextPhysicalBufferAddress = divisionMetadata.getNextPhysicalAddress(address);
        if (nextPhysicalBufferAddress != TLSFSpaceManager.NULL_SPACE_ADDRESS
                && divisionMetadata.isFree(nextPhysicalBufferAddress)) {
            Pair<Integer, Integer> fliWithSli = mapping(
                    divisionMetadata.getAccessibleMemSize(nextPhysicalBufferAddress));
            removeFreeSpace(nextPhysicalBufferAddress, fliWithSli);
            address = absorb(address, nextPhysicalBufferAddress);
        }

        return address;
    }

    protected void merge(long address) {
        address = mergePre(address);
        address = mergeNext(address);

        Pair<Integer, Integer> fliWithSli = mapping(divisionMetadata.getAccessibleMemSize(address));
        insertFreeSpace(address, fliWithSli);
    }

    /**
     * Split the buffer to two parts: The first part is the space with the given desired size; The second part is the
     * remaining space.
     * 
     * @param address
     *            address of free suitable space
     * @param size
     *            desired size of user allocation
     * @return address of the second part which is the remaining space
     */
    protected long splitBuffer(long address, long size) {
        if (address == TLSFSpaceManager.NULL_SPACE_ADDRESS) {
            throw new IllegalArgumentException("Illegal address: " + address);
        }
        if(!divisionMetadata.isFree(address)) {
            throw new IllegalArgumentException("Illegal space locating address: " + address);
        }

        long addrOfRemainingBufferMeta, sizeOfRemainingAccessableMem;

        if (divisionMetadata.seperated()) {
            addrOfRemainingBufferMeta = address + size;
            sizeOfRemainingAccessableMem = divisionMetadata.getAccessibleMemSize(address) - size;
        } else {
            addrOfRemainingBufferMeta = address + TLSFSpaceDivisionMetadata.ASSEMEM_OFFSET
                    + (size - TLSFSpaceDivisionMetadata.METADATA_UNIT_BYTES);
            sizeOfRemainingAccessableMem = divisionMetadata.getAccessibleMemSize(address)
                    - (size + TLSFSpaceDivisionMetadata.BUFFER_METADATA_OVERHEAD);
        }

        // initialize the remaining division space
        divisionMetadata.setPrePhysicalAddress(addrOfRemainingBufferMeta, address);
        divisionMetadata.setAddress(addrOfRemainingBufferMeta);
        divisionMetadata.setAccessibleMemSize(addrOfRemainingBufferMeta, sizeOfRemainingAccessableMem);
        divisionMetadata.setFree(addrOfRemainingBufferMeta);
        divisionMetadata.setPreFree(addrOfRemainingBufferMeta);

        if (!isTheEnd(address)) {
            // link the remaining division space to its next division
            long nextPhysicalBufferAddress = divisionMetadata.getNextPhysicalAddress(address);
            if (nextPhysicalBufferAddress != TLSFSpaceManager.NULL_SPACE_ADDRESS) {
                // the buffer is not end buffer of memory
                divisionMetadata.setPrePhysicalAddress(nextPhysicalBufferAddress, addrOfRemainingBufferMeta);
                divisionMetadata.setPreFree(nextPhysicalBufferAddress);
            }
        }

        divisionMetadata.setAccessibleMemSize(address, size);

        return addrOfRemainingBufferMeta;
    }

    /**
     * Fix the free space locating the given address to fit the given desired size. That is to put back the extra free
     * space.
     * 
     * @param address
     *            address of free suitable space
     * @param size
     *            desired size of user alloction.
     */
    protected void trimFreeSpace(long address, long size) {
        long splittingThreshold = (divisionMetadata.seperated()) ? size + alignment
                : (size + Math.max(TLSFSpaceDivisionMetadata.METADATA_SIZE_BYTES, alignment));

        // check if the buffer can be splitted
        if (divisionMetadata.getAccessibleMemSize(address) < splittingThreshold) {
            return;
        }

        long remainingBufferAddress = splitBuffer(address, size);
        Pair<Integer, Integer> fliWithSli = mapping(divisionMetadata.getAccessibleMemSize(remainingBufferAddress));
        insertFreeSpace(remainingBufferAddress, fliWithSli);
    }

    /**
     * 
     * @param size
     * @return
     */
    protected long pickoutFreeBuffer(long size) {
        // make the size biggest in list
        if (size >= (1l << SECOND_LEVEL_INDEX_COUNT_LOG2)) {
            long round = (1l << (TLSFSpaceManager.locateMostLeftOneBit(size) - SECOND_LEVEL_INDEX_COUNT_LOG2)) - 1;
            size += round;
        }

        Pair<Integer, Integer> fliWithSli = mapping(size);
        long suitableBufferAddress = searchSuitableSpace(fliWithSli);

        if (suitableBufferAddress != TLSFSpaceManager.NULL_SPACE_ADDRESS) {
            removeFreeSpace(suitableBufferAddress, fliWithSli);
        }

        return suitableBufferAddress;
    }

    /**
     * 
     * @return
     */
    protected long pickoutBiggestFreeSpace() {
        int firstLevelIndex, secondLevelIndex;

        firstLevelIndex = TLSFSpaceManager.locateMostLeftOneBit(metadata.getFirstLevelBitmap());
        if (firstLevelIndex < 0 || firstLevelIndex == TLSFSpaceManager.FIRST_LEVEL_INDEX_COUNT) {
            return TLSFSpaceManager.NULL_SPACE_ADDRESS;
        }

        secondLevelIndex = TLSFSpaceManager.locateMostLeftOneBit(metadata.getSecondLevelBitmap(firstLevelIndex));
        Pair<Integer, Integer> fliWithSli = new Pair<Integer, Integer>(firstLevelIndex, secondLevelIndex);

        long targetAddr = searchSuitableSpace(fliWithSli);
        removeFreeSpace(targetAddr, fliWithSli);

        return targetAddr;
    }

    /**
     * After a suitable free space for desired size being found, it is necessary to trim this space to fit the desired
     * size. Also, it is necessary to mark the free space is now used!
     * 
     * @param address
     *            address of free suitable space
     * @param size
     *            desired size of user allocation.
     * @return
     */
    protected void prepareSpaceForUse(long address, long size) {
        if (address == TLSFSpaceManager.NULL_SPACE_ADDRESS) {
            throw new IllegalArgumentException("Illegal address: " + address);
        }

        trimFreeSpace(address, size);

        if (!isTheEnd(address)) {
            long nextPhysicalBufferAddress = divisionMetadata.getNextPhysicalAddress(address);
            if (nextPhysicalBufferAddress != TLSFSpaceManager.NULL_SPACE_ADDRESS) {
                divisionMetadata.setPreUsed(nextPhysicalBufferAddress);
            }
        }

        divisionMetadata.setUsed(address);
    }

    /**
     * Align the given size up to its ceiling bound.
     * 
     * @param size
     * @return ceiling alignment bound of the given size
     */
    protected long alignUp(long size) {
        return (size + alignment - 1) & ~(alignment - 1);
    }

    /**
     * Align the given size down to its floor bound.
     * 
     * @param size
     * @return floor alignment bound of the given size
     */
    protected long alignDown(long size) {
        return size - (size & (alignment - 1));
    }

    protected boolean isTheEnd(long address) {
        return address + ((divisionMetadata.seperated()) ? 0 : TLSFSpaceDivisionMetadata.ASSEMEM_OFFSET)
                + divisionMetadata.getAccessibleMemSize(address) >= this.endingPos;
    }

}
