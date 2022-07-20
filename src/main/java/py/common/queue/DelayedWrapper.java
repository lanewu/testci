package py.common.queue;

import javax.annotation.Nonnull;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class DelayedWrapper<E> implements Delayed {

    private final E wrappedInstance;
    private long expirationTime;

    public DelayedWrapper(E wrappedInstance, long delayMS) {
        this.wrappedInstance = wrappedInstance;
        updateDelay(delayMS);
    }

    public E getWrappedInstance() {
        return wrappedInstance;
    }

    public void updateDelay(long delayMS) {
        this.expirationTime = System.currentTimeMillis() + delayMS;
    }

    @Override
    public long getDelay(@Nonnull TimeUnit unit) {
        long delayMS = expirationTime - System.currentTimeMillis();
        return unit.convert(delayMS, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@Nonnull Delayed o) {
        return Long.compare(getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
    }
}
