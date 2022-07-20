package py.common.bitmap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Bitmap is used for some scenarios which may use a lot of bits to indicate some resource occupied or not.
 * 
 * For convenient, the bitmap size must be times of 8 in order to persist bitmap to disk. If not, the constructor will
 * throw exception. Bitmap is not thread safety. So the user must take care of object of bitmap with thread safety.
 * 
 * 
 * @author david
 * @author tyr
 *
 */
public class Bitmap {
    // the number of nbits
    final int nbits;
    // the underline data hold the bit map
    private volatile long[] words;
    private final int wordsLength;

    private static final int ADDRESS_BITS_PER_WORD = 6;
    private final static int WORD_PER_BYTE = 1 << ADDRESS_BITS_PER_WORD;

    private static final long WORD_MASK = 0xffffffffffffffffL;

    public Bitmap(int nbits) {
        if (nbits % 8 != 0)
            throw new IllegalArgumentException("the nbits must be byte aligned, but nbits is " + nbits);

        this.nbits = nbits;
        wordsLength = (int) (((long)nbits + WORD_PER_BYTE - 1) / WORD_PER_BYTE);
        words = new long[wordsLength];
    }

    /**
     * Set the bit indicated by bitIndex with true
     * 
     * @param bitIndex
     */
    public void set(int bitIndex) {
        if (bitIndex < 0 || nbits <= bitIndex)
            throw new IndexOutOfBoundsException("bitIndex is " + bitIndex + ", but max range is " + nbits);

        int wordIndex = wordIndex(bitIndex);
        words[wordIndex] |= (1l << bitIndex);
    }

    /**
     * Get the result of bit indicated by bitIndex
     * 
     * @param bitIndex
     * @return true if the bit has been set, or false
     */
    public boolean get(int bitIndex) {
        if (bitIndex < 0 || nbits <= bitIndex)
            throw new IndexOutOfBoundsException("bitIndex is " + bitIndex + ", but max range is " + nbits);

        int wordIndex = wordIndex(bitIndex);
        return ((words[wordIndex] & (1l << bitIndex)) != 0);
    }

    /**
     * Clear the bit
     */
    public void clear(int bitIndex) {
        if (bitIndex < 0 || nbits <= bitIndex)
            throw new IndexOutOfBoundsException("bitIndex is " + bitIndex + ", but max range is " + nbits);

        int wordIndex = wordIndex(bitIndex);
        words[wordIndex] &= ~(1l << bitIndex);
    }

    /**
     * Get the count of bits which has been set;
     * 
     * @return
     */
    public int cardinality() {
        int count = 0;
        for (int i = 0; i < wordsLength - 1; i++)
            count += Long.bitCount(words[i]);

        // deal with other data, those data may not occupied the whole long
        if (nbits % WORD_PER_BYTE == 0) {
            count += Long.bitCount(words[wordsLength - 1]);
        } else {
            int remainBytes = nbits % WORD_PER_BYTE / 8;
            long tmp = words[wordsLength - 1];
            while (remainBytes > 0) {
                count += this.cardinality((byte) (tmp & 0xff));
                tmp >>>= 8;
                remainBytes--;
            }
        }

        return count;
    }

    /**
     * Get the all count of bits;
     *
     * @return
     */
    public int getNbits() {
        return nbits;
    }

    /**
     * Return the bit array represent the bitmap
     * 
     * @return
     */
    public byte[] toByteArray() {
        byte[] bytes = new byte[nbits / 8];
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < wordsLength - 1; i++) {
            bb.putLong(words[i]);
        }

        // deal with other data, those data may not occupied the whole long
        if (nbits % WORD_PER_BYTE == 0) {
            bb.putLong(words[wordsLength - 1]);
        } else {
            int remainBytes = nbits % WORD_PER_BYTE / 8;
            long tmp = words[wordsLength - 1];
            while (remainBytes > 0) {
                bb.put((byte) (tmp & 0xff));
                tmp >>>= 8;
                remainBytes--;
            }
        }

        return bytes;
    }

    /**
     * Construct the a bitmap from an byte array
     * 
     * @param array
     * @return
     */
    public static Bitmap valueOf(byte[] array) {
        return new Bitmap(Arrays.copyOf(array, array.length));
    }

    /**
     * Get the next clear bit position beginning with fromIndex if there is available bitmap space, the nbits will be
     * return. The user should check the return value
     * 
     * @param fromIndex
     * @return
     */
    public int nextClearBit(int fromIndex) {
        if (fromIndex < 0 || fromIndex >= nbits)
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);

        int u = wordIndex(fromIndex);
        long value = (~words[u] & (WORD_MASK << fromIndex));

        while (true) {
            if (value != 0)
                return (u * WORD_PER_BYTE) + Long.numberOfTrailingZeros(value);
            if (++u == wordsLength)
                return wordsLength * WORD_PER_BYTE;
            value = ~words[u];
        }
    }

    public void clear() {
        int length = wordsLength;
        while (length > 0)
            words[--length] = 0;
    }

    /**
     * create bitmap from the array
     * 
     * @param array
     */
    private Bitmap(byte[] array) {
        this.nbits = array.length * 8;
        ByteBuffer bb = ByteBuffer.wrap(array).slice().order(ByteOrder.LITTLE_ENDIAN);
        wordsLength = (nbits + WORD_PER_BYTE - 1) / WORD_PER_BYTE;
        words = new long[wordsLength];

        int i = 0;
        while (bb.remaining() >= 8)
            words[i++] = bb.getLong();
        for (int remaining = bb.remaining(), j = 0; j < remaining; j++)
            words[i] |= (bb.get() & 0xffL) << (8 * j);
    }

    /**
     * Given a bit index, return word index containing it.
     */
    private static int wordIndex(int bitIndex) {
        return bitIndex >> ADDRESS_BITS_PER_WORD;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("bitmap: nbits=").append(nbits).append(", value below: ");
        int i = 0;
        while (i < wordsLength) {
            sb.append(Long.toHexString(words[i]));
            if (++i % 5 == 0) {
                sb.append("\n");
            } else {
                sb.append(", ");
            }
        }

        return sb.toString();
    }

    private int cardinality(byte value) {
        int n = 0;
        while (value != 0) {
            value &= (value - 1);
            n++;
        }
        return n;
    }

    @Override
    public boolean equals(Object another) {
        if (!(another instanceof Bitmap))
            return false;

        Bitmap anotherBitmap = (Bitmap) another;
        if (anotherBitmap.nbits != this.nbits)
            return false;

        for (int i = 0; i < words.length; i++) {
            if (words[i] != anotherBitmap.words[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Performs a logical <b>AND</b> of this target bit map with the
     * argument bit map. This bit map is modified so that each bit in it
     * has the value {@code true} if and only if it both initially
     * had the value {@code true} and the corresponding bit in the
     * bit map argument also had the value {@code true}.
     *
     * @param map a bit map
     */
    public void and(Bitmap map) {
        if (this == map)
            return;

        // Perform logical AND on words
        for (int i = 0; i < words.length; i++)
            words[i] &= map.words[i];

    }

    /**
     * Performs a logical <b>OR</b> of this bit map with the bit map
     * argument. This bit map is modified so that a bit in it has the
     * value {@code true} if and only if it either already had the
     * value {@code true} or the corresponding bit in the bit map
     * argument has the value {@code true}.
     *
     * @param map a bit map
     */
    public void or(Bitmap map) {
        if (this == map)
            return;

        // Perform logical OR on words
        for (int i = 0; i < words.length; i++)
            words[i] |= map.words[i];

    }

    public void inverse() {
        for (int i = 0; i < words.length; i++)
            words[i] ^= WORD_MASK;
    }

    public boolean allSet() {
        return cardinality() == nbits;
    }
}
