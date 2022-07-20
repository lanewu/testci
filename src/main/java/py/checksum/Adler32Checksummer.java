package py.checksum;

import java.nio.ByteBuffer;
import java.util.zip.Adler32;

import io.netty.buffer.ByteBuf;
import py.exception.ChecksumMismatchedException;

/**
 * 
 * @author lx
 *
 */
public class Adler32Checksummer implements Checksummer {
    private Adler32 checksum;

    public Adler32Checksummer() {
        checksum = new Adler32();
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
