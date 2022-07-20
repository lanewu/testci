package py.common;

import java.nio.ByteBuffer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.exception.BufferOverflowException;
import py.exception.BufferUnderflowException;

public abstract class AbstractFastBuffer implements FastBuffer {
    private final static Logger logger = LoggerFactory.getLogger(AbstractFastBuffer.class);
    private List<FastBuffer> fastBuffers;
    private long totalSize;

    public AbstractFastBuffer(List<FastBuffer> fastBuffers, long totalSize) {
        this.fastBuffers = fastBuffers;
        this.totalSize = totalSize;
    }

    @Override
    public void get(byte[] dst) throws BufferUnderflowException {
        get(dst, 0, dst.length);
    }

    @Override
    public void get(byte[] dst, int dstOffset, int length) throws BufferUnderflowException {
        get(0, dst, dstOffset, length);
    }

    @Override
    public void get(ByteBuffer dst) throws BufferUnderflowException {
        get(dst, dst.position(), dst.remaining());
    }

    @Override
    public void get(ByteBuffer dst, int dstOffset, int length) throws BufferUnderflowException {
        get(0, dst, dstOffset, length);
    }

    @Override
    public void get(long srcOffset, ByteBuffer dst, int dstOffset, int length) throws BufferUnderflowException {
        if (srcOffset + length > totalSize) {
            throw new BufferUnderflowException("dst' length " + length + " is larger than the buffer size:" + totalSize
                    + ", offset: " + srcOffset);
        }

        for (FastBuffer fastBuffer : fastBuffers) {
            if (length == 0) {
                // the data to read has gotten.
                break;
            }

            int size = (int) fastBuffer.size();
            if (size <= srcOffset) {
                // the data is not in this buffer.
                srcOffset -= size;
            } else {
                int currentLength = (int) Math.min(size - srcOffset, length);
                // reading some data from this fast buffer, maybe there are more data to read.
                fastBuffer.get(srcOffset, dst, dstOffset, currentLength);
                dstOffset += currentLength;
                length -= currentLength;

                // the reading position of next buffer should start from zero.
                srcOffset = 0;
            }
        }
    }

    @Override
    public void get(long srcOffset, byte[] dst, int dstOffset, int length) throws BufferUnderflowException {
        if (srcOffset + length > totalSize) {
            throw new BufferUnderflowException("dst' length " + dst.length + " is larger than the buffer size "
                    + totalSize + ", offset: " + srcOffset);
        }

        for (FastBuffer fastBuffer : fastBuffers) {
            if (length == 0) {
                // the data to read has gotten.
                break;
            }

            int size = (int) fastBuffer.size();
            if (size <= srcOffset) {
                // the data is not in this buffer.
                srcOffset -= size;
            } else {
                int currentLength = (int) Math.min(size - srcOffset, length);
                // reading some data from this fast buffer, maybe there are more data to read.
                fastBuffer.get(srcOffset, dst, dstOffset, currentLength);
                dstOffset += currentLength;
                length -= currentLength;

                // the reading position of next buffer should start from zero.
                srcOffset = 0;
            }
        }
    }

    @Override
    public void put(byte[] src) throws BufferOverflowException {
        put(0, src, 0, src.length);
    }

    @Override
    public void put(byte[] src, int srcOffset, int length) throws BufferOverflowException {
        put(0, src, srcOffset, length);
    }

    @Override
    public void put(long dstOffset, byte[] src, int srcOffset, int length) throws BufferOverflowException {
        if (dstOffset + length > this.totalSize) {
            throw new BufferOverflowException("src' length " + length + " is larger than the buffer size "
                    + this.totalSize + ", dstOffset: " + dstOffset);
        }

        for (FastBuffer fastBuffer : fastBuffers) {
            if (length == 0) {
                // the data to read has gotten.
                break;
            }

            int size = (int) fastBuffer.size();
            if (size <= dstOffset) {
                // the data is not in this buffer.
                dstOffset -= size;
            } else {
                int currentLength = (int) Math.min(size - dstOffset, length);
                // reading some data from this fast buffer, maybe there are more data to read.
                fastBuffer.put(dstOffset, src, srcOffset, currentLength);
                srcOffset += currentLength;
                length -= currentLength;

                // the reading position of next buffer should start from zero.
                dstOffset = 0;
            }
        }
    }

    @Override
    public void put(ByteBuffer src) throws BufferOverflowException {
        put(0l, src, src.position(), src.remaining());
    }

    @Override
    public void put(ByteBuffer src, int srcOffset, int length) throws BufferOverflowException {
        put(0l, src, srcOffset, length);
    }

    @Override
    public void put(long dstOffset, ByteBuffer src, int srcOffset, int length) throws BufferOverflowException {
        if (dstOffset + length > this.totalSize) {
            throw new BufferOverflowException("src' length " + length + " is larger than the buffer size "
                    + this.totalSize + ", dstOffset: " + dstOffset);
        }

        for (FastBuffer fastBuffer : fastBuffers) {
            if (length == 0) {
                // the data to read has gotten.
                break;
            }

            long size = fastBuffer.size();
            if (size <= dstOffset) {
                // the data is not in this buffer.
                dstOffset -= size;
            } else {
                int currentLength = (int) Math.min(size - dstOffset, length);
                // reading some data from this fast buffer, maybe there are more data to read.
                fastBuffer.put(dstOffset, src, srcOffset, currentLength);
                srcOffset += currentLength;
                length -= currentLength;

                // the reading position of next buffer should start from zero.
                dstOffset = 0;
            }
        }
    }

    @Override
    public long size() {
        return totalSize;
    }

    @Override
    public byte[] array() {
        if (size() == 0) {
            return null;
        }

        try {
            byte[] temp = new byte[(int) size()];
            int desOffset = 0;
            for (FastBuffer fastBuffer : fastBuffers) {
                fastBuffer.get(temp, desOffset, (int) fastBuffer.size());
                desOffset += fastBuffer.size();
            }
            return temp;
        } catch (BufferUnderflowException e) {
            logger.error("can't clone an array", e);
            return null;
        }
    }

    public List<FastBuffer> getFastBuffers() {
        return fastBuffers;
    }

    @Override
    public String toString() {
        return "AbstractFastBuffer [fastBuffers=" + fastBuffers + ", totalSize=" + totalSize + "]";
    }

}
