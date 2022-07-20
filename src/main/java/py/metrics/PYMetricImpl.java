package py.metrics;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements PYMetric interface
 * 
 * @author chenlia
 */
public class PYMetricImpl implements PYMetric {
    private static final Logger logger = LoggerFactory.getLogger(PYTimerContextImpl.class);
    private final Metric metric;

    public PYMetricImpl(Metric metric) {
        this.metric = metric;
    }

    /**
     * Increment the counter by one.
     */
    @Override
	public void incCounter() {
        if (metric instanceof Counter) {
            ((Counter) metric).inc();
        } else {
            throw new RuntimeException("metric is not counter type");
        }
    }

    /**
     * Increment the counter by {@code n}.
     * 
     * @param n
     *            the amount by which the counter will be increased
     */
    @Override
	public void incCounter(long n) {
        if (metric instanceof Counter) {
            ((Counter) metric).inc(n);
        } else {
            throw new RuntimeException("metric is not counter type");
        }
    }

    /**
     * Decrement the counter by one.
     */
    @Override
	public void decCounter() {
        if (metric instanceof Counter) {
            ((Counter) metric).dec();
        } else {
            throw new RuntimeException("metric is not counter type");
        }
    }

    /**
     * Decrement the counter by {@code n}.
     * 
     * @param n
     *            the amount by which the counter will be decreased
     */
    @Override
	public void decCounter(long n) {
        if (metric instanceof Counter) {
            ((Counter) metric).dec(n);
        } else {
            throw new RuntimeException("metric is not counter type");
        }
    }

    /**
     * Adds a recorded value.
     * 
     * @param value
     *            the length of the value
     */
    @Override
	public void updateHistogram(int value) {
        if (metric instanceof Histogram) {
            ((Histogram) metric).update(value);
        } else {
            throw new RuntimeException("metric is not histogram type");
        }
    }

    /**
     * Adds a recorded value.
     * 
     * @param value
     *            the length of the value
     */
    @Override
	public void updateHistogram(long value) {
        if (metric instanceof Histogram) {
            ((Histogram) metric).update(value);
        } else {
            throw new RuntimeException("metric is not histogram type");
        }
    }

    /**
     * A meter metric which measures mean throughput and one-, five-, and fifteen-minute exponentially-weighted moving
     * average throughputs.
     * 
     * @see EWMA
     */
    /**
     * Use as a meter to mark the occurrence of an event.
     */
    @Override
	public void mark() {
        if (metric instanceof Meter) {
            ((Meter) metric).mark();
        } else {
            throw new RuntimeException("metric is not meter type");
        }
    }

    /**
     * Use as a meter to get the number of events which have been marked.
     */
    @Override
	public long getCount() {
        if (metric instanceof Meter) {
            return ((Meter) metric).getCount();
        } else if (metric instanceof Counter) {
            return ((Counter) metric).getCount();
        } else {
            throw new RuntimeException("metric is not meter type");
        }
    }

    /**
     * Use as a meter Mark the occurrence of a given number of events.
     * 
     * @param n
     *            the number of events
     */
    @Override
	public void mark(long n) {
        if (metric instanceof Meter) {
            ((Meter) metric).mark(n);
        } else {
            throw new RuntimeException("metric is not meter type");
        }
    }

    /**
     * Returns a new {@link Context}.
     * 
     * @return a new {@link Context}
     * @see Context
     */
    @Override
	public PYTimerContext time() {
        if (metric instanceof Timer) {
            Context context = ((Timer) metric).time();
            if (context == null) {
                // Keep a validation here because this happened once, but not reproducing in the
                // following days --tyr 2018.12.18
                logger.error("got a null context !! {}", metric, metric.getClass().toString());
            }
            return new PYTimerContextImpl(context);
        } else {
            throw new RuntimeException("metric is not timer type");
        }
    }

    /**
     * Times and records the duration of event.
     * 
     * @param event
     *            a {@link Callable} whose {@link Callable#call()} method implements a process whose duration should be
     *            timed
     * @param <T>
     *            the type of the value returned by {@code event}
     * @return the value returned by {@code event}
     * @throws Exception
     *             if {@code event} throws an {@link Exception}
     */
    @Override
	public <T> T time(Callable<T> event) throws Exception {
        if (metric instanceof Timer) {
            return ((Timer) metric).time(event);
        } else {
            throw new RuntimeException("metric is not timer type");
        }
    };

    /**
     * Adds a recorded duration.
     * 
     * @param duration
     *            the length of the duration
     * @param unit
     *            the scale unit of {@code duration}
     */
    @Override
	public void update(long duration, TimeUnit unit) {
        if (metric instanceof Timer) {
            ((Timer) metric).update(duration, unit);
        } else {
            throw new RuntimeException("metric is not timer type");
        }
    }
}
