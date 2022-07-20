package py.common.reservoir;

import com.codahale.metrics.Clock;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A {@link Reservoir} implementation backed by a sliding window that stores only the measurements made
 * in the last {@code N} seconds (or other time unit).
 *
 * Copied from {@link com.codahale.metrics.SlidingTimeWindowReservoir}
 */
public class SlidingTimeWindowReservoir implements Reservoir<Long> {

    // allow for this many duplicate ticks before overwriting measurements
    private static final int COLLISION_BUFFER = 256;
    // only trim on updating once every N
    private static final int TRIM_THRESHOLD = 256;

    private final Clock clock;
    private final ConcurrentSkipListMap<Long, Long> measurements;
    private final long window;
    private final AtomicLong lastTick;
    private final AtomicLong count;

    /**
     * Creates a new {@link SlidingTimeWindowReservoir} with the given window of time.
     *
     * @param window     the window of time
     * @param windowUnit the unit of {@code window}
     */
    public SlidingTimeWindowReservoir(long window, TimeUnit windowUnit) {
        this(window, windowUnit, Clock.defaultClock());
    }

    /**
     * Creates a new {@link SlidingTimeWindowReservoir} with the given clock and window of time.
     *
     * @param window     the window of time
     * @param windowUnit the unit of {@code window}
     * @param clock      the {@link Clock} to use
     */
    public SlidingTimeWindowReservoir(long window, TimeUnit windowUnit, Clock clock) {
        this.clock = clock;
        this.measurements = new ConcurrentSkipListMap<>();
        this.window = windowUnit.toNanos(window) * COLLISION_BUFFER;
        this.lastTick = new AtomicLong();
        this.count = new AtomicLong();
    }

    @Override
    public int size() {
        trim();
        return measurements.size();
    }

    @Override
    public void update(Long value) {
        if (count.incrementAndGet() % TRIM_THRESHOLD == 0) {
            trim();
        }
        measurements.put(getTick(), value);
    }

    @Override
    public Long[] getSnapshot() {
        trim();
        final Object[] copy = measurements.values().toArray();
        Long[] values = new Long[copy.length];
        for (int i = 0; i < copy.length; i++) {
            values[i] = (Long) copy[i];
        }
        return values;
    }

    private long getTick() {
        for (; ; ) {
            final long oldTick = lastTick.get();
            final long tick = clock.getTick() * COLLISION_BUFFER;
            // ensure the tick is strictly incrementing even if there are duplicate ticks
            long newTick = tick > oldTick ? tick : oldTick + 1;
            if (lastTick.compareAndSet(oldTick, newTick)) {
                return newTick;
            }
        }
    }

    private void trim() {
        measurements.headMap(getTick() - window).clear();
    }
}
