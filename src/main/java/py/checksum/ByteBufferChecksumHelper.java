package py.checksum;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import py.exception.ChecksumMismatchedException;

/**
 * A helper class to calculate checksum of data in bytebuffer.
 * 
 * @author chenlia
 */
public class ByteBufferChecksumHelper {

    /**
     * The ByteBuffer's pointers are not changed
     * 
     * @param byteBuffer
     * @return CRC32 checksum
     */
    public static long computeCRC32Checksum(ByteBuffer byteBuffer) {
        return computeCRC32Checksum(byteBuffer, byteBuffer.position(), byteBuffer.limit() - byteBuffer.position());
    }

    /**
     * The ByteBuffer's pointers are not changed
     * 
     * @param byteBuffer
     * @param initialPosition
     *            the start position to read data
     * @param length
     *            the length the data to calculate checksum
     * @return CRC32 checksum
     */
    public static long computeCRC32Checksum(ByteBuffer byteBuffer, int initialPosition, int length) {
        Checksum Checksum = new CRC32();
        update(Checksum, byteBuffer, initialPosition, length);
        return Checksum.getValue();
    }

    /**
     * The ByteBuffer's pointers are not changed
     * 
     * @param checksumer
     *            the checksum that is used to calculate checksum
     * @param buf
     * @param initialPosition
     * @param length
     */
    public static void update(Checksum checksumer, ByteBuffer buf, int initialPosition, int length) {
        if (buf.hasArray()) {
            checksumer.update(buf.array(), buf.arrayOffset() + initialPosition, length);
        } else {
            for (int i = 0; i < length; i++) {
                checksumer.update(buf.get(initialPosition + i));
            }
        }
    }

    public static void verifyCRC32Checksum(ByteBuffer buf, int initialPosition, int length, long expectedChecksum)
            throws ChecksumMismatchedException {
        verifyChecksum(new CRC32(), buf, initialPosition, length, expectedChecksum);
    }

    public static void verifyChecksum(Checksum checksumer, ByteBuffer buf, int initialPosition, int length,
            long expectedChecksum) throws ChecksumMismatchedException {
        update(checksumer, buf, initialPosition, length);
        long calculatedChecksum = checksumer.getValue();
        if (calculatedChecksum != expectedChecksum) {
            byte[] array;
            if (buf.hasArray()) {
                array = buf.array();
            } else {
                array = new byte[length];
                for (int i = initialPosition; i < initialPosition + length; i++)
                    array[i] = buf.get(i);
            }
            
            throw new ChecksumMismatchedException(
                    "Checksum mismatched. start position: " + initialPosition,
                    expectedChecksum, calculatedChecksum); 
        }
    }
}
