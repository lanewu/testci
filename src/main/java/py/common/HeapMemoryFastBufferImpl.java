package py.common;

import java.nio.ByteBuffer;
import java.util.Arrays;

import sun.nio.ch.DirectBuffer;

import org.apache.commons.lang.Validate;

import py.exception.BufferOverflowException;
import py.exception.BufferUnderflowException;

@SuppressWarnings("restriction")
public class HeapMemoryFastBufferImpl extends HeapIndependentObject implements FastBuffer {
    private final byte[] buffer;
    private ByteBuffer originByteBuffer;

    public HeapMemoryFastBufferImpl(byte[] buffer) {
        this.buffer = buffer;
    }

    public FastBuffer allocate(int size) {
        originByteBuffer = ByteBuffer.wrap(buffer, 0, size).slice();
        return this;
    }

    public FastBuffer allocate(byte[] data) {
        originByteBuffer = ByteBuffer.wrap(buffer, 0, data.length).slice();
        originByteBuffer.duplicate().put(data);
        return this;
    }

    @Override
    public long size() {
        return originByteBuffer.capacity();
    }

    @Override
    public void get(byte[] dst) throws BufferUnderflowException {
        get(dst, 0, dst.length);
    }

    @Override
    public void get(byte[] dst, int offset, int length) throws BufferUnderflowException {
        get(0, dst, offset, length);
    }

    @Override
    public void get(long srcOffset, byte[] dst, int dstOffset, int length) throws BufferUnderflowException {
        long size = size();
        if (length + srcOffset > size) {
            throw new BufferUnderflowException("dst' length " + length + " is larger than the buffer size " + size
                            + ", offset: " + srcOffset);
        }
        System.arraycopy(originByteBuffer.array(), originByteBuffer.arrayOffset() + (int)srcOffset, dst, dstOffset, length);
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
        long size = size();
        if (length + srcOffset > size) {
            throw new BufferUnderflowException("dst' length " + length + " is larger than the buffer size " + size
                            + ", offset: " + srcOffset);
        }

        if (dst.isDirect()) {
            DirectAlignedBufferAllocator.copyMemory(originByteBuffer.array(), originByteBuffer.arrayOffset()
                            + (int)srcOffset, length, ((DirectBuffer) dst).address() + dstOffset);
        } else {
            Validate.isTrue(dst.hasArray());
            get(srcOffset, dst.array(), dst.arrayOffset() + dstOffset, length);
        }
    }

    @Override
    public byte[] array() {
        return Arrays.copyOfRange(buffer, 0, originByteBuffer.capacity());
    }

    @Override
    public void put(byte[] src) throws BufferOverflowException {
        put(src, 0, src.length);
    }

    @Override
    public void put(byte[] src, int srcOffset, int length) throws BufferOverflowException {
        put(0, src, srcOffset, length);
    }

    @Override
    public void put(long dstOffset, byte[] src, int srcOffset, int length) throws BufferOverflowException {
        long size = size();
        if (dstOffset + length > size) {
            throw new BufferOverflowException("length " + length + " is larger than the buffer size " + size
                            + ", offset: " + dstOffset);
        }
        System.arraycopy(src, srcOffset, originByteBuffer.array(), originByteBuffer.arrayOffset() + (int)dstOffset, length);
    }

    @Override
    public void put(ByteBuffer src) throws BufferOverflowException {
        put(src, src.position(), src.remaining());
    }

    @Override
    public void put(ByteBuffer src, int srcOffset, int length) throws BufferOverflowException {
        put(0, src, srcOffset, length);
    }

    @Override
    public void put(long dstOffset, ByteBuffer src, int srcOffset, int length) throws BufferOverflowException {
        long size = size();
        if (dstOffset + length > size) {
            throw new BufferOverflowException("length " + length + " is larger than the buffer size " + size
                            + ", dstOffset: " + dstOffset);
        }

        if (src.isDirect()) {
            DirectAlignedBufferAllocator.copyMemory(((DirectBuffer) src).address() + srcOffset,
                            originByteBuffer.array(), originByteBuffer.arrayOffset() + (int)dstOffset, length);
        } else {
            put(dstOffset, src.array(), src.arrayOffset() + srcOffset, length);
        }
    }
}
