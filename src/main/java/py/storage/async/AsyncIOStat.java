package py.storage.async;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.DirectAlignedBufferAllocator;
import py.exception.StorageException;
import py.storage.Storage;
import py.storage.impl.AsyncStorage;
import py.storage.impl.AsynchronousFileChannelStorageFactory;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

/**
 * Created by xcs on 18-8-27.
 */
public class AsyncIOStat {
    private static final Logger logger = LoggerFactory.getLogger(AsyncIOStat.class);
    AsynchronousFileChannelStorageFactory asynchronousFileChannelStorageFactory;
    private AsyncStorage asyncStorage = null;
    private String devName;

    interface IOStat {
        <A> void doIO(AsyncStorage asyncFileStorage, ByteBuffer buffer, long pos, A attachment,
                CompletionHandler<Integer, ? super A> handler) throws StorageException;
    }

    class IORead implements IOStat {

        @Override
        public <A> void doIO(AsyncStorage asyncStorage, ByteBuffer buffer, long offset, A attachment,
                CompletionHandler<Integer, ? super A> handler) throws StorageException {
            asyncStorage.read(buffer, offset, attachment, handler);
        }

        @Override
        public String toString() {
            return "IORead{read}";
        }
    }

    class IOWrite implements IOStat {
        @Override
        public <A> void doIO(AsyncStorage asyncStorage, ByteBuffer buffer, long offset, A attachment,
                CompletionHandler<Integer, ? super A> handler) throws StorageException {
            asyncStorage.write(buffer, offset, attachment, handler);
        }

        @Override
        public String toString() {
            return "IOWrite{write}";
        }
    }

    public boolean randomReadChecker(int blockSize, int IOPSThreshold) {
        IORead ioRead = new IORead();
        boolean isSlowDisk = false;
        try {
            isSlowDisk = slowDiskChecker(ioRead, devName, blockSize, IOPSThreshold, true);
        } catch (StorageException e) {
            logger.warn("random read exception.", e);
        }
        return isSlowDisk;
    }

    public boolean randomWriteChecker(int blockSize, int IOPSThreshold) {
        IOWrite ioWrite = new IOWrite();
        boolean isSlowDisk = false;
        try {
            isSlowDisk = slowDiskChecker(ioWrite, devName, blockSize, IOPSThreshold, true);
        } catch (StorageException e) {
            logger.warn("random write exception.", e);
        }
        return isSlowDisk;
    }

    public boolean sequentialReadChecker(int blockSize, int IOPSThreshold) {
        IORead ioRead = new IORead();
        boolean isSlowDisk = false;
        try {
            isSlowDisk = slowDiskChecker(ioRead, devName, blockSize, IOPSThreshold, false);
        } catch (StorageException e) {
            logger.warn("sequential read exception.", e);
        }
        return isSlowDisk;
    }

    public boolean sequentialWriteChecker(int blockSize, int IOPSThreshold) {
        IOWrite ioWrite = new IOWrite();
        boolean isSlowDisk = false;
        try {
            isSlowDisk = slowDiskChecker(ioWrite, devName, blockSize, IOPSThreshold, false);
        } catch (StorageException e) {
            logger.warn("sequential write exception.", e);
        }
        return isSlowDisk;
    }

    public void open(String devName) throws StorageException {
        this.devName = devName;
        try {
            asynchronousFileChannelStorageFactory = AsynchronousFileChannelStorageFactory.getInstance()
                    .setMaxThreadpoolSizePerStorage(2).setMaxThreadpoolSizePerSSD(2);
            asyncStorage = (AsyncStorage) asynchronousFileChannelStorageFactory.generate(devName);
            asyncStorage.open();
        } catch (StorageException e) {
            logger.error("open {} error.", devName);
            throw e;
        }
    }

    public void close() throws StorageException {
        if (asyncStorage != null) {
            asyncStorage.close();
        }

        if (asynchronousFileChannelStorageFactory != null) {
            asynchronousFileChannelStorageFactory.close();
        }
    }

    /**
     * @param deviceName
     * @param IOPSThreshold if average io cost(random) over this, it be as a slow disk
     * @param IOPSThreshold if average io cost(sequential) over this, it be as a slow disk
     * @return if is slow disk, return true, else return false.
     */
    public boolean slowDiskChecker(IOStat ioStat, String deviceName, int blockSize, int IOPSThreshold, boolean isRandom)
            throws StorageException {
        boolean isSlowDisk = false;
        int ioDepth = 32;
        int ailgn = 512;
        int IOCount = IOPSThreshold * 3;
        long offset = 0;
        long startTime = 0;
        long endTime = 0;
        Path path = Paths.get(deviceName);

        RandomDataGenerator random = new RandomDataGenerator();
        long maxRange = asyncStorage.size() / blockSize;

        /* sequential read */
        CountDownLatch countDownLatchSequential = new CountDownLatch(IOCount);
        startTime = System.currentTimeMillis();
        for (int i = 0; i < IOCount; i++) {

            if (offset >= asyncStorage.size() - blockSize) {
                offset = 0; // reset offset.
            }

            ByteBuffer byteBuffer = DirectAlignedBufferAllocator.allocateAlignedByteBuffer(blockSize, ailgn);
            ioStat.doIO(asyncStorage, byteBuffer, offset, countDownLatchSequential,
                    new CompletionHandler<Integer, CountDownLatch>() {

                        @Override
                        public void completed(Integer result, CountDownLatch attachment) {
                            attachment.countDown();
                        }

                        @Override
                        public void failed(Throwable exc, CountDownLatch attachment) {
                            attachment.countDown();
                            logger.warn("{} sequential read failed.", deviceName);
                        }
                    });

            if (isRandom) {
                offset = (random.nextLong(0, maxRange - 1)) * blockSize;
            } else {
                offset = offset + blockSize;
            }
        }

        try {
            countDownLatchSequential.await();
            endTime = System.currentTimeMillis();
            //asyncFileStorage.close();
        } catch (InterruptedException e) {
            logger.error("storage:{} IOPS check exception. ", deviceName, e);
        }
        if (endTime <= startTime) {
            return false;
        }

        long cost = endTime - startTime;
        long IOPS = IOCount * 1000 / cost;

        logger.info("device:{} IOPS:{} block size:{} threshold:{} random:{} iostat:{}", deviceName, IOPS, blockSize,
                IOPSThreshold, isRandom, ioStat);
        return IOPS > IOPSThreshold ? false : true;
    }
}
