package py.checksum;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import py.exception.ChecksumMismatchedException;

/**
 * A helper class to calculate checksum for network.
 *
 * 
 * compute:use checksummer which configered when service start verify:use dynamic checksummer which should create by the
 * algorithm
 * 
 * By using this class, we avoid to format the whole io network.
 * 
 * 
 * @author lc, update by lx
 */

public class NetworkChecksumHelper {
    public static long computeChecksum(byte[] buffer) {
        return computeChecksum(buffer, 0, buffer.length);
    }

    public static long computeChecksum(byte[] buffer, int offset, int length) {
        Checksummer checksummer = NetworkChecksummerFactoryHelper.create();
        return checksummer.calculate(buffer, offset, length);
    }

    /**
     * Compute the checksum of the {@link #ByteBuffer} between position and limited of this buffer, the position of the
     * buffer will not be changed.
     * 
     * @param byteBuffer
     * @return
     */
    public static long computeChecksum(ByteBuffer byteBuffer) {
        Checksummer checksummer = NetworkChecksummerFactoryHelper.create();
        return checksummer.calculate(byteBuffer.duplicate());
    }

    /**
     * Compute the checksum of the {@link #ByteBuffer} between position and limited of this buffer, the readerIndex and
     * writerIndex of the buffer will not be changed.
     * 
     * @param byteBuffer
     * @return
     */
    public static long computeChecksum(ByteBuf byteBuf) {
        Checksummer checksummer = NetworkChecksummerFactoryHelper.create();
        return checksummer.calculate(byteBuf);
    }

    /**
     * verify whether the specified checksum is valid and also calculate the checksum of bytes in the buffer and check
     * whether it matches the specified checksum
     * 
     * @param buf
     * @param initialPosition
     * @param length
     * @param expectedChecksum
     * @return false if the specified checksum is invalid. True means everything is good
     * @throws ChecksumMismatchedException
     */
    public static boolean verifyChecksum(byte[] buffer, int offset, int length, long expectedChecksum)
            throws ChecksumMismatchedException {
        Checksummer checksummer = NetworkChecksummerFactoryHelper.create();
        long calculatedChecksum = checksummer.calculate(buffer, offset, length);

        if (calculatedChecksum == expectedChecksum) {
            return true;
        }

        throw new ChecksumMismatchedException("Checksum mismatched. start position: " + offset, expectedChecksum,
                calculatedChecksum);
    }

    /**
     * verify whether the specified checksum is valid and also calculate the checksum of bytes in the buffer and check
     * if it matches the specified checksum. After finishing the method, the position of {@link #ByteBuffer} won't be
     * changed.
     * 
     * @param byteBuffer
     * @param expectedChecksum
     * @return
     * @throws ChecksumMismatchedException
     */
    public static boolean verifyChecksum(ByteBuffer byteBuffer, long expectedChecksum)
            throws ChecksumMismatchedException {
        Checksummer checksummer = NetworkChecksummerFactoryHelper.create();
        long calculatedChecksum = checksummer.calculate(byteBuffer);

        if (calculatedChecksum == expectedChecksum) {
            return true;
        }

        throw new ChecksumMismatchedException("Checksum mismatched. start position: " + byteBuffer, expectedChecksum,
                calculatedChecksum);
    }

    /**
     * verify whether the specified checksum is valid and also calculate the checksum of bytes in the buffer and check
     * if it matches the specified checksum. After finishing the method, the position of {@link #ByteBuffer} won't be
     * changed.
     * 
     * @param byteBuf
     * @param expectedChecksum
     * @return
     * @throws ChecksumMismatchedException
     */
    public static boolean verifyChecksum(ByteBuf byteBuf, long expectedChecksum) throws ChecksumMismatchedException {
        Checksummer checksummer = NetworkChecksummerFactoryHelper.create();
        long calculatedChecksum = checksummer.calculate(byteBuf);

        if (calculatedChecksum == expectedChecksum) {
            return true;
        }

        throw new ChecksumMismatchedException("Checksum mismatched. start position: " + byteBuf, expectedChecksum,
                calculatedChecksum);
    }
}
