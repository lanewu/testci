package py.common.bitmap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;

public class AtomicBitSet {

    protected static Logger logger = LoggerFactory.getLogger(AtomicBitSet.class);

    /**
     * The internal field corresponding to the serialField "bits".
     */
    private AtomicLongArray words;

    private final int nbits;

    private final AtomicInteger setBits = new AtomicInteger(0);

    /*
     * BitSets are packed into arrays of "words."  Currently a word is
     * a long, which consists of 64 bits, requiring 6 address bits.
     * The choice of word size is determined purely by performance concerns.
     */
    private static final int ADDRESS_BITS_PER_WORD = 6;
    private final static int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;
    private static final long WORD_MASK = 0xffffffffffffffffL;

    public AtomicBitSet(int nbits) {
        // nbits can't be negative; size 0 is OK
        if (nbits < 0)
            throw new NegativeArraySizeException("nbits < 0: " + nbits);

        this.nbits = nbits;
        this.words = new AtomicLongArray(wordIndex(nbits - 1) + 1);
    }

    public int cardinality() {
        return setBits.get();
    }

    // just for test
    public int calculateCardinality() {
        int sum = 0;
        for (int i = 0; i < words.length(); i++) {
            sum += Long.bitCount(words.get(i));
        }
        return sum;
    }

    private static int wordIndex(int bitIndex) {
        return bitIndex >> ADDRESS_BITS_PER_WORD;
    }

    public int nextSetBitAndClear(int fromIndex) {
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("bitIndex is " + fromIndex + ", but max range is " + nbits);
        }

        if (fromIndex >= nbits) {
            return -1;
        }

        int u = wordIndex(fromIndex);

        long word = words.get(u) & (WORD_MASK << fromIndex);

        boolean first = true;
        while (true) {
            while (word != 0) {
                int bitIndex = Long.numberOfTrailingZeros(word);

                long newVal = word;
                newVal &= ~(1L << bitIndex);
                if (word != newVal) {
                    if (words.compareAndSet(u, word, newVal)) {
                        setBits.decrementAndGet();
                        return (u * BITS_PER_WORD) + bitIndex;
                    }
                }
                if (first) {
                    word = words.get(u) & (WORD_MASK << fromIndex);
                }
                else {
                    word = words.get(u);
                }
            }
            if (++u == words.length()) {
                return -1;
            }
            word = words.get(u);
            first = false;
        }
    }
    public int nextSetBit(int fromIndex) {
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("bitIndex is " + fromIndex + ", but max range is " + nbits);
        }

        if (fromIndex >= nbits) {
            return -1;
        }

        int u = wordIndex(fromIndex);

        long word = words.get(u) & (WORD_MASK << fromIndex);

        while (true) {
            if (word != 0) {
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            }
            if (++u == words.length()) {
                return -1;
            }
            word = words.get(u);
        }
    }

    public boolean get(int bitIndex) {
        if (bitIndex < 0) {
            throw new IndexOutOfBoundsException("bitIndex is " + bitIndex + ", but max range is " + nbits);
        }

        if (bitIndex >= nbits) {
            return false;
        }

        int wordIndex = wordIndex(bitIndex);
        return ((words.get(wordIndex) & (1L << bitIndex)) != 0);
    }

    public void clear(int bitIndex) {
        if (bitIndex < 0) {
            throw new IndexOutOfBoundsException("bitIndex is " + bitIndex + ", but max range is " + nbits);
        }

        if (bitIndex >= nbits) {
            return;
        }

        int wordIndex = wordIndex(bitIndex);

        for (; ; ) {
            long oldVal = words.get(wordIndex);
            long newVal = oldVal;
            newVal &= ~(1L << bitIndex);
            if (oldVal != newVal) {
                if (words.compareAndSet(wordIndex, oldVal, newVal)) {
                    setBits.decrementAndGet();
                    break;
                }
            } else {
                break;
            }
        }
    }

    public void set(int bitIndex) {
        if (bitIndex < 0 || bitIndex >= nbits) {
            throw new IndexOutOfBoundsException("bitIndex is " + bitIndex + ", but max range is " + nbits);
        }

        int wordIndex = wordIndex(bitIndex);

        for (; ; ) {
            long oldVal = words.get(wordIndex);
            long newVal = oldVal;
            newVal |= (1L << bitIndex);
            if (oldVal != newVal) {
                if (words.compareAndSet(wordIndex, oldVal, newVal)) {
                    setBits.incrementAndGet();
                    break;
                }
            } else {
                break;
            }
        }
    }

}
