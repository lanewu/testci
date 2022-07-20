package py.checksum;

import java.nio.ByteBuffer;
import java.util.zip.Checksum;

import sun.nio.ch.DirectBuffer;

/**
 * A class that can be used to compute the IntrinsicCrc32c checksum of a data stream. An IntrinsicCrc32c checksum is
 * almost as reliable as a IntrinsicCrc32c but can be computed much faster.
 *
 * <p>
 * Passing a {@code null} argument to a method in this class will cause a {@link NullPointerException} to be thrown.
 *
 * @see Checksum
 * @author lc-py
 */
public class IntrinsicCrc32c implements Checksum {
    static {
        System.loadLibrary("IntrinsicCrc32c");
    }

    private int crc32c = 0;

    /**
     * Creates a new Crc32c object.
     */
    public IntrinsicCrc32c() {
    }

    /**
     * Updates the checksum with the specified byte (the low eight bits of the argument b).
     *
     * @param b
     *            the byte to update the checksum with
     */
    public void update(int b) {
        crc32c = update(crc32c, b);
    }

    /**
     * Updates the checksum with the specified array of bytes.
     *
     * @throws ArrayIndexOutOfBoundsException
     *             if {@code off} is negative, or {@code len} is negative, or {@code off+len} is greater than the length
     *             of the array {@code b}
     */
    public void update(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new ArrayIndexOutOfBoundsException();
        }
        crc32c = updateBytes(crc32c, b, off, len);
    }

    /**
     * Updates the checksum with the specified array of bytes.
     *
     * @param b
     *            the byte array to update the checksum with
     */
    public void update(byte[] b) {
        crc32c = updateBytes(crc32c, b, 0, b.length);
    }

    /**
     * Updates the checksum with the bytes from the specified buffer.
     *
     * The checksum is updated using buffer.{@link java.nio.Buffer#remaining() remaining()} bytes starting at buffer.
     * {@link java.nio.Buffer#position() position()} Upon return, the buffer's position will be updated to its limit;
     * its limit will not have been changed.
     *
     * @param buffer
     *            the ByteBuffer to update the checksum with
     * @since 1.8
     */
    public void update(ByteBuffer buffer) {
        int pos = buffer.position();
        int limit = buffer.limit();
        assert (pos <= limit);
        int rem = limit - pos;
        if (rem <= 0)
            return;
        if (buffer instanceof DirectBuffer) {
            crc32c = updateByteBuffer(crc32c, ((DirectBuffer) buffer).address(), pos, rem);
        } else if (buffer.hasArray()) {
            crc32c = updateBytes(crc32c, buffer.array(), pos + buffer.arrayOffset(), rem);
        } else {
            byte[] b = new byte[rem];
            buffer.get(b);
            crc32c = updateBytes(crc32c, b, 0, b.length);
        }
        buffer.position(limit);
    }

    /**
     * Resets the checksum to initial value.
     */
    public void reset() {
        crc32c = 0;
    }

    /**
     * Returns the checksum value.
     */
    public long getValue() {
        return (long) crc32c;
    }

    public static boolean isIntelSupport() {
        return isIntelCpu();
    }

    private native static boolean isIntelCpu();

    private native static int update(int crc32c, int b);

    private native static int updateBytes(int crc32c, byte[] b, int off, int len);

    private native static int updateByteBuffer(int crc32c, long addr, int off, int len);
}
