package py.common;

import java.util.concurrent.atomic.AtomicLong;

public class FastRequestIdBuilder {
    private final static AtomicLong idGenerator = new AtomicLong();
    public static long get() {
        return idGenerator.getAndIncrement();
    }
}
