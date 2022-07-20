package py.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.Counter;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * <p>An implementation of {@link ConsumerService} with several working threads dealing with all queued elements
 *
 * @param <E> the type of element
 */
public abstract class AbstractMultiThreadConsumerService<E> implements ConsumerService<E> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractMultiThreadConsumerService.class);

    /**
     * The consumer
     */
    private final Consumer<? super E> consumer;

    /**
     * The worker thread
     */
    private final Thread[] workers;

    /**
     * The name, also used as the thread's name
     */
    private final String name;

    private final int threadCount;

    /**
     * The stop signal
     */
    private volatile boolean isStopped = true;

    private Counter counter = Counter.NullCounter;

    protected AbstractMultiThreadConsumerService(int threadCount, Consumer<? super E> consumer, String name) {
        this.consumer = consumer;
        this.name = name;
        this.workers = new Thread[threadCount];
        this.threadCount = threadCount;
    }

    @Override
    public synchronized void start() {
        if (!isStopped) {
            logger.warn("already started, no need to start again {}", name);
        } else {
            isStopped = false;
            for (int i = 0; i < threadCount; i++) {
                workers[i] = new Thread(this::work, name + "-" + i);
            }
            for (int i = 0; i < threadCount; i++) {
                workers[i].start();
            }
        }
    }

    @Override
    public synchronized void stop() {
        if (isStopped) {
            logger.warn("already stopped or not started, no need to stop {}", name);
        } else {
            isStopped = true;
            try {
                for (Thread thread : workers) {
                    thread.join();
                }
            } catch (InterruptedException e) {
                logger.warn("interrupted before stopped", e);
            }
        }
    }

    public void setCounter(Counter counter) {
        this.counter = counter;
    }

    private void work() {
        try {
            internalWork();
        } catch (Throwable t) {
            logger.error("caught an throwable, exit!", t);
        }
    }

    private void internalWork() throws InterruptedException {
        while (true) {
            if (!isStopped) {
                // no drainTo should be used to ensure the elements' ordering in queue
                E element = pollElement(1, TimeUnit.SECONDS);
                if (element == null) {
                    continue;
                }
                counter.decrement();
                consumer.accept(element);
            } else {
                logger.warn("got a stop signal, deal with the last {} elements", size());
                E element;
                while ((element = pollElement()) != null) {
                    counter.decrement();
                    consumer.accept(element);
                }
                break;
            }
        }
    }

    @Override
    public boolean submit(E element) {
        if (isStopped) {
            return false;
        } else if (enqueue(element)) {
            counter.increment();
            return !isStopped;
        }

        return false;
    }

    public abstract int size();

    protected abstract boolean enqueue(E element);

    protected abstract E pollElement();

    protected abstract E pollElement(int time, TimeUnit timeUnit) throws InterruptedException;

}
