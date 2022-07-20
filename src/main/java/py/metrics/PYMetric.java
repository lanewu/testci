package py.metrics;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Timer.Context;

/**
 * This class contains counter, histogram, meter, and timer functions.
 * 
 * @author chenlia
 */
public interface PYMetric {
    /**
     * Increment the counter by one.
     */
    public void incCounter();

    /**
     * Increment the counter by {@code n}.
     * 
     * @param n
     *            the amount by which the counter will be increased
     */
    public void incCounter(long n);

    /**
     * Decrement the counter by one.
     */
    public void decCounter();

    /**
     * Decrement the counter by {@code n}.
     * 
     * @param n
     *            the amount by which the counter will be decreased
     */
    public void decCounter(long n);

    /**
     * Adds a recorded value.
     * 
     * @param value
     *            the length of the value
     */
    public void updateHistogram(int value);

    /**
     * Adds a recorded value.
     * 
     * @param value
     *            the length of the value
     */
    public void updateHistogram(long value);

    /**
     * A meter metric which measures mean throughput and one-, five-, and fifteen-minute exponentially-weighted moving
     * average throughputs.
     * 
     * @see EWMA
     */
    /**
     * Use as a meter to mark the occurrence of an event.
     */
    public void mark();

    /**
     * Use as a meter to get the number of events which have been marked.
     */
    public long getCount();

    /**
     * Use as a meter Mark the occurrence of a given number of events.
     * 
     * @param n
     *            the number of events
     */
    public void mark(long n);

    /**
     * Returns a new {@link Context}.
     * 
     * @return a new {@link Context}
     * @see Context
     */
    public PYTimerContext time();

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
    public <T> T time(Callable<T> event) throws Exception;

    /**
     * Adds a recorded duration.
     * 
     * @param duration
     *            the length of the duration
     * @param unit
     *            the scale unit of {@code duration}
     */
    public void update(long duration, TimeUnit unit);
}
