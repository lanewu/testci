package py.checksum;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.netty.buffer.ByteBuf;
import py.exception.ChecksumMismatchedException;

/**
 * Verifies and computes digest checksum for ByteBuffer. Making a MessageDigest is heavy weight, so only do it when
 * needed
 * 
 * @author lx
 * 
 */
public class DigestChecksummer implements Checksummer {
    private MessageDigest messageDigest;

    public DigestChecksummer(String digestName) {
        try {
            messageDigest = MessageDigest.getInstance(digestName);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 MessageDigest can't be found");
        }
    }

    public long calculate(byte[] buffer) {
        return calculate(buffer, 0, buffer.length);
    }

    @Override
    public long calculate(ByteBuffer byteBuffer) {
        messageDigest.reset();
        messageDigest.update(byteBuffer);
        return ByteBuffer.wrap(messageDigest.digest()).getLong();
    }

    public long calculate(ByteBuf byteBuf) {
        messageDigest.reset();
        ByteBuffer[] buffers = byteBuf.nioBuffers();
        for (ByteBuffer buffer : buffers) {
            messageDigest.update(buffer);
        }
        return ByteBuffer.wrap(messageDigest.digest()).getLong();
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
            throw new ChecksumMismatchedException("digest checksum mismatch ", expectedChecksum, value);
        }
    }

    @Override
    public long calculate(byte[] buffer, int offset, int length) {
        messageDigest.update(buffer, offset, length);
        return ByteBuffer.wrap(messageDigest.digest()).getLong();
    }

    @Override
    public void verify(byte[] buffer, int offset, int length, long expectedChecksum)
            throws ChecksumMismatchedException {
        messageDigest.update(buffer, offset, length);
        long value = ByteBuffer.wrap(messageDigest.digest()).getLong();
        if (value == expectedChecksum) {
            throw new ChecksumMismatchedException("digest checksum mismatch ", expectedChecksum, value);
        }
    }

}
