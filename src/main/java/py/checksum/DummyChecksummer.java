package py.checksum;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import py.exception.ChecksumMismatchedException;

public class DummyChecksummer implements Checksummer {

    @Override
    public long calculate(ByteBuffer byteBuffer) {
        return 0l;
    }

    @Override
    public void verify(ByteBuffer byteBuffer, long expectedChecksum) throws ChecksumMismatchedException {

    }

    @Override
    public long calculate(byte[] buffer, int offset, int length) {
        return 0l;
    }

    @Override
    public void verify(byte[] buffer, int offset, int length, long expectedChecksum)
            throws ChecksumMismatchedException {
        throw new ChecksumMismatchedException("this is dummy check summer");
    }

    @Override
    public long calculate(byte[] buffer) {
        return 0;
    }

    @Override
    public long calculate(ByteBuf byteBuf) {
        return 0;
    }

    @Override
    public void verify(ByteBuf byteBuf, long expectedChecksum) throws ChecksumMismatchedException {

    }

}
