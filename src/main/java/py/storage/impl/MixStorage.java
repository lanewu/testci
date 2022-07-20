package py.storage.impl;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.exception.StorageException;
import py.storage.Storage;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

/**
 * Created by fq on 17-11-27.
 */
public class MixStorage extends AsyncStorage {
    private static final Logger logger = LoggerFactory.getLogger(MixStorage.class);
    private final Storage delegate;

    private final long offsetInStorage;
    private final long length;
    public MixStorage(Storage delegate) {
        super(delegate.identifier());
        this.delegate = delegate;
        this.offsetInStorage = 0;
        this.length = delegate.size();
    }
    public MixStorage(Storage delegate, long offset,long length){
        super(delegate.identifier());
        if (delegate.size() < offset + length){
            logger.error("the error is storage size is {}, the write is {}, length is ", delegate.size(), offset , length);
            Validate.isTrue(false);
        }
        this.delegate = delegate;
        this.offsetInStorage = offset;
        this.length = length;
    }

    public Storage getDelegate() {
        return delegate;
    }

    //because of maybe some use the storage at the same time ,so the storage can not be close
    @Override
    public void close() throws StorageException {
    }

    @Override
    public boolean isClosed() {
        return delegate.isClosed();
    }

    @Override
    public boolean isBroken() {
        return delegate.isBroken();
    }

    @Override
    public String identifier() {
        return delegate.identifier();
    }

    @Override
    public void setBroken(boolean broken) {
        delegate.setBroken(broken);
    }

    @Override
    public void read(long offset, ByteBuffer byteBuffer) throws StorageException {
        if (offset  + byteBuffer.remaining() > length){
            logger.error("the read is out of memory");
            throw  new StorageException();
        }
        try {
            delegate.read(offset + offsetInStorage, byteBuffer);
        } catch (StorageException exception) {
            logger.error("StorageException caught. storage:" + delegate + " offset " + offset + " byteBuffer "
                + byteBuffer, exception);
            throw exception;
        }
    }

    @Override
    public void write(long offset, ByteBuffer byteBuffer) throws StorageException {
        if (offset  + byteBuffer.remaining() > length){
            logger.error("the read is out of memory");
            throw  new StorageException();
        }
        try {
            delegate.write(offset + offsetInStorage, byteBuffer);
        } catch (StorageException exception) {
            logger.error("StorageException caught storage:" + delegate + " offset " + offset + " byteBuffer "
                + byteBuffer, exception);
            throw exception;
        }
    }

    @Override
    public String toString() {
        return "ExceptionInterceptingStorage[" + delegate.toString() + "]";
    }

    @Override
    public void open() throws StorageException {
        delegate.open();
    }

    @Override
    public void read(long pos, byte[] dstBuf, int off, int len) throws StorageException {
        ByteBuffer buffer = ByteBuffer.wrap(dstBuf, off, len);
        read(pos, buffer);
    }

    @Override
    public void write(long pos, byte[] buf, int off, int len) throws StorageException {
        ByteBuffer buffer = ByteBuffer.wrap(buf, off, len);
        write(pos, buffer);
    }

    @Override
    public long size() {
        return length;
    }

    @Override
    public <A> void read(ByteBuffer buffer, long pos, A attachment, CompletionHandler<Integer, ? super A> handler)
        throws StorageException {
        if (buffer.remaining() == 0) {
            logger.error("read offset: {}, buffer: {}", pos, buffer, new Exception());
        }

        if (pos + buffer.remaining() > length){
            long longger = pos + buffer.capacity();
            logger.error("the length is {}, the read is {}",length, longger);
            logger.error("the read is out of memory");
            throw  new StorageException();
        }
        if (delegate instanceof AsyncStorage) {
            ((AsyncStorage) delegate).read(buffer, pos + offsetInStorage, attachment, handler);
        } else {
            throw new NotImplementedException("read");
        }
    }

    @Override
    public <A> void write(ByteBuffer buffer, long pos, A attachment, CompletionHandler<Integer, ? super A> handler)
        throws StorageException {
        if (pos + buffer.remaining() > length){
            long lengthis = pos + offsetInStorage + buffer.capacity();
            long longer = length - lengthis;
            logger.error("the read is out of memory");
            throw  new StorageException();
        }

        if (delegate instanceof AsyncStorage) {
            ((AsyncStorage) delegate).write(buffer, pos + offsetInStorage, attachment, handler);
        } else {
            throw new NotImplementedException("read");
        }
    }

    public long getOffsetInStorage() {
        return offsetInStorage;
    }

    public long getLength() {
        return length;
    }
}

