package py.storage.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.exception.StorageException;
import py.storage.Storage;
import py.storage.impl.AsyncStorage;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;

/**
 * An asynchronous implementation of {@link Storage} based on files for operating systems that support async file I/O.
 *
 * @author tyr
 */
public class AsyncFileStorage extends AsyncStorage {
    private static final Logger logger = LoggerFactory.getLogger(AsyncFileStorage.class);

    private boolean isOpen;
    private boolean isEnableDiskCheck = false;
    private AsyncFileAccessor accessor;
    private final Path path;
    private final int sectorSize;

    private long size;

    /**
     * @param path    The file path
     * @param ioDepth IO depth, the requests' queue length
     */
    public AsyncFileStorage(Path path, int ioDepth, int sectorSize) {
        super(path.toString());
        this.path = path;
        this.ioDepth = ioDepth;
        this.sectorSize = sectorSize;
    }

    /**
     * Open this storage
     *
     * @throws StorageException if failed
     */
    @Override
    public synchronized void open() throws StorageException {
        if (isOpen) {
            logger.warn("no need to open again. {}", path);
            return;
        }

        accessor = AsyncFileAccessor.open(path, ioDepth);
        this.size = findUsableStorageSize(sectorSize);
        isOpen = true;
    }

    @Override
    public synchronized void close() throws StorageException {
        if (!isOpen) {
            logger.warn("no need to close a closed storage. {}", path);
            return;
        }

        accessor.close();
        isOpen = false;
    }

    @Override
    public boolean isClosed() {
        return !isOpen;
    }

    public void enableSlowDisk(int seqential, int random, double quartile, int policy, Callback callback) {
        if(!isEnableDiskCheck) {

            accessor.enableSlowDisk(seqential, random, quartile, policy, callback);
            isEnableDiskCheck = true;
            logger.warn("storage:{} enable slow disk check.", path);
        } else {
            logger.warn("storage:{} slow disk check already enabled.", path);
        }
    }

    /**
     * Write data to the file from the given buffer
     *
     * @param buffer     data source, an extra memory copy will be needed if it's not direct
     * @param pos     file position
     * @param attachment attachment attached to this I/O operation
     * @param handler    consumer
     * @param <A>        attachment type
     */
    @Override
    public <A> void write(ByteBuffer buffer, long pos, A attachment, CompletionHandler<Integer, ? super A> handler) {
        IOUtil.write(buffer, pos, accessor, attachment, handler);
    }

    /**
     * Read data from the file into the given buffer
     *
     * @param buffer     data holder, an extra memory copy will be needed if it's not direct
     * @param pos     file position
     * @param attachment attachment attached to this I/O operation
     * @param handler    consumer
     * @param <A>        attachment type
     */
    @Override
    public <A> void read(ByteBuffer buffer, long pos, A attachment, CompletionHandler<Integer, ? super A> handler) {
        IOUtil.read(buffer, pos, accessor, attachment, handler);
    }

    @Override
    public long size() {
        return size;
    }

}
