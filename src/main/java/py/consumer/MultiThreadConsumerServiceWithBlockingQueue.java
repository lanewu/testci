package py.consumer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * <p>The consuming order will be exactly the same as the elements order in the given {@link BlockingQueue}.
 * No {@link BlockingQueue#drainTo} is used to guarantee the ordering.
 *
 * @param <E> the type of element
 */
public class MultiThreadConsumerServiceWithBlockingQueue<E> extends AbstractMultiThreadConsumerService<E> {

    /**
     * Element queue
     */
    private final BlockingQueue<E> elementQueue;

    public MultiThreadConsumerServiceWithBlockingQueue(int threadCount, Consumer<? super E> consumer,
            BlockingQueue<E> elementQueue, String name) {
        super(threadCount, consumer, name);
        this.elementQueue = elementQueue;
    }

    @Override
    public int size() {
        return elementQueue.size();
    }

    @Override
    protected boolean enqueue(E element) {
        return elementQueue.offer(element);
    }

    @Override
    protected E pollElement() {
        return elementQueue.poll();
    }

    @Override
    protected E pollElement(int time, TimeUnit timeUnit) throws InterruptedException {
        return elementQueue.poll(time, timeUnit);
    }
}
