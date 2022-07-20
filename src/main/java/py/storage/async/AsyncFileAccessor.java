package py.storage.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.exception.StorageException;

import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An async file accessor for operating systems that support asynchronous file I/O.
 *
 * <p>
 * this class is package-private, and should be accessed through {@link AsyncFileStorage}
 * </p>
 *
 * @author tyr
 */
class AsyncFileAccessor {
    private final static Logger logger = LoggerFactory.getLogger(AsyncFileAccessor.class);

    static final int ERR_SUCCESS = 0;
    static final int ERR_FAIL = -1;

    private volatile boolean isClosed;

    /**
     * counters used for not closing storage until read or write returned
     * (method invoking completion is enough, no need to wait until callback done or failed)
     */
    private final AtomicInteger readCounter;
    private final AtomicInteger writeCounter;
    private final long pyFD;
    private final Path storagePath;

    private AsyncFileAccessor(long pyFD, Path storagePath) {
        this.pyFD = pyFD;
        this.storagePath = storagePath;
        this.readCounter = new AtomicInteger(0);
        this.writeCounter = new AtomicInteger(0);
    }

    /**
     * open an accessor
     *
     * @param path    the file path
     * @param ioDepth io depth, the requests' queue length
     */
    static AsyncFileAccessor open(Path path, int ioDepth) throws StorageException {
        long pyFD = open(path.toUri().getPath(), ioDepth);
        if (pyFD < 0) {
            logger.error("open:{} ioDepth:{}", path.toString(), ioDepth);
            throw new StorageException("open:" + path.toString() + "ioDepth:" + ioDepth);
        }
        return new AsyncFileAccessor(pyFD, path);
    }

    <A> void write(long address, long offset, int length, A attachment,
            CompletionHandler<Integer, A> completionHandler) {
        writeCounter.incrementAndGet();

        if (isClosed) {
            writeCounter.decrementAndGet();
            completionHandler.failed(new StorageException("file accessor has been closed !"), attachment);
            return;
        }

        try {
            write(pyFD, address, offset, length, new IOCallback<>(offset, length, attachment, completionHandler));
        } finally {
            writeCounter.decrementAndGet();
        }
    }

    <A> void read(long address, long offset, int length, A attachment,
            CompletionHandler<Integer, A> completionHandler) {
        readCounter.incrementAndGet();
        if (isClosed) {
            readCounter.decrementAndGet();
            completionHandler.failed(new StorageException("file accessor has been closed !"), attachment);
            return;
        }

        try {
            read(pyFD, address, offset, length, new IOCallback<>(offset, length, attachment, completionHandler));
        } finally {
            readCounter.decrementAndGet();
        }
    }

    void enableSlowDisk(int seqential, int random, double quartile, int policy, Callback callback) {
        enableDiskCheck(pyFD, seqential, random, quartile, policy, callback);
    }

    void close() {
        if (!isClosed) {
            isClosed = true;
            while (readCounter.get() != 0 || writeCounter.get() != 0) {
                logger.warn("storage path:{} read counter:{}  write counter:{} sleep and try again", storagePath,
                        readCounter.get());
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    logger.warn("sleep interrupted exception.", e);
                }
            }
            logger.warn("storage path:{} will be closed", storagePath);
            close(pyFD);
            logger.warn("storage path:{} been closed completely", storagePath);
        }
    }


    /* JNI method imp on liblinux-aysnc-io.c */

    /**
     * open storage and start event handle thread
     */
    private static native long open(String path, int ioDepth);

    /**
     * close storage and stop event handle thread, free resource
     */
    private static native void close(long pyFD);

    /**
     * async submit write request
     * @param pyFD        returned from open
     * @param address     direct buffer.(ailg by sector(512))
     * @param offset      offset on disk or file
     * @param length      data length
     */
    private static native void write(long pyFD, long address, long offset, int length, Callback callback);

    /**
     * async submit read request
     * @param address     direct buffer.(ailg by sector(512))
     * @param offset      offset on disk or file
     * @param length      data length    * @param pyFD        returned from open
     */
    private static native void read(long pyFD, long address, long offset, int length, Callback callback);

    /**
     * enable slow disk checking
     * @param pyFD             returned from open
     * @param seqential        nanosecond for sequential r/w
     * @param random           nanosecond for random r/w
     * @param quartile         digital more than total
     * @param callback         when discover a slow disk ,invoke it
     * @param policy           0: disable 1:IO cost time 2:await time
     */
    private static native void enableDiskCheck(long pyFD, int seqential, int random, double quartile, int policy,  Callback callback);

    static {
        logger.warn("load path:{}", System.getProperty("java.library.path"));
        System.loadLibrary("linux-async-io");
    }
}
