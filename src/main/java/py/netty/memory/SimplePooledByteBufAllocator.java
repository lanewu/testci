package py.netty.memory;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.netty.buffer.*;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import py.common.DirectAlignedBufferAllocator;
import py.common.tlsf.bytebuffer.manager.TLSFByteBufferManager;
import py.common.tlsf.bytebuffer.manager.TLSFByteBufferManagerFactory;
import py.metrics.PYMetric;
import py.metrics.PYMetricRegistry;

/**
 * manager the direct buffer for network decode and encode.<br>
 * you can create a {@link PooledByteBufAllocator}, which capacity can not exceed 4G<br>
 *
 * @author lx
 */
public class SimplePooledByteBufAllocator extends AbstractByteBufAllocator {
    private final static Logger logger = LoggerFactory.getLogger(SimplePooledByteBufAllocator.class);
    private static final int DEFAULT_POOL_SIZE = 256 * 1024 * 1024;

    private static final int DEFAULT_MEDIUM_PAGE_SIZE = 8 * 1024;
    private static final int DEFAULT_LITTLE_PAGE_SIZE = 1024;
    private static final int DEFAULT_LARGE_PAGE_SIZE = 128 * 1024;

    public final ConcurrentLinkedQueue<ByteBuf> littleQueue = new ConcurrentLinkedQueue<ByteBuf>();
    public final ConcurrentLinkedQueue<ByteBuf> mediumQueue = new ConcurrentLinkedQueue<ByteBuf>();
    public final ConcurrentLinkedQueue<ByteBuf> largeQueue = new ConcurrentLinkedQueue<ByteBuf>();

    public final ByteBuffer directBuffer;
    public final UnpooledByteBufAllocator allocator = new UnpooledByteBufAllocator(false);

    public final int mediumPageSize;
    public int mediumPageCount;
    public final int mediumMaxOrder;

    public final int littlePageSize;
    public int littlePageCount;
    public final int littleMaxOrder;

    public final int largePageSize;
    public int largePageCount;
    public final int largeMaxOrder;

    private PYMetric counterMediumQueueLength;
    private PYMetric counterLittleQueueLength;
    private PYMetric counterLargeQueueLength;

    private PYMetric meterNoEnoughMediumPage;
    private PYMetric meterNoEnoughLittlePage;
    private PYMetric meterNoEnoughLargePage;

    protected PYMetric meterAllocateMediumPage;
    protected PYMetric meterAllocateLittlePage;
    protected PYMetric meterAllocateLargePage;

    protected PYMetric histoAllocateMediumPageSize;
    protected PYMetric histoAllocateLittlePageSize;
    protected PYMetric histoAllocateLargePageSize;
    private String prefix;
    private boolean align512;

    public SimplePooledByteBufAllocator(String prefix) {
        this(DEFAULT_POOL_SIZE, DEFAULT_MEDIUM_PAGE_SIZE, DEFAULT_LITTLE_PAGE_SIZE, DEFAULT_LARGE_PAGE_SIZE, prefix);
    }

    public SimplePooledByteBufAllocator() {
        this(DEFAULT_POOL_SIZE, DEFAULT_MEDIUM_PAGE_SIZE, DEFAULT_LITTLE_PAGE_SIZE, DEFAULT_LARGE_PAGE_SIZE, "network");
    }

    public SimplePooledByteBufAllocator(int poolSize, int mediumPageSize) {
        this(poolSize, mediumPageSize, DEFAULT_LITTLE_PAGE_SIZE, DEFAULT_LARGE_PAGE_SIZE, "network");
    }

    public SimplePooledByteBufAllocator(int poolSize, int pageMediumSize, int littlePageSize, int largePageSize) {
        this(poolSize, pageMediumSize, littlePageSize, largePageSize, "network");
    }

    public SimplePooledByteBufAllocator(int poolSize, int pageMediumSize, int littlePageSize, int largePageSize,
            String prefix) {
        super(true);
        this.prefix = prefix;
        // shutdown the leak detector.
        ResourceLeakDetector.setLevel(Level.DISABLED);

        if (pageMediumSize <= littlePageSize) {
            throw new IllegalArgumentException("pageSize: " + pageMediumSize + " (expected: " + littlePageSize + "+)");
        }

        if (pageMediumSize >= largePageSize) {
            throw new IllegalArgumentException("pageSize: " + pageMediumSize + " (expected: " + largePageSize + "-)");
        }

        this.mediumMaxOrder = validateAndCalculatePageShifts(pageMediumSize);
        this.mediumPageSize = pageMediumSize;

        this.littleMaxOrder = validateAndCalculatePageShifts(littlePageSize);
        this.littlePageSize = littlePageSize;

        this.largeMaxOrder = validateAndCalculatePageShifts(largePageSize);
        this.largePageSize = largePageSize;

        // allocate buffer
        this.largePageCount = poolSize >> largeMaxOrder;
        Validate.isTrue(this.largePageCount > 0);
        poolSize = this.largePageCount * largePageSize;
        directBuffer = allocateInitialize(poolSize);

        // create big queue for allocating.
        int position = 0;
        byte[] zeroBuffer = new byte[largePageSize];
        for (int i = 0; i < this.largePageCount; i++) {
            directBuffer.position(position).limit(position + largePageSize);
            largeQueue.offer(new PyPooledDirectByteBuf(this, directBuffer.slice().put(zeroBuffer)));
            position += largePageSize;
        }

        // create medium queue for allocating.
        int fromBigCount = (this.largePageCount << mediumMaxOrder) >> largeMaxOrder;
        int mediumPageTimesOfLargePage = (1 << largeMaxOrder) >> mediumMaxOrder;
        this.largePageCount -= fromBigCount;

        while (fromBigCount-- > 0) {
            ByteBuffer buffer = largeQueue.poll().nioBuffer();
            position = 0;
            for (int i = 0; i < mediumPageTimesOfLargePage; i++) {
                buffer.position(position);
                buffer.limit(position + mediumPageSize);
                mediumQueue.offer(new PyPooledDirectByteBuf(this, buffer.slice()));
                position += mediumPageSize;
                this.mediumPageCount++;
            }
        }

        int fromMediumCount = (this.mediumPageCount << littleMaxOrder) >> mediumMaxOrder;
        int littlePageTimesOfMediumPage = (1 << mediumMaxOrder) >> littleMaxOrder;
        this.mediumPageCount -= fromMediumCount;

        while (fromMediumCount-- > 0) {
            ByteBuffer buffer = mediumQueue.poll().nioBuffer();
            position = 0;
            for (int i = 0; i < littlePageTimesOfMediumPage; i++) {
                buffer.position(position);
                buffer.limit(position + littlePageSize);
                littleQueue.offer(new PyPooledDirectByteBuf(this, buffer.slice()));
                position += littlePageSize;
                this.littlePageCount++;
            }
        }

        logger.warn(
                "pool size: {}, big page size: {}. big page count: {}, medium page size: {}, medium page count: {}, little page size: {}, little page count: {}",
                poolSize, this.largePageSize, this.largePageCount, this.mediumPageSize, this.mediumPageCount,
                this.littlePageSize, this.littlePageCount);
        initMetric();
    }

    protected ByteBuffer allocateInitialize(int poolSize) {
        return ByteBuffer.allocateDirect(poolSize);
    }

    protected ByteBuf allocateFromExternal(int initialCapacity, int maxCapacity) {
        return allocator.buffer(initialCapacity, maxCapacity);
    }

    private void initMetric() {
        PYMetricRegistry metricRegistry = PYMetricRegistry.getMetricRegistry();
        counterMediumQueueLength = metricRegistry.register(MetricRegistry
                        .name(SimplePooledByteBufAllocator.class.getSimpleName(), prefix, "counter_medium_queue_length"),
                Counter.class);
        counterLittleQueueLength = metricRegistry.register(MetricRegistry
                        .name(SimplePooledByteBufAllocator.class.getSimpleName(), prefix, "counter_Little_queue_length"),
                Counter.class);
        counterLargeQueueLength = metricRegistry.register(MetricRegistry
                        .name(SimplePooledByteBufAllocator.class.getSimpleName(), prefix, "counter_Large_queue_length"),
                Counter.class);
        meterNoEnoughMediumPage = metricRegistry.register(MetricRegistry
                        .name(SimplePooledByteBufAllocator.class.getSimpleName(), prefix, "meter_no_enough_medium_page"),
                Meter.class);
        meterNoEnoughLittlePage = metricRegistry.register(MetricRegistry
                        .name(SimplePooledByteBufAllocator.class.getSimpleName(), prefix, "meter_no_enough_little_page"),
                Meter.class);
        meterNoEnoughLargePage = metricRegistry.register(MetricRegistry
                        .name(SimplePooledByteBufAllocator.class.getSimpleName(), prefix, "meter_no_enough_large_page"),
                Meter.class);
        meterAllocateMediumPage = metricRegistry.register(MetricRegistry
                        .name(SimplePooledByteBufAllocator.class.getSimpleName(), prefix, "meter_allocate_medium_page"),
                Meter.class);
        meterAllocateLittlePage = metricRegistry.register(MetricRegistry
                        .name(SimplePooledByteBufAllocator.class.getSimpleName(), prefix, "meter_allocate_little_page"),
                Meter.class);
        meterAllocateLargePage = metricRegistry.register(MetricRegistry
                        .name(SimplePooledByteBufAllocator.class.getSimpleName(), prefix, "meter_allocate_large_page"),
                Meter.class);
        histoAllocateMediumPageSize = metricRegistry.register(MetricRegistry
                        .name(SimplePooledByteBufAllocator.class.getSimpleName(), prefix, "histo_allocate_medium_page_size"),
                Histogram.class);
        histoAllocateLittlePageSize = metricRegistry.register(MetricRegistry
                        .name(SimplePooledByteBufAllocator.class.getSimpleName(), prefix, "histo_allocate_little_page_size"),
                Histogram.class);
        histoAllocateLargePageSize = metricRegistry.register(MetricRegistry
                        .name(SimplePooledByteBufAllocator.class.getSimpleName(), prefix, "histo_allocate_large_page_size"),
                Histogram.class);
        counterMediumQueueLength.incCounter(this.mediumPageCount);
        counterLittleQueueLength.incCounter(this.littlePageCount);
        counterLargeQueueLength.incCounter(this.largePageCount);
    }

    private static int validateAndCalculatePageShifts(int pageSize) {
        if ((pageSize & pageSize - 1) != 0) {
            throw new IllegalArgumentException("pageSize: " + pageSize + " (expected: power of 2)");
        }

        // Logarithm base 2. At this point we know that pageSize is a power of two.
        return Integer.SIZE - 1 - Integer.numberOfLeadingZeros(pageSize);
    }

    @Override
    public boolean isDirectBufferPooled() {
        return true;
    }

    public void release(ByteBuf byteBuf) {

        Validate.isTrue(byteBuf instanceof PyPooledDirectByteBuf);
        int capacity = byteBuf.capacity();
        if (capacity < this.mediumPageSize) {
            //logger.trace("@@ release buffer from little: {}", byteBuf);
            Validate.isTrue(littleQueue.offer(byteBuf));
            counterLittleQueueLength.incCounter();
        } else if (capacity < this.largePageSize) {
            //logger.trace("@@ release buffer from medium: {}", byteBuf);
            Validate.isTrue(mediumQueue.offer(byteBuf));
            counterMediumQueueLength.incCounter();
        } else {
            //logger.trace("@@ release buffer from big: {}", byteBuf);
            Validate.isTrue(largeQueue.offer(byteBuf));
            counterLargeQueueLength.incCounter();
        }
    }

    @Override
    protected ByteBuf newHeapBuffer(int initialCapacity, int maxCapacity) {
        throw new UnsupportedOperationException("simple allocator, just for direct buffer");
    }

    @Override
    public ByteBuf ioBuffer() {
        throw new UnsupportedOperationException("simple allocator");
    }

    @Override
    public ByteBuf ioBuffer(int initialCapacity) {
        if (initialCapacity == 0) {
            logger.warn("why need empty bytebuf", new Exception());
        }

        ByteBuf buf = ioBuffer(initialCapacity, initialCapacity);
        Validate.isTrue(buf.writableBytes() >= initialCapacity);
        return buf;
    }

    @Override
    public ByteBuf directBuffer() {
        throw new UnsupportedOperationException("simple allocator");
    }

    @Override
    public ByteBuf directBuffer(int initialCapacity) {
        return directBuffer(initialCapacity, initialCapacity);
    }

    @Override
    protected ByteBuf newDirectBuffer(int initialCapacity, int maxCapacity) {
        Validate.isTrue(maxCapacity > 0);
        if (maxCapacity < this.mediumPageSize) {
            // histoAllocateLittlePageSize.updateHistogram(maxCapacity);
            meterAllocateLittlePage.mark();
            //logger.trace("allocate from little queue, size: {}, {}", initialCapacity, maxCapacity);
            // allocate from little page queue for reducing the wast of the memory and supporting more memory
            // allocation.
            return allocateFromLittleQueue(initialCapacity, maxCapacity);
        } else if (maxCapacity < this.largePageSize) {
            //logger.trace("allocate from big queue, size: {}, {}", initialCapacity, maxCapacity);
            meterAllocateMediumPage.mark();
            // histoAllocateMediumPageSize.updateHistogram(maxCapacity);
            return allocateFromMediumQueue(initialCapacity, maxCapacity);
        } else {
            meterAllocateLargePage.mark();
            // histoAllocateLargePageSize.updateHistogram(maxCapacity);
            return allocateFromLargeQueue(initialCapacity, maxCapacity);
        }

    }

    protected ByteBuf allocateFromLittleQueue(int initialCapacity, int maxCapacity) {
        int pageCount = (maxCapacity + littlePageSize - 1) >> littleMaxOrder;
        if (pageCount == 1) {
            ByteBuf buf = littleQueue.poll();
            if (buf == null) {
                meterNoEnoughLittlePage.mark();
                return allocateFromExternal(initialCapacity, maxCapacity);
            } else {
                //logger.trace("@@ allocate buffer from little: {}", buf);
                counterLittleQueueLength.decCounter();
                return buf;
            }
        }

        ByteBuf[] buffers = new ByteBuf[pageCount];
        int i = 0;
        for (; i < pageCount; i++) {
            buffers[i] = littleQueue.poll();
            if (buffers[i] == null) {
                break;
            }
            //logger.trace("@@ allocate buffer from little: {}", buffers[i]);
            buffers[i].writerIndex(buffers[i].capacity());
        }

        if (i == pageCount) {
            // allocate buffer from cache successfully.
            counterLittleQueueLength.decCounter(pageCount);
            return new PyCompositeByteBuf(this, true, pageCount, buffers).writerIndex(0);
            // return new CompositeByteBuf(this, true, pageCount, buffers).writerIndex(0);
        } else {
            // there is no enough space for max capacity.
            for (int j = 0; j < i; j++) {
                Validate.isTrue(littleQueue.offer(buffers[j].clear()));
            }
            meterNoEnoughLittlePage.mark();
            return allocateFromExternal(initialCapacity, maxCapacity);
        }
    }

    protected ByteBuf allocateFromMediumQueue(int initialCapacity, int maxCapacity) {
        int pageCount = (maxCapacity + mediumPageSize - 1) >> mediumMaxOrder;
        if (pageCount == 1) {
            ByteBuf buf = mediumQueue.poll();
            if (buf == null) {
                meterNoEnoughMediumPage.mark();
                return allocateFromExternal(initialCapacity, maxCapacity);
            } else {
                //   logger.trace("@@ allocate buffer from medium: {}", buf);
                counterMediumQueueLength.decCounter();
                return buf;
            }
        }

        ByteBuf[] buffers = new ByteBuf[pageCount];
        int i = 0;
        for (; i < pageCount; i++) {
            buffers[i] = mediumQueue.poll();
            if (buffers[i] == null) {
                break;
            }
            // logger.trace("@@ allocate buffer from medium: {}", buffers[i]);
            buffers[i].writerIndex(buffers[i].capacity());
        }

        if (i == pageCount) {
            // allocate buffer from cache successfully.
            counterMediumQueueLength.decCounter(pageCount);
            return new PyCompositeByteBuf(this, true, pageCount, buffers).writerIndex(0);
            // return new CompositeByteBuf(this, true, pageCount, buffers).writerIndex(0);
        } else {
            // there is no enough space for max capacity.
            for (int j = 0; j < i; j++) {
                Validate.isTrue(mediumQueue.offer(buffers[j].clear()));
            }
            meterNoEnoughMediumPage.mark();
            return allocateFromExternal(initialCapacity, maxCapacity);
        }
    }

    protected ByteBuf allocateFromLargeQueue(int initialCapacity, int maxCapacity) {
        int pageCount = (maxCapacity + largePageSize - 1) >> largeMaxOrder;
        if (pageCount == 1) {
            ByteBuf buf = largeQueue.poll();
            if (buf == null) {
                meterNoEnoughLargePage.mark();
                return allocateFromExternal(initialCapacity, maxCapacity);
            } else {
                // logger.trace("@@ allocate buffer from big: {}", buf);
                counterLargeQueueLength.decCounter();
                return buf;
            }
        }

        ByteBuf[] buffers = new ByteBuf[pageCount];
        int i = 0;
        for (; i < pageCount; i++) {
            buffers[i] = largeQueue.poll();
            if (buffers[i] == null) {
                break;
            }
            // logger.trace("@@ allocate buffer from big: {}", buffers[i]);
            buffers[i].writerIndex(buffers[i].capacity());
        }

        if (i == pageCount) {
            // allocate buffer from cache successfully.
            counterLargeQueueLength.decCounter(pageCount);
            return new PyCompositeByteBuf(this, true, pageCount, buffers).writerIndex(0);
            // return new CompositeByteBuf(this, true, pageCount, buffers).writerIndex(0);
        } else {
            // there is no enough space for max capacity.
            for (int j = 0; j < i; j++) {
                Validate.isTrue(largeQueue.offer(buffers[j].clear()));
            }

            meterNoEnoughLargePage.mark();
            return allocateFromExternal(initialCapacity, maxCapacity);
        }
    }

    public int getTotolPageCount() {
        return mediumPageCount + littlePageCount;
    }

    public int getLittlePageCount() {
        return littlePageCount;
    }

    public int getMediumPageCount() {
        return mediumPageCount;
    }

    public int getLargePageCount() {
        return largePageCount;
    }

    /**
     * It is a insufficient interface, so don't call this method unless you must.
     *
     * @return
     */
    public int getAvailableMediumPageCount() {
        return mediumQueue.size();
    }

    /**
     * It is a insufficient interface, so don't call this method unless you must.
     *
     * @return
     */
    public int getAvailableLittlePageCount() {
        return littleQueue.size();
    }

    /**
     * It is a insufficient interface, so don't call this method unless you must.
     *
     * @return
     */
    public int getAvailableLargePageCount() {
        return largeQueue.size();
    }
}
