package py.common;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.exception.NoAvailableBufferException;
import py.metrics.PYMetric;
import py.metrics.PYMetricRegistry;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

public class FastBufferManagerProxy {
    private final static Logger logger = LoggerFactory.getLogger(FastBufferManagerProxy.class);
    /**
     * The default minimum alignment is 512 bytes, because the read and write unit of storage is aligned with 512 which
     * equals the size of sector. We will split the total buffer by the size of sector, then we can support the max size
     * of buffer up to 2T.
     */
    private static int SECTOR_BITS = 9;

    private static final String METRIC_PREFIX = "FastBufferManagerProxy";

    private final FastBufferManager fastBufferManager;
    private final Semaphore semaphore;
    private PYMetric meterAllocationBufferTimeout;
    private PYMetric meterAllocationBufferFailure;
    private PYMetric counterSemaphore;
    private PYMetric meterAllocateFastBuffer;
    private PYMetric meterReleaseFastBuffer;
    private int totalSectorCount;

    public FastBufferManagerProxy(FastBufferManager fastBufferManager,String devName) {
        this.fastBufferManager = fastBufferManager;
        totalSectorCount = (int) (fastBufferManager.size() >> SECTOR_BITS);
        Validate.isTrue(totalSectorCount <= Integer.MAX_VALUE,
                "count:" + totalSectorCount + ", size:" + fastBufferManager.size() + ", exceed " + Integer.MAX_VALUE);
        Validate.isTrue(totalSectorCount > 0,
                "totalSectorCount:" + totalSectorCount + ", manager size:" + fastBufferManager.size() + "sector_bits:" + SECTOR_BITS);
        logger.warn("fast buffer manager size:{}, total sector size:{}, sector_bits:{}", fastBufferManager.size(),
                totalSectorCount, SECTOR_BITS);

        meterAllocationBufferTimeout = PYMetricRegistry.getMetricRegistry().register(
                MetricRegistry.name(METRIC_PREFIX, devName,"meter_allocation_buffer_timeout"),
                Meter.class);
        meterAllocationBufferFailure = PYMetricRegistry.getMetricRegistry().register(
                MetricRegistry.name(METRIC_PREFIX, devName,"meter_allocation_buffer_failure"),
                Meter.class);
        counterSemaphore = PYMetricRegistry.getMetricRegistry()
                .register(MetricRegistry.name(METRIC_PREFIX, devName,"counter_semaphore_size"),
                        Counter.class);
        meterAllocateFastBuffer = PYMetricRegistry.getMetricRegistry()
                .register(MetricRegistry.name(METRIC_PREFIX, devName,"meter_allocate_fast_buffer"),
                        Meter.class);
        meterReleaseFastBuffer = PYMetricRegistry.getMetricRegistry()
            .register(MetricRegistry.name(METRIC_PREFIX, devName,"meter_release_fast_buffer"),
                Meter.class);

        semaphore = new Semaphore(totalSectorCount, true);
        counterSemaphore.incCounter(totalSectorCount);
    }

    public int getTotalSectorCount(){
        return totalSectorCount;
    }

    public int getFreeSectorCount(){
        return semaphore.availablePermits();
    }

    public static void setSectorBits(int sectorBits) {
        SECTOR_BITS = sectorBits;
    }

    public FastBuffer allocateBuffer(long size) throws NoAvailableBufferException {
        return allocateBuffer(size, 0);
    }

    /**
     * Allocate buffer of specified size for at most specified time.
     * <p>
     * When allocation at this time failed with {@link NoAvailableBufferException}
     *
     * @param size
     * @param timeoutMS
     * @return
     * @throws NoAvailableBufferException
     */
    public FastBuffer allocateBuffer(long size, long timeoutMS) throws NoAvailableBufferException {
        if (size <= 0) {
            logger.error("can't assign a fast buffer with not-positive value {} ", size);
            return null;
        }

        if (size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("size: " + size + " exceed " + Integer.MAX_VALUE);
        }

        int sectorCount = (int) (size >> SECTOR_BITS);
        if ((size - (((long) (sectorCount)) << SECTOR_BITS)) != 0) {
            throw new IllegalArgumentException("size: " + size + " is not aligned ");
        }

        boolean isSuccess;

        try {
            isSuccess = semaphore.tryAcquire(sectorCount, timeoutMS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.error("caught an interrupted exception while waiting for free buffers");
            throw new NoAvailableBufferException();
        }

        if (isSuccess) {
            try {
                isSuccess = false;
                FastBuffer fastBuffer = fastBufferManager.allocateBuffer(size);
                counterSemaphore.decCounter(sectorCount);
                meterAllocateFastBuffer.mark(sectorCount);
                isSuccess = true;
                return fastBuffer;
            } finally {
                if (!isSuccess) {
                    logger.debug("fail to allocate, size={}, count={}, available={}", size, sectorCount,
                            semaphore.availablePermits());
                    meterAllocationBufferFailure.mark();
                    semaphore.release(sectorCount);
                }
            }
        } else {
            meterAllocationBufferTimeout.mark();
            logger.debug("Timeout for {} ms to allocate buffer with size {}, count={}, available={}", timeoutMS, size,
                    sectorCount, semaphore.availablePermits());
            throw new NoAvailableBufferException();
        }
    }

    public void releaseBuffer(FastBuffer retbuf) {
        long size = retbuf.size();
        fastBufferManager.releaseBuffer(retbuf);
        int allocateCount = (int) (size >> SECTOR_BITS);
        semaphore.release(allocateCount);
        counterSemaphore.incCounter(allocateCount);
        meterReleaseFastBuffer.mark(allocateCount);
    }

    public void close() {
        fastBufferManager.close();
    }
}
