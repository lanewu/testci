package py.storage.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.Constants;
import py.common.NamedThreadFactory;
import py.exception.StorageException;
import py.storage.PYOSInfo;
import py.storage.Storage;
import py.storage.StorageIOType;
import py.storage.async.AsyncFileStorage;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// TODO rename this class, because it's not only a factory for AsynchronousFileChannnelStorage anymore
public class AsynchronousFileChannelStorageFactory extends FileStorageFactory {
    private final static Logger logger = LoggerFactory.getLogger(AsynchronousFileChannelStorageFactory.class);
    private int maxThreadpoolSizePerStorage = 0;
    private int maxThreadpoolSizePerSSD = 0;
    private final static int DEFAULT_MAX_IO_DEPTH_HDD = 64;
    private final static int DEFAULT_MAX_IO_DEPTH_SSD = 64;
    private int maxIODepthHDD = DEFAULT_MAX_IO_DEPTH_HDD;
    private int maxIODepthSSD = DEFAULT_MAX_IO_DEPTH_SSD;
    private StorageIOType storageIOType = null;
    private static Map<Storage, ExecutorService> mapStorageToExecutor = new ConcurrentHashMap<Storage, ExecutorService>();
    private static AsynchronousFileChannelStorageFactory factory = new AsynchronousFileChannelStorageFactory();
    private AtomicInteger index = new AtomicInteger(0);

    private AsynchronousFileChannelStorageFactory() {
        autoCheckIOModel();
    }

    public static AsynchronousFileChannelStorageFactory getInstance() {
        return factory;
    }

    public void close(Storage storage) {
        ExecutorService executorService = mapStorageToExecutor.remove(storage);
        if (executorService != null) {
            boolean success = false;
            try {
                executorService.shutdown();
                success = executorService.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.error("caught an exception", e);
            }

            if (!success) {
                logger.warn("wait for the storage {} thread pool shutdown, timeout", storage);
                // throw new RuntimeException("storage: " + storage);
            }
        }
    }

    public void close() {
        List<Storage> storages = new ArrayList<Storage>(mapStorageToExecutor.keySet());
        while (!storages.isEmpty()) {
            logger.warn("close storage: {}", storages);
            close(storages.remove(0));
        }
    }

    public AsynchronousFileChannelStorageFactory setMaxThreadpoolSizePerStorage(int maxThreadpoolSizePerStorage) {
        this.maxThreadpoolSizePerStorage = maxThreadpoolSizePerStorage;
        return this;
    }

    public AsynchronousFileChannelStorageFactory setMaxThreadpoolSizePerSSD(int maxThreadpoolSizePerSSD) {
        this.maxThreadpoolSizePerSSD = maxThreadpoolSizePerSSD;
        return this;
    }

    public AsynchronousFileChannelStorageFactory setMaxIODepthHDD(int maxIODepthHDD) {
        this.maxIODepthHDD = maxIODepthHDD;
        return this;
    }

    public AsynchronousFileChannelStorageFactory setMaxIODepthSSD(int maxIODepthSSD) {
        this.maxIODepthSSD = maxIODepthSSD;
        return this;
    }

    // manual set storageIOType, if you don't want check os info automatic
    public AsynchronousFileChannelStorageFactory setStorageIOType(StorageIOType storageIOType) {
        this.storageIOType = storageIOType;
        return this;
    }

    public void autoCheckIOModel() {
        // check OS info
        if (storageIOType == null) {
            PYOSInfo.OS os = PYOSInfo.getOs();
            logger.warn("os:name:{} version:{}", os, os.getVersion());
            if (os.equals(PYOSInfo.OS.LINUX)) {
                storageIOType = StorageIOType.LINUXAIO;
            } else {
                storageIOType = StorageIOType.SYNCAIO;
            }
        } else {
            logger.warn("storageIOType:{}", storageIOType);
            return;
        }

        if(!storageIOType.equals(StorageIOType.LINUXAIO)) {
            return;
        }

        boolean hasLib = false;
        /* if LINUXAIO, must can load lib */
        String libPaths = System.getProperty("java.library.path");
        String libPath[] = libPaths.split(":");
        for(String path : libPath) {
            String libFile = path + "/liblinux-async-io.so";
            File file = new File(libFile);
            if (file.exists()) {
                /* don't find liblinux-async-io.so, so can't use LINUX-AIO */
                hasLib = true;
                logger.warn("find lib:{}", libFile);
                return;
            }
        }

        if(!hasLib) {
            storageIOType = StorageIOType.SYNCAIO;
            logger.error("OS is linux, but can't find lib");
        }
    }

    @Override
    public Storage generate(String id) throws StorageException {
        if (StorageUtils.isSATA(id)) {
            logger.warn("this is a sata disk: {}", id);
            return generateStorage(id, maxThreadpoolSizePerStorage * 4, maxIODepthHDD, maxThreadpoolSizePerStorage);
        } else if (StorageUtils.isSSD(id)) {
            logger.warn("this is a ssd disk: {}", id);
            return generateStorage(id, maxThreadpoolSizePerSSD * 4, maxIODepthSSD, maxThreadpoolSizePerSSD);
        } else if (StorageUtils.isPCIE(id)) {
            logger.warn("this is a pcie disk: {}", id);
            return generateStorage(id, maxThreadpoolSizePerSSD * 4, maxIODepthSSD, maxThreadpoolSizePerSSD);
        } else {
            // default sata disk for some junit test or integration test
            logger.warn("this is a default sata disk: {}", id);
            return generateStorage(id, maxThreadpoolSizePerStorage * 4, maxIODepthHDD, maxThreadpoolSizePerStorage);
        }
    }

    public Storage generateStorage(String identifier, int queueSize, int ioDepth, int threadPoolSize) throws StorageException {
        ThreadPoolExecutor threadPoolExecutor = null;
        try {
            String deviceName = identifier;
            if (deviceName != null && deviceName.contains("/")) {
                deviceName = deviceName.substring(deviceName.lastIndexOf('/') + 1);
            }

            @SuppressWarnings("serial")
            BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(queueSize) {
                @Override
                public boolean offer(Runnable runnable) {
                    // turn offer() and add() into a blocking calls (unless interrupted)
                    try {
                        put(runnable);
                        return true;
                    } catch (InterruptedException e) {
                        logger.error("caught an exception", e);
                        Thread.currentThread().interrupt();
                    }
                    return false;
                }
            };

            threadPoolExecutor = new ThreadPoolExecutor(threadPoolSize, threadPoolSize, 60L, TimeUnit.SECONDS, queue,
                    new NamedThreadFactory("Async-IO-Threadpool-" + deviceName + "-" + index.getAndIncrement()));

            /**
             * A handler for rejected tasks that runs the rejected task directly in the calling thread of the
             * {@code execute} method, unless the executor has been shut down, in which case the task is discarded.
             */
            threadPoolExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

            logger.warn("identifier:{} storageIOType:{}", identifier, storageIOType);
            Storage storage;
            if(storageIOType.equals(StorageIOType.LINUXAIO)) {
                /* invoke linux-async-io.so LINUX AIO function. link io_xxxxx */
                storage = new AsyncFileStorage(Paths.get(identifier), ioDepth, Constants.SECTOR_SIZE);
            } else {
                storage = new AsynchronousFileChannelStorage(Paths.get(identifier), queueSize, threadPoolSize,
                        threadPoolExecutor);
            }
            ExecutorService previousExecutor = mapStorageToExecutor.put(storage, threadPoolExecutor);
            if (previousExecutor != null) {
                logger.warn("previousExecutor should be shutdown now, identifier:{}", identifier);
                previousExecutor.shutdown();
            }
            return storage;
        } catch (Exception e) {
            logger.error("caught an exception", e);
            if (threadPoolExecutor != null) {
                threadPoolExecutor.shutdown();
            }

            throw e;
        }
    }
}
