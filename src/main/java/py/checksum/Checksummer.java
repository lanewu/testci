package py.checksum;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import py.exception.ChecksumMismatchedException;

/**
 * the interface is used to supply how to operate the checksum of buffer.
 * 
 * @author lx
 * 
 */
public interface Checksummer {

    /**
     * get the checksum of buffer
     * 
     * @param buffer
     * @param offset
     * @param length
     * @return
     */
    public long calculate(byte[] buffer, int offset, int length);

    public long calculate(byte[] buffer);

    /**
     * get the checksum of buffer. it will calculate the checksum between the current position and the limit of
     * byteBuffer. the position of {@link #ByteBuffer} will be changed.
     * 
     * @param byteBuffer
     * @return
     */
    public long calculate(ByteBuffer byteBuffer);

    /**
     * get the checksum of buffer.because the {@link #ByteBuf} support composite ByteBuf, so it is convenient for
     * computing the checksum of composite-buffer without copying. After calling the method, the readerIndex of
     * {@link #ByteBuf} will be changed.
     * 
     * @param byteBuf
     * @return
     */
    public long calculate(ByteBuf byteBuf);

    /**
     * get the checksum of buffer and compare with the specific checksum. it will calculate the checksum between the
     * current position and the limit of byteBuffer. the current position will be changed.
     * 
     * @param byteBuffer
     * @param expectedChecksum
     * @throws ChecksumMismatchedException
     *             when the checksum does not match what was expected
     */
    public void verify(ByteBuffer byteBuffer, long expectedChecksum) throws ChecksumMismatchedException;

    /**
     * get the checksum of buffer and compare with the specific checksum. because the {@link #ByteBuf} support composite
     * ByteBuf, so it is convenient for computing the checksum of composite-buffer without copying. After calling the
     * method, the readerIndex of {@link #ByteBuf} will be changed.
     * 
     * @param byteBuf
     * @param expectedChecksum
     * @throws ChecksumMismatchedException
     */
    public void verify(ByteBuf byteBuf, long expectedChecksum) throws ChecksumMismatchedException;

    /**
     * get the checksum of buffer and compare with the specific checksum.
     * 
     * @param buffer
     * @param offset
     * @param length
     * @param expectedChecksum
     * @return
     * @throws ChecksumMismatchedException
     */
    public void verify(byte[] buffer, int offset, int length, long expectedChecksum) throws ChecksumMismatchedException;
}
