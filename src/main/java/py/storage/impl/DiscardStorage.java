package py.storage.impl;

import py.common.NamedThreadFactory;
import py.exception.StorageException;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class DiscardStorage extends AsyncStorage {

    private static final AtomicInteger GLOBAL_INDEX = new AtomicInteger(0);

    private final ExecutorService executorService;

    private final long size;

    public DiscardStorage(String identifier, long size) {
        super(identifier);
        executorService = Executors
                .newSingleThreadExecutor(new NamedThreadFactory("discard-storage-" + GLOBAL_INDEX.incrementAndGet()));
        this.size = size;
    }

    @Override
    public <A> void read(ByteBuffer buffer, long offset, A attachment, CompletionHandler<Integer, ? super A> handler)
            throws StorageException {
        executorService.execute(() -> {
            int length = buffer.remaining();
            while (buffer.hasRemaining()) {
                buffer.put((byte) 0);
            }
            handler.completed(length, attachment);
        });
    }

    @Override
    public <A> void write(ByteBuffer buffer, long offset, A attachment, CompletionHandler<Integer, ? super A> handler)
            throws StorageException {
        executorService.execute(() -> {
            int length = buffer.remaining();
            while (buffer.hasRemaining()) {
                buffer.get();
            }
            handler.completed(length, attachment);
        });
    }

    @Override
    public long size() {
        return size;
    }

}
