package py.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Created by kobofare on 2017/4/12.
 */
public abstract class DelayedTask extends AbstractTask {
    private final Logger logger = LoggerFactory.getLogger(DelayedTask.class);

    private long delayMs;
    private long origin;

    public DelayedTask(int delayMs) {
        this.origin = System.currentTimeMillis();
        this.delayMs = delayMs;
    }

    protected long getOrigin() {
        return origin;
    }

    /**
     * Only update the delay to new value when the new one is larger than the current one
     *
     * @param newDelayMs
     */
    public void updateDelay(long newDelayMs) {
        long now = System.currentTimeMillis();
        if (now + newDelayMs >= delayMs + origin) {
            delayMs = newDelayMs;
            origin = now;
        }
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(origin + delayMs - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public final int compareTo(Delayed delayed) {
        logger.trace("this={}, delayed={}", this, delayed);
        if (delayed == null) {
            return 1;
        }

        if (delayed == this) {
            return 0;
        }

        long d = (getDelay(TimeUnit.MILLISECONDS) - delayed.getDelay(TimeUnit.MILLISECONDS));
        return ((d == 0) ? 0 : ((d < 0) ? -1 : 1));
    }

    @Override
    public abstract Result work();

    @Override
    public String toString() {
        return "DelayedTask{" + "origin=" + origin + ", delayMs=" + delayMs + '}';
    }
}
