package py.storage.impl;

import py.exception.StorageException;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

public abstract class NonBlockingStorage extends AsyncStorage {

    public NonBlockingStorage(String identifier) {
        super(identifier);
    }

    /**
     * get the available permits without actually acquire them
     *
     * @return the available permits for write
     */
    public abstract int availablePermitsForWrite();

    /**
     * get the available permits without actually acquire them
     *
     * @return the available permits for read
     */
    public abstract int availablePermitsForRead();

    /**
     * pre allocate some slots to ensure the following write is not blocked
     *
     * @param permits number of wanted slots
     * @return success or not
     */
    public abstract boolean preWrite(int permits);


    /**
     * pre allocate some slots to ensure the following write is not blocked
     *
     * @param permits number of wanted slots
     * @return success or not
     */
    public abstract boolean preWrite(int permits,Boolean block);

    public abstract void discardWrite(int permits);

    /**
     * pre allocate some slots to ensure the following write is not blocked
     *
     * @param permits number of wanted slots
     * @return success or not
     */
    public abstract boolean preRead(int permits);

    public abstract void discardRead(int permits);

    /**
     * write after preWrite succeeded
     */
    public abstract <A> void nonBlockingWrite(ByteBuffer buffer, long offset, A attachment,
            CompletionHandler<Integer, A> handler) throws StorageException;

    /**
     * read after preRead succeeded
     */
    public abstract <A> void nonBlockingRead(ByteBuffer buffer, long offset, A attachment,
            CompletionHandler<Integer, A> handler) throws StorageException;
}
