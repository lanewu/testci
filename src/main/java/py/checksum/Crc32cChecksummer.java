package py.checksum;

import java.nio.ByteBuffer;

import org.apache.commons.lang.NotImplementedException;

import io.netty.buffer.ByteBuf;
import py.exception.ChecksumMismatchedException;

/**
 * Verifies and computes crc32c checksum for ByteBuffer. Making a crc32c checksum is heavy weight, so only do it when
 * needed
 * 
 * @author lc, update by lx
 * 
 */
public class Crc32cChecksummer implements Checksummer {
    private IntrinsicCrc32c checksum;

    public Crc32cChecksummer() {
        if (!IntrinsicCrc32c.isIntelSupport()) {
            throw new NotImplementedException("not support the crc32c algrithm");
        }
        checksum = new IntrinsicCrc32c();
    }

    @Override
    public long calculate(byte[] buffer, int offset, int length) {
        checksum.reset();
        checksum.update(buffer, offset, length);
        return checksum.getValue();
    }

    @Override
    public long calculate(ByteBuffer byteBuffer) {
        checksum.reset();
        checksum.update(byteBuffer);
        return checksum.getValue();
    }

    public long calculate(byte[] buffer) {
        return calculate(buffer, 0, buffer.length);
    }

    public long calculate(ByteBuf byteBuf) {
        checksum.reset();
        ByteBuffer[] buffers = byteBuf.nioBuffers();
        for (ByteBuffer buffer : buffers) {
            checksum.update(buffer);
        }
        return checksum.getValue();
    }

    @Override
    public void verify(ByteBuf byteBuf, long expectedChecksum) throws ChecksumMismatchedException {
        long value = calculate(byteBuf);
        if (value != expectedChecksum) {
            throw new ChecksumMismatchedException("Adler32 checksum mismatch ", expectedChecksum, value);
        }
    }

    @Override
    public void verify(ByteBuffer byteBuffer, long expectedChecksum) throws ChecksumMismatchedException {
        long value = calculate(byteBuffer);
        if (value != expectedChecksum) {
            throw new ChecksumMismatchedException("Adler32 checksum mismatch ", expectedChecksum, value);
        }
    }

    @Override
    public void verify(byte[] buffer, int offset, int length, long expectedChecksum)
            throws ChecksumMismatchedException {
        long value = calculate(buffer, offset, length);
        if (value != expectedChecksum) {
            throw new ChecksumMismatchedException("Adler32 checksum checksum: ", expectedChecksum, value);
        }
    }
}
