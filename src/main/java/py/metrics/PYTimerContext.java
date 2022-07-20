package py.metrics;

public interface PYTimerContext extends AutoCloseable {
    long stop();

    default void close() {
        stop();
    }

}
