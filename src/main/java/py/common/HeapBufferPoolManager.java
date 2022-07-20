package py.common;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

import py.metrics.PYMetric;
import py.metrics.PYMetricRegistry;

public class HeapBufferPoolManager {
    private final static Logger logger = LoggerFactory.getLogger(HeapBufferPoolManager.class);

    private final BlockingQueue<ByteBuffer> bufferQueue;
    private final static HeapBufferPoolManager bufferPoolManager = new HeapBufferPoolManager();
    private PYMetric counterBufferQueueLength;
    private PYMetric meterBufferNotEnough;

    private HeapBufferPoolManager() {
        this.bufferQueue = new LinkedBlockingQueue<ByteBuffer>();
        PYMetricRegistry metricRegistry = PYMetricRegistry.getMetricRegistry();
        counterBufferQueueLength = metricRegistry.register(
                MetricRegistry.name(HeapBufferPoolManager.class.getSimpleName(), "counter_buffer_queue_length"),
                Counter.class);
        meterBufferNotEnough = metricRegistry.register(
                MetricRegistry.name(HeapBufferPoolManager.class.getSimpleName(), "meter_buffer_not_enough"),
                Meter.class);
    }

    public static HeapBufferPoolManager getInstance() {
        return bufferPoolManager;
    }

    public void init(int pageCount, int pageSize) {
        for (int i = 0; i < pageCount; i++) {
            release(ByteBuffer.wrap(new byte[pageSize]));
        }

        logger.warn("heap pool buffer: {}, pageSize: {}", bufferQueue.size(), pageSize);
    }

    public ByteBuffer allocate() {
        ByteBuffer byteBuffer = bufferQueue.poll();
        if (byteBuffer != null) {
            counterBufferQueueLength.decCounter();
        } else {
            meterBufferNotEnough.mark();
        }

        return byteBuffer;
    }

    public void release(ByteBuffer byteBuffer) {
        if (!bufferQueue.offer(byteBuffer)) {
            logger.warn("oh, my god, i can add the page back, i will lost a page, but the total count is limited, so you can imagine the result");
        } else {
            counterBufferQueueLength.incCounter();
        }
    }

    public int size() {
        return bufferQueue.size();
    }
}
