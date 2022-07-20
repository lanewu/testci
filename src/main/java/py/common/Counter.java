package py.common;

public interface Counter {

    void increment(int n);

    default void increment() {
        increment(1);
    }

    default void decrement() {
        decrement(1);
    }

    default void decrement(int n) {
        increment(-n);
    }

    Counter NullCounter = n -> {
    };

}
