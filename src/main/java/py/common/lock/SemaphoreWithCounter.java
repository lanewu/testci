package py.common.lock;

import py.common.Counter;

import java.util.Collection;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class SemaphoreWithCounter extends Semaphore {

    private final Counter counter;

    public SemaphoreWithCounter(int permits, Counter counter) {
        super(permits);

        counter.increment(permits);
        this.counter = counter;
    }

    public SemaphoreWithCounter(int permits, boolean fair, Counter counter) {
        super(permits, fair);

        counter.increment(permits);
        this.counter = counter;
    }

    @Override
    public void acquire() throws InterruptedException {
        acquire(1);
    }

    @Override
    public void acquireUninterruptibly() {
        acquireUninterruptibly(1);
    }

    @Override
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    @Override
    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        return tryAcquire(1, timeout, unit);
    }

    @Override
    public void release() {
        release(1);
    }

    @Override
    public void acquire(int permits) throws InterruptedException {
        super.acquire(permits);
        counter.decrement(permits);
    }

    @Override
    public void acquireUninterruptibly(int permits) {
        super.acquireUninterruptibly(permits);
        counter.decrement(permits);
    }

    @Override
    public boolean tryAcquire(int permits) {
        if (super.tryAcquire(permits)) {
            counter.decrement(permits);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) throws InterruptedException {
        if (super.tryAcquire(permits, timeout, unit)) {
            counter.decrement(permits);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void release(int permits) {
        super.release(permits);
        counter.increment(permits);
    }

    @Override
    public int drainPermits() {
        int val = super.drainPermits();
        counter.decrement(val);
        return val;
    }

    @Override
    protected void reducePermits(int reduction) {
        throw new UnsupportedOperationException();
    }

}
