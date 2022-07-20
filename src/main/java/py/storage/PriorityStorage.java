package py.storage;

import py.exception.StorageException;
import py.storage.impl.AsyncStorage;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

/**
 * Storage with a priority attached to each I/O requests
 */
public abstract class PriorityStorage extends AsyncStorage {

    public enum Priority {
        HIGH(0), MIDDLE(1), LOW(2);

        int val;

        Priority(int val) {
            this.val = val;
        }

        public int getVal() {
            return val;
        }

    }

    public PriorityStorage(String identifier) {
        super(identifier);
    }

    @Override
    public <A> void read(ByteBuffer buffer, long pos, A attachment, CompletionHandler<Integer, ? super A> handler)
        throws StorageException {
        // default priority is HIGH
        read(buffer, pos, attachment, handler, Priority.HIGH);
    }

    @Override
    public <A> void write(ByteBuffer buffer, long pos, A attachment, CompletionHandler<Integer, ? super A> handler)
        throws StorageException {
        // default priority is HIGH
        write(buffer, pos, attachment, handler, Priority.HIGH);
    }

    public abstract <A> void read(ByteBuffer buffer, long pos, A attachment,
        CompletionHandler<Integer, ? super A> handler, Priority priority) throws StorageException;

    public abstract <A> void write(ByteBuffer buffer, long pos, A attachment,
        CompletionHandler<Integer, ? super A> handler, Priority priority) throws StorageException;

}
