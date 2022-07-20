package py.metrics;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * This class simulate metric class and literally does nothing 
 * @author chenlia
 */
public class PYNullMetric implements PYMetric {
    public static final PYNullMetric defaultNullMetric = new PYNullMetric();

    @Override
    public void incCounter() {
    }

    @Override
    public void incCounter(long n) {
    }

    @Override
    public void decCounter() {
    }

    @Override
    public void decCounter(long n) {
    }

    @Override
    public void updateHistogram(int value) {
    }

    @Override
    public void updateHistogram(long value) {
    }

    @Override
    public void mark() {
    }

    @Override
    public void mark(long n) {
    }

    @Override
    public PYTimerContext time() {
        return PYNullTimerContext.defaultNullTimerContext;
    }

    @Override
    public <T> T time(Callable<T> event) throws Exception{
        throw new RuntimeException("This method is not implemented");
    }

    @Override
    public void update(long duration, TimeUnit unit) {
    }

    @Override
    public long getCount() {
        return 0;
    }
}
