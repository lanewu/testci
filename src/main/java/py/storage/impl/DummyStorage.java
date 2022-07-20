package py.storage.impl;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.exception.StorageException;

/**
 * In memory storage for testing purpose
 * 
 * @author chenlia
 * 
 */
public class DummyStorage extends AsyncStorage {
    protected Logger logger = LoggerFactory.getLogger(DummyStorage.class);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private ByteBuffer buffer;
    private AtomicInteger numReads;
    private AtomicInteger numWrites;

    public DummyStorage(String identifier, int size) {
        super(identifier);
        this.buffer = ByteBuffer.allocate(size);
        numReads = new AtomicInteger();
        numWrites = new AtomicInteger();
    }

    @Override
	public void read(long pos, byte[] buf, int off, int len) throws StorageException {
        ByteBuffer buffer = ByteBuffer.wrap(buf, off, len);
        read(pos, buffer);
    }

    @Override
	public void write(long pos, byte[] buf, int off, int len) throws StorageException {
        ByteBuffer buffer = ByteBuffer.wrap(buf, off, len);
        write(pos, buffer);
    }

    @Override
	public void open() {
        closed.set(false);
        try {
            super.open();
        } catch (StorageException e) {
            e.printStackTrace();
        }
    }

    @Override
	public void close() {
        closed.set(true);
        try {
            super.close();
        } catch (StorageException e) {
            e.printStackTrace();
        }
    }

    @Override
	public long size() {
        return buffer.capacity();
    }

    public synchronized void read(long offset, ByteBuffer byteBuffer) throws StorageException {
        if (closed.get()) {
            throw new StorageException("closed");
        }
        checkRange(offset, byteBuffer.remaining());

        buffer.clear(); // reset position and limit, doesn't erase
        buffer.position((int) offset);
        buffer.limit(buffer.position() + byteBuffer.remaining());
        logger.debug("offset:{}, buffer position:{}, buffer limit:{}, remaining:{}", offset,
                        buffer.position(), buffer.limit(), byteBuffer.remaining());
        byteBuffer.put(buffer);
        numReads.incrementAndGet();
    }

    @Override
	public synchronized void write(long offset, ByteBuffer byteBuffer) throws StorageException {
        if (closed.get())
            throw new StorageException("closed");
        checkRange(offset, byteBuffer.remaining());
        buffer.clear(); // reset position and limit, doesn't erase
        buffer.position((int) offset);
        logger.debug("offset:{}, buffer position:{}, buffer limit:{}, remaining: {}", offset,
                        buffer.position(), buffer.limit(), byteBuffer.remaining());
        buffer.limit(buffer.position() + byteBuffer.remaining());
        buffer.put(byteBuffer);
        numWrites.incrementAndGet();
    }

    /**
     * checks that the request is not out of range
     * 
     * @param offset
     *            the offset of the request
     * @param requestSize
     *            the size of the request
     * @throws StorageException
     *             if requested size exceeds range
     */
    private void checkRange(long offset, long requestSize) throws StorageException {
        if (offset + requestSize > size())
            throw new StorageException("request out of range -- " + " offset:" + offset + " requestSize:" + requestSize
                            + " size:" + size());
    }
    
    public void cleanRWCounts() {
        numReads.set(0);
        numWrites.set(0);
    }
    
    public int getReadCounts() {
        return numReads.get();
    }
    
    public int getWriteCounts() {
        return numWrites.get();
    }

    @Override
    public <A> void read(ByteBuffer buffer, long pos, A attachment, CompletionHandler<Integer, ? super A> handler)
            throws StorageException {
        int remaining = buffer.remaining();
        read(pos, buffer);
        handler.completed(remaining, attachment);
    }

    @Override
    public <A> void write(ByteBuffer buffer, long pos, A attachment, CompletionHandler<Integer, ? super A> handler)
            throws StorageException {
        int remaining = buffer.remaining();
        write(pos, buffer);
        handler.completed(remaining, attachment);
    }

}
