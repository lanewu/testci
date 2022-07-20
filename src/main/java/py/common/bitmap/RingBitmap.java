package py.common.bitmap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author zjm
 *
 */
public class RingBitmap {
    static public final int WORD_SHIFT = 6;

    static public final int WORD_LEN = 1 << WORD_SHIFT;

    static public final long WORD_MASK = 0xFFFFFFFFFFFFFFFFl;

    static private final Logger logger = LoggerFactory.getLogger(RingBitmap.class);

    final int nbits;

    final long[] words;

    int curIndex = -1;

    public RingBitmap(int nbits) {
        this.nbits = nbits;
        this.words = new long[(int) (((long) nbits + WORD_LEN - 1) / WORD_LEN)];
    }

    /**
     * 
     * @param bitIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public boolean get(int bitIndex) throws IndexOutOfBoundsException {
        if (bitIndex < 0 || bitIndex >= nbits) {
            throw new IndexOutOfBoundsException(String.format("Index %s is out of bound [0, %s)", bitIndex, nbits));
        }

        return (words[bitIndex >> WORD_SHIFT] & (1l << bitShift(bitIndex))) != 0;
    }

    /**
     * 
     * @param bitIndex
     * @throws IndexOutOfBoundsException
     */
    public void set(int bitIndex) throws IndexOutOfBoundsException {
        if (bitIndex < 0 || bitIndex >= nbits) {
            throw new IndexOutOfBoundsException(String.format("Index %s is out of bound [0, %s)", bitIndex, nbits));
        }

        words[bitIndex >> WORD_SHIFT] |= 1l << bitShift(bitIndex);

        curIndex = bitIndex;
    }

    /**
     * 
     * @param bitIndex
     * @throws IndexOutOfBoundsException
     */
    public void clear(int bitIndex) throws IndexOutOfBoundsException {
        if (bitIndex < 0 || bitIndex >= nbits) {
            throw new IndexOutOfBoundsException(String.format("Index %s is out of bound [0, %s)", bitIndex, nbits));
        }

        words[bitIndex >> WORD_SHIFT] &= ~(1l << bitShift(bitIndex));
    }

    /**
     * Find next clear bit from next of bit which is the last set (excluding the bit last set itself).
     * <p>
     * If there is no clear bit after the bit last set, we then find clear bit from beginning of bitmap.
     * 
     * @return bit index if exist clear bit or -1
     */
    public int nextClearBit() {
        int nextBitIndex = curIndex + 1;
        if (nextBitIndex >= nbits) {
            nextBitIndex = 0;
        }
        try {
            return nextClearBit(nextBitIndex);
        } catch (IndexOutOfBoundsException e) {
            logger.error("Unexpected exception", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find next clear bit from the given bit index (including the bit on the given index itself).
     * <p>
     * If there is no clear bit after the given bit, we then find clear bit from beginning of bitmap.
     * 
     * @param fromIndex
     *            the beginning position to find clear bit
     * @return bit index if exist clear bit or -1
     * @throws IndexOutOfBoundsException
     */
    public int nextClearBit(int fromIndex) throws IndexOutOfBoundsException {
        if (fromIndex < 0 || fromIndex >= nbits) {
            throw new IndexOutOfBoundsException(String.format("Index %s is out of bound [0, %s)", fromIndex, nbits));
        }

        try {
            for (int i = 0; i < nbits && get(fromIndex); i++) {
                fromIndex += 1;
                if (fromIndex == nbits) {
                    fromIndex = 0;
                }
            }

            return (get(fromIndex) ? -1 : fromIndex);
        } catch (IndexOutOfBoundsException e) {
            logger.error("Unexpected exception", e);
            throw new RuntimeException(e);
        }
    }

    private int bitShift(int bitIndex) {
        return bitIndex & ((1 << WORD_SHIFT) - 1);
    }
}
