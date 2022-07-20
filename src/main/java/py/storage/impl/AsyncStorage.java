package py.storage.impl;

import py.exception.StorageException;
import py.storage.Storage;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;

public abstract class AsyncStorage extends Storage {

    public AsyncStorage(String identifier) {
        super(identifier);
    }

    /**
     * Read a sequence of bytes from this storage, starting from the given file position
     *
     * @param buffer     The data holder
     * @param pos     The file position
     * @param attachment Attachment attached to this I/O operation
     * @param handler    The handler for consuming the result
     * @param <A>        The type of attachment
     */
    public abstract <A> void read(ByteBuffer buffer, long pos, A attachment,
            CompletionHandler<Integer, ? super A> handler) throws StorageException;

    /**
     * Write a sequence of bytes into this storage, starting from the given file position
     *
     * @param buffer     The data
     * @param pos     The file position
     * @param attachment Attachment attached to this I/O operation
     * @param handler    The handler for consuming the result
     * @param <A>        The type of attachment
     */
    public abstract <A> void write(ByteBuffer buffer, long pos, A attachment,
            CompletionHandler<Integer, ? super A> handler) throws StorageException;


    @Override
    public void read(long pos, byte[] dstBuf, int off, int len) throws StorageException {
        read(pos, ByteBuffer.wrap(dstBuf, off, len));
    }

    @Override
    public void write(long pos, byte[] buf, int off, int len) throws StorageException {
        write(pos, ByteBuffer.wrap(buf, off, len));
    }

    @Override
    public void write(long pos, ByteBuffer buffer) throws StorageException {
        CompletableFuture<Throwable> future = new CompletableFuture<>();
        write(buffer, pos, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer result, Void attachment) {
                future.complete(null);
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                future.complete(exc);
            }
        });

        // wait for the result
        try {
            Throwable exception = future.get();
            if (exception != null) {
                throw new StorageException(exception);
            }
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }


    @Override
    public void read(long pos, ByteBuffer buffer) throws StorageException {
        CompletableFuture<Throwable> future = new CompletableFuture<>();
        read(buffer, pos, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer result, Void attachment) {
                if (result <= 0) {
                    future.complete(new StorageException("result <= zero. throw exception"));
                } else {
                    future.complete(null);
                }
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                future.complete(exc);
            }
        });

        // wait for the result
        try {
            Throwable exception = future.get();
            if (exception != null) {
                throw new StorageException(exception);
            }
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
}
