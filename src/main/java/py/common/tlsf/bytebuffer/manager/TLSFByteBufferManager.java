package py.common.tlsf.bytebuffer.manager;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import py.common.DirectAlignedBufferAllocator;
import py.common.tlsf.BaseTLSFSpaceManager;
import py.common.tlsf.OutOfSpaceException;
import py.common.tlsf.SimpleTLSFMetadata;
import py.metrics.PYMetric;
import py.metrics.PYMetricRegistry;
import py.metrics.PYNullMetric;
import py.metrics.PYTimerContext;

/**
 * The instance of this class manages space in a direct byte buffer. It allocates space as a direct byte buffer and
 * releases unused direct byte buffer.
 * 
 * @author zjm
 *
 */
public class TLSFByteBufferManager {
    private static final Logger logger = LoggerFactory.getLogger(TLSFByteBufferManager.class);

    private final BaseTLSFSpaceManager baseTLSFSpaceManager;

    private final ByteBuffer buffer;

    private final long basePhysicalAddr;

    private BlockingQueue<Semaphore> releasingEventListeners = new LinkedBlockingQueue<>();

    private PYMetric meterAllocateAlignedBuffer = new PYNullMetric();

    private PYMetric timerAllocateAlignedBuffer = new PYNullMetric();

    private PYMetric usedAllocateAlignedBuffer = new PYNullMetric();

    private PYMetric restAllocateAlignedBuffer = new PYNullMetric();

    /**
     * Constructor for this class.
     * 
     * @param sizeAlignment
     *            alignment of each allocated size
     * @param size
     *            the total size of direct byte buffer in the manager
     * @param addressAligned
     *            a flag represents if the direct byte buffer in manager is address-aligned
     */
    public TLSFByteBufferManager(int sizeAlignment, int size, boolean addressAligned) {
        this.buffer = (addressAligned) ? DirectAlignedBufferAllocator.allocateAlignedByteBuffer(size)
                : ByteBuffer.allocateDirect(size);
        this.basePhysicalAddr = DirectAlignedBufferAllocator.getAddress(buffer);
        this.baseTLSFSpaceManager = new BaseTLSFSpaceManager(new SimpleTLSFMetadata(), new ByteBufferDivisionMetadata(),
                sizeAlignment, 0l, size);
    }

    public void initAllocateAlignedBufferMetric(int size) {
        PYMetricRegistry metricRegistry = PYMetricRegistry.getMetricRegistry();
        meterAllocateAlignedBuffer = metricRegistry.register(
                MetricRegistry.name(TLSFByteBufferManager.class.getSimpleName(), "meter_allocate_aligned_buffer"),
                Meter.class);
        timerAllocateAlignedBuffer = metricRegistry.register(
                MetricRegistry.name(TLSFByteBufferManager.class.getSimpleName(), "timer_allocate_aligned_buffer"),
                Timer.class);
        usedAllocateAlignedBuffer = metricRegistry.register(
                MetricRegistry.name(TLSFByteBufferManager.class.getSimpleName(), "used_allocate_aligned_buffer"),
                Counter.class);
        restAllocateAlignedBuffer = metricRegistry.register(
                MetricRegistry.name(TLSFByteBufferManager.class.getSimpleName(), "rest_allocate_aligned_buffer"),
                Counter.class);
        restAllocateAlignedBuffer.incCounter(size);
    }

    /**
     * Allocate direct byte buffer with size largger than or equal to the given size. If the size is aligned, then the
     * size of returned buffer will be equal to the desired size. Otherwise, the size of returned buffer will be
     * largger.
     * 
     * @param size
     *            desired size
     * @return direct byte buffer with size largger than or equal to the given size
     * @throws OutOfSpaceException
     *             if no more direct byte buffer can be allocated to fit the given size
     * @throws IllegalArgumentException
     *             if size is less than or equal to zero
     */
    public synchronized ByteBuffer allocate(int size) throws OutOfSpaceException, IllegalArgumentException {
        if (size <= 0) {
            logger.error("Illegal size {}! Size should be positive.", size);
            throw new IllegalArgumentException("Invaid size: " + size);
        }

        int address = (int) baseTLSFSpaceManager.allocate(size);

        buffer.clear();
        buffer.position(address);
        buffer.limit((int) (address + baseTLSFSpaceManager.getDivisionMetadata().getAccessibleMemSize(address)));
        return buffer.slice();
    }

    /**
     * Allocate direct byte buffer with size largger than or equal to the given size until successfully.
     * 
     * @param size
     *            desired size
     * @return direct byte buffer with size largger than or equal to the given size
     * @throws IllegalArgumentException
     *             if size is less than or equal to zero
     */
    public ByteBuffer blockingAllocate(int size) throws IllegalArgumentException {
        Semaphore semaphore = null;
        PYTimerContext timeAllocateBuffer = null;
        try {
            timeAllocateBuffer = timerAllocateAlignedBuffer.time();
            while (true) {
                synchronized (this) {
                    try {
                        return allocate(size);
                    } catch (OutOfSpaceException e) {
                        if (semaphore == null) {
                            semaphore = new Semaphore(0);
                        }
                        releasingEventListeners.offer(semaphore);
                    }
                }

                semaphore.acquireUninterruptibly();
            }
        } finally {
            meterAllocateAlignedBuffer.mark();
            timeAllocateBuffer.stop();
            usedAllocateAlignedBuffer.incCounter(size);
            restAllocateAlignedBuffer.decCounter(size);
        }
    }

    /**
     * Release the allocated buffer to the manager.
     * 
     * @param buffer
     *            buffer which was allocated from manager before.
     * @throws IllegalArgumentException
     *             if the buffer was not allocated from the manager before
     */
    public synchronized void release(ByteBuffer buffer) throws IllegalArgumentException {
        if (!buffer.isDirect()) {
            logger.error("Illegal byte buffer! Expect a direct byte buffer, but it was not!");
            throw new IllegalArgumentException();
        }

        long physicalAddr = DirectAlignedBufferAllocator.getAddress(buffer);

        int address = (int) (physicalAddr - basePhysicalAddr);
        if (address < 0 || address >= this.buffer.capacity()) {
            logger.error("Illegal byte buffer! Address of it {} is out of bound.", address);
            throw new IllegalArgumentException("Invalid buffer");
        }

        long size = buffer.capacity();
        usedAllocateAlignedBuffer.decCounter(size);
        restAllocateAlignedBuffer.incCounter(size);

        baseTLSFSpaceManager.release(address);

        while (releasingEventListeners.peek() != null) {
            Semaphore listener = releasingEventListeners.poll();
            listener.release();
        }
    }

    /**
     * Allocate multiple direct byte buffers to fit the given desired size. If no more direct byte buffer in manager
     * with size equal to or larger than the desired size, then manager will find buffers with small size to fit the
     * desired size.
     * 
     * @param size
     *            desired size
     * @return a list of multiple direct byte buffers to fit the given desired size.
     * @throws OutOfSpaceException
     *             if all remaining buffers cannot fit the desired size
     * @throws IllegalArgumentException
     *             if size is less than or equal to zero
     */
    public synchronized List<ByteBuffer> tryAllocate(int size) throws OutOfSpaceException, IllegalArgumentException {
        if (size <= 0) {
            logger.error("Illegal size {}! Size should be positive.", size);
            throw new IllegalArgumentException("Invaid size: " + size);
        }

        List<Integer> bufferAddrList = new ArrayList<>();

        long remainingSize = size;
        while (remainingSize > 0) {
            int address;
            try {
                address = (int) baseTLSFSpaceManager.tryAllocate(size);
                bufferAddrList.add(address);
            } catch (OutOfSpaceException e) {
                for (int bufferAddr : bufferAddrList) {
                    baseTLSFSpaceManager.release(bufferAddr);
                }
                throw e;
            }

            remainingSize -= baseTLSFSpaceManager.getDivisionMetadata().getAccessibleMemSize(address);
        }

        List<ByteBuffer> bufferList = new ArrayList<>();
        for (int bufferAddr : bufferAddrList) {
            buffer.clear();
            buffer.position(bufferAddr);
            buffer.limit(
                    (int) (bufferAddr + baseTLSFSpaceManager.getDivisionMetadata().getAccessibleMemSize(bufferAddr)));
            bufferList.add(buffer.slice());
        }

        return bufferList;
    }

    /**
     * Allocate multiple direct byte buffers to fit the given desired size until successfully. If no more direct byte
     * buffer in manager with size equal to or larger than the desired size, then manager will find buffers with small
     * size to fit the desired size.
     * 
     * @param size
     *            desired size
     * @return a list of multiple direct byte buffers to fit the given desired size.
     * @throws IllegalArgumentException
     *             if size is less than or equal to zero
     */
    public List<ByteBuffer> blockingTryAllocate(int size) throws IllegalArgumentException {
        Semaphore semaphore = null;
        while (true) {
            try {
                return tryAllocate(size);
            } catch (OutOfSpaceException e) {
                if (semaphore == null) {
                    semaphore = new Semaphore(0);
                }
                releasingEventListeners.offer(semaphore);
                semaphore.acquireUninterruptibly();
            }
        }
    }
}
