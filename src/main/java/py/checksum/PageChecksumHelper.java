package py.checksum;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.checksum.ChecksummerConfiguration.Algorithm;
import py.exception.ChecksumMismatchedException;
import py.exception.UnsupportedChecksumAlgorithmException;

/**
 * A helper class to calculate checksum of a page.
 * 
 * The checksum has 8 bytes. It consists of two parts. The first part is the 4 least significant bytes of 8 byte
 * calculated checksum (adler32). The second part is the predefined magic number which indicates the first part is a
 * calculated checksum instead of a random number.
 * 
 * compute:use checksummer which configured when service start<br>
 * verify:use dynamic checksummer which should create by the checksum magic number of the expected checksum <br>
 * --------------------------------------------<br>
 * magic number (4 bytes) | checksum (4 bytes) <br>
 * --------------------------------------------<br>
 * 
 * By using this class, we avoid to format the whole archive.
 * 
 * It makes use of ByteBufferChecksumHelper to calculate 8 byte checksum
 * 
 * 
 * @author chenlia
 */

public class PageChecksumHelper {
    public static int CHECKSUM_MAGIC_NUM = Algorithm.CRC32.getChecksumMagicNum();

    private static final Logger logger = LoggerFactory.getLogger(PageChecksumHelper.class);

    public static void setChecksumMagicNum(Algorithm algorithm) throws UnsupportedChecksumAlgorithmException {
        try {
            CHECKSUM_MAGIC_NUM = algorithm.getChecksumMagicNum();
        } catch (Exception e) {
            throw new UnsupportedChecksumAlgorithmException("not supported algorithm: " + algorithm);
        }
    }

    /**
     * compute checksum of the {@link #ByteBuffer}, it will modify the position of {@link #ByteBuffer}.
     * 
     * @param byteBuffer
     * @return
     */
    public static long computeChecksum(ByteBuffer byteBuffer) {
        Checksummer checksummer = ChecksummerFactoryHelper.create();
        ByteBuffer result = ByteBuffer.allocate(8);
        result.putLong(0, checksummer.calculate(byteBuffer));
        /**
         * overwrite the magic number to the first 4 bytes
         */
        result.putInt(0, CHECKSUM_MAGIC_NUM);
        return result.getLong();
    }

    public static long computeChecksum(byte[] buffer, int offset, int length) {
        Checksummer checksummer = ChecksummerFactoryHelper.create();
        ByteBuffer result = ByteBuffer.allocate(8);

        result.putLong(0, checksummer.calculate(buffer, offset, length));
        /**
         * overwrite the magic number to the first 4 bytes
         */
        result.putInt(0, CHECKSUM_MAGIC_NUM);
        return result.getLong();
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
        /**
         * get checksummer by magic number of the expected checksum
         */
        ByteBuffer expectedChecksumBuf = ByteBuffer.allocate(8);
        expectedChecksumBuf.putLong(0, expectedChecksum);

        try {
            Algorithm algorithm = Algorithm.findByMagicNum(expectedChecksumBuf.getInt());
            Checksummer checksummer = ChecksummerFactoryHelper.create(algorithm);
            long calculatedChecksum = checksummer.calculate(buffer, offset, length);

            if ((int) calculatedChecksum != expectedChecksumBuf.getInt()) {
                throw new ChecksumMismatchedException(
                        "Checksum mismatched. start position: " + offset + ", length: " + length, expectedChecksum,
                        calculatedChecksum);
            }
        } catch (UnsupportedChecksumAlgorithmException e) {
            logger.info("caught an exception,unsupported", e);
            return false;
        }

        return true;
    }

    /**
     * compute checksum of the {@link #ByteBuffer}, it will modify the position of {@link #ByteBuffer} and compare with
     * the expected checksum. the position of {@link #ByteBuffer} will be changed. <br>
     * first, get the algorithm of the {@link #ByteBuffer} from expected checksum. <br>
     * then, compute the checksum of the {@link #ByteBuffer} with the algorithm.<br>
     * last, compare with expected checksum.<br>
     * 
     * @param byteBuffer
     * @param expectedChecksum
     * @throws ChecksumMismatchedException
     */
    public static void verifyChecksum(ByteBuffer byteBuffer, long expectedChecksum) throws ChecksumMismatchedException {
        /**
         * get checksummer by magic number of the expected checksum
         */
        ByteBuffer expectedChecksumBuf = ByteBuffer.allocate(8);
        expectedChecksumBuf.putLong(0, expectedChecksum);
        try {
            Algorithm algorithm = Algorithm.findByMagicNum(expectedChecksumBuf.getInt());
            Checksummer checksummer = ChecksummerFactoryHelper.create(algorithm);
            long calculatedChecksum = checksummer.calculate(byteBuffer);

            if ((int) calculatedChecksum != expectedChecksumBuf.getInt()) {
                throw new ChecksumMismatchedException("Checksum mismatched. buffer: " + byteBuffer, expectedChecksum,
                        calculatedChecksum);
            }
        } catch (ChecksumMismatchedException e) {
            throw e;
        } catch (Exception e) {
            throw new ChecksumMismatchedException("caught an exception,unsupported");
        }
    }
}
