package py.common.lock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class HashLockImpl<T> implements HashLock<T> {

    private final ConcurrentHashMap<T, AtomicInteger> locks = new ConcurrentHashMap<>();

    @Override
    public void lock(T val) throws InterruptedException {
        for (; ; ) {
            AtomicInteger waitingCount = locks.computeIfAbsent(val, v -> new AtomicInteger(0));
            synchronized (waitingCount) {
                if (waitingCount == locks.get(val)) {
                    int waiting = waitingCount.getAndIncrement();
                    if (waiting != 0) {
                        waitingCount.wait();
                    }
                    return;
                }
            }
        }
    }

    @Override
    public void unlock(T val) {
        AtomicInteger waitingCount = locks.get(val);
        synchronized (waitingCount) {
            int waiting = waitingCount.decrementAndGet();
            if (waiting != 0) {
                waitingCount.notify();
            } else {
                locks.remove(val);
            }
        }
    }

}
