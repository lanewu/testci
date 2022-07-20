package py.netty.memory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;

import org.apache.commons.lang3.Validate;

import io.netty.buffer.AbstractReferenceCountedByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import sun.nio.ch.DirectBuffer;

@SuppressWarnings("restriction")
public class PyPooledDirectByteBuf extends AbstractReferenceCountedByteBuf {
    private final ByteBuffer memory;
    private SimplePooledByteBufAllocator alloc;

    public PyPooledDirectByteBuf(SimplePooledByteBufAllocator alloc, ByteBuffer memory) {
        super(memory.capacity());
        this.alloc = alloc;
        memory.clear();
        this.memory = memory;
    }

    @Override
    public boolean isDirect() {
        return true;
    }

    @Override
    protected byte _getByte(int index) {
        return memory.get(index);
    }

    @Override
    protected short _getShort(int index) {
        return memory.getShort(index);
    }

    @Override
    protected int _getUnsignedMedium(int index) {
        return (memory.get(index) & 0xff) << 16 | (memory.get(index + 1) & 0xff) << 8 | memory.get(index + 2) & 0xff;
    }

    @Override
    protected int _getInt(int index) {
        return memory.getInt(index);
    }

    @Override
    protected long _getLong(int index) {
        return memory.getLong(index);
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length) {
        checkDstIndex(index, length, dstIndex, dst.capacity());
        if (dst.hasArray()) {
            getBytes(index, dst.array(), dst.arrayOffset() + dstIndex, length);
        } else if (dst.nioBufferCount() > 0) {
            for (ByteBuffer bb : dst.nioBuffers(dstIndex, length)) {
                int bbLen = bb.remaining();
                getBytes(index, bb);
                index += bbLen;
            }
        } else {
            dst.setBytes(dstIndex, this, index, length);
        }
        return this;
    }

    @Override
    public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length) {
        checkDstIndex(index, length, dstIndex, dst.length);
        ByteBuffer tmpBuf = memory.duplicate();
        tmpBuf.position(index).limit(index + length);
        tmpBuf.get(dst, dstIndex, length);
        return this;
    }

    @Override
    public ByteBuf readBytes(byte[] dst, int dstIndex, int length) {
        checkReadableBytes(length);
        int readerIndex = readerIndex();
        getBytes(readerIndex, dst, dstIndex, length);
        readerIndex(readerIndex + length);
        return this;
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuffer dst) {
        checkIndex(index);
        int bytesToCopy = Math.min(capacity() - index, dst.remaining());
        ByteBuffer tmpBuf = memory.duplicate();
        tmpBuf.position(index).limit(index + bytesToCopy);
        dst.put(tmpBuf);
        return this;
    }

    @Override
    public ByteBuf readBytes(ByteBuffer dst) {
        int length = dst.remaining();
        checkReadableBytes(length);
        int readerIndex = readerIndex();
        getBytes(readerIndex, dst);
        readerIndex(readerIndex + length);
        return this;
    }

    @Override
    public ByteBuf getBytes(int index, OutputStream out, int length) throws IOException {
        checkIndex(index, length);
        if (length == 0) {
            return this;
        }

        memory.clear().position(index);
        byte[] tmp = new byte[length];
        memory.get(tmp);
        out.write(tmp);
        return this;
    }

    @Override
    public ByteBuf readBytes(OutputStream out, int length) throws IOException {
        checkReadableBytes(length);
        int readerIndex = readerIndex();
        getBytes(readerIndex, out, length);
        readerIndex(readerIndex + length);
        return this;
    }

    @Override
    public int getBytes(int index, GatheringByteChannel out, int length) throws IOException {
        checkIndex(index, length);
        if (length == 0) {
            return 0;
        }

        ByteBuffer tmpBuf = memory.duplicate();
        tmpBuf.position(index).limit(index + length);
        return out.write(tmpBuf);
    }

    @Override
    public int readBytes(GatheringByteChannel out, int length) throws IOException {
        checkReadableBytes(length);
        int readerIndex = readerIndex();
        int readBytes = getBytes(readerIndex, out, length);
        readerIndex(readerIndex + readBytes);
        return readBytes;
    }

    @Override
    protected void _setByte(int index, int value) {
        memory.put(index, (byte) value);
    }

    @Override
    protected void _setShort(int index, int value) {
        memory.putShort(index, (short) value);
    }

    @Override
    protected void _setMedium(int index, int value) {
        memory.put(index, (byte) (value >>> 16));
        memory.put(index + 1, (byte) (value >>> 8));
        memory.put(index + 2, (byte) value);
    }

    @Override
    protected void _setInt(int index, int value) {
        memory.putInt(index, value);
    }

    @Override
    protected void _setLong(int index, long value) {
        memory.putLong(index, value);
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length) {
        checkSrcIndex(index, length, srcIndex, src.capacity());
        if (src.hasArray()) {
            setBytes(index, src.array(), src.arrayOffset() + srcIndex, length);
        } else if (src.nioBufferCount() > 0) {
            for (ByteBuffer bb : src.nioBuffers(srcIndex, length)) {
                int bbLen = bb.remaining();
                setBytes(index, bb);
                index += bbLen;
            }
        } else {
            src.getBytes(srcIndex, this, index, length);
        }
        return this;
    }

    @Override
    public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length) {
        checkSrcIndex(index, length, srcIndex, src.length);
        memory.position(index);
        memory.put(src, srcIndex, length);
        return this;
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuffer src) {
        checkIndex(index, src.remaining());
        memory.position(index);
        memory.put(src);
        return this;
    }

    @Override
    public int setBytes(int index, InputStream in, int length) throws IOException {
        checkIndex(index, length);
        byte[] tmp = new byte[length];
        int readBytes = in.read(tmp);
        if (readBytes <= 0) {
            return readBytes;
        }

        memory.position(index);
        memory.put(tmp, 0, readBytes);
        return readBytes;
    }

    @Override
    public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException {
        checkIndex(index, length);
        ByteBuffer tmpBuf = memory.duplicate();
        tmpBuf.position(index).limit(index + length);
        try {
            return in.read(tmpBuf);
        } catch (ClosedChannelException ignored) {
            return -1;
        }
    }

    @Override
    public ByteBuf copy(int index, int length) {
        checkIndex(index, length);
        ByteBuf copy = alloc().directBuffer(length, length);
        copy.writeBytes(this, index, length);
        return copy;
    }

    @Override
    public int nioBufferCount() {
        return 1;
    }

    @Override
    public ByteBuffer nioBuffer(int index, int length) {
        checkIndex(index, length);
        return ((ByteBuffer) memory.duplicate().position(index).limit(index + length));
    }

    @Override
    public ByteBuffer[] nioBuffers(int index, int length) {
        return new ByteBuffer[] { nioBuffer(index, length) };
    }

    @Override
    public ByteBuffer internalNioBuffer(int index, int length) {
        checkIndex(index, length);
        return (ByteBuffer) memory.duplicate().position(index).limit(index + length);
    }

    @Override
    public boolean hasArray() {
        return false;
    }

    @Override
    public byte[] array() {
        throw new UnsupportedOperationException("direct buffer");
    }

    @Override
    public int arrayOffset() {
        throw new UnsupportedOperationException("direct buffer");
    }

    @Override
    public boolean hasMemoryAddress() {
        return true;
    }

    @Override
    public long memoryAddress() {
        return ((DirectBuffer) memory).address();
    }

    @Override
    protected void deallocate() {
        Validate.isTrue(refCnt() == 0);
        setRefCnt(1);
        clear();
        alloc.release(this);
    }

    @Override
    public int capacity() {
        return memory.capacity();
    }

    @Override
    public ByteBuf capacity(int newCapacity) {
        throw new UnsupportedOperationException("direct buffer");
    }

    @Override
    public ByteBufAllocator alloc() {
        return alloc;
    }

    @Override
    public final ByteOrder order() {
        return ByteOrder.BIG_ENDIAN;
    }

    @Override
    public ByteBuf unwrap() {
        return null;
    }

	@Override
	protected short _getShortLE(int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int _getUnsignedMediumLE(int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int _getIntLE(int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected long _getLongLE(int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected void _setShortLE(int index, int value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void _setMediumLE(int index, int value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void _setIntLE(int index, int value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void _setLongLE(int index, long value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getBytes(int index, FileChannel out, long position, int length)
			throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int setBytes(int index, FileChannel in, long position, int length)
			throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

}
