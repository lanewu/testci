package py.storage.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.common.Constants;
import py.exception.StorageException;

/**
 * @author wzy, lx
 */
public class AsynchronousFileChannelStorage extends AsyncStorage {
    private static final Logger logger = LoggerFactory.getLogger(AsynchronousFileChannelStorage.class);
    private AsynchronousFileChannel afc;
    private long size;
    private Path path;
    private final long sectorSize;
    private ExecutorService taskExecutor;
    private Set<StandardOpenOption> options = new HashSet<StandardOpenOption>();
    public AsynchronousFileChannelStorage(Path path, int queueSize, int threadSize, ExecutorService taskExecutor) throws StorageException {
        this(path, queueSize, threadSize, taskExecutor, Constants.SECTOR_SIZE);
    }

    public AsynchronousFileChannelStorage(Path path, int queueSize, int threadSize, ExecutorService taskExecutor, int sectorSize)
            throws StorageException {
        super(path.toString());
        this.path = path;
        this.sectorSize = sectorSize;
        this.taskExecutor = taskExecutor;
        this.ioDepth = queueSize + threadSize;
        options.add(StandardOpenOption.CREATE);
        options.add(StandardOpenOption.READ);
        options.add(StandardOpenOption.WRITE);

        String deviceName = path.toString();
        if (deviceName != null && deviceName.contains("/")) {
            deviceName = deviceName.substring(deviceName.lastIndexOf('/') + 1);
        }

        try {
            afc = AsynchronousFileChannel.open(path, options, taskExecutor);
            size = findUsableStorageSize(sectorSize);
        } catch (IOException e) {
            logger.error("caught an exception, path: {}", path, e);
            throw new StorageException("can not open the storage: " + path, e);
        }
    }

    @Override
    public void close() throws StorageException {
        try {
            super.close();
            logger.warn("This file will be closed, path {}", path);
            afc.close();
        } catch (Exception e) {
            logger.error("can not close storage: {}", path);
            throw new StorageException(e);
        }
    }

    @Override
    public void open() throws StorageException {
        if (afc.isOpen()) {
            logger.warn("This file has been opend, path {}", path);
            return;
        }

        try {
            logger.warn("This file will be opend, path {}", path);
            afc = AsynchronousFileChannel.open(path, options, taskExecutor);
            size = findUsableStorageSize((int) sectorSize);
            super.open();
        } catch (IOException e) {
            logger.error("caught an exception when opening path: {}", path, e);
            throw new StorageException("can not open the storage: " + path, e);
        }
    }

    @Override
    public long size() {
        return size;
    }

    public AsynchronousFileChannel getDelegate() {
        return afc;
    }

    public void setDelegate(AsynchronousFileChannel delegate) {
        this.afc = delegate;
    }

    @Override
    public <A> void read(ByteBuffer buffer, long pos, A attachment, CompletionHandler<Integer, ? super A> handler)
            throws StorageException {
        int remaining = buffer.remaining();
        try {
            afc.read(buffer, pos, attachment, handler);
        } catch (NonReadableChannelException e) {
            throw new StorageException("read pos=" + pos + ", buffer=" + buffer + ", attachment=" + attachment, e)
                    .setIoException(false).setOffsetAndLength(pos, remaining);
        } catch (Exception e) {
            throw new StorageException("read pos=" + pos + ", buffer=" + buffer + ", attachment=" + attachment, e)
                    .setIoException(e instanceof IOException);
        }
    }

    @Override
    public <A> void write(ByteBuffer buffer, long pos, A attachment, CompletionHandler<Integer, ? super A> handler)
            throws StorageException {
        int remaining = buffer.remaining();
        try {
            afc.write(buffer, pos, attachment, handler);
        } catch (NonWritableChannelException e) {
            throw new StorageException("write pos=" + pos + ", buffer=" + buffer + ", attachment=" + attachment, e)
                    .setIoException(false).setOffsetAndLength(pos, remaining);
        } catch (Exception e) {
            throw new StorageException("write pos=" + pos + ", buffer=" + buffer + ", attachment=" + attachment, e)
                    .setIoException(e instanceof IOException);
        }
    }

}
