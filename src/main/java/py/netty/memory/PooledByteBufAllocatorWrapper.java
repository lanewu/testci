package py.netty.memory;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.netty.buffer.*;
import io.netty.util.ResourceLeakTracker;
import io.netty.util.internal.PlatformDependent;
import py.metrics.PYMetric;
import py.metrics.PYMetricRegistry;
import py.metrics.PYTimerContext;

/**
 * This class wraps around a PooledByteBufAllocator and add metrics around allocation functions of
 * PooledByteBufAllocator's
 */
public class PooledByteBufAllocatorWrapper extends PooledByteBufAllocator {

    public final static ByteBufAllocator INSTANCE = new PooledByteBufAllocatorWrapper(
            PlatformDependent.directBufferPreferred());

    private PYMetric timerNewHeapBuffer;
    private PYMetric timerNewDirectBuffer;
    private PYMetric timerNewHeapCompositeBuffer;
    private PYMetric timerNewDirectCompositeBuffer;

    private PYMetric histoNewHeapBuffer;
    private PYMetric histoNewDirectBuffer;

    public PooledByteBufAllocatorWrapper(boolean preferDirect) {
        super(preferDirect);

        PYMetricRegistry metricRegistry = PYMetricRegistry.getMetricRegistry();
        timerNewHeapBuffer = metricRegistry
                .register(MetricRegistry.name(PooledByteBufAllocatorWrapper.class.getSimpleName(),
                        "timer_new_heap_buffer"),
                        Timer.class);
        timerNewDirectBuffer = metricRegistry
                .register(MetricRegistry.name(PooledByteBufAllocatorWrapper.class.getSimpleName(),
                        "timer_new_direct_buffer"),
                        Timer.class);
        timerNewDirectCompositeBuffer = metricRegistry
                .register(MetricRegistry.name(PooledByteBufAllocatorWrapper.class.getSimpleName(),
                        "timer_new_direct_composite buffer"),
                        Timer.class);
        timerNewHeapCompositeBuffer = metricRegistry
                .register(MetricRegistry.name(PooledByteBufAllocatorWrapper.class.getSimpleName(),
                        "timer_new_composite_heap_buffer"),
                        Timer.class);
        histoNewDirectBuffer = metricRegistry
                .register(MetricRegistry.name(PooledByteBufAllocatorWrapper.class.getSimpleName(),
                        "histo_new_direct_buffer"),
                        Histogram.class);
        histoNewHeapBuffer = metricRegistry
                .register(MetricRegistry.name(PooledByteBufAllocatorWrapper.class.getSimpleName(),
                        "histo_new_heap_buffer"),
                        Histogram.class);
    }

    @Override
    protected ByteBuf newHeapBuffer(int initialCapacity, int maxCapacity) {
        PYTimerContext context = timerNewHeapBuffer.time();
        ByteBuf byteBuf = super.newHeapBuffer(initialCapacity, maxCapacity);
        context.stop();

        histoNewHeapBuffer.updateHistogram(initialCapacity);
        return byteBuf;
//        return toLeakAwareBuffer(byteBuf);
    }

    @Override
    protected ByteBuf newDirectBuffer(int initialCapacity, int maxCapacity) {
        PYTimerContext context = timerNewDirectBuffer.time();
        ByteBuf byteBuf = super.newDirectBuffer(initialCapacity, maxCapacity);
        context.stop();

        histoNewDirectBuffer.updateHistogram(initialCapacity);
        return byteBuf;
//        return toLeakAwareBuffer(byteBuf);
    }

    @Override
    public CompositeByteBuf compositeHeapBuffer(int maxNumComponents) {
        PYTimerContext context = timerNewHeapCompositeBuffer.time();
        CompositeByteBuf byteBuf = super.compositeHeapBuffer(maxNumComponents);
        context.stop();

        return byteBuf;
 //       return toLeakAwareBuffer(byteBuf);
    }

    @Override
    public CompositeByteBuf compositeDirectBuffer(int maxNumComponents) {
        PYTimerContext context = timerNewDirectCompositeBuffer.time();
        CompositeByteBuf byteBuf = super.compositeDirectBuffer(maxNumComponents);
        context.stop();

        return byteBuf;
//        return toLeakAwareBuffer(byteBuf);
    }



/*  The following codes can be used to debug PooledByteBufAllocator and now commented out

    private final static ResourceLeakDetector<ByteBuf> resourceLeakDetector = new ResourceLeakDetector<ByteBuf>(
            ByteBuf.class, 0);
    protected static ByteBuf toLeakAwareBuffer(ByteBuf buf) {
        ResourceLeakTracker<ByteBuf> leak = resourceLeakDetector.track(buf);

        return new AdvancedLeakAwareByteBuf(buf, leak);
    }

    protected static CompositeByteBuf toLeakAwareBuffer(CompositeByteBuf buf) {
        ResourceLeakTracker<ByteBuf> leak = resourceLeakDetector.track(buf);
        return new AdvancedLeakAwareCompositeByteBuf(buf, leak);
    } */
}
