package py.consumer;

import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

public class SingleThreadConsumerService<E> extends MultiThreadConsumerServiceWithBlockingQueue<E> {
    public SingleThreadConsumerService(Consumer<? super E> consumer, BlockingQueue<E> elementQueue, String name) {
        super(1, consumer, elementQueue, name);
    }
}
